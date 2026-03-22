package com.example.emcschematicannon;

import com.example.emcschematicannon.block.EMCSchematicCannonBlock;
import com.example.emcschematicannon.block.EMCSchematicCannonBlockEntity;
import com.example.emcschematicannon.block.FrameBlock;
import com.example.emcschematicannon.gui.EMCSchematicCannonMenu;
import com.example.emcschematicannon.item.AirPlacementWandItem;
import com.example.emcschematicannon.item.RangeBoardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRegistry {
    // Deferred Registers
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, EMCSchematicCannon.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, EMCSchematicCannon.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EMCSchematicCannon.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EMCSchematicCannon.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EMCSchematicCannon.MOD_ID);

    // ===== Block =====
    public static final Supplier<EMCSchematicCannonBlock> EMC_CANNON_BLOCK =
            BLOCKS.register("emc_schematic_cannon", EMCSchematicCannonBlock::new);

    public static final Supplier<FrameBlock> FRAME_BLOCK =
            BLOCKS.register("frame_block", FrameBlock::new);

    // ===== Item =====
    public static final Supplier<BlockItem> EMC_CANNON_ITEM =
            ITEMS.register("emc_schematic_cannon", () ->
                    new BlockItem(EMC_CANNON_BLOCK.get(), new Item.Properties()));

    public static final Supplier<RangeBoardItem> RANGE_BOARD_ITEM =
            ITEMS.register("range_board", () ->
                    new RangeBoardItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<BlockItem> FRAME_BLOCK_ITEM =
            ITEMS.register("frame_block", () ->
                    new BlockItem(FRAME_BLOCK.get(), new Item.Properties()));

    public static final Supplier<AirPlacementWandItem> AIR_PLACEMENT_WAND =
            ITEMS.register("air_placement_wand", () ->
                    new AirPlacementWandItem(new Item.Properties()));

    // ===== Block Entity =====
    @SuppressWarnings("ConstantConditions")
    public static final Supplier<BlockEntityType<EMCSchematicCannonBlockEntity>> EMC_CANNON_BE =
            BLOCK_ENTITIES.register("emc_schematic_cannon", () ->
                    BlockEntityType.Builder.of(EMCSchematicCannonBlockEntity::new,
                            EMC_CANNON_BLOCK.get()).build(null));

    // ===== Menu =====
    public static final Supplier<MenuType<EMCSchematicCannonMenu>> EMC_CANNON_MENU =
            MENUS.register("emc_schematic_cannon", () ->
                    IMenuTypeExtension.create(EMCSchematicCannonMenu::fromNetwork));

    // ===== Creative Tab =====
    public static final Supplier<CreativeModeTab> TAB = CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.emcschematicannon"))
                    .icon(() -> EMC_CANNON_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(EMC_CANNON_ITEM.get());
                        output.accept(RANGE_BOARD_ITEM.get());
                        output.accept(AIR_PLACEMENT_WAND.get());
                        output.accept(FRAME_BLOCK_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        CREATIVE_TABS.register(bus);
    }
}
