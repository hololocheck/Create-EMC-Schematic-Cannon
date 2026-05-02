package com.example.advancedschematicannon.gui;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Push/nopush button - shows normal texture, switches to hover texture on mouse hover.
 * 18x18px buttons.
 */
public class IconButton extends Button {
    private final ResourceLocation normalTexture;
    private final ResourceLocation hoverTexture;

    public IconButton(int x, int y, String normalTex, String hoverTex,
                      Component tooltip, OnPress onPress) {
        super(x, y, 18, 18, tooltip, onPress, Button.DEFAULT_NARRATION);
        this.normalTexture = ResourceLocation.fromNamespaceAndPath(
                AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/" + normalTex);
        this.hoverTexture = ResourceLocation.fromNamespaceAndPath(
                AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/" + hoverTex);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation tex = (this.active && this.isHovered()) ? hoverTexture : normalTexture;

        if (!this.active) {
            graphics.setColor(0.5f, 0.5f, 0.5f, 0.5f);
        }

        graphics.blit(tex, getX(), getY(), 0, 0, 18, 18, 18, 18);

        if (!this.active) {
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
