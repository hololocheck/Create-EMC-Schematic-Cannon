package com.example.emcschematicannon;

import com.example.emcschematicannon.block.EMCSchematicCannonRenderer;
import com.example.emcschematicannon.network.CannonActionPacket;
import com.example.emcschematicannon.network.CannonSettingsPacket;
import com.example.emcschematicannon.network.RangeBoardEditPacket;
import com.example.emcschematicannon.network.WandDistancePacket;
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

@Mod(EMCSchematicCannon.MOD_ID)
public class EMCSchematicCannon {
    public static final String MOD_ID = "emcschematicannon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public EMCSchematicCannon(IEventBus modEventBus) {
        LOGGER.info("EMC Schematic Cannon initializing...");
        ModRegistry.register(modEventBus);
        com.example.emcschematicannon.item.ModDataComponents.register(modEventBus);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerAdditionalModels);
        modEventBus.addListener(this::registerCapabilities);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("EMC Schematic Cannon client setup complete.");
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.EMC_CANNON_MENU.get(),
                com.example.emcschematicannon.gui.EMCSchematicCannonScreen::new);
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
                new com.example.emcschematicannon.item.WandEnergyStorage(stack),
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
