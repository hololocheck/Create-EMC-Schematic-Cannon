package com.example.advancedschematicannon.gui;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ModeToggleButton extends AbstractWidget {
    private static final ResourceLocation TEX_SCHEMATIC = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/tab_mode_schematic.png");
    private static final ResourceLocation TEX_FILLER = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/tab_mode_filler.png");

    private boolean fillerMode = false;
    private final Runnable onChanged;

    public ModeToggleButton(int x, int y, Runnable onChanged) {
        super(x, y, 26, 26, Component.translatable("gui.advancedschematicannon.mode_toggle"));
        this.onChanged = onChanged;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        fillerMode = !fillerMode;
        onChanged.run();
    }

    public boolean isFillerMode() { return fillerMode; }

    public void setFillerMode(boolean filler) { this.fillerMode = filler; }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
        ResourceLocation tex = fillerMode ? TEX_FILLER : TEX_SCHEMATIC;
        graphics.blit(tex, getX(), getY(), 0, 0, 26, 26, 26, 26);
        if (isHovered()) {
            graphics.fill(getX(), getY(), getX() + 26, getY() + 26, 0x30FFFFFF);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
