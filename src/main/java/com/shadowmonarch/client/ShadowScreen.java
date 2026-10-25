package com.shadowmonarch.client;

import com.shadowmonarch.MobDefs;
import com.shadowmonarch.ModItems;
import com.shadowmonarch.data.SMData;
import com.shadowmonarch.event.ShadowManager;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** The Shadow Army menu (key L): see your shadows, equip (max 2), upgrade with Magic Stones, summon. */
public class ShadowScreen extends Screen {
    private int seenVersion = -1;

    public ShadowScreen() {
        super(new StringTextComponent("Shadow Army"));
    }

    @Override
    protected void init() {
        build();
    }

    private int stonesInInventory() {
        int n = 0;
        if (minecraft == null || minecraft.player == null) return 0;
        for (int i = 0; i < minecraft.player.inventory.getSizeInventory(); i++) {
            ItemStack s = minecraft.player.inventory.getStackInSlot(i);
            if (s.getItem() == ModItems.MAGIC_STONE) n += s.getCount();
        }
        return n;
    }

    private void build() {
        this.buttons.clear();
        this.children.clear();
        seenVersion = ClientData.version;
        SMData d = ClientData.get();
        List<String> equipped = d.equipped();
        int left = this.width / 2 - 150;
        int y = 56;
        for (String id : MobDefs.shadowIds()) {
            if (d.hasShadow(id)) {
                final String sid = id;
                boolean eq = equipped.contains(id);
                int lvl = d.shadowLevel(id);
                this.addButton(new Button(left + 150, y - 4, 70, 20, eq ? "Unequip" : "Equip",
                        b -> ModNetwork.sendAction(eq ? ModNetwork.ACT_UNEQUIP : ModNetwork.ACT_EQUIP, sid)));
                String up = lvl >= MobDefs.MAX_SHADOW_LEVEL ? "MAX" : "Upgrade (" + ShadowManager.upgradeCost(lvl) + ")";
                Button ub = new Button(left + 224, y - 4, 76, 20, up, b -> ModNetwork.sendAction(ModNetwork.ACT_UPGRADE, sid));
                ub.active = lvl < MobDefs.MAX_SHADOW_LEVEL;
                this.addButton(ub);
            }
            y += 28;
        }
        this.addButton(new Button(this.width / 2 - 100, this.height - 52, 98, 20, "Summon / Dismiss",
                b -> ModNetwork.sendAction(ModNetwork.ACT_TOGGLE_SUMMON, "")));
        this.addButton(new Button(this.width / 2 + 2, this.height - 52, 98, 20, "Close",
                b -> this.minecraft.displayGuiScreen(null)));
    }

    @Override
    public void tick() {
        super.tick();
        if (seenVersion != ClientData.version) build();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        SMData d = ClientData.get();
        this.drawCenteredString(this.font, "§5§lShadow Army", this.width / 2, 16, 0xFFFFFF);
        this.drawCenteredString(this.font, "§9MP " + d.mp() + "/" + d.maxMp() + "   §dEquipped "
                + d.equipped().size() + "/" + SMData.MAX_EQUIPPED + "   §7Magic Stones: " + stonesInInventory(),
                this.width / 2, 30, 0xFFFFFF);
        int left = this.width / 2 - 150;
        int y = 56;
        for (String id : MobDefs.shadowIds()) {
            MobDefs.Def def = MobDefs.SHADOWS.get(id);
            if (d.hasShadow(id)) {
                boolean eq = d.equipped().contains(id);
                this.drawString(this.font, (eq ? "§a● " : "§7○ ") + "§d" + def.name.split(",")[0]
                        + " §7Lv." + d.shadowLevel(id), left, y - 2, 0xFFFFFF);
                this.drawString(this.font, "§8Summon cost: " + def.summonCost() + " MP", left, y + 8, 0xFFFFFF);
            } else {
                this.drawString(this.font, "§8??? (defeat " + def.name.split(",")[0] + ")", left, y + 2, 0xFFFFFF);
            }
            y += 28;
        }
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_L || key == GLFW.GLFW_KEY_K) {
            this.minecraft.displayGuiScreen(null);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
