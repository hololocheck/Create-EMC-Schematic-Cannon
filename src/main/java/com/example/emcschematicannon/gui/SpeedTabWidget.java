package com.example.emcschematicannon.gui;

import com.example.emcschematicannon.EMCSchematicCannon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SpeedTabWidget extends AbstractWidget {
    private static final ResourceLocation TAB_ICON = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/tab_speed.png");
    private static final ResourceLocation TAB_EXPANDED = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/tab_speed_expanded.png");
    private static final ResourceLocation SLIDER_TRACK = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/speed_slider_track.png");
    private static final ResourceLocation SLIDER_FILL = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/speed_slider_fill.png");
    private static final ResourceLocation SLIDER_KNOB = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/speed_slider_knob.png");

    private static final int TAB_SIZE = 26;
    private static final int PANEL_WIDTH = 100;
    private static final int PANEL_HEIGHT = 20; // content below icon (texture is 100x46, top 26 rows = icon)
    private static final int TRACK_WIDTH = 86;
    private static final int TRACK_HEIGHT = 5;
    private static final int KNOB_WIDTH = 5;
    private static final int KNOB_HEIGHT = 9;

    private boolean expanded = false;
    private float animationProgress = 0;
    private final int guiRight;
    private int tabY;

    private int blocksPerTick;
    private final Runnable onChanged;
    private boolean dragging = false;

    public SpeedTabWidget(int guiRight, int tabY, int initialSpeed, Runnable onChanged) {
        super(guiRight, tabY, TAB_SIZE, TAB_SIZE,
                Component.translatable("gui.emcschematicannon.speed"));
        this.guiRight = guiRight;
        this.tabY = tabY;
        this.blocksPerTick = initialSpeed;
        this.onChanged = onChanged;
    }

    public void setTabY(int newY) {
        this.tabY = newY;
        this.setY(newY);
    }

    public void tick() {
        float target = expanded ? 1.0f : 0.0f;
        animationProgress += (target - animationProgress) * 0.35f;
        if (Math.abs(animationProgress - target) < 0.05f) animationProgress = target;
    }

    private float ease(float t) {
        return t * t * (3 - 2 * t);
    }

    public boolean isExpanded() { return expanded; }

    public void setCollapsed() {
        expanded = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0) return false;
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        if (mouseX >= guiRight && mouseX < guiRight + TAB_SIZE
                && mouseY >= tabY && mouseY < tabY + TAB_SIZE) {
            expanded = !expanded;
            return true;
        }

        // Slider drag start (grey frame inner area at texture y=31)
        if (animationProgress > 0.5f) {
            int sliderX = guiRight + 6;
            int sliderY = tabY + 31;
            if (mouseX >= sliderX && mouseX < sliderX + TRACK_WIDTH
                    && mouseY >= sliderY - 4 && mouseY < sliderY + TRACK_HEIGHT + 4) {
                dragging = true;
                updateSliderValue(mouseX, sliderX);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            int sliderX = guiRight + 7;
            updateSliderValue(mouseX, sliderX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateSliderValue(double mouseX, int sliderX) {
        float ratio = (float)((mouseX - sliderX) / TRACK_WIDTH);
        ratio = Math.max(0, Math.min(1, ratio));
        int newValue = 1 + (int)(ratio * 255);
        newValue = Math.max(1, Math.min(256, newValue));
        if (newValue != blocksPerTick) {
            blocksPerTick = newValue;
            onChanged.run();
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (mouseX >= guiRight && mouseX < guiRight + TAB_SIZE
                && mouseY >= tabY && mouseY < tabY + TAB_SIZE) return true;
        if (animationProgress > 0.001f) {
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            if (mouseX >= guiRight && mouseX < guiRight + animatedWidth
                    && mouseY >= tabY && mouseY < tabY + totalHeight) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.active || !this.visible) return false;
        if (animationProgress > 0.5f && isMouseOver(mouseX, mouseY)) {
            int delta = (int) scrollY;
            int newValue = Math.max(1, Math.min(256, blocksPerTick + delta));
            if (newValue != blocksPerTick) {
                blocksPerTick = newValue;
                onChanged.run();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
        if (animationProgress > 0.001f) {
            // Expanding/expanded: render expanded texture starting at tab position (includes icon)
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            graphics.enableScissor(guiRight, tabY, guiRight + animatedWidth, tabY + totalHeight);
            graphics.blit(TAB_EXPANDED, guiRight, tabY, 0, 0, PANEL_WIDTH, TAB_SIZE + PANEL_HEIGHT,
                    PANEL_WIDTH, TAB_SIZE + PANEL_HEIGHT);

            if (animationProgress > 0.5f) {
                int panelY = tabY + TAB_SIZE;
                renderPanelContent(graphics, panelY, mouseX, mouseY);
            }
            graphics.disableScissor();

            // Hover highlight on tab icon area
            if (mouseX >= guiRight && mouseX < guiRight + TAB_SIZE
                    && mouseY >= tabY && mouseY < tabY + TAB_SIZE) {
                graphics.fill(guiRight, tabY, guiRight + TAB_SIZE, tabY + TAB_SIZE, 0x30FFFFFF);
            }
        } else {
            // Collapsed: render just the tab icon
            graphics.blit(TAB_ICON, guiRight, tabY, 0, 0, TAB_SIZE, TAB_SIZE, TAB_SIZE, TAB_SIZE);
            if (mouseX >= guiRight && mouseX < guiRight + TAB_SIZE
                    && mouseY >= tabY && mouseY < tabY + TAB_SIZE) {
                graphics.fill(guiRight, tabY, guiRight + TAB_SIZE, tabY + TAB_SIZE, 0x30FFFFFF);
            }
        }
    }

    private void renderPanelContent(GuiGraphics graphics, int panelY, int mx, int my) {
        var font = Minecraft.getInstance().font;

        // Speed text (next to icon, in the icon row area)
        String text = blocksPerTick + " b/t";
        graphics.drawString(font, text, guiRight + TAB_SIZE + 2, tabY + 9, 0xFFFFFFFF, true);

        // Slider is inside the grey frame drawn in the expanded texture
        // Grey frame inner area: texture (6,31) to (91,35) = 86x5px
        // In screen coords: (guiRight+6, tabY+31)
        int sliderX = guiRight + 6;
        int sliderY = tabY + 31;

        // Fill: render from left edge to knob center (blue over dark background)
        float ratio = (float)(blocksPerTick - 1) / 255;
        int knobOffset = (int)(ratio * (TRACK_WIDTH - KNOB_WIDTH));
        int fillWidth = Math.max(1, knobOffset + KNOB_WIDTH / 2);
        graphics.blit(SLIDER_FILL, sliderX, sliderY, 0, 0, fillWidth, TRACK_HEIGHT, TRACK_WIDTH, TRACK_HEIGHT);

        // Knob
        int knobX = sliderX + knobOffset;
        int knobY = sliderY - (KNOB_HEIGHT - TRACK_HEIGHT) / 2;
        graphics.blit(SLIDER_KNOB, knobX, knobY, 0, 0, KNOB_WIDTH, KNOB_HEIGHT, KNOB_WIDTH, KNOB_HEIGHT);
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        if (animationProgress > 0.001f) {
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            areas.add(new Rect2i(guiRight, tabY, animatedWidth, totalHeight));
        } else {
            areas.add(new Rect2i(guiRight, tabY, TAB_SIZE, TAB_SIZE));
        }
        return areas;
    }

    public int getBlocksPerTick() { return blocksPerTick; }
    public void setBlocksPerTick(int speed) { this.blocksPerTick = speed; }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
