package com.example.advancedschematicannon.integration;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.ModRegistry;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * AE2ケーブル接続用capability登録。
 * AE2が存在しない場合は安全にスキップする。
 */
public class AE2CableConnection {

    public static void register(RegisterCapabilitiesEvent event) {
        try {
            registerInternal(event);
            AdvancedSchematicCannon.LOGGER.info("AE2 cable connection capability registered");
        } catch (NoClassDefFoundError | Exception e) {
            AdvancedSchematicCannon.LOGGER.debug("AE2 not available, skipping cable connection: {}", e.getMessage());
        }
    }

    private static void registerInternal(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModRegistry.EMC_CANNON_BE.get(),
                (be, direction) -> {
                    Object host = be.getAe2GridNodeHost();
                    return host instanceof appeng.api.networking.IInWorldGridNodeHost gridHost
                            ? gridHost : null;
                }
        );
    }
}
