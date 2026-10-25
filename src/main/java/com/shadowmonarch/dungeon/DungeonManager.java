package com.shadowmonarch.dungeon;

import com.shadowmonarch.ModItems;
import com.shadowmonarch.MobDefs;
import com.shadowmonarch.data.Progression;
import com.shadowmonarch.data.Quests;
import com.shadowmonarch.data.SMData;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gates (dungeons). A gate is a sealed stone arena built high above the player in the overworld.
 * Clear every monster to win; the player is teleported back afterwards (or with /sm leave).
 */
public final class DungeonManager {
    private static final int SIZE = 29, HEIGHT = 12, Y0 = 190;
    private static final Random RNG = new Random();

    private static final Block[] WALLS = {Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.PRISMARINE_BRICKS,
            Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.OBSIDIAN};

    /** monsters per gate rank E..S, "namespace:id*count" */
    private static final String[][] TABLE = {
            {"shadowmonarch:goblin*8"},
            {"shadowmonarch:goblin*6", "shadowmonarch:dire_wolf*5"},
            {"shadowmonarch:dire_wolf*6", "shadowmonarch:goblin*6", "minecraft:skeleton*4"},
            {"shadowmonarch:dire_wolf*8", "minecraft:skeleton*6", "minecraft:spider*4", "shadowmonarch:goblin_chief*1"},
            {"shadowmonarch:goblin_chief*2", "shadowmonarch:dire_wolf*8", "minecraft:husk*6"},
            {"shadowmonarch:goblin_chief*3", "shadowmonarch:dire_wolf*10", "minecraft:wither_skeleton*6"},
    };

    private static final class Run {
        BlockPos origin;
        int rank;
        boolean boss, cleared;
        int exitTimer;
        final List<UUID> mobs = new ArrayList<>();
        double rx, ry, rz;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    public static boolean isInGate(ServerPlayerEntity p) { return RUNS.containsKey(p.getUniqueID()); }

    // ------------------------------------------------------------------ entering
    public static boolean enter(ServerPlayerEntity p, int rank) {
        return start(p, rank, null);
    }

    public static boolean enterBoss(ServerPlayerEntity p) {
        SMData d = SMData.get(p);
        String boss = Progression.gateBoss(d);
        if (boss == null) {
            Progression.say(p, "There is no Gate Master left for you.");
            return false;
        }
        if (!Progression.atGate(d)) {
            Progression.say(p, "§cThe Gate Master's Gate is sealed. §fReach level "
                    + Progression.GATE_LEVELS[d.rank()] + " first.");
            return false;
        }
        return start(p, Math.min(d.rank(), 5), boss);
    }

    private static boolean start(ServerPlayerEntity p, int rank, String bossId) {
        if (RUNS.containsKey(p.getUniqueID())) {
            Progression.say(p, "You are already inside a Gate.");
            return false;
        }
        if (p.dimension != DimensionType.OVERWORLD) {
            Progression.say(p, "Gates can only be opened in the Overworld.");
            return false;
        }
        ServerWorld w = p.getServerWorld();
        Run r = new Run();
        r.rank = rank;
        r.boss = bossId != null;
        r.rx = p.posX; r.ry = p.posY; r.rz = p.posZ;
        r.origin = new BlockPos(((int) Math.floor(p.posX / 16.0)) * 16 - 6, Y0, ((int) Math.floor(p.posZ / 16.0)) * 16 - 6);
        build(w, r.origin, rank);

        SMData d = SMData.get(p);
        d.setInDungeon(true);
        d.setReturn(p.posX, p.posY, p.posZ);

        double px = r.origin.getX() + SIZE / 2.0 + 0.5, pz = r.origin.getZ() + 3.5, py = Y0 + 1;
        // spawn monsters (away from the entrance)
        if (r.boss) {
            spawn(w, r, "shadowmonarch:" + bossId, r.origin.getX() + SIZE / 2.0 + 0.5, r.origin.getZ() + SIZE - 8);
            for (int i = 0; i < 4; i++) spawnRandom(w, r, TABLE[rank][0].split("\\*")[0], px, pz);
        } else {
            for (String entry : TABLE[rank]) {
                String[] parts = entry.split("\\*");
                int n = Integer.parseInt(parts[1]);
                for (int i = 0; i < n; i++) spawnRandom(w, r, parts[0], px, pz);
            }
        }
        RUNS.put(p.getUniqueID(), r);

        p.connection.setPlayerLocation(px, py, pz, 0f, 0f);
        p.fallDistance = 0f;
        w.playSound(null, p.getPosition(), SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.5f, 1.2f);
        Progression.say(p, r.boss
                ? "§4You entered the Gate Master's chamber. §fDefeat §e" + MobDefs.MOBS.get(bossId).name + "§f!"
                : "§6You entered an " + Progression.rankName(rank) + "-Rank Gate. §fClear all monsters! (/sm leave to escape)");
        ModNetwork.sync(p);
        return true;
    }

    private static void build(ServerWorld w, BlockPos o, int rank) {
        Block wall = WALLS[Math.min(rank, WALLS.length - 1)];
        BlockState wallState = wall.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState glow = Blocks.GLOWSTONE.getDefaultState();
        BlockState pillar = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
        BlockState lantern = Blocks.LANTERN.getDefaultState();
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y < HEIGHT; y++) {
                    boolean shell = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1 || y == 0 || y == HEIGHT - 1;
                    BlockState s = shell ? wallState : air;
                    if (y == HEIGHT - 1 && x % 8 == 4 && z % 8 == 4) s = glow;
                    boolean corner = (x == 5 || x == SIZE - 6) && (z == 5 || z == SIZE - 6);
                    if (corner && y >= 1 && y <= 9) s = pillar;
                    if (corner && y == 10) s = glow;
                    w.setBlockState(o.add(x, y, z), s, 2);
                }
            }
        }
        // lanterns on the floor along the walls
        for (int i = 4; i < SIZE - 4; i += 6) {
            w.setBlockState(o.add(i, 1, 1), lantern, 2);
            w.setBlockState(o.add(i, 1, SIZE - 2), lantern, 2);
            w.setBlockState(o.add(1, 1, i), lantern, 2);
            w.setBlockState(o.add(SIZE - 2, 1, i), lantern, 2);
        }
    }

    private static void spawnRandom(ServerWorld w, Run r, String id, double avoidX, double avoidZ) {
        double x, z;
        int tries = 0;
        do {
            x = r.origin.getX() + 3 + RNG.nextInt(SIZE - 6) + 0.5;
            z = r.origin.getZ() + 3 + RNG.nextInt(SIZE - 6) + 0.5;
            tries++;
        } while (tries < 20 && (x - avoidX) * (x - avoidX) + (z - avoidZ) * (z - avoidZ) < 64);
        spawn(w, r, id, x, z);
    }

    private static void spawn(ServerWorld w, Run r, String id, double x, double z) {
        EntityType<?> type = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(id));
        if (type == null) return;
        Entity e = type.create(w);
        if (e == null) return;
        e.setLocationAndAngles(x, Y0 + 1, z, RNG.nextFloat() * 360f, 0f);
        if (e instanceof MobEntity) {
            MobEntity m = (MobEntity) e;
            m.onInitialSpawn(w, w.getDifficultyForLocation(new BlockPos(x, Y0 + 1, z)), SpawnReason.EVENT, null, null);
            m.enablePersistence();
        }
        w.addEntity(e);
        r.mobs.add(e.getUniqueID());
    }

    // ------------------------------------------------------------------ per second
    public static void tick(ServerPlayerEntity p) {
        Run r = RUNS.get(p.getUniqueID());
        if (r == null) return;
        if (!p.isAlive()) { end(p, false); return; }
        double cx = r.origin.getX() + SIZE / 2.0, cz = r.origin.getZ() + SIZE / 2.0;
        if (p.dimension != DimensionType.OVERWORLD || p.getDistanceSq(cx, Y0 + 5, cz) > 50 * 50) {
            end(p, true);
            return;
        }
        ServerWorld w = p.getServerWorld();
        if (!r.cleared) {
            int alive = 0;
            for (UUID id : r.mobs) {
                Entity e = w.getEntityByUuid(id);
                if (e != null && e.isAlive()) alive++;
            }
            if (alive == 0) cleared(p, r);
        } else if (--r.exitTimer <= 0) {
            leave(p);
        }
    }

    private static void cleared(ServerPlayerEntity p, Run r) {
        r.cleared = true;
        r.exitTimer = 10;
        Progression.say(p, "§a§lGATE CLEARED! §r§fYou will be returned in 10 seconds (or use /sm leave).");
        p.world.playSound(null, p.getPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
        ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModItems.MAGIC_STONE, 4 + 3 * r.rank + RNG.nextInt(4)));
        if (RNG.nextInt(2) == 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModItems.MANA_CRYSTAL, 1));
        if (!r.boss) {
            Progression.addXp(p, 100 * (r.rank + 1) * (r.rank + 1));
            Quests.advance(p, Quests.Type.GATE, String.valueOf(r.rank), 1);
        }
    }

    // ------------------------------------------------------------------ leaving
    public static void leave(ServerPlayerEntity p) {
        end(p, true);
    }

    public static void end(ServerPlayerEntity p, boolean teleportBack) {
        Run r = RUNS.remove(p.getUniqueID());
        SMData d = SMData.get(p);
        if (r != null) {
            ServerWorld w = p.getServerWorld();
            for (UUID id : r.mobs) {
                Entity e = w.getEntityByUuid(id);
                if (e != null) e.remove();
            }
        }
        if (teleportBack && p.isAlive()) {
            double[] pos = r != null ? new double[]{r.rx, r.ry, r.rz} : d.returnPos();
            p.connection.setPlayerLocation(pos[0], pos[1], pos[2], p.rotationYaw, p.rotationPitch);
            p.fallDistance = 0f;
        }
        d.setInDungeon(false);
        ModNetwork.sync(p);
    }

    /** Safety net: the player logged out/crashed while inside a gate. */
    public static void onLogin(ServerPlayerEntity p) {
        SMData d = SMData.get(p);
        if (d.inDungeon() && !RUNS.containsKey(p.getUniqueID())) {
            double[] pos = d.returnPos();
            if (pos[1] > 0) p.connection.setPlayerLocation(pos[0], pos[1], pos[2], p.rotationYaw, p.rotationPitch);
            d.setInDungeon(false);
        }
    }

    private DungeonManager() {}
}
