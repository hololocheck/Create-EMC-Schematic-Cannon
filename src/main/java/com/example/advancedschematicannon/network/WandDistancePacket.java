package com.example.advancedschematicannon.network;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.ModRegistry;
import com.example.advancedschematicannon.item.AirPlacementWandItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント→サーバー: 空中設置杖の設置距離変更。
 */
public record WandDistancePacket(int distance) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WandDistancePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    AdvancedSchematicCannon.MOD_ID, "wand_distance"));

    public static final StreamCodec<FriendlyByteBuf, WandDistancePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, WandDistancePacket::distance,
                    WandDistancePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WandDistancePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();
                if (!stack.is(ModRegistry.AIR_PLACEMENT_WAND.get())) {
                    stack = player.getOffhandItem();
                    if (!stack.is(ModRegistry.AIR_PLACEMENT_WAND.get())) return;
                }
                AirPlacementWandItem.setDistance(stack, packet.distance());
            }
        });
    }
}
