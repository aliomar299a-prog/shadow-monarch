package com.shadowmonarch.item;

import com.shadowmonarch.dungeon.DungeonManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Right-click to open a Gate (dungeon). rank 0..5 = E..S, rank -1 = the Gate Master's (boss) key. */
public class GateKeyItem extends Item {
    private final int rank;

    public GateKeyItem(int rank, Properties props) {
        super(props);
        this.rank = rank;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player instanceof ServerPlayerEntity) {
            boolean ok = rank < 0
                    ? DungeonManager.enterBoss((ServerPlayerEntity) player)
                    : DungeonManager.enter((ServerPlayerEntity) player, rank);
            if (ok && !player.isCreative()) stack.shrink(1);
        }
        return new ActionResult<>(ActionResultType.SUCCESS, stack);
    }
}
