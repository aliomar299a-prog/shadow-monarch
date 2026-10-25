package com.shadowmonarch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Central table of every anime monster (and the shadow version of the ones that can be extracted). */
public final class MobDefs {
    public enum Body { BIPED, QUAD, INSECT }

    public static final class Def {
        public final String id, name;
        public final Body body;
        public final float scale, width, height;
        public final double health, damage, speed;
        public final boolean boss;
        public final int xp, stones, tier, egg1, egg2;
        /** shadow id unlocked when this monster is killed (null = none) */
        public final String shadow;

        Def(String id, String name, Body body, float scale, float width, float height, double health, double damage,
            double speed, boolean boss, int xp, int stones, String shadow, int tier, int egg1, int egg2) {
            this.id = id; this.name = name; this.body = body; this.scale = scale; this.width = width;
            this.height = height; this.health = health; this.damage = damage; this.speed = speed;
            this.boss = boss; this.xp = xp; this.stones = stones; this.shadow = shadow; this.tier = tier;
            this.egg1 = egg1; this.egg2 = egg2;
        }

        /** MP needed to summon this shadow. */
        public int summonCost() { return 10 + 15 * tier; }
    }

    public static final Map<String, Def> MOBS = new LinkedHashMap<>();
    /** shadow id -> definition of the original monster */
    public static final Map<String, Def> SHADOWS = new LinkedHashMap<>();
    public static final int MAX_SHADOW_LEVEL = 10;

    private static void add(String id, String name, Body body, float scale, float w, float h, double hp, double dmg,
                            double speed, boolean boss, int xp, int stones, String shadow, int tier, int e1, int e2) {
        Def d = new Def(id, name, body, scale, w, h, hp, dmg, speed, boss, xp, stones, shadow, tier, e1, e2);
        MOBS.put(id, d);
        if (shadow != null) SHADOWS.put(shadow, d);
    }

    static {
        //   id            name                          body         scale  w     h     hp     dmg   speed boss   xp   stn shadow  tier egg1      egg2
        add("goblin",       "Goblin",                    Body.BIPED,  0.8f,  0.5f, 1.5f,  24,    4,   0.30, false,  12,  1, null,   0, 0x54Aa46, 0x6e5032);
        add("goblin_chief", "Goblin Chief",              Body.BIPED,  1.35f, 0.85f,2.6f,  150,   10,  0.28, true,  150,  4, null,   0, 0x3a6c34, 0xd2aa28);
        add("dire_wolf",    "Dire Wolf",                 Body.QUAD,   1.0f,  0.9f, 0.9f,  32,    6,   0.34, false,  20,  1, null,   0, 0x303652, 0xffdc00);
        add("fang",         "Fang, the Alpha",           Body.QUAD,   1.25f, 1.1f, 1.1f,  220,   14,  0.38, true,  300,  5, "fang", 1, 0xa5afc3, 0x4696ff);
        add("cerberus",     "Cerberus",                  Body.QUAD,   2.0f,  1.8f, 1.8f,  420,   22,  0.32, true,  600,  7, null,   0, 0x3e0e0e, 0xff8c14);
        add("tank",         "Tank, the Ice Bear",        Body.QUAD,   1.7f,  1.5f, 1.5f,  600,   26,  0.26, true,  900,  8, "tank", 2, 0x60422a, 0xaa8c69);
        add("iron",         "Iron, the Knight",          Body.BIPED,  1.25f, 0.8f, 2.4f,  700,   28,  0.30, true, 1200,  9, "iron", 2, 0x7d828c, 0x6ebeff);
        add("igris",        "Igris, the Blood-Red Knight", Body.BIPED,1.5f,  0.9f, 2.9f,  1000,  36,  0.34, true, 1800, 12, "igris",3, 0x8c121a, 0xdcbe3c);
        add("beru",         "Beru, the Ant King",        Body.INSECT, 2.2f,  2.4f, 1.6f,  1800,  50,  0.38, true, 4000, 20, "beru", 4, 0x16141e, 0xff2828);
    }

    /** Accepts "goblin" or "shadow_igris". */
    public static Def get(String id) {
        if (id == null) return null;
        if (id.startsWith("shadow_")) id = id.substring(7);
        return MOBS.get(id);
    }

    public static List<String> shadowIds() {
        return new ArrayList<>(SHADOWS.keySet());
    }

    private MobDefs() {}
}
