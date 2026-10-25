package com.shadowmonarch.client;

import com.shadowmonarch.data.Progression;
import com.shadowmonarch.data.Quests;
import com.shadowmonarch.data.SMData;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Key presses (K = summon, L = menu) and the System HUD. */
public class ClientEvents {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (ClientSetup.SUMMON.isPressed()) {
            if (mc.currentScreen == null) ModNetwork.sendAction(ModNetwork.ACT_TOGGLE_SUMMON, "");
        }
        while (ClientSetup.MENU.isPressed()) {
            if (mc.currentScreen == null && ClientData.ready()) mc.displayGuiScreen(new ShadowScreen());
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameSettings.hideGUI || !ClientData.ready()) return;
        FontRenderer font = mc.fontRenderer;
        SMData d = ClientData.get();

        int x = 6, y = 6, w = 120;
        AbstractGui.fill(x - 3, y - 3, x + w + 3, y + 62, 0x88000000);
        font.drawStringWithShadow("§bLv." + d.level() + "  §6" + Progression.rankName(d.rank()) + "-Rank",
                x, y, 0xFFFFFF);

        // XP bar
        int need = Progression.xpNeeded(d.level());
        boolean capped = d.level() >= Progression.levelCap(d.rank());
        float xpFrac = capped ? 1f : Math.min(1f, d.xp() / (float) need);
        bar(x, y + 12, w, 6, xpFrac, capped ? 0xFFCC3333 : 0xFF55CC55);
        font.drawStringWithShadow(capped ? "§cGATE MASTER BLOCKS YOU" : "XP " + d.xp() + "/" + need, x, y + 20, 0xFFFFFF);

        // MP bar
        bar(x, y + 32, w, 6, d.mp() / (float) Math.max(1, d.maxMp()), 0xFF3C8CFF);
        font.drawStringWithShadow("§9MP " + d.mp() + "/" + d.maxMp(), x, y + 40, 0xFFFFFF);

        // quest
        Quests.Quest q = Quests.get(d.quest());
        if (q != null) {
            font.drawStringWithShadow("§e" + q.title, x, y + 51, 0xFFFFFF);
            String prog = "§7" + Math.min(d.prog(), q.count) + "/" + q.count;
            font.drawStringWithShadow(prog, x + w - font.getStringWidth("0/000"), y + 51, 0xFFFFFF);
        } else {
            font.drawStringWithShadow("§6All quests complete", x, y + 51, 0xFFFFFF);
        }
    }

    private static void bar(int x, int y, int w, int h, float frac, int color) {
        AbstractGui.fill(x, y, x + w, y + h, 0xFF222222);
        AbstractGui.fill(x, y, x + (int) (w * Math.max(0f, Math.min(1f, frac))), y + h, color);
    }
}
