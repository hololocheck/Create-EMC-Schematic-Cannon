package com.example.advancedschematicannon;

import com.example.advancedschematicannon.integration.AE2CableConnection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * NeoForge Capabilities登録。
 * FEエネルギーストレージ、IItemHandler、AE2グリッドノードのcapabilityを提供。
 */
@EventBusSubscriber(modid = AdvancedSchematicCannon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // FE エネルギーストレージ capability
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModRegistry.EMC_CANNON_BE.get(),
                (be, direction) -> be.getEnergyStorage()
        );

        // IItemHandler capability（AE2 Storage Bus等の外部接続用）
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModRegistry.EMC_CANNON_BE.get(),
                (be, direction) -> be.getItemHandler()
        );

        // AE2 グリッドノード capability（AE2ケーブル接続用）
        AE2CableConnection.register(event);
    }
}
