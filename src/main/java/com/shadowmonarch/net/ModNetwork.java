package com.shadowmonarch.net;

import com.shadowmonarch.ShadowMonarch;
import com.shadowmonarch.client.ClientData;
import com.shadowmonarch.data.SMData;
import com.shadowmonarch.event.ShadowManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    public static final int ACT_TOGGLE_SUMMON = 0;
    public static final int ACT_EQUIP = 1;
    public static final int ACT_UNEQUIP = 2;
    public static final int ACT_UPGRADE = 3;

    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(ShadowMonarch.MODID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        int id = 0;
        CHANNEL.registerMessage(id++, SyncMsg.class, SyncMsg::encode, SyncMsg::decode, SyncMsg::handle);
        CHANNEL.registerMessage(id++, ActionMsg.class, ActionMsg::encode, ActionMsg::decode, ActionMsg::handle);
    }

    /** Server -> client: send the whole System data of this player. */
    public static void sync(ServerPlayerEntity p) {
        if (CHANNEL == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new SyncMsg(SMData.get(p).copy()));
    }

    /** Client -> server. */
    public static void sendAction(int action, String arg) {
        CHANNEL.sendToServer(new ActionMsg(action, arg));
    }

    // ------------------------------------------------------------------------------------------
    public static class SyncMsg {
        private final CompoundNBT tag;

        public SyncMsg(CompoundNBT tag) { this.tag = tag; }

        public static void encode(SyncMsg m, PacketBuffer buf) { buf.writeCompoundTag(m.tag); }

        public static SyncMsg decode(PacketBuffer buf) { return new SyncMsg(buf.readCompoundTag()); }

        public static void handle(SyncMsg m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> ClientData.set(m.tag));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ActionMsg {
        private final int action;
        private final String arg;

        public ActionMsg(int action, String arg) { this.action = action; this.arg = arg == null ? "" : arg; }

        public static void encode(ActionMsg m, PacketBuffer buf) {
            buf.writeVarInt(m.action);
            buf.writeString(m.arg);
        }

        public static ActionMsg decode(PacketBuffer buf) {
            int a = buf.readVarInt();
            String s = buf.readString(64);
            return new ActionMsg(a, s);
        }

        public static void handle(ActionMsg m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayerEntity p = ctx.get().getSender();
                if (p == null) return;
                switch (m.action) {
                    case ACT_TOGGLE_SUMMON: ShadowManager.toggleSummon(p); break;
                    case ACT_EQUIP: ShadowManager.equip(p, m.arg); break;
                    case ACT_UNEQUIP: ShadowManager.unequip(p, m.arg); break;
                    case ACT_UPGRADE: ShadowManager.upgrade(p, m.arg); break;
                    default: break;
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private ModNetwork() {}
}
