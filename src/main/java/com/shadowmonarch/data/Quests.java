package com.shadowmonarch.data;

import com.shadowmonarch.ShadowMonarch;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** The quest chain (follows the rough shape of the Solo Leveling story, no cutscenes). */
public final class Quests {
    public enum Type { KILL_ANY, KILL, SPRINT, GATE, BOSS, LEVEL, SUMMON, UPGRADE }

    public static final class Quest {
        public final String title, desc, target;
        public final Type type;
        public final int count, xp;
        public final String[] rewards; // "item_id:count"

        Quest(String title, String desc, Type type, String target, int count, int xp, String[] rewards) {
            this.title = title; this.desc = desc; this.type = type; this.target = target;
            this.count = count; this.xp = xp; this.rewards = rewards;
        }
    }

    public static final List<Quest> LIST = new ArrayList<>();

    private static void q(String title, String desc, Type type, String target, int count, int xp, String... rewards) {
        LIST.add(new Quest(title, desc, type, target, count, xp, rewards));
    }

    static {
        // ---------------- Prologue: E-rank
        q("Awakening", "Defeat 5 monsters.", Type.KILL_ANY, null, 5, 30, "magic_stone:3");
        q("Daily Quest: Training", "Run 300 blocks.", Type.SPRINT, null, 300, 40, "gate_key_e:1");
        q("The Double Dungeon", "Enter and clear an E-Rank Gate (right-click the key).", Type.GATE, "0", 1, 80,
                "magic_stone:5", "knight_killer:1");
        q("Hunter's Exam", "Slay 10 Goblins.", Type.KILL, "goblin", 10, 100, "hunter_chestplate:1", "gate_key_e:1");
        q("Limit Reached", "Reach level 5.", Type.LEVEL, null, 5, 0, "magic_stone:4");
        q("Gate Master: Goblin Chief", "Defeat the Goblin Chief with the Gate Master's Key.", Type.BOSS,
                "goblin_chief", 1, 150, "hunter_helmet:1", "hunter_leggings:1", "hunter_boots:1", "mana_crystal:2");
        // ---------------- D-rank
        q("Hungry Packs", "Slay 10 Dire Wolves.", Type.KILL, "dire_wolf", 10, 150, "magic_stone:6");
        q("Daily Quest: Endurance", "Run 800 blocks.", Type.SPRINT, null, 800, 120, "gate_key_d:1");
        q("D-Rank Gate", "Clear a D-Rank Gate.", Type.GATE, "1", 1, 200, "kasaka_fang:1", "magic_stone:6");
        q("Rising Hunter", "Reach level 10.", Type.LEVEL, null, 10, 0, "mana_crystal:2");
        q("Gate Master: Fang", "Defeat Fang, the Alpha. His shadow will be yours.", Type.BOSS, "fang", 1, 300,
                "magic_stone:10", "gate_key_c:1");
        // ---------------- C-rank
        q("The Shadow Army", "Summon your shadows once (K). Equip them in the Shadow Army menu (L).", Type.SUMMON,
                null, 1, 150, "magic_stone:6");
        q("Strength of the Hunter", "Defeat 30 monsters.", Type.KILL_ANY, null, 30, 250, "magic_stone:8");
        q("C-Rank Gate", "Clear a C-Rank Gate.", Type.GATE, "2", 1, 350, "baruka_dagger:1", "knight_helmet:1");
        q("Beyond Ordinary", "Reach level 15.", Type.LEVEL, null, 15, 0, "mana_crystal:3");
        q("Gate Master: Cerberus", "Defeat the three-headed hound Cerberus.", Type.BOSS, "cerberus", 1, 600,
                "knight_chestplate:1", "gate_key_b:1");
        // ---------------- B-rank
        q("Elite Hunt", "Defeat 50 monsters.", Type.KILL_ANY, null, 50, 450, "magic_stone:10");
        q("B-Rank Gate", "Clear a B-Rank Gate.", Type.GATE, "3", 1, 600, "knight_leggings:1", "knight_boots:1");
        q("Elite Hunter", "Reach level 20.", Type.LEVEL, null, 20, 0, "mana_crystal:3");
        q("Gate Master: Tank", "Defeat Tank, the Ice Bear.", Type.BOSS, "tank", 1, 900, "magic_stone:12",
                "gate_key_a:1");
        // ---------------- A-rank
        q("Strengthen the Army", "Upgrade a shadow with Magic Stones (Shadow Army menu, L).", Type.UPGRADE, null, 1,
                300, "magic_stone:10");
        q("A-Rank Gate", "Clear an A-Rank Gate.", Type.GATE, "4", 1, 1000, "demon_king_dagger:1", "mana_crystal:3");
        q("Peak Hunter", "Reach level 25.", Type.LEVEL, null, 25, 0, "magic_stone:10");
        q("Gate Master: Iron", "Defeat Iron, the Knight.", Type.BOSS, "iron", 1, 1200, "magic_stone:15",
                "gate_key_s:1");
        q("The Job Change", "Reach level 30.", Type.LEVEL, null, 30, 0, "mana_crystal:4");
        q("Gate Master: Igris", "Defeat Igris, the Blood-Red Knight. Make him your shadow.", Type.BOSS, "igris", 1,
                1800, "igris_sword:1", "monarch_helmet:1");
        // ---------------- S-rank
        q("S-Rank Gate", "Clear an S-Rank Gate.", Type.GATE, "5", 1, 2500, "monarch_chestplate:1", "magic_stone:20");
        q("Beyond the Limit", "Reach level 40.", Type.LEVEL, null, 40, 0, "monarch_leggings:1", "mana_crystal:5");
        q("Gate Master: Beru", "Defeat Beru, the Ant King.", Type.BOSS, "beru", 1, 4000, "monarch_boots:1",
                "kamish_wrath:1");
        q("The Monarch's Path", "Reach the maximum level (50).", Type.LEVEL, null, 50, 5000, "mana_crystal:10");
    }

    public static Quest get(int idx) {
        return idx >= 0 && idx < LIST.size() ? LIST.get(idx) : null;
    }

    public static Quest current(SMData d) { return get(d.quest()); }

    // ------------------------------------------------------------------ events
    public static void onKill(ServerPlayerEntity p, String path) {
        advance(p, Type.KILL_ANY, null, 1);
        advance(p, Type.KILL, path, 1);
    }

    public static void onLevel(ServerPlayerEntity p) {
        SMData d = SMData.get(p);
        Quest q = current(d);
        if (q != null && q.type == Type.LEVEL) {
            d.setProg(Math.min(q.count, d.level()));
            if (d.level() >= q.count) complete(p, d, q);
            else ModNetwork.sync(p);
        }
    }

    public static void advance(ServerPlayerEntity p, Type type, String target, int amount) {
        SMData d = SMData.get(p);
        Quest q = current(d);
        if (q == null || q.type != type) return;
        if (target != null && q.target != null && !q.target.equals(target)) return;
        d.setProg(d.prog() + amount);
        if (d.prog() >= q.count) complete(p, d, q);
        else ModNetwork.sync(p);
    }

    private static long distanceCm(ServerPlayerEntity p) {
        return p.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM))
                + p.getStats().getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM));
    }

    /** Called once per second. */
    public static void tick(ServerPlayerEntity p) {
        SMData d = SMData.get(p);
        Quest q = current(d);
        if (q == null || q.type != Type.SPRINT) return;
        int blocks = (int) Math.max(0, (distanceCm(p) - d.sprintBase()) / 100);
        if (blocks != d.prog()) {
            d.setProg(Math.min(blocks, q.count));
            if (blocks >= q.count) complete(p, d, q);
            else ModNetwork.sync(p);
        }
    }

    /** Announce the active quest and prepare its counters. */
    public static void start(ServerPlayerEntity p, SMData d) {
        Quest q = current(d);
        if (q == null) {
            Progression.say(p, "§6All quests completed. You are the Shadow Monarch.");
            return;
        }
        d.setProg(0);
        if (q.type == Type.SPRINT) d.setSprintBase(distanceCm(p));
        Progression.say(p, "§e[Quest] " + q.title + "§f - " + q.desc);
        if (q.type == Type.LEVEL && d.level() >= q.count) complete(p, d, q);
    }

    private static void complete(ServerPlayerEntity p, SMData d, Quest q) {
        int idx = d.quest();
        d.setQuest(idx + 1);
        d.setProg(0);
        Progression.say(p, "§a[Quest Complete] §f" + q.title);
        for (String r : q.rewards) {
            String[] parts = r.split(":");
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ShadowMonarch.MODID, parts[0]));
            if (item == null) continue;
            int n = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(item, n));
            Progression.say(p, "§7Reward: §f" + n + "x " + new ItemStack(item).getDisplayName().getString());
        }
        start(p, d);
        if (q.xp > 0) Progression.addXp(p, q.xp);
        ModNetwork.sync(p);
    }

    private Quests() {}
}
