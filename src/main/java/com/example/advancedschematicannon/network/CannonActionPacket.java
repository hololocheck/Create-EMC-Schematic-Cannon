package com.example.advancedschematicannon.network;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント→サーバーのアクションパケット。
 * GUI上のボタンクリックでキャノンの動作を制御する。
 */
public record CannonActionPacket(BlockPos pos, Action action) implements CustomPacketPayload {

    public enum Action {
        START, PAUSE, RESUME, STOP
    }

    public static final CustomPacketPayload.Type<CannonActionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    AdvancedSchematicCannon.MOD_ID, "cannon_action"));

    public static final StreamCodec<FriendlyByteBuf, CannonActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CannonActionPacket::pos,
                    ByteBufCodecs.INT.map(
                            i -> Action.values()[i],
                            a -> a.ordinal()
                    ), CannonActionPacket::action,
                    CannonActionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * サーバー側でパケットを処理する。
     */
    public static void handle(CannonActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            BlockEntity be = serverPlayer.level().getBlockEntity(packet.pos());
            if (!(be instanceof EMCSchematicCannonBlockEntity cannon)) return;

            // プレイヤーとブロックの距離チェック
            if (serverPlayer.distanceToSqr(
                    packet.pos().getX() + 0.5,
                    packet.pos().getY() + 0.5,
                    packet.pos().getZ() + 0.5) > 64.0) {
                return;
            }

            switch (packet.action()) {
                case START -> cannon.startPlacement(serverPlayer);
                case PAUSE -> cannon.pausePlacement();
                case RESUME -> cannon.resumePlacement();
                case STOP -> cannon.stopPlacement();
            }
        });
    }
}
