package com.example.advancedschematicannon.gui;

import com.example.advancedschematicannon.ModRegistry;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * EMC概略図砲のメニュー（コンテナ）。
 * サーバー↔クライアント間でデータスロットを使い同期:
 *   0: エネルギー残量
 *   1: 進捗（設置済み）
 *   2: 進捗（合計）
 *   3: ステータス（State ordinal）
 *   4-5: EMC残量（long → int×2分割）
 */
public class EMCSchematicCannonMenu extends AbstractContainerMenu {
    public static final int DATA_SIZE = 13;

    // スロットインデックス
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_SCHEMATIC = 1;
    public static final int SLOT_OUTPUT = 2;
    private static final int PLAYER_INV_START = EMCSchematicCannonBlockEntity.TOTAL_SLOTS; // 55
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36; // 55 + 27 main + 9 hotbar

    private final EMCSchematicCannonBlockEntity blockEntity;
    private final ContainerData data;

    /**
     * サーバーサイドコンストラクタ。
     * BlockEntityから直接値を読み取るContainerDataを使用。
     */
    public EMCSchematicCannonMenu(int containerId, Inventory playerInv,
                                   EMCSchematicCannonBlockEntity be) {
        super(ModRegistry.EMC_CANNON_MENU.get(), containerId);
        this.blockEntity = be;

        // サーバー側: BlockEntityから直接データを読み取る
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> be.getEnergyStorage().getEnergyStored();
                    case 1 -> be.getPlacedBlocks();
                    case 2 -> be.getTotalBlocks();
                    case 3 -> be.getCannonState().ordinal();
                    case 4 -> (int)(be.getCachedPlayerEmc() & 0xFFFFFFFFL);
                    case 5 -> (int)(be.getCachedPlayerEmc() >>> 32);
                    case 6 -> be.getReplaceMode().ordinal();
                    case 7 -> (be.isSkipMissing() ? 1 : 0)
                            | (be.isSkipTileEntities() ? 2 : 0)
                            | (be.isUseEmc() ? 4 : 0)
                            | (be.isReuseSchematic() ? 8 : 0)
                            | (be.supportsEmc() ? 16 : 0);
                    case 8 -> be.getBlocksPerTick();
                    case 9 -> be.getStorageMode().ordinal();
                    case 10 -> be.isFillerMode() ? 1 : 0;
                    case 11 -> be.getFillerModule().ordinal();
                    case 12 -> be.isPreviewVisible() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // サーバー側では設定不要
            }

            @Override
            public int getCount() { return DATA_SIZE; }
        };

        addDataSlots(this.data);
        addBlockEntitySlots(be);
        addPlayerInventory(playerInv, 87, 174);
    }

    /**
     * クライアントサイドコンストラクタ（ネットワークから）。
     * SimpleContainerDataを使用してサーバーからの同期値を保持。
     */
    private EMCSchematicCannonMenu(int containerId, Inventory playerInv,
                                    EMCSchematicCannonBlockEntity be,
                                    ContainerData clientData) {
        super(ModRegistry.EMC_CANNON_MENU.get(), containerId);
        this.blockEntity = be;
        this.data = clientData;

        addDataSlots(this.data);
        addBlockEntitySlots(be);
        addPlayerInventory(playerInv, 87, 174);
    }

    /**
     * ネットワークからのファクトリメソッド。
     * クライアント側ではSimpleContainerDataを使用して、サーバーからの同期データを正しく受信する。
     */
    public static EMCSchematicCannonMenu fromNetwork(int containerId,
                                                      Inventory playerInv,
                                                      FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);

        // クライアント側はSimpleContainerDataを使用（set()で値を更新できる）
        SimpleContainerData clientData = new SimpleContainerData(DATA_SIZE);

        if (be instanceof EMCSchematicCannonBlockEntity cannon) {
            return new EMCSchematicCannonMenu(containerId, playerInv, cannon, clientData);
        }
        // フォールバック
        return new EMCSchematicCannonMenu(containerId, playerInv,
                new EMCSchematicCannonBlockEntity(pos,
                        ModRegistry.EMC_CANNON_BLOCK.get().defaultBlockState()),
                clientData);
    }

    /**
     * BlockEntityのアイテムスロットを追加する。
     */
    private void addBlockEntitySlots(EMCSchematicCannonBlockEntity be) {
        var handler = be.getItemHandler();
        // EMC燃料スロット — EMC対応BE(ProjectE導入+EMC砲)のみアクティブ化。強化型では非表示・非操作。
        addSlot(new SlotItemHandler(handler, EMCSchematicCannonBlockEntity.SLOT_FUEL, 87, 124) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EMCSchematicCannonMenu.this.supportsEmc() && super.mayPlace(stack);
            }

            @Override
            public boolean isActive() {
                return EMCSchematicCannonMenu.this.supportsEmc();
            }
        });
        // 概略図入力スロット / フィラーモード: 範囲指定ボードスロット (Gen2テクスチャ座標: slot内部 (123,124))
        addSlot(new SlotItemHandler(handler, EMCSchematicCannonBlockEntity.SLOT_SCHEMATIC, 123, 124));
        // 出力スロット (Gen2テクスチャ座標: slot内部 (231,124))
        addSlot(new SlotItemHandler(handler, EMCSchematicCannonBlockEntity.SLOT_OUTPUT, 231, 124) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 出力スロットはアイテムを入れられない
            }
        });
        // フィラーモード用アイテムスロット（材料リストエリア: 4列×13行）
        for (int row = 0; row < EMCSchematicCannonBlockEntity.FILLER_SLOT_ROWS; row++) {
            for (int col = 0; col < EMCSchematicCannonBlockEntity.FILLER_SLOT_COLS; col++) {
                int slotIndex = EMCSchematicCannonBlockEntity.SLOT_FILLER_START
                        + row * EMCSchematicCannonBlockEntity.FILLER_SLOT_COLS + col;
                int slotX = 9 + col * 18;
                int slotY = 16 + row * 18;
                addSlot(new SlotItemHandler(handler, slotIndex, slotX, slotY) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return EMCSchematicCannonMenu.this.isFillerMode()
                                && EMCSchematicCannonMenu.this.getFillerModule()
                                != EMCSchematicCannonBlockEntity.FillerModule.REMOVE;
                    }

                    @Override
                    public boolean isActive() {
                        return EMCSchematicCannonMenu.this.isFillerMode()
                                && EMCSchematicCannonMenu.this.getFillerModule()
                                != EMCSchematicCannonBlockEntity.FillerModule.REMOVE;
                    }
                });
            }
        }
    }

    private void addPlayerInventory(Inventory inv, int x, int y) {
        // プレイヤーインベントリ 3行
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        // ホットバー (y + 58: 3行×18 + 4pxギャップ = 58)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, x + col * 18, y + 58));
        }
    }

    // ===== データアクセサ =====
    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return EMCSchematicCannonBlockEntity.MAX_ENERGY; }
    public int getPlacedBlocks() { return data.get(1); }
    public int getTotalBlocks() { return data.get(2); }
    public EMCSchematicCannonBlockEntity.State getCannonState() {
        int ord = data.get(3);
        EMCSchematicCannonBlockEntity.State[] states = EMCSchematicCannonBlockEntity.State.values();
        return ord >= 0 && ord < states.length ? states[ord] : EMCSchematicCannonBlockEntity.State.IDLE;
    }
    public long getPlayerEmc() {
        long low = data.get(4) & 0xFFFFFFFFL;
        long high = data.get(5) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }
    public EMCSchematicCannonBlockEntity getBlockEntity() { return blockEntity; }

    public EMCSchematicCannonBlockEntity.ReplaceMode getReplaceMode() {
        int ord = data.get(6);
        EMCSchematicCannonBlockEntity.ReplaceMode[] modes = EMCSchematicCannonBlockEntity.ReplaceMode.values();
        return ord >= 0 && ord < modes.length ? modes[ord] : EMCSchematicCannonBlockEntity.ReplaceMode.REPLACE_ANY;
    }
    public boolean isSkipMissing() { return (data.get(7) & 1) != 0; }
    public boolean isSkipTileEntities() { return (data.get(7) & 2) != 0; }
    public boolean isUseEmc() { return (data.get(7) & 4) != 0; }
    public boolean isReuseSchematic() { return (data.get(7) & 8) != 0; }
    public boolean supportsEmc() { return (data.get(7) & 16) != 0; }
    public int getBlocksPerTick() { return data.get(8); }
    public EMCSchematicCannonBlockEntity.StorageMode getStorageMode() {
        int ord = data.get(9);
        EMCSchematicCannonBlockEntity.StorageMode[] modes = EMCSchematicCannonBlockEntity.StorageMode.values();
        return ord >= 0 && ord < modes.length ? modes[ord] : EMCSchematicCannonBlockEntity.StorageMode.AE_AND_CHEST;
    }

    public boolean isFillerMode() { return data.get(10) != 0; }
    public EMCSchematicCannonBlockEntity.FillerModule getFillerModule() {
        int ord = data.get(11);
        EMCSchematicCannonBlockEntity.FillerModule[] modules = EMCSchematicCannonBlockEntity.FillerModule.values();
        return ord >= 0 && ord < modules.length ? modules[ord] : EMCSchematicCannonBlockEntity.FillerModule.FILL;
    }
    public boolean isPreviewVisible() { return data.get(12) != 0; }

    public float getProgress() {
        int total = getTotalBlocks();
        return total > 0 ? (float) getPlacedBlocks() / total : 0f;
    }

    // Ctrl+クリックフラグ（EMC燃料スロットへのルーティング用）
    private boolean ctrlQuickMove = false;

    /**
     * Ctrl+クリック: button=2, QUICK_MOVEとして送信。
     * EMC燃料スロットへのみルーティングする。
     */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (clickType == net.minecraft.world.inventory.ClickType.QUICK_MOVE && button == 2) {
            // Ctrl+クリック: EMC燃料スロットへルーティング
            ctrlQuickMove = true;
            super.clicked(slotId, 0, clickType, player);
            ctrlQuickMove = false;
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();

            if (index < PLAYER_INV_START) {
                // BlockEntityスロット → プレイヤーインベントリ
                if (!moveItemStackTo(current, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (ctrlQuickMove) {
                // Ctrl+クリック: EMC燃料スロットのみ
                if (!moveItemStackTo(current, SLOT_FUEL, SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 通常シフトクリック: EMC燃料スロットを除外
                boolean moved = false;
                // フィラーモード時: まずフィラースロットを試す（範囲指定ボードは除外）
                if (isFillerMode() && !current.is(ModRegistry.RANGE_BOARD_ITEM.get())) {
                    int fillerEnd = EMCSchematicCannonBlockEntity.SLOT_FILLER_START
                            + EMCSchematicCannonBlockEntity.FILLER_SLOT_COUNT;
                    if (moveItemStackTo(current, EMCSchematicCannonBlockEntity.SLOT_FILLER_START, fillerEnd, false)) {
                        moved = true;
                    }
                }
                // 次に概略図スロットを試す
                if (!moved && moveItemStackTo(current, SLOT_SCHEMATIC, SLOT_SCHEMATIC + 1, false)) {
                    moved = true;
                }
                if (!moved) {
                    // インベントリ内の移動
                    if (index < PLAYER_INV_START + 27) {
                        // メインインベントリ → ホットバー
                        if (!moveItemStackTo(current, PLAYER_INV_START + 27, PLAYER_INV_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // ホットバー → メインインベントリ
                        if (!moveItemStackTo(current, PLAYER_INV_START, PLAYER_INV_START + 27, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null &&
                player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }
}
