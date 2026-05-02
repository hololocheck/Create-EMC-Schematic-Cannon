package com.example.advancedschematicannon.gui;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity;
import com.example.advancedschematicannon.network.CannonActionPacket;
import com.example.advancedschematicannon.network.CannonSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class EMCSchematicCannonScreen extends AbstractContainerScreen<EMCSchematicCannonMenu> {

    // Gen2 Textures
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/gui_gen2.png");
    private static final ResourceLocation FE_BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/energybar.png");
    private static final ResourceLocation PROGRESS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/progressbar.png");
    private static final ResourceLocation EMC_USED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AdvancedSchematicCannon.MOD_ID, "textures/gui/gen2/emc_used.png");

    // GUI dimensions
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 256;

    // Energy bar (fill area inside border at x=86,y=33: inner fill x=89,y=36)
    private static final int FE_BAR_X = 89;
    private static final int FE_BAR_Y = 36;
    private static final int FE_BAR_W = 12;
    private static final int FE_BAR_H = 66;

    // Progress bar (inside arrow body x=140-229, y=124-139)
    private static final int PROGRESS_X = 144;
    private static final int PROGRESS_Y = 127;
    private static final int PROGRESS_W = 81;
    private static final int PROGRESS_H = 11;

    // Block list (material slots area) - 4 cols x 13 rows
    private static final int BLOCK_LIST_X = 9;
    private static final int BLOCK_LIST_Y = 16;
    private static final int BLOCK_LIST_COLS = 4;
    private static final int BLOCK_LIST_ROWS = 13;

    // Info panel text area (inside border of info box, inner fill starts at x=104)
    private static final int INFO_PANEL_X = 130;
    private static final int INFO_PANEL_Y = 38;

    // Buttons
    private static final int BTN_Y = 108;
    private static final int BTN_START_X = 156;

    // Tab base positions
    private static final int SETTINGS_TAB_Y_OFFSET = 10;
    private static final int SPEED_TAB_GAP = 4;

    // Scroll
    private int blockListScroll = 0;

    // Widgets
    private IconButton playButton;
    private IconButton pauseButton;
    private IconButton stopButton;
    private SettingsTabWidget settingsTab;
    private SpeedTabWidget speedTab;
    private InformationTabWidget infoTab;
    private ToggleButton emcToggle;
    private StorageToggleButton storageToggle;
    private ReuseToggleButton reuseToggle;
    private VisibilityToggleButton visibilityToggle;
    private ModeToggleButton modeToggle;

    private int syncCooldown = 0;

    public EMCSchematicCannonScreen(EMCSchematicCannonMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos;
        int y = topPos;

        // Play/Pause/Stop buttons (push/nopush textures)
        playButton = new IconButton(x + BTN_START_X, y + BTN_Y,
                "btn_play_normal.png", "btn_play_hover.png",
                Component.translatable("gui.advancedschematicannon.start"),
                b -> onPlayPressed());
        addRenderableWidget(playButton);

        pauseButton = new IconButton(x + BTN_START_X + 20, y + BTN_Y,
                "btn_pause_normal.png", "btn_pause_hover.png",
                Component.translatable("gui.advancedschematicannon.pause"),
                b -> onPausePressed());
        addRenderableWidget(pauseButton);

        stopButton = new IconButton(x + BTN_START_X + 40, y + BTN_Y,
                "btn_stop_normal.png", "btn_stop_hover.png",
                Component.translatable("gui.advancedschematicannon.stop"),
                b -> onStopPressed());
        addRenderableWidget(stopButton);

        // Settings tab (right side)
        int guiRight = x + GUI_WIDTH;
        int settingsTabY = y + SETTINGS_TAB_Y_OFFSET;
        settingsTab = new SettingsTabWidget(guiRight, settingsTabY,
                menu.getReplaceMode(), menu.isSkipMissing(), menu.isSkipTileEntities(),
                this::sendAllSettings);
        addRenderableWidget(settingsTab);

        // Speed tab (below settings tab, shifts when settings expanded)
        int speedTabY = settingsTabY + 26 + SPEED_TAB_GAP;
        speedTab = new SpeedTabWidget(guiRight, speedTabY,
                Math.max(1, menu.getBlocksPerTick()),
                this::sendAllSettings);
        addRenderableWidget(speedTab);

        // Information tab (left side)
        infoTab = new InformationTabWidget(x, y + SETTINGS_TAB_Y_OFFSET);
        addRenderableWidget(infoTab);

        // Toggle buttons (aligned with inventory slot backgrounds)
        visibilityToggle = new VisibilityToggleButton(x + 176, y + 148,
                menu.isPreviewVisible(), () -> {
            // クライアント側BEに即時反映（描画遅延を防止）
            EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
            if (be != null) be.setPreviewVisible(visibilityToggle.isToggled());
            sendAllSettings();
        });
        addRenderableWidget(visibilityToggle);

        reuseToggle = new ReuseToggleButton(x + 194, y + 148,
                menu.isReuseSchematic(), this::sendAllSettings);
        addRenderableWidget(reuseToggle);

        boolean ae2Available = menu.getBlockEntity() != null && menu.getBlockEntity().isAe2Available();
        storageToggle = new StorageToggleButton(x + 212, y + 148,
                menu.getStorageMode(), ae2Available, this::sendAllSettings);
        addRenderableWidget(storageToggle);

        if (menu.supportsEmc()) {
            emcToggle = new ToggleButton(x + 230, y + 148,
                    menu.isUseEmc(),
                    Component.translatable("gui.advancedschematicannon.emc_toggle"),
                    this::sendAllSettings);
            addRenderableWidget(emcToggle);
        } else {
            emcToggle = null;
        }

        // Mode toggle (right wall, below tabs)
        modeToggle = new ModeToggleButton(guiRight, y + GUI_HEIGHT - 30, this::onModeChanged);
        modeToggle.setFillerMode(menu.isFillerMode());
        addRenderableWidget(modeToggle);

        // 初期フィラーモード同期
        if (settingsTab != null) {
            settingsTab.setFillerMode(menu.isFillerMode());
            settingsTab.setFillerModule(menu.getFillerModule());
        }
    }

    private void onModeChanged() {
        if (settingsTab != null) {
            settingsTab.setFillerMode(modeToggle.isFillerMode());
            // モード変更時に設定タブを閉じる
            settingsTab.setCollapsed();
        }
        sendAllSettings();
    }

    private void sendAllSettings() {
        syncCooldown = 10;
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        PacketDistributor.sendToServer(new CannonSettingsPacket(
                be.getBlockPos(),
                CannonSettingsPacket.packModes(
                        settingsTab.getReplaceMode().ordinal(),
                        storageToggle.getMode().ordinal()),
                settingsTab.isSkipMissing(),
                settingsTab.isProtectBlockEntities(),
                emcToggle != null && emcToggle.isToggled(),
                CannonSettingsPacket.packSpeedAndFlags(
                        speedTab.getBlocksPerTick(),
                        reuseToggle.isToggled()),
                CannonSettingsPacket.packFillerModeAndModule(
                        modeToggle.isFillerMode(),
                        settingsTab.getFillerModule().ordinal(),
                        visibilityToggle.isToggled())
        ));
    }

    private void onPlayPressed() {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        var state = menu.getCannonState();
        CannonActionPacket.Action action = switch (state) {
            case IDLE, FINISHED, ERROR -> CannonActionPacket.Action.START;
            case PAUSED -> CannonActionPacket.Action.RESUME;
            default -> null;
        };
        if (action != null) {
            PacketDistributor.sendToServer(new CannonActionPacket(be.getBlockPos(), action));
        }
    }

    private void onPausePressed() {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        if (menu.getCannonState() == EMCSchematicCannonBlockEntity.State.RUNNING) {
            PacketDistributor.sendToServer(new CannonActionPacket(be.getBlockPos(), CannonActionPacket.Action.PAUSE));
        }
    }

    private void onStopPressed() {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        PacketDistributor.sendToServer(new CannonActionPacket(be.getBlockPos(), CannonActionPacket.Action.STOP));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        var state = menu.getCannonState();

        playButton.active = state == EMCSchematicCannonBlockEntity.State.IDLE
                || state == EMCSchematicCannonBlockEntity.State.FINISHED
                || state == EMCSchematicCannonBlockEntity.State.ERROR
                || state == EMCSchematicCannonBlockEntity.State.PAUSED;
        pauseButton.active = state == EMCSchematicCannonBlockEntity.State.RUNNING;
        stopButton.active = state == EMCSchematicCannonBlockEntity.State.RUNNING
                || state == EMCSchematicCannonBlockEntity.State.PAUSED;

        // Tick tab animations
        if (settingsTab != null) settingsTab.tick();
        if (speedTab != null) speedTab.tick();
        if (infoTab != null) infoTab.tick();

        // Mutual exclusion: settings and speed tabs
        if (settingsTab != null && speedTab != null) {
            if (settingsTab.isExpanded() && speedTab.isExpanded()) {
                speedTab.setCollapsed();
            }
            // Shift speed tab Y position based on settings tab expansion
            int baseSpeedY = topPos + SETTINGS_TAB_Y_OFFSET + 26 + SPEED_TAB_GAP;
            int expandedOffset = settingsTab.getExpandedHeight();
            speedTab.setTabY(baseSpeedY + expandedOffset);
        }

        // Server sync
        if (syncCooldown > 0) {
            syncCooldown--;
        } else {
            if (settingsTab != null) {
                settingsTab.updateFromServer(menu.getReplaceMode(), menu.isSkipMissing(), menu.isSkipTileEntities());
                settingsTab.setFillerMode(menu.isFillerMode());
                settingsTab.updateFillerFromServer(menu.getFillerModule());
            }
            if (emcToggle != null) emcToggle.setToggled(menu.isUseEmc());
            if (storageToggle != null) storageToggle.setMode(menu.getStorageMode());
            if (reuseToggle != null) reuseToggle.setToggled(menu.isReuseSchematic());
            if (visibilityToggle != null) visibilityToggle.setToggled(menu.isPreviewVisible());
            if (speedTab != null && menu.getBlocksPerTick() > 0) {
                speedTab.setBlocksPerTick(menu.getBlocksPerTick());
            }
            if (modeToggle != null) {
                modeToggle.setFillerMode(menu.isFillerMode());
            }
        }

        // 撤去モード状態をストレージボタンに反映
        if (storageToggle != null) {
            storageToggle.setRemovalMode(isRemovalMode());
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.blit(GUI_TEXTURE, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        renderFEBar(graphics, x, y);
        renderProgressBar(graphics, x, y);

        boolean inFillerMode = modeToggle != null && modeToggle.isFillerMode();
        boolean removalMode = inFillerMode && isRemovalMode();
        if (removalMode) {
            // 撤去モード: 概略図モードと同じブロック一覧表示（範囲内のブロック）
            renderBlockList(graphics, x, y, mouseX, mouseY);
        } else if (inFillerMode) {
            renderFillerItemSlotOverlay(graphics, x, y);
        } else {
            renderBlockList(graphics, x, y, mouseX, mouseY);
        }
        renderInfoPanel(graphics, x, y);
    }

    private void renderFEBar(GuiGraphics graphics, int x, int y) {
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (maxEnergy <= 0 || energy <= 0) return;

        float ratio = Math.min(1.0f, (float) energy / maxEnergy);
        int barHeight = (int) (FE_BAR_H * ratio);
        if (barHeight <= 0) return;

        int srcY = FE_BAR_H - barHeight;
        graphics.blit(FE_BAR_TEXTURE,
                x + FE_BAR_X, y + FE_BAR_Y + srcY,
                0, srcY,
                FE_BAR_W, barHeight,
                FE_BAR_W, FE_BAR_H);
    }

    private void renderProgressBar(GuiGraphics graphics, int x, int y) {
        float progress = menu.getProgress();
        if (progress <= 0) return;

        int barWidth = (int) (PROGRESS_W * progress);
        if (barWidth <= 0) return;

        graphics.blit(PROGRESS_TEXTURE,
                x + PROGRESS_X, y + PROGRESS_Y,
                0, 0,
                barWidth, PROGRESS_H,
                PROGRESS_W, PROGRESS_H);
    }

    private void renderBlockList(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        LinkedHashMap<String, Integer> summary = be.getBlockSummary();
        if (summary.isEmpty()) return;

        var entries = new ArrayList<>(summary.entrySet());
        int maxVisible = BLOCK_LIST_COLS * BLOCK_LIST_ROWS;
        int startIndex = blockListScroll * BLOCK_LIST_COLS;

        boolean inRemoval = isRemovalMode();
        for (int i = 0; i < maxVisible && (startIndex + i) < entries.size(); i++) {
            var entry = entries.get(startIndex + i);
            int col = i % BLOCK_LIST_COLS;
            int row = i / BLOCK_LIST_COLS;

            int slotX = x + BLOCK_LIST_X + col * 18;
            int slotY = y + BLOCK_LIST_Y + row * 18;

            ItemStack itemStack = getItemStackFromRegistryName(entry.getKey());
            if (!itemStack.isEmpty()) {
                // EMC indicator using EMCused.png texture (16x16)
                // 概略図モード: EMCを使用して設置 / 撤去モード: EMCに変換
                if (menu.isUseEmc() && hasEmcValue(itemStack)) {
                    graphics.blit(EMC_USED_TEXTURE,
                            slotX, slotY,
                            0, 0, 16, 16, 16, 16);
                }

                // Item icon
                graphics.renderItem(itemStack, slotX, slotY);

                // Count text overlay（撤去モードでは非表示: ツールチップで確認可能）
                if (!inRemoval) {
                    String countText = formatCount(entry.getValue());
                    int textWidth = font.width(countText);
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200);
                    graphics.drawString(font, countText,
                            slotX + 16 - textWidth, slotY + 8,
                            0xFFFFFFFF, true);
                    graphics.pose().popPose();
                }
            } else {
                String shortName = entry.getKey().contains(":") ?
                        entry.getKey().substring(entry.getKey().indexOf(':') + 1) : entry.getKey();
                if (shortName.length() > 4) shortName = shortName.substring(0, 3) + "~";
                graphics.drawString(font, shortName, slotX, slotY + 1, 0xFFAAAAAA, true);
                if (!inRemoval) {
                    String countText = formatCount(entry.getValue());
                    int textWidth = font.width(countText);
                    graphics.drawString(font, countText, slotX + 16 - textWidth, slotY + 8, 0xFFFFFFFF, true);
                }
            }
        }
    }

    /** フィラーモード時: スロットは自動描画されるので追加描画不要 */
    private void renderFillerItemSlotOverlay(GuiGraphics graphics, int x, int y) {
        // フィラースロットはSlotItemHandlerとして自動描画される
        // 追加のオーバーレイは不要
    }

    private void renderInfoPanel(GuiGraphics graphics, int x, int y) {
        int px = x + INFO_PANEL_X;
        int py = y + INFO_PANEL_Y;
        int lineHeight = 10;
        int maxTextWidth = 140;
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();

        // Scissor clip to info box bounds (inside border, inner fill x=104+)
        graphics.enableScissor(x + 104, y + 36, x + 244, y + 101);

        // Status
        var state = menu.getCannonState();
        String statusKey = switch (state) {
            case IDLE -> "gui.advancedschematicannon.status.idle";
            case RUNNING -> "gui.advancedschematicannon.status.running";
            case PAUSED -> "gui.advancedschematicannon.status.paused";
            case FINISHED -> "gui.advancedschematicannon.status.finished";
            case ERROR -> "gui.advancedschematicannon.status.error";
        };
        int statusColor = switch (state) {
            case IDLE -> 0xFFAAAAAA;
            case RUNNING -> 0xFF27AE60;
            case PAUSED -> 0xFFF39C12;
            case FINISHED -> 0xFF2ECC71;
            case ERROR -> 0xFFE74C3C;
        };
        graphics.drawString(font, Component.translatable(statusKey), px, py, statusColor, false);
        py += lineHeight;

        // フィラーモード表示
        if (modeToggle != null && modeToggle.isFillerMode() && settingsTab != null) {
            String moduleKey = "gui.advancedschematicannon.filler." + settingsTab.getFillerModule().name().toLowerCase();
            graphics.drawString(font, Component.translatable(moduleKey), px, py, 0xFF55DDFF, false);
            py += lineHeight;
        }

        // EMC used — EMC対応砲のみ表示
        if (menu.supportsEmc() && be != null && be.getTotalEmcUsed() > 0) {
            String emcUsedText = "EMC Used: " + formatNumber(be.getTotalEmcUsed());
            graphics.drawString(font, truncateText(emcUsedText, maxTextWidth), px, py, 0xFFE67E22, false);
            py += lineHeight;
        }

        // Progress
        int placed = menu.getPlacedBlocks();
        int total = menu.getTotalBlocks();
        if (total > 0) {
            String progressText = String.format("%d/%d (%.0f%%)", placed, total, menu.getProgress() * 100);
            graphics.drawString(font, progressText, px, py, 0xFF55DDFF, false);
            py += lineHeight;
        }

        // Remaining
        int remaining = total - placed;
        if (remaining > 0) {
            String remainText = Component.translatable("gui.advancedschematicannon.remaining", remaining).getString();
            graphics.drawString(font, truncateText(remainText, maxTextWidth), px, py, 0xFFF0C040, false);
            py += lineHeight;
        }

        // Missing block
        if (be != null) {
            String missingBlock = be.getMissingBlockName();
            if (missingBlock != null && !missingBlock.isEmpty()) {
                String label = Component.translatable("gui.advancedschematicannon.missing").getString();
                graphics.drawString(font, label, px, py + 4, 0xFFE74C3C, false);
                ItemStack missingItem = getItemStackFromRegistryName(missingBlock);
                int labelWidth = font.width(label);
                if (!missingItem.isEmpty()) {
                    graphics.renderItem(missingItem, px + labelWidth + 2, py);
                } else {
                    String shortName = missingBlock.contains(":") ?
                            missingBlock.substring(missingBlock.indexOf(':') + 1) : missingBlock;
                    graphics.drawString(font, shortName, px + labelWidth + 2, py + 4, 0xFFFF6666, false);
                }
            }
        }

        graphics.disableScissor();

        // EMC holdings (above inventory label, not inside info box) — EMC対応砲のみ表示
        if (menu.supportsEmc()) {
            long emc = menu.getPlayerEmc();
            String emcText = "EMC: " + formatNumber(emc);
            graphics.drawString(font, emcText, x + 87, y + 148, 0xFF9B59B6, false);
        }
    }

    private String truncateText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (font.width(text + "...") > maxWidth && text.length() > 3) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // FE bar tooltip
        int feX = leftPos + FE_BAR_X;
        int feY = topPos + FE_BAR_Y;
        if (mouseX >= feX && mouseX <= feX + FE_BAR_W
                && mouseY >= feY && mouseY <= feY + FE_BAR_H) {
            graphics.renderTooltip(font,
                    Component.literal(formatNumber(menu.getEnergy()) + " / " + formatNumber(menu.getMaxEnergy()) + " FE"),
                    mouseX, mouseY);
        }

        boolean inFillerMode = modeToggle != null && modeToggle.isFillerMode();
        boolean removalMode = inFillerMode && isRemovalMode();
        if (!inFillerMode || removalMode) {
            renderBlockListTooltips(graphics, mouseX, mouseY);
        }
        renderToggleTooltips(graphics, mouseX, mouseY);
    }

    private boolean isRemovalMode() {
        return modeToggle != null && modeToggle.isFillerMode()
                && settingsTab != null && settingsTab.getFillerModule() == EMCSchematicCannonBlockEntity.FillerModule.REMOVE;
    }

    private void renderToggleTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (visibilityToggle != null && visibilityToggle.isHovered()) {
            String key = visibilityToggle.isToggled()
                    ? "gui.advancedschematicannon.visibility.on"
                    : "gui.advancedschematicannon.visibility.off";
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
        } else if (reuseToggle != null && reuseToggle.isHovered()) {
            String key = reuseToggle.isToggled()
                    ? "gui.advancedschematicannon.reuse.on"
                    : "gui.advancedschematicannon.reuse.off";
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
        } else if (storageToggle != null && storageToggle.isHovered()) {
            if (isRemovalMode()) {
                // 撤去モード: 搬入先として表示
                String key = switch (storageToggle.getMode()) {
                    case AE_ONLY -> "gui.advancedschematicannon.storage.insert_ae";
                    case CHEST_ONLY -> "gui.advancedschematicannon.storage.insert_chest";
                    default -> "gui.advancedschematicannon.storage.insert_chest";
                };
                graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
            } else {
                String key = switch (storageToggle.getMode()) {
                    case AE_AND_CHEST -> "gui.advancedschematicannon.storage.ae_chest";
                    case AE_ONLY -> "gui.advancedschematicannon.storage.ae_only";
                    case CHEST_ONLY -> "gui.advancedschematicannon.storage.chest_only";
                };
                graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
            }
        } else if (emcToggle != null && emcToggle.isHovered()) {
            if (isRemovalMode()) {
                String key = emcToggle.isToggled()
                        ? "gui.advancedschematicannon.emc_convert"
                        : "gui.advancedschematicannon.emc_convert_off";
                graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
            } else {
                graphics.renderTooltip(font, emcToggle.getMessage(), mouseX, mouseY);
            }
        } else if (modeToggle != null && modeToggle.isHovered()) {
            String key = modeToggle.isFillerMode()
                    ? "gui.advancedschematicannon.mode.filler"
                    : "gui.advancedschematicannon.mode.schematic";
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
        }
    }

    private void renderBlockListTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        LinkedHashMap<String, Integer> summary = be.getBlockSummary();
        if (summary.isEmpty()) return;

        var entries = new ArrayList<>(summary.entrySet());
        int maxVisible = BLOCK_LIST_COLS * BLOCK_LIST_ROWS;
        int startIndex = blockListScroll * BLOCK_LIST_COLS;

        for (int i = 0; i < maxVisible && (startIndex + i) < entries.size(); i++) {
            var entry = entries.get(startIndex + i);
            int col = i % BLOCK_LIST_COLS;
            int row = i / BLOCK_LIST_COLS;

            int slotX = leftPos + BLOCK_LIST_X + col * 18;
            int slotY = topPos + BLOCK_LIST_Y + row * 18;

            if (mouseX >= slotX && mouseX < slotX + 18
                    && mouseY >= slotY && mouseY < slotY + 18) {
                ItemStack itemStack = getItemStackFromRegistryName(entry.getKey());
                List<Component> tooltip = new ArrayList<>();
                if (!itemStack.isEmpty()) {
                    tooltip.add(itemStack.getHoverName());
                } else {
                    tooltip.add(Component.literal(entry.getKey())
                            .withStyle(net.minecraft.ChatFormatting.YELLOW));
                }
                tooltip.add(Component.literal(entry.getValue() + "x")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                if (!itemStack.isEmpty() && menu.isUseEmc() && hasEmcValue(itemStack)) {
                    String emcKey = isRemovalMode()
                            ? "gui.advancedschematicannon.emc_converted"
                            : "gui.advancedschematicannon.emc_placed";
                    tooltip.add(Component.translatable(emcKey)
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                }
                graphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // タイトルを中央に配置
        int titleWidth = font.width(this.title);
        graphics.drawString(font, this.title, (GUI_WIDTH - titleWidth) / 2, 5, 0xFF404040, false);

        // フィラーモード: タイトルの元の位置（左上）に「材料スロット」を表示（撤去モード以外）
        boolean inFillerMode = modeToggle != null && modeToggle.isFillerMode();
        if (inFillerMode && !isRemovalMode()) {
            graphics.drawString(font,
                    Component.translatable("gui.advancedschematicannon.filler.item_slot"),
                    8, 5, 0xFF404040, false);
        }

        graphics.drawString(font, this.playerInventoryTitle, 87, 163, 0xFF404040, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (speedTab != null && speedTab.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (infoTab != null && infoTab.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        boolean inFillerMode = modeToggle != null && modeToggle.isFillerMode();
        if (!inFillerMode || isRemovalMode()) {
            int bx = leftPos + BLOCK_LIST_X;
            int by = topPos + BLOCK_LIST_Y;
            int bw = BLOCK_LIST_COLS * 18;
            int bh = BLOCK_LIST_ROWS * 18;
            if (mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh) {
                EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
                if (be != null) {
                    int totalEntries = be.getBlockSummary().size();
                    int maxScroll = Math.max(0, (totalEntries - 1) / BLOCK_LIST_COLS - BLOCK_LIST_ROWS + 1);
                    blockListScroll = Math.max(0, Math.min(maxScroll,
                            blockListScroll - (int) scrollY));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // Handle mutual exclusion of settings and speed tabs + Ctrl+click for EMC fuel slot
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Ctrl+左クリック: EMC燃料スロットへの搬入 (EMC対応砲のみ)
        if (menu.supportsEmc() && button == 0 && hasControlDown() && !hasShiftDown()) {
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                // button=2をQUICK_MOVEとして送信（Menuのclickedでctrlフラグを設定）
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 2,
                        net.minecraft.world.inventory.ClickType.QUICK_MOVE);
                return true;
            }
        }

        // Check if settings tab is being clicked to expand
        if (settingsTab != null && !settingsTab.isExpanded()) {
            int guiRight = leftPos + GUI_WIDTH;
            int tabY = topPos + SETTINGS_TAB_Y_OFFSET;
            if (mouseX >= guiRight && mouseX < guiRight + 26
                    && mouseY >= tabY && mouseY < tabY + 26) {
                // Settings tab will expand - close speed tab
                if (speedTab != null && speedTab.isExpanded()) {
                    speedTab.setCollapsed();
                }
            }
        }
        // Check if speed tab is being clicked to expand
        if (speedTab != null && !speedTab.isExpanded()) {
            int guiRight = leftPos + GUI_WIDTH;
            if (mouseX >= guiRight && mouseX < guiRight + 26
                    && mouseY >= speedTab.getY() && mouseY < speedTab.getY() + 26) {
                // Speed tab will expand - close settings tab
                if (settingsTab != null && settingsTab.isExpanded()) {
                    settingsTab.setCollapsed();
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        if (settingsTab != null) areas.addAll(settingsTab.getExclusionAreas());
        if (speedTab != null) areas.addAll(speedTab.getExclusionAreas());
        if (infoTab != null) areas.addAll(infoTab.getExclusionAreas());
        if (modeToggle != null) {
            areas.add(new Rect2i(modeToggle.getX(), modeToggle.getY(), 26, 26));
        }
        return areas;
    }

    private static ItemStack getItemStackFromRegistryName(String registryName) {
        try {
            ResourceLocation rl = ResourceLocation.parse(registryName);
            var blockOpt = BuiltInRegistries.BLOCK.getOptional(rl);
            if (blockOpt.isPresent()) {
                ItemStack stack = new ItemStack(blockOpt.get().asItem());
                if (!stack.isEmpty() && stack.getItem() != Items.AIR) return stack;
            }
            if (BuiltInRegistries.ITEM.containsKey(rl)) {
                var item = BuiltInRegistries.ITEM.get(rl);
                if (item != Items.AIR) return new ItemStack(item);
            }
        } catch (Exception e) { }
        return ItemStack.EMPTY;
    }

    private static boolean hasEmcValue(ItemStack stack) {
        return com.example.advancedschematicannon.integration.ProjectEBridge.hasEmcValue(stack);
    }

    private static String formatCount(int count) {
        if (count >= 1000) return String.format("%.1fK", count / 1000.0);
        return String.valueOf(count);
    }

    private static String formatNumber(long num) {
        if (num >= 1_000_000_000) return String.format("%.1fB", num / 1_000_000_000.0);
        if (num >= 1_000_000) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }
}
