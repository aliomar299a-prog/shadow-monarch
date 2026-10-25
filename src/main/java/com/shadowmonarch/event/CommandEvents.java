package com.shadowmonarch.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.shadowmonarch.MobDefs;
import com.shadowmonarch.ModItems;
import com.shadowmonarch.data.Progression;
import com.shadowmonarch.data.Quests;
import com.shadowmonarch.data.SMData;
import com.shadowmonarch.dungeon.DungeonManager;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.items.ItemHandlerHelper;

/** /sm status | key | leave   and (ops) /sm setlevel n | setrank n | unlock id | quest n */
public class CommandEvents {

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent e) {
        LiteralArgumentBuilder<CommandSource> root = Commands.literal("sm")
                .then(Commands.literal("status").executes(c -> status(c.getSource().asPlayer())))
                .then(Commands.literal("key").executes(c -> key(c.getSource().asPlayer())))
                .then(Commands.literal("leave").executes(c -> {
                    DungeonManager.leave(c.getSource().asPlayer());
                    return 1;
                }))
                .then(Commands.literal("setlevel").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, Progression.MAX_LEVEL))
                                .executes(c -> setLevel(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "n")))))
                .then(Commands.literal("setrank").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, Progression.GATE_LEVELS.length))
                                .executes(c -> setRank(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "n")))))
                .then(Commands.literal("quest").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, Quests.LIST.size()))
                                .executes(c -> setQuest(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "n")))))
                .then(Commands.literal("unlock").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(c -> unlock(c.getSource().asPlayer(), StringArgumentType.getString(c, "id")))));
        e.getCommandDispatcher().register(root);
    }

    private static int status(ServerPlayerEntity p) {
        SMData d = SMData.get(p);
        Progression.say(p, "Level " + d.level() + " | Rank " + Progression.rankName(d.rank()) + " | XP " + d.xp() + "/"
                + Progression.xpNeeded(d.level()) + " | MP " + d.mp() + "/" + d.maxMp());
        Quests.Quest q = Quests.current(d);
        if (q != null) Progression.say(p, "Quest: " + q.title + " (" + d.prog() + "/" + q.count + ") - " + q.desc);
        return 1;
    }

    private static int key(ServerPlayerEntity p) {
        SMData d = SMData.get(p);
        if (!Progression.atGate(d)) {
            Progression.say(p, "You have no Gate Master waiting for you yet.");
            return 0;
        }
        for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
            if (p.inventory.getStackInSlot(i).getItem() == ModItems.BOSS_KEY) {
                Progression.say(p, "You already carry the Gate Master's Key.");
                return 0;
            }
        }
        ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModItems.BOSS_KEY));
        return 1;
    }

    private static int setLevel(ServerPlayerEntity p, int n) {
        SMData d = SMData.get(p);
        d.setLevel(Math.min(n, Progression.levelCap(d.rank())));
        d.setXp(0);
        Progression.applyStats(p);
        Quests.onLevel(p);
        ModNetwork.sync(p);
        return 1;
    }

    private static int setRank(ServerPlayerEntity p, int n) {
        SMData d = SMData.get(p);
        d.setRank(n);
        d.setLevel(Math.max(d.level(), n > 0 ? Progression.GATE_LEVELS[n - 1] : 1));
        d.setKeyRank(0);
        Progression.applyStats(p);
        p.setHealth(p.getMaxHealth());
        ModNetwork.sync(p);
        return 1;
    }

    private static int setQuest(ServerPlayerEntity p, int n) {
        SMData d = SMData.get(p);
        d.setQuest(n);
        Quests.start(p, d);
        ModNetwork.sync(p);
        return 1;
    }

    private static int unlock(ServerPlayerEntity p, String id) {
        if (!MobDefs.SHADOWS.containsKey(id)) {
            Progression.say(p, "Unknown shadow. Available: " + MobDefs.shadowIds());
            return 0;
        }
        SMData.get(p).unlockShadow(id);
        ModNetwork.sync(p);
        return 1;
    }
}
