package com.shadowmonarch.item;

import com.shadowmonarch.data.Progression;
import com.shadowmonarch.data.SMData;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Right-click: instantly restores a big chunk of MP. */
public class ManaCrystalItem extends Item {
    public ManaCrystalItem(Properties props) { super(props); }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity sp = (ServerPlayerEntity) player;
            SMData d = SMData.get(sp);
            if (d.mp() >= d.maxMp()) {
                Progression.say(sp, "Your MP is already full.");
                return new ActionResult<>(ActionResultType.FAIL, stack);
            }
            d.setMp(d.mp() + Math.max(40, d.maxMp() / 2));
            player.getCooldownTracker().setCooldown(this, 30);
            if (!player.isCreative()) stack.shrink(1);
            ModNetwork.sync(sp);
        }
        return new ActionResult<>(ActionResultType.SUCCESS, stack);
    }
}
