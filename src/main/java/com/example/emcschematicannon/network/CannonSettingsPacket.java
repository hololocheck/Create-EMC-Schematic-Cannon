package com.example.emcschematicannon.network;

import com.example.emcschematicannon.EMCSchematicCannon;
import com.example.emcschematicannon.block.EMCSchematicCannonBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント→サーバーの設定同期パケット。
 * キャノンの動作設定をサーバーに送信する。
 *
 * replaceModeAndStorage: 下位8bit=ReplaceMode ordinal, 上位8bit=StorageMode ordinal
 * speedAndFlags: 下位16bit=blocksPerTick, bit16=reuseSchematic
 */
public record CannonSettingsPacket(
        BlockPos pos,
        int replaceModeAndStorage,
        boolean skipMissing,
        boolean skipTileEntities,
        boolean useEmc,
        int speedAndFlags,
        int fillerModeAndModule
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CannonSettingsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    EMCSchematicCannon.MOD_ID, "cannon_settings"));

    public static final StreamCodec<ByteBuf, CannonSettingsPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        BlockPos.STREAM_CODEC.encode(buf, pkt.pos);
                        ByteBufCodecs.INT.encode(buf, pkt.replaceModeAndStorage);
                        ByteBufCodecs.BOOL.encode(buf, pkt.skipMissing);
                        ByteBufCodecs.BOOL.encode(buf, pkt.skipTileEntities);
                        ByteBufCodecs.BOOL.encode(buf, pkt.useEmc);
                        ByteBufCodecs.INT.encode(buf, pkt.speedAndFlags);
                        ByteBufCodecs.INT.encode(buf, pkt.fillerModeAndModule);
                    },
                    buf -> new CannonSettingsPacket(
                            BlockPos.STREAM_CODEC.decode(buf),
                            ByteBufCodecs.INT.decode(buf),
                            ByteBufCodecs.BOOL.decode(buf),
                            ByteBufCodecs.BOOL.decode(buf),
                            ByteBufCodecs.BOOL.decode(buf),
                            ByteBufCodecs.INT.decode(buf),
                            ByteBufCodecs.INT.decode(buf)
                    )
            );

    /** ReplaceMode + StorageModeを1つのintにパック */
    public static int packModes(int replaceModeOrd, int storageModeOrd) {
        return (replaceModeOrd & 0xFF) | ((storageModeOrd & 0xFF) << 8);
    }

    /** blocksPerTick + reuseSchematicを1つのintにパック */
    public static int packSpeedAndFlags(int blocksPerTick, boolean reuseSchematic) {
        return (blocksPerTick & 0xFFFF) | (reuseSchematic ? 0x10000 : 0);
    }

    /** fillerMode + previewVisible + fillerModuleをパック */
    public static int packFillerModeAndModule(boolean fillerMode, int fillerModuleOrd, boolean previewVisible) {
        return (fillerMode ? 1 : 0) | (previewVisible ? 2 : 0) | ((fillerModuleOrd & 0xFF) << 8);
    }

    public int replaceMode() { return replaceModeAndStorage & 0xFF; }
    public int storageMode() { return (replaceModeAndStorage >>> 8) & 0xFF; }
    public int blocksPerTick() { return speedAndFlags & 0xFFFF; }
    public boolean reuseSchematic() { return (speedAndFlags & 0x10000) != 0; }
    public boolean fillerMode() { return (fillerModeAndModule & 1) != 0; }
    public boolean previewVisible() { return (fillerModeAndModule & 2) != 0; }
    public int fillerModule() { return (fillerModeAndModule >>> 8) & 0xFF; }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CannonSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (serverPlayer.distanceToSqr(
                    packet.pos.getX() + 0.5,
                    packet.pos.getY() + 0.5,
                    packet.pos.getZ() + 0.5) > 64.0) return;

            BlockEntity be = serverPlayer.level().getBlockEntity(packet.pos);
            if (be instanceof EMCSchematicCannonBlockEntity cannon) {
                EMCSchematicCannonBlockEntity.ReplaceMode[] modes =
                        EMCSchematicCannonBlockEntity.ReplaceMode.values();
                int modeOrd = packet.replaceMode();
                if (modeOrd >= 0 && modeOrd < modes.length) {
                    cannon.setReplaceMode(modes[modeOrd]);
                }

                EMCSchematicCannonBlockEntity.StorageMode[] storageModes =
                        EMCSchematicCannonBlockEntity.StorageMode.values();
                int storageOrd = packet.storageMode();
                if (storageOrd >= 0 && storageOrd < storageModes.length) {
                    cannon.setStorageMode(storageModes[storageOrd]);
                }

                cannon.setSkipMissing(packet.skipMissing);
                cannon.setSkipTileEntities(packet.skipTileEntities);
                cannon.setUseEmc(packet.useEmc);
                cannon.setBlocksPerTick(packet.blocksPerTick());
                cannon.setReuseSchematic(packet.reuseSchematic());

                cannon.setFillerMode(packet.fillerMode());
                cannon.setPreviewVisible(packet.previewVisible());
                EMCSchematicCannonBlockEntity.FillerModule[] fillerModules =
                        EMCSchematicCannonBlockEntity.FillerModule.values();
                int fillerOrd = packet.fillerModule();
                if (fillerOrd >= 0 && fillerOrd < fillerModules.length) {
                    cannon.setFillerModule(fillerModules[fillerOrd]);
                }
            }
        });
    }
}
