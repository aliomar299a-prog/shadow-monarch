# Shadow Monarch (Solo Leveling) — Forge 1.14.4 mod — PROGRESS

> If the user says "كمل" / "continue": read this file, resume from the first unchecked item.

## Spec (from user, Arabic)
- Forge mod for MC 1.14.4. Solo Leveling inspired, NO cutscenes/story scenes.
- Many quests like the story, play dungeons (gates), kill bosses.
- Level system; every few levels a mandatory strong boss; beating it gives a big power jump.
- Anime weapons (a few famous ones) + simple new armors.
- Anime mobs: goblins, wolves, Igris, Beru, etc.
- Shadow summoning with key K; GUI to see shadows + upgrade them with an item; max 2 equipped.
- Shadows attack monsters/zombies (hostiles); attack animals only if the player hit them first.
- MP to summon; MP regenerates with rest and food.
- New world starts with NO shadows; shadows are unlocked by quests; Igris/Beru etc. summonable after killing them.
- Last item: "عند صنع العالم خلي زي اضافة المود" -> interpreted as: the System auto-activates on first join to a NEW world (no manual setup). (flag to user)

## Environment findings (2026-10-24)
- Sandbox has JDK 21 only. Forge/Gradle/Maven/Mojang/Adoptium hosts are BLOCKED (403 policy) -> cannot compile/test the mod here.
- GitHub not linked (gh token invalid) -> cannot use GitHub Actions to build yet.
- User's installed game (uploaded repack) has no Forge; needs Forge 1.14.4 installed separately.

## Status
- [x] Check build environment  -> blocked, asked user to choose path
- [x] Decision from user: (A) connect GitHub so Actions builds the jar (user will link GitHub from phone; NOT linked yet as of 2026-10-25)
- [x] Project skeleton (build.gradle FG3, mods.toml, workflow .github/workflows/build.yml, tools/gen_assets.py -> all textures/models/lang/recipes GENERATED & committed)
- [ ] Player system (level/XP/MP/shadows capability + sync)
- [ ] Quests + level-gated bosses
- [ ] Mobs/bosses
- [ ] Weapons/armor
- [ ] Dungeons
- [ ] Shadow summon (K) + GUI + upgrade
- [ ] New-world start + build + deliver

## Code written so far (src/main/java/com/shadowmonarch)
DONE: ShadowMonarch.java, MobDefs.java, data/{SMData,Progression,Quests}.java, item/*, ModItems, entity/* (SMMonster+3, ShadowEntity+3, ShadowGoals, IVariant), ModEntities, net/ModNetwork, event/ShadowManager
STATUS 2026-10-25: ALL JAVA SOURCE WRITTEN (first draft, never compiled).
DONE also: dungeon/DungeonManager, event/ServerEvents, event/CommandEvents (/sm status|key|leave|setlevel|setrank|quest|unlock), client/* (ClientData, SMRenderer, ClientSetup[K summon, L menu], ClientEvents[HUD], ShadowScreen).
NEXT:
 1. Static self-review of the code for obvious API mistakes (1.14.4 / MCP snapshot 20190719-1.14.3).
 2. Compile via GitHub Actions once user links GitHub (push repo, read Actions logs, fix errors, download jar artifact).
 3. Write README with install steps (Forge 1.14.4-28.2.26 needed; jar -> mods folder).
Design notes: keys K = summon/dismiss equipped shadows (cost MP), L = Shadow Army menu. Max 2 equipped. Shadows hit IMob + owner's last target/revenge target only.
Gate levels 5,10,15,20,25,30,40 -> bosses goblin_chief,fang,cerberus,tank,iron,igris,beru. Max level 50.
Interpretation flagged to user: "عند صنع العالم خلي زي اضافة المود" = System auto-activates on first join of any new world.
