package com.example.advancedschematicannon.gui;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class VisibilityToggleButton extends AbstractWidget {
    private static final ResourceLocation ON_NORMAL = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_visibility_on_normal.png");
    private static final ResourceLocation ON_HOVER = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_visibility_on_hover.png");
    private static final ResourceLocation OFF_NORMAL = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_visibility_off_normal.png");
    private static final ResourceLocation OFF_HOVER = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_visibility_off_hover.png");

    private boolean toggled;
    private final Runnable onChange;

    public VisibilityToggleButton(int x, int y, boolean initialState, Runnable onChange) {
        super(x, y, 18, 18, Component.empty());
        this.toggled = initialState;
        this.onChange = onChange;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        toggled = !toggled;
        onChange.run();
    }

    public boolean isToggled() { return toggled; }
    public void setToggled(boolean value) { this.toggled = value; }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
        ResourceLocation tex;
        if (toggled) {
            tex = isHovered() ? ON_HOVER : ON_NORMAL;
        } else {
            tex = isHovered() ? OFF_HOVER : OFF_NORMAL;
        }
        graphics.blit(tex, getX(), getY(), 0, 0, 18, 18, 18, 18);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
