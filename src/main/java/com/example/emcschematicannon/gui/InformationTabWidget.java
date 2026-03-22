package com.example.emcschematicannon.gui;

import com.example.emcschematicannon.EMCSchematicCannon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class InformationTabWidget extends AbstractWidget {
    private static final ResourceLocation TAB_ICON = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/tab_info.png");
    private static final ResourceLocation TAB_EXPANDED = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/tab_info_expanded.png");

    private static final int TAB_SIZE = 26;
    private static final int PANEL_WIDTH = 100;
    private static final int PANEL_HEIGHT = 66; // content below icon (texture is 100x92, top 26 rows include icon at right)
    private static final int TEXT_MARGIN = 6;
    private static final int LINE_HEIGHT = 10;

    private boolean expanded = false;
    private float animationProgress = 0;
    private final int guiLeft;
    private final int tabY;
    private int scrollOffset = 0;

    private final List<FormattedCharSequence> descriptionLines = new ArrayList<>();

    public InformationTabWidget(int guiLeft, int tabY) {
        super(guiLeft - TAB_SIZE, tabY, TAB_SIZE, TAB_SIZE,
                Component.translatable("gui.emcschematicannon.information"));
        this.guiLeft = guiLeft;
        this.tabY = tabY;

        // Build description text with proper word wrapping
        var font = Minecraft.getInstance().font;
        int maxWidth = PANEL_WIDTH - TEXT_MARGIN * 2;

        String[] keys = {
            "gui.emcschematicannon.info.line1",
            "gui.emcschematicannon.info.line2",
            "gui.emcschematicannon.info.line3",
            "gui.emcschematicannon.info.line4",
            "gui.emcschematicannon.info.line5"
        };

        for (String key : keys) {
            String text = Component.translatable(key).getString();
            if (text.equals(key)) continue;
            // Add blank line between paragraphs (except before first)
            if (!descriptionLines.isEmpty()) {
                descriptionLines.add(FormattedCharSequence.EMPTY);
            }
            // Word-wrap each line and store the wrapped segments
            List<FormattedCharSequence> wrapped = font.split(
                    Component.literal(text), maxWidth);
            descriptionLines.addAll(wrapped);
        }
    }

    public void tick() {
        float target = expanded ? 1.0f : 0.0f;
        animationProgress += (target - animationProgress) * 0.35f;
        if (Math.abs(animationProgress - target) < 0.05f) animationProgress = target;
    }

    private float ease(float t) {
        return t * t * (3 - 2 * t);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0) return false;
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        int tabX = guiLeft - TAB_SIZE;
        if (mouseX >= tabX && mouseX < tabX + TAB_SIZE
                && mouseY >= tabY && mouseY < tabY + TAB_SIZE) {
            expanded = !expanded;
            scrollOffset = 0;
            return true;
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int tabX = guiLeft - TAB_SIZE;
        if (mouseX >= tabX && mouseX < tabX + TAB_SIZE
                && mouseY >= tabY && mouseY < tabY + TAB_SIZE) return true;
        if (animationProgress > 0.001f) {
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            if (mouseX >= guiLeft - animatedWidth && mouseX < guiLeft
                    && mouseY >= tabY && mouseY < tabY + totalHeight) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.active || !this.visible) return false;
        if (animationProgress > 0.5f && isMouseOver(mouseX, mouseY)) {
            int maxScroll = Math.max(0, descriptionLines.size() * LINE_HEIGHT - (PANEL_HEIGHT - TEXT_MARGIN * 2));
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(scrollY * LINE_HEIGHT)));
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
        int tabX = guiLeft - TAB_SIZE;
        int texHeight = TAB_SIZE + PANEL_HEIGHT; // 92 = full texture height

        if (animationProgress > 0.001f) {
            // Expanding/expanded: render expanded texture (includes icon at top-right)
            // Horizontal: animate from showing just TAB_SIZE (right side) to full PANEL_WIDTH
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            int scissorLeft = guiLeft - animatedWidth;

            graphics.enableScissor(scissorLeft, tabY, guiLeft, tabY + totalHeight);
            // Position texture so right edge aligns with guiLeft
            graphics.blit(TAB_EXPANDED, guiLeft - PANEL_WIDTH, tabY, 0, 0,
                    PANEL_WIDTH, texHeight, PANEL_WIDTH, texHeight);

            if (animationProgress > 0.5f) {
                renderPanelContent(graphics, guiLeft - PANEL_WIDTH, tabY + TAB_SIZE);
            }
            graphics.disableScissor();

            // Hover highlight on tab icon area
            if (mouseX >= tabX && mouseX < tabX + TAB_SIZE
                    && mouseY >= tabY && mouseY < tabY + TAB_SIZE) {
                graphics.fill(tabX, tabY, tabX + TAB_SIZE, tabY + TAB_SIZE, 0x30FFFFFF);
            }
        } else {
            // Collapsed: render just the tab icon
            graphics.blit(TAB_ICON, tabX, tabY, 0, 0, TAB_SIZE, TAB_SIZE, TAB_SIZE, TAB_SIZE);
            if (mouseX >= tabX && mouseX < tabX + TAB_SIZE
                    && mouseY >= tabY && mouseY < tabY + TAB_SIZE) {
                graphics.fill(tabX, tabY, tabX + TAB_SIZE, tabY + TAB_SIZE, 0x30FFFFFF);
            }
        }
    }

    private void renderPanelContent(GuiGraphics graphics, int panelX, int panelY) {
        var font = Minecraft.getInstance().font;
        int textX = panelX + TEXT_MARGIN;
        int textAreaY = panelY + TEXT_MARGIN;
        int textAreaH = PANEL_HEIGHT - TEXT_MARGIN * 2;

        graphics.enableScissor(panelX + TEXT_MARGIN, textAreaY,
                panelX + PANEL_WIDTH - TEXT_MARGIN, textAreaY + textAreaH);

        for (int i = 0; i < descriptionLines.size(); i++) {
            int lineY = textAreaY + i * LINE_HEIGHT - scrollOffset;
            if (lineY + LINE_HEIGHT > textAreaY && lineY < textAreaY + textAreaH) {
                graphics.drawString(font, descriptionLines.get(i), textX, lineY, 0xFFFFFFFF, false);
            }
        }

        graphics.disableScissor();
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        int tabX = guiLeft - TAB_SIZE;
        if (animationProgress > 0.001f) {
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            areas.add(new Rect2i(guiLeft - animatedWidth, tabY, animatedWidth, totalHeight));
        } else {
            areas.add(new Rect2i(tabX, tabY, TAB_SIZE, TAB_SIZE));
        }
        return areas;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
