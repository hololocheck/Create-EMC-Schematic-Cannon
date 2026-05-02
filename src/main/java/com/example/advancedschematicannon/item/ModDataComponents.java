package com.example.advancedschematicannon.item;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AdvancedSchematicCannon.MOD_ID);

    public static final Supplier<DataComponentType<BlockPos>> RANGE_POS1 =
            DATA_COMPONENTS.register("range_pos1", () ->
                    DataComponentType.<BlockPos>builder()
                            .persistent(BlockPos.CODEC)
                            .networkSynchronized(BlockPos.STREAM_CODEC)
                            .build());

    public static final Supplier<DataComponentType<BlockPos>> RANGE_POS2 =
            DATA_COMPONENTS.register("range_pos2", () ->
                    DataComponentType.<BlockPos>builder()
                            .persistent(BlockPos.CODEC)
                            .networkSynchronized(BlockPos.STREAM_CODEC)
                            .build());

    public static final Supplier<DataComponentType<Integer>> WAND_ENERGY =
            DATA_COMPONENTS.register("wand_energy", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    /** 範囲指定ボードの編集モード: 0=なし, 1=Pos1編集中, 2=Pos2編集中 */
    public static final Supplier<DataComponentType<Integer>> RANGE_EDIT_MODE =
            DATA_COMPONENTS.register("range_edit_mode", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    /** 空中設置杖の設置距離 (1-15) */
    public static final Supplier<DataComponentType<Integer>> WAND_DISTANCE =
            DATA_COMPONENTS.register("wand_distance", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    public static final Supplier<DataComponentType<List<BlockPos>>> WAND_PLACED_BLOCKS =
            DATA_COMPONENTS.register("wand_placed_blocks", () ->
                    DataComponentType.<List<BlockPos>>builder()
                            .persistent(BlockPos.CODEC.listOf())
                            .networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build());

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}
