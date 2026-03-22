package com.example.emcschematicannon.gui;

import com.example.emcschematicannon.EMCSchematicCannon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ToggleButton extends AbstractWidget {
    private static final ResourceLocation ON_NORMAL = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/btn_emc_on_normal.png");
    private static final ResourceLocation ON_HOVER = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/btn_emc_on_hover.png");
    private static final ResourceLocation OFF_NORMAL = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/btn_emc_off_normal.png");
    private static final ResourceLocation OFF_HOVER = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/btn_emc_off_hover.png");

    private boolean toggled;
    private final Runnable onToggle;

    public ToggleButton(int x, int y, boolean initialState, Component tooltip, Runnable onToggle) {
        super(x, y, 18, 18, tooltip);
        this.toggled = initialState;
        this.onToggle = onToggle;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        toggled = !toggled;
        onToggle.run();
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
