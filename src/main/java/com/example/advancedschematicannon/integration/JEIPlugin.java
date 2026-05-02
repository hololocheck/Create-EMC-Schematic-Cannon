package com.example.advancedschematicannon.integration;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.gui.EMCSchematicCannonScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AdvancedSchematicCannon.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(EMCSchematicCannonScreen.class,
                new IGuiContainerHandler<>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(EMCSchematicCannonScreen screen) {
                        return screen.getExclusionAreas();
                    }
                });
    }
}
