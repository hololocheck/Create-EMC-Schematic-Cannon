package com.example.emcschematicannon.gui;

import com.example.emcschematicannon.EMCSchematicCannon;
import com.example.emcschematicannon.block.EMCSchematicCannonBlockEntity.FillerModule;
import com.example.emcschematicannon.block.EMCSchematicCannonBlockEntity.ReplaceMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SettingsTabWidget extends AbstractWidget {
    private static final ResourceLocation TAB_ICON = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/tab_settings.png");
    private static final ResourceLocation TAB_EXPANDED = ResourceLocation.fromNamespaceAndPath(
            EMCSchematicCannon.MOD_ID, "textures/gui/gen2/tab_settings_expanded.png");

    private static final int TAB_SIZE = 26;
    private static final int PANEL_WIDTH = 100;
    private static final int PANEL_HEIGHT = 66;
    private static final int BUTTON_SIZE = 18;

    // === 概略図モード用ボタン (3x2 = 6) ===
    private static final int SCHEMATIC_BTN_COUNT = 6;
    private static final String[] SCHEMATIC_BTN_NORMAL = {
        "btn_replace_none_normal.png",
        "btn_replace_solid_normal.png",
        "btn_replace_any_normal.png",
        "btn_replace_air_normal.png",
        "btn_skip_missing_normal.png",
        "btn_protect_be_normal.png"
    };
    private static final String[] SCHEMATIC_BTN_HOVER = {
        "btn_replace_none_hover.png",
        "btn_replace_solid_hover.png",
        "btn_replace_any_hover.png",
        "btn_replace_air_hover.png",
        "btn_skip_missing_hover.png",
        "btn_protect_be_hover.png"
    };
    private static final String[] SCHEMATIC_TOOLTIP_KEYS = {
        "gui.emcschematicannon.settings.dont_replace",
        "gui.emcschematicannon.settings.replace_solid",
        "gui.emcschematicannon.settings.replace_any",
        "gui.emcschematicannon.settings.replace_empty",
        "gui.emcschematicannon.settings.skip_missing",
        "gui.emcschematicannon.settings.protect_be"
    };
    private static final int SCHEMATIC_GRID_COLS = 3;
    private static final int SCHEMATIC_GRID_MARGIN_X = 17;
    private static final int SCHEMATIC_GRID_MARGIN_Y = 11;
    private static final int SCHEMATIC_GRID_GAP_X = 6;
    private static final int SCHEMATIC_GRID_GAP_Y = 8;

    // === フィラーモード用ボタン (4+3 = 7) ===
    private static final int FILLER_BTN_COUNT = 7;
    private static final String[] FILLER_BTN_NORMAL = {
        "btn_filler_fill_normal.png",
        "btn_filler_erase_normal.png",
        "btn_filler_remove_normal.png",
        "btn_filler_wall_normal.png",
        "btn_filler_tower_normal.png",
        "btn_filler_box_normal.png",
        "btn_filler_circle_wall_normal.png"
    };
    private static final String[] FILLER_BTN_HOVER = {
        "btn_filler_fill_hover.png",
        "btn_filler_erase_hover.png",
        "btn_filler_remove_hover.png",
        "btn_filler_wall_hover.png",
        "btn_filler_tower_hover.png",
        "btn_filler_box_hover.png",
        "btn_filler_circle_wall_hover.png"
    };
    private static final String[] FILLER_TOOLTIP_KEYS = {
        "gui.emcschematicannon.filler.fill",
        "gui.emcschematicannon.filler.erase",
        "gui.emcschematicannon.filler.remove",
        "gui.emcschematicannon.filler.wall",
        "gui.emcschematicannon.filler.tower",
        "gui.emcschematicannon.filler.box",
        "gui.emcschematicannon.filler.circle_wall"
    };
    // フィラーモード: 1段目4列、2段目3列
    private static final int FILLER_ROW1_COLS = 4;
    private static final int FILLER_GRID_MARGIN_X = 5;
    private static final int FILLER_GRID_MARGIN_Y = 11;
    private static final int FILLER_GRID_GAP_X = 6;
    private static final int FILLER_GRID_GAP_Y = 8;

    // テクスチャキャッシュ
    private final ResourceLocation[] schematicNormalTex = new ResourceLocation[SCHEMATIC_BTN_COUNT];
    private final ResourceLocation[] schematicHoverTex = new ResourceLocation[SCHEMATIC_BTN_COUNT];
    private final ResourceLocation[] fillerNormalTex = new ResourceLocation[FILLER_BTN_COUNT];
    private final ResourceLocation[] fillerHoverTex = new ResourceLocation[FILLER_BTN_COUNT];

    private boolean expanded = false;
    private float animationProgress = 0;
    private final int guiRight;
    private final int baseTabY;

    // 概略図モード設定
    private ReplaceMode replaceMode;
    private boolean skipMissing;
    private boolean protectBlockEntities;

    // フィラーモード設定
    private boolean fillerMode = false;
    private FillerModule fillerModule = FillerModule.FILL;

    private final Runnable onChanged;

    // Deferred tooltip
    private int hoveredButton = -1;
    private int hoveredMx, hoveredMy;

    public SettingsTabWidget(int guiRight, int tabY,
                              ReplaceMode initialMode, boolean skipMissing,
                              boolean protectBE, Runnable onChanged) {
        super(guiRight, tabY, TAB_SIZE, TAB_SIZE,
                Component.translatable("gui.emcschematicannon.settings"));
        this.guiRight = guiRight;
        this.baseTabY = tabY;
        this.replaceMode = initialMode;
        this.skipMissing = skipMissing;
        this.protectBlockEntities = protectBE;
        this.onChanged = onChanged;

        for (int i = 0; i < SCHEMATIC_BTN_COUNT; i++) {
            schematicNormalTex[i] = ResourceLocation.fromNamespaceAndPath(
                    EMCSchematicCannon.MOD_ID, "textures/gui/gen2/" + SCHEMATIC_BTN_NORMAL[i]);
            schematicHoverTex[i] = ResourceLocation.fromNamespaceAndPath(
                    EMCSchematicCannon.MOD_ID, "textures/gui/gen2/" + SCHEMATIC_BTN_HOVER[i]);
        }
        for (int i = 0; i < FILLER_BTN_COUNT; i++) {
            fillerNormalTex[i] = ResourceLocation.fromNamespaceAndPath(
                    EMCSchematicCannon.MOD_ID, "textures/gui/gen2/" + FILLER_BTN_NORMAL[i]);
            fillerHoverTex[i] = ResourceLocation.fromNamespaceAndPath(
                    EMCSchematicCannon.MOD_ID, "textures/gui/gen2/" + FILLER_BTN_HOVER[i]);
        }
    }

    public void setFillerMode(boolean filler) {
        this.fillerMode = filler;
    }

    public void setFillerModule(FillerModule module) {
        this.fillerModule = module;
    }

    public FillerModule getFillerModule() { return fillerModule; }

    public void tick() {
        float target = expanded ? 1.0f : 0.0f;
        animationProgress += (target - animationProgress) * 0.35f;
        if (Math.abs(animationProgress - target) < 0.05f) animationProgress = target;
    }

    private float ease(float t) {
        return t * t * (3 - 2 * t);
    }

    public int getExpandedHeight() {
        return expanded || animationProgress > 0.001f ? (int)(PANEL_HEIGHT * ease(animationProgress)) : 0;
    }

    public boolean isExpanded() { return expanded; }

    public void setCollapsed() {
        expanded = false;
    }

    // === ボタン位置計算 ===

    private int getCurrentButtonCount() {
        return fillerMode ? FILLER_BTN_COUNT : SCHEMATIC_BTN_COUNT;
    }

    private int getButtonX(int panelX, int index) {
        if (fillerMode) {
            int col;
            if (index < FILLER_ROW1_COLS) {
                col = index;
            } else {
                col = index - FILLER_ROW1_COLS;
            }
            return panelX + FILLER_GRID_MARGIN_X + col * (BUTTON_SIZE + FILLER_GRID_GAP_X);
        } else {
            int col = index % SCHEMATIC_GRID_COLS;
            return panelX + SCHEMATIC_GRID_MARGIN_X + col * (BUTTON_SIZE + SCHEMATIC_GRID_GAP_X);
        }
    }

    private int getButtonY(int panelY, int index) {
        if (fillerMode) {
            int row = index < FILLER_ROW1_COLS ? 0 : 1;
            return panelY + FILLER_GRID_MARGIN_Y + row * (BUTTON_SIZE + FILLER_GRID_GAP_Y);
        } else {
            int row = index / SCHEMATIC_GRID_COLS;
            return panelY + SCHEMATIC_GRID_MARGIN_Y + row * (BUTTON_SIZE + SCHEMATIC_GRID_GAP_Y);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0) return false;
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        if (mouseX >= guiRight && mouseX < guiRight + TAB_SIZE
                && mouseY >= baseTabY && mouseY < baseTabY + TAB_SIZE) {
            expanded = !expanded;
            return true;
        }
        if (animationProgress > 0.5f) {
            int panelX = guiRight;
            int panelY = baseTabY + TAB_SIZE;
            int count = getCurrentButtonCount();
            for (int i = 0; i < count; i++) {
                int btnX = getButtonX(panelX, i);
                int btnY = getButtonY(panelY, i);
                if (mouseX >= btnX && mouseX < btnX + BUTTON_SIZE
                        && mouseY >= btnY && mouseY < btnY + BUTTON_SIZE) {
                    handleOptionClick(i);
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (mouseX >= guiRight && mouseX < guiRight + TAB_SIZE
                && mouseY >= baseTabY && mouseY < baseTabY + TAB_SIZE) return true;
        if (animationProgress > 0.001f) {
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            if (mouseX >= guiRight && mouseX < guiRight + animatedWidth
                    && mouseY >= baseTabY && mouseY < baseTabY + totalHeight) return true;
        }
        return false;
    }

    private void handleOptionClick(int index) {
        if (fillerMode) {
            FillerModule[] modules = FillerModule.values();
            if (index >= 0 && index < modules.length) {
                fillerModule = modules[index];
            }
        } else {
            switch (index) {
                case 0 -> replaceMode = ReplaceMode.DONT_REPLACE;
                case 1 -> replaceMode = ReplaceMode.REPLACE_SOLID;
                case 2 -> replaceMode = ReplaceMode.REPLACE_ANY;
                case 3 -> replaceMode = ReplaceMode.REPLACE_EMPTY;
                case 4 -> skipMissing = !skipMissing;
                case 5 -> protectBlockEntities = !protectBlockEntities;
            }
        }
        onChanged.run();
    }

    private boolean isOptionActive(int index) {
        if (fillerMode) {
            FillerModule[] modules = FillerModule.values();
            return index >= 0 && index < modules.length && fillerModule == modules[index];
        }
        return switch (index) {
            case 0 -> replaceMode == ReplaceMode.DONT_REPLACE;
            case 1 -> replaceMode == ReplaceMode.REPLACE_SOLID;
            case 2 -> replaceMode == ReplaceMode.REPLACE_ANY;
            case 3 -> replaceMode == ReplaceMode.REPLACE_EMPTY;
            case 4 -> skipMissing;
            case 5 -> protectBlockEntities;
            default -> false;
        };
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
        hoveredButton = -1;

        if (animationProgress > 0.001f) {
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            graphics.enableScissor(guiRight, baseTabY, guiRight + animatedWidth, baseTabY + totalHeight);
            graphics.blit(TAB_EXPANDED, guiRight, baseTabY, 0, 0, PANEL_WIDTH, TAB_SIZE + PANEL_HEIGHT,
                    PANEL_WIDTH, TAB_SIZE + PANEL_HEIGHT);

            if (animationProgress > 0.5f) {
                int panelY = baseTabY + TAB_SIZE;
                renderPanelContent(graphics, guiRight, panelY, mouseX, mouseY);
            }
            graphics.disableScissor();

            if (mouseX >= guiRight && mouseX < guiRight + TAB_SIZE
                    && mouseY >= baseTabY && mouseY < baseTabY + TAB_SIZE) {
                graphics.fill(guiRight, baseTabY, guiRight + TAB_SIZE, baseTabY + TAB_SIZE, 0x30FFFFFF);
            }
        } else {
            graphics.blit(TAB_ICON, guiRight, baseTabY, 0, 0, TAB_SIZE, TAB_SIZE, TAB_SIZE, TAB_SIZE);
            if (mouseX >= guiRight && mouseX < guiRight + TAB_SIZE
                    && mouseY >= baseTabY && mouseY < baseTabY + TAB_SIZE) {
                graphics.fill(guiRight, baseTabY, guiRight + TAB_SIZE, baseTabY + TAB_SIZE, 0x30FFFFFF);
            }
        }

        if (hoveredButton >= 0) {
            String[] tooltipKeys = fillerMode ? FILLER_TOOLTIP_KEYS : SCHEMATIC_TOOLTIP_KEYS;
            if (hoveredButton < tooltipKeys.length) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable(tooltipKeys[hoveredButton]), hoveredMx, hoveredMy);
            }
        }
    }

    private void renderPanelContent(GuiGraphics graphics, int panelX, int panelY, int mx, int my) {
        int count = getCurrentButtonCount();
        ResourceLocation[] normalTex = fillerMode ? fillerNormalTex : schematicNormalTex;
        ResourceLocation[] hoverTex = fillerMode ? fillerHoverTex : schematicHoverTex;

        for (int i = 0; i < count; i++) {
            int btnX = getButtonX(panelX, i);
            int btnY = getButtonY(panelY, i);

            boolean active = isOptionActive(i);
            boolean hovered = mx >= btnX && mx < btnX + BUTTON_SIZE
                    && my >= btnY && my < btnY + BUTTON_SIZE;

            ResourceLocation tex = hovered ? hoverTex[i] : normalTex[i];
            graphics.blit(tex, btnX, btnY, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);

            if (active) {
                graphics.fill(btnX, btnY + BUTTON_SIZE - 2, btnX + BUTTON_SIZE, btnY + BUTTON_SIZE, 0xFF27AE60);
            }

            if (hovered) {
                hoveredButton = i;
                hoveredMx = mx;
                hoveredMy = my;
            }
        }
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        if (animationProgress > 0.001f) {
            float eased = ease(animationProgress);
            int animatedWidth = TAB_SIZE + (int)((PANEL_WIDTH - TAB_SIZE) * eased);
            int totalHeight = TAB_SIZE + (int)(PANEL_HEIGHT * eased);
            areas.add(new Rect2i(guiRight, baseTabY, animatedWidth, totalHeight));
        } else {
            areas.add(new Rect2i(guiRight, baseTabY, TAB_SIZE, TAB_SIZE));
        }
        return areas;
    }

    public ReplaceMode getReplaceMode() { return replaceMode; }
    public boolean isSkipMissing() { return skipMissing; }
    public boolean isProtectBlockEntities() { return protectBlockEntities; }

    public void updateFromServer(ReplaceMode mode, boolean skip, boolean protect) {
        this.replaceMode = mode;
        this.skipMissing = skip;
        this.protectBlockEntities = protect;
    }

    public void updateFillerFromServer(FillerModule module) {
        this.fillerModule = module;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
