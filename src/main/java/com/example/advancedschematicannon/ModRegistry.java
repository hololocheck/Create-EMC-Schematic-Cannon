package com.example.advancedschematicannon;

import com.example.advancedschematicannon.block.EMCSchematicCannonBlock;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity;
import com.example.advancedschematicannon.block.EnhancedSchematicCannonBlock;
import com.example.advancedschematicannon.block.FrameBlock;
import com.example.advancedschematicannon.gui.EMCSchematicCannonMenu;
import com.example.advancedschematicannon.item.AirPlacementWandItem;
import com.example.advancedschematicannon.item.RangeBoardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModRegistry {
    /**
     * Whether ProjectE is installed. Evaluated once at class initialization
     * (ModRegistry is first touched during mod construction, after ModList is populated).
     */
    public static final boolean PROJECTE_LOADED = isProjectELoaded();

    private static boolean isProjectELoaded() {
        try {
            return ModList.get() != null && ModList.get().isLoaded("projecte");
        } catch (Throwable t) {
            return false;
        }
    }

    // Deferred Registers
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AdvancedSchematicCannon.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AdvancedSchematicCannon.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AdvancedSchematicCannon.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AdvancedSchematicCannon.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdvancedSchematicCannon.MOD_ID);

    // ===== Block =====
    /** Enhanced variant — always registered. */
    public static final Supplier<EnhancedSchematicCannonBlock> ENHANCED_CANNON_BLOCK =
            BLOCKS.register("enhanced_schematic_cannon", EnhancedSchematicCannonBlock::new);

    /** EMC variant — registered only when ProjectE is installed. Null otherwise. */
    public static final Supplier<EMCSchematicCannonBlock> EMC_CANNON_BLOCK = PROJECTE_LOADED
            ? BLOCKS.register("emc_schematic_cannon", EMCSchematicCannonBlock::new)
            : null;

    public static final Supplier<FrameBlock> FRAME_BLOCK =
            BLOCKS.register("frame_block", FrameBlock::new);

    // ===== Item =====
    public static final Supplier<BlockItem> ENHANCED_CANNON_ITEM =
            ITEMS.register("enhanced_schematic_cannon", () ->
                    new BlockItem(ENHANCED_CANNON_BLOCK.get(), new Item.Properties()));

    public static final Supplier<BlockItem> EMC_CANNON_ITEM = PROJECTE_LOADED
            ? ITEMS.register("emc_schematic_cannon", () ->
                    new BlockItem(EMC_CANNON_BLOCK.get(), new Item.Properties()))
            : null;

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
    /** Single BE type shared by both Enhanced and (optional) EMC cannon blocks. */
    @SuppressWarnings("ConstantConditions")
    public static final Supplier<BlockEntityType<EMCSchematicCannonBlockEntity>> EMC_CANNON_BE =
            BLOCK_ENTITIES.register("emc_schematic_cannon", () -> {
                List<Block> validBlocks = new ArrayList<>();
                validBlocks.add(ENHANCED_CANNON_BLOCK.get());
                if (PROJECTE_LOADED && EMC_CANNON_BLOCK != null) {
                    validBlocks.add(EMC_CANNON_BLOCK.get());
                }
                return BlockEntityType.Builder.of(EMCSchematicCannonBlockEntity::new,
                        validBlocks.toArray(new Block[0])).build(null);
            });

    // ===== Menu =====
    public static final Supplier<MenuType<EMCSchematicCannonMenu>> EMC_CANNON_MENU =
            MENUS.register("emc_schematic_cannon", () ->
                    IMenuTypeExtension.create(EMCSchematicCannonMenu::fromNetwork));

    // ===== Creative Tab =====
    public static final Supplier<CreativeModeTab> TAB = CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.advancedschematicannon"))
                    .icon(() -> ENHANCED_CANNON_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ENHANCED_CANNON_ITEM.get());
                        if (PROJECTE_LOADED && EMC_CANNON_ITEM != null) {
                            output.accept(EMC_CANNON_ITEM.get());
                        }
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
