package com.example.emcschematicannon.network;

import com.example.emcschematicannon.EMCSchematicCannon;
import com.example.emcschematicannon.item.ModDataComponents;
import com.example.emcschematicannon.item.RangeBoardItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント→サーバー: 範囲指定ボードの編集モード変更。
 * editMode: 0=解除, 1=Pos1編集, 2=Pos2編集
 */
public record RangeBoardEditPacket(int editMode) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RangeBoardEditPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    EMCSchematicCannon.MOD_ID, "range_board_edit"));

    public static final StreamCodec<FriendlyByteBuf, RangeBoardEditPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, RangeBoardEditPacket::editMode,
                    RangeBoardEditPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RangeBoardEditPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                // メインハンド・オフハンドの範囲指定ボードを検索
                ItemStack stack = RangeBoardItem.findHeldRangeBoard(player);
                if (stack.isEmpty()) return;

                if (packet.editMode() == 0) {
                    stack.remove(ModDataComponents.RANGE_EDIT_MODE.get());
                } else {
                    stack.set(ModDataComponents.RANGE_EDIT_MODE.get(), packet.editMode());
                }
            }
        });
    }
}
