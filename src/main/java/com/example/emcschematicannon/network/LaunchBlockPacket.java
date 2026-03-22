package com.example.emcschematicannon.network;

import com.example.emcschematicannon.EMCSchematicCannon;
import com.example.emcschematicannon.block.EMCSchematicCannonBlockEntity;
import com.example.emcschematicannon.block.LaunchedItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * サーバー→クライアント: ブロック発射アニメーション。
 * 1tick分の設置ブロックをバッチで送信する。
 */
public record LaunchBlockPacket(
        BlockPos cannonPos,
        List<BlockPos> targets
) implements CustomPacketPayload {

    public static final Type<LaunchBlockPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    EMCSchematicCannon.MOD_ID, "launch_block"));

    public static final StreamCodec<ByteBuf, LaunchBlockPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public LaunchBlockPacket decode(ByteBuf buf) {
                    BlockPos cannon = BlockPos.STREAM_CODEC.decode(buf);
                    int count = ByteBufCodecs.INT.decode(buf);
                    List<BlockPos> list = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        list.add(BlockPos.STREAM_CODEC.decode(buf));
                    }
                    return new LaunchBlockPacket(cannon, list);
                }

                @Override
                public void encode(ByteBuf buf, LaunchBlockPacket pkt) {
                    BlockPos.STREAM_CODEC.encode(buf, pkt.cannonPos);
                    ByteBufCodecs.INT.encode(buf, pkt.targets.size());
                    for (BlockPos pos : pkt.targets) {
                        BlockPos.STREAM_CODEC.encode(buf, pos);
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** @deprecated アニメーション削除により未使用 */
    @Deprecated
    public static void handle(LaunchBlockPacket packet, IPayloadContext context) {
        // アニメーション削除済み: 何もしない
    }
}
