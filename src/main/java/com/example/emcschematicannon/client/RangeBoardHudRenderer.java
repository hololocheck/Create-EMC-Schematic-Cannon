package com.example.emcschematicannon.client;

import com.example.emcschematicannon.EMCSchematicCannon;
import com.example.emcschematicannon.item.ModDataComponents;
import com.example.emcschematicannon.item.RangeBoardItem;
import com.example.emcschematicannon.network.RangeBoardEditPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 範囲指定ボードのHUDオーバーレイ。
 * Station Sound System のロジックをベースに移植。
 *
 * Mode 0: 通常範囲指定 - 2点クリックで範囲設定、Shift+右クリックで全解除
 * Mode 1: 編集モード - Shift+スクロールで点を選択、Shift+右クリックで選択した点のみ解除
 */
@EventBusSubscriber(modid = EMCSchematicCannon.MOD_ID, value = Dist.CLIENT)
public class RangeBoardHudRenderer {

    // -------- Textures --------
    private static final ResourceLocation PANEL_BG =
            ResourceLocation.fromNamespaceAndPath(EMCSchematicCannon.MOD_ID, "textures/gui/range_board_panel.png");
    private static final ResourceLocation[] MODE_ICONS = {
            ResourceLocation.fromNamespaceAndPath(EMCSchematicCannon.MOD_ID, "textures/gui/range_mode_normal.png"),
            ResourceLocation.fromNamespaceAndPath(EMCSchematicCannon.MOD_ID, "textures/gui/range_mode_edit.png"),
    };

    // Bar & Panel: 192x32
    private static final int BAR_W = 192, BAR_H = 32;
    // Icon: 10x10
    private static final int ICON_W = 10, ICON_H = 10;
    // 2 icons in 192px bar: each in 96px section, centered
    private static final int[] ICON_X_OFF = {43, 139};
    private static final int ICON_Y_OFF = 11;
    private static final int ICON_LIFT = 4;

    private static final int MODE_COUNT = 2;

    // -------- Client-side state --------
    public static int currentMode = 0;
    /** 編集モード時の編集対象: 0=なし, 1=Pos1, 2=Pos2 */
    public static int editTarget = 0;

    private static float altProgress = 0f;
    private static long lastNano = System.nanoTime();

    private static float descScrollOffset = 0f;
    private static int lastAltMode = -1;

    // -------- Mode info --------
    private static final String[] MODE_KEYS = {
            "message.emcschematicannon.mode_normal",
            "message.emcschematicannon.mode_edit"
    };
    private static final String[] DESC_KEYS = {
            "message.emcschematicannon.mode_normal_desc",
            "message.emcschematicannon.mode_edit_desc"
    };

    // -------- Rendering --------

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        ItemStack stack = RangeBoardItem.findHeldRangeBoard(mc.player);
        if (stack.isEmpty()) {
            altProgress = 0f;
            lastNano = System.nanoTime();
            return;
        }

        long now = System.nanoTime();
        float dt = (now - lastNano) / 1_000_000_000f;
        lastNano = now;
        boolean altHeld = Screen.hasAltDown();
        if (altHeld) {
            altProgress = Math.min(1f, altProgress + dt * 5f);
        } else {
            altProgress = Math.max(0f, altProgress - dt * 5f);
        }

        GuiGraphics gui = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        int barX = (sw - BAR_W) / 2;
        int altPanelVisibleY = sh - 22 - 4 - BAR_H;
        int altPanelHiddenY = sh + BAR_H;
        int barY = altPanelVisibleY - 4 - BAR_H;

        // -- Alt panel (slides in from below) --
        if (altProgress > 0f) {
            int panelY = (int) (altPanelHiddenY + (altPanelVisibleY - altPanelHiddenY) * altProgress);
            renderBar(gui, barX, panelY);

            String desc = Component.translatable(DESC_KEYS[currentMode]).getString();
            int descW = font.width(desc);
            int maxW = BAR_W - 8;
            int descY = panelY + (BAR_H - font.lineHeight) / 2;

            if (descW <= maxW) {
                descScrollOffset = 0f;
                lastAltMode = currentMode;
                gui.drawCenteredString(font, desc, sw / 2, descY, 0xCCCCCC);
            } else {
                if (lastAltMode != currentMode) {
                    lastAltMode = currentMode;
                    descScrollOffset = 0f;
                }
                descScrollOffset += 40f * dt;
                float totalScroll = descW + maxW;
                if (descScrollOffset > totalScroll) descScrollOffset = 0f;

                int clipX1 = barX + 4;
                int clipX2 = barX + BAR_W - 4;
                int textX = clipX2 - (int) descScrollOffset;
                gui.enableScissor(clipX1, panelY, clipX2, panelY + BAR_H);
                gui.drawString(font, desc, textX, descY, 0xCCCCCC);
                gui.disableScissor();
            }
        } else {
            descScrollOffset = 0f;
            lastAltMode = -1;
        }

        // -- Main bar (always visible) --
        renderBar(gui, barX, barY);

        // Icons: all displayed, selected one is lifted
        for (int i = 0; i < MODE_COUNT; i++) {
            int ix = barX + ICON_X_OFF[i];
            int iy = barY + ICON_Y_OFF - (i == currentMode ? ICON_LIFT : 0);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.blit(MODE_ICONS[i], ix, iy, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
            RenderSystem.disableBlend();
        }

        // Selected mode name under selected icon
        int nameInsideY = barY + BAR_H - font.lineHeight - 2;
        int nameCx = barX + ICON_X_OFF[currentMode] + ICON_W / 2;
        gui.drawCenteredString(font, Component.translatable(MODE_KEYS[currentMode]).getString(),
                nameCx, nameInsideY, 0xFFFFFF);

        // Item name above bar
        int aboveBarY = barY - font.lineHeight - 2;
        gui.drawCenteredString(font, stack.getHoverName(), sw / 2, aboveBarY, 0xFFFFFF);

        // 編集モード時: 編集対象の表示
        if (currentMode == 1 && editTarget > 0) {
            String editInfo = Component.translatable(
                    editTarget == 1 ? "message.emcschematicannon.edit_pos1"
                            : "message.emcschematicannon.edit_pos2").getString();
            int editY = aboveBarY - font.lineHeight - 2;
            gui.drawCenteredString(font, editInfo, sw / 2, editY, 0xFFA500);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!RangeBoardItem.findHeldRangeBoard(mc.player).isEmpty()) {
            event.setCanceled(true);
        }
    }

    private static void renderBar(GuiGraphics gui, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gui.blit(PANEL_BG, x, y, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);
        RenderSystem.disableBlend();
    }

    // -------- Scroll input --------

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        ItemStack stack = RangeBoardItem.findHeldRangeBoard(mc.player);
        if (stack.isEmpty()) return;

        boolean altHeld = Screen.hasAltDown();
        boolean shiftHeld = Screen.hasShiftDown();
        double delta = event.getScrollDeltaY();
        if (delta == 0) return;

        if (altHeld) {
            // Alt + Scroll: モード切替
            int dir = delta > 0 ? -1 : 1;
            currentMode = ((currentMode + dir) % MODE_COUNT + MODE_COUNT) % MODE_COUNT;
            // モード変更時に編集対象をリセット
            if (currentMode != 1) {
                editTarget = 0;
                syncEditMode(stack, 0);
            }
            event.setCanceled(true);

        } else if (shiftHeld && currentMode == 1) {
            // 編集モード + Shift + Scroll: 編集対象を切り替え
            int dir = delta > 0 ? -1 : 1;
            editTarget = ((editTarget + dir) % 3 + 3) % 3;
            syncEditMode(stack, editTarget);
            event.setCanceled(true);
        }
    }

    // -------- Left-click handler --------

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!event.isAttack()) return;
        if (!Screen.hasShiftDown()) return;

        ItemStack stack = RangeBoardItem.findHeldRangeBoard(mc.player);
        if (stack.isEmpty()) return;

        if (currentMode == 1 && editTarget > 0) {
            editTarget = 0;
            syncEditMode(stack, 0);
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static void syncEditMode(ItemStack stack, int mode) {
        if (mode == 0) {
            stack.remove(ModDataComponents.RANGE_EDIT_MODE.get());
        } else {
            stack.set(ModDataComponents.RANGE_EDIT_MODE.get(), mode);
        }
        PacketDistributor.sendToServer(new RangeBoardEditPacket(mode));
    }
}
