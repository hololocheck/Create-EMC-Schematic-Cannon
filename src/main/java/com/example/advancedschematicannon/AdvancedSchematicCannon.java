package com.example.advancedschematicannon;

import com.example.advancedschematicannon.block.EMCSchematicCannonRenderer;
import com.example.advancedschematicannon.network.CannonActionPacket;
import com.example.advancedschematicannon.network.CannonSettingsPacket;
import com.example.advancedschematicannon.network.RangeBoardEditPacket;
import com.example.advancedschematicannon.network.WandDistancePacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AdvancedSchematicCannon.MOD_ID)
public class AdvancedSchematicCannon {
    public static final String MOD_ID = "advancedschematicannon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public AdvancedSchematicCannon(IEventBus modEventBus) {
        LOGGER.info("Advanced Schematic Cannon initializing...");
        ModRegistry.register(modEventBus);
        com.example.advancedschematicannon.item.ModDataComponents.register(modEventBus);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerAdditionalModels);
        modEventBus.addListener(this::registerCapabilities);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Advanced Schematic Cannon client setup complete.");
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.EMC_CANNON_MENU.get(),
                com.example.advancedschematicannon.gui.EMCSchematicCannonScreen::new);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistry.EMC_CANNON_BE.get(),
                EMCSchematicCannonRenderer::new);
    }

    private void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(EMCSchematicCannonRenderer.PIPE_MODEL);
        event.register(EMCSchematicCannonRenderer.CONNECTOR_MODEL);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) ->
                new com.example.advancedschematicannon.item.WandEnergyStorage(stack),
                ModRegistry.AIR_PLACEMENT_WAND.get());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID).versioned("2.0");

        // クライアント→サーバー: キャノン操作パケット
        registrar.playToServer(
                CannonActionPacket.TYPE,
                CannonActionPacket.STREAM_CODEC,
                CannonActionPacket::handle
        );

        // クライアント→サーバー: キャノン設定パケット
        registrar.playToServer(
                CannonSettingsPacket.TYPE,
                CannonSettingsPacket.STREAM_CODEC,
                CannonSettingsPacket::handle
        );

        // クライアント→サーバー: 範囲指定ボード編集モード
        registrar.playToServer(
                RangeBoardEditPacket.TYPE,
                RangeBoardEditPacket.STREAM_CODEC,
                RangeBoardEditPacket::handle
        );

        // クライアント→サーバー: 空中設置杖の設置距離変更
        registrar.playToServer(
                WandDistancePacket.TYPE,
                WandDistancePacket.STREAM_CODEC,
                WandDistancePacket::handle
        );
    }
}
