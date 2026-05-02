package com.example.advancedschematicannon.gui;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity.StorageMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StorageToggleButton extends AbstractWidget {
    private static final ResourceLocation AE_CHEST_NORMAL = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_ae_chest_normal.png");
    private static final ResourceLocation AE_CHEST_HOVER = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_ae_chest_hover.png");
    private static final ResourceLocation AE_ONLY_NORMAL = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_ae_only_normal.png");
    private static final ResourceLocation AE_ONLY_HOVER = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_ae_only_hover.png");
    private static final ResourceLocation CHEST_ONLY_NORMAL = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_chest_only_normal.png");
    private static final ResourceLocation CHEST_ONLY_HOVER = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/btn_chest_only_hover.png");

    private StorageMode mode;
    private final boolean ae2Available;
    private final Runnable onChange;
    private boolean removalMode = false;

    public StorageToggleButton(int x, int y, StorageMode initialMode,
                                boolean ae2Available, Runnable onChange) {
        super(x, y, 18, 18, Component.empty());
        this.ae2Available = ae2Available;
        this.onChange = onChange;
        this.mode = ae2Available ? initialMode : StorageMode.CHEST_ONLY;
    }

    public void setRemovalMode(boolean removal) {
        this.removalMode = removal;
        // 撤去モード時はAE_AND_CHESTを使えない（AEに搬入/チェストに搬入の2択）
        if (removalMode && mode == StorageMode.AE_AND_CHEST) {
            mode = ae2Available ? StorageMode.AE_ONLY : StorageMode.CHEST_ONLY;
            onChange.run();
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!ae2Available) return;
        if (removalMode) {
            // 撤去モード: AE_ONLY ↔ CHEST_ONLY の2択
            mode = mode == StorageMode.AE_ONLY ? StorageMode.CHEST_ONLY : StorageMode.AE_ONLY;
        } else {
            mode = switch (mode) {
                case AE_AND_CHEST -> StorageMode.AE_ONLY;
                case AE_ONLY -> StorageMode.CHEST_ONLY;
                case CHEST_ONLY -> StorageMode.AE_AND_CHEST;
            };
        }
        onChange.run();
    }

    public StorageMode getMode() { return mode; }
    public void setMode(StorageMode mode) {
        this.mode = ae2Available ? mode : StorageMode.CHEST_ONLY;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
        boolean hovered = isHovered() && ae2Available;
        ResourceLocation tex = switch (mode) {
            case AE_AND_CHEST -> hovered ? AE_CHEST_HOVER : AE_CHEST_NORMAL;
            case AE_ONLY -> hovered ? AE_ONLY_HOVER : AE_ONLY_NORMAL;
            case CHEST_ONLY -> hovered ? CHEST_ONLY_HOVER : CHEST_ONLY_NORMAL;
        };
        graphics.blit(tex, getX(), getY(), 0, 0, 18, 18, 18, 18);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
