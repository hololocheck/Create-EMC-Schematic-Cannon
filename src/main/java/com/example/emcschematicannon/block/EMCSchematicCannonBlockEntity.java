package com.example.emcschematicannon.block;

import com.example.emcschematicannon.EMCSchematicCannon;
import com.example.emcschematicannon.ModRegistry;
import com.example.emcschematicannon.gui.EMCSchematicCannonMenu;
import com.example.emcschematicannon.integration.AE2Integration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EMCSchematicCannonBlockEntity extends BlockEntity implements MenuProvider {

    public static final int MAX_ENERGY = 100_000;
    public static final int FE_PER_BLOCK = 500;
    public static final int BLOCKS_PER_TICK = 256;
    private static final int MAX_AIR_VOLUME_PLACEMENTS = 50_000;

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_SCHEMATIC = 1;  // 概略図モード: 概略図, フィラーモード: 範囲指定ボード
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_FILLER_START = 3; // フィラーモード用: アイテム搬入スロット開始
    public static final int FILLER_SLOT_COLS = 4;
    public static final int FILLER_SLOT_ROWS = 13;
    public static final int FILLER_SLOT_COUNT = FILLER_SLOT_COLS * FILLER_SLOT_ROWS; // 52スロット
    public static final int TOTAL_SLOTS = SLOT_FILLER_START + FILLER_SLOT_COUNT; // 55

    /** extractEnergy()は外部消費用でextract=0のため使えない。直接fieldアクセスで内部消費する。 */
    private static final java.lang.reflect.Field ENERGY_FIELD;
    static {
        try {
            ENERGY_FIELD = EnergyStorage.class.getDeclaredField("energy");
            ENERGY_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access EnergyStorage.energy field", e);
        }
    }

    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY, MAX_ENERGY, 0, 0);

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == SLOT_SCHEMATIC) {
                schematicSlotChanged = true;
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_FUEL) return hasEmcValue(stack);
            if (slot == SLOT_SCHEMATIC) {
                if (fillerMode) return isRangeBoardItem(stack);
                else return isSchematicItem(stack);
            }
            if (slot == SLOT_OUTPUT) return false;
            if (slot >= SLOT_FILLER_START && slot < SLOT_FILLER_START + FILLER_SLOT_COUNT) {
                return fillerMode; // フィラーモード時のみアイテム受け入れ
            }
            return false;
        }
    };

    public enum State { IDLE, RUNNING, PAUSED, FINISHED, ERROR }

    private State state = State.IDLE;
    private UUID ownerUUID = null;

    private final List<BlockPlacement> pendingPlacements = new ArrayList<>();
    private int totalBlocks = 0;
    private int placedBlocks = 0;

    private final LinkedHashMap<String, Integer> blockSummary = new LinkedHashMap<>();
    // 撤去モード用: 範囲内の既存ブロックサマリー（loadPlacements後にblockSummaryにコピー）
    private final LinkedHashMap<String, Integer> removeSummary = new LinkedHashMap<>();

    private long cachedPlayerEmc = 0;
    private boolean ae2Available = false;

    // AE2グリッドノード（ケーブル接続用、AE2存在時のみ非null）
    private Object ae2GridNodeManager = null;

    // AE2グリッドノード初期化リトライ用カウンター（マルチプレイでのタイミング問題対策）
    private int ae2RetryCounter = 0;
    private static final int AE2_RETRY_INTERVAL = 20; // 1秒ごとにリトライ
    private static final int AE2_MAX_RETRIES = 10;    // 最大10回リトライ
    private int ae2RetryCount = 0;

    // スケマティック挿入検知
    private boolean schematicSlotChanged = false;

    // 設置中のブロック名
    private String currentBlockRegistryName = "";

    // 内部EMCバッファ（燃料スロットから変換）
    private long internalEmcBuffer = 0;

    // 不足ブロック名（設置中断時にGUIに表示）
    private String missingBlockName = "";

    // EMC使用量（現在のジョブ全体の累計）
    private long totalEmcUsed = 0;

    // 設定
    private ReplaceMode replaceMode = ReplaceMode.REPLACE_ANY;
    private boolean skipMissing = false;
    private boolean skipTileEntities = false;
    private boolean useEmc = true;
    private StorageMode storageMode = StorageMode.AE_AND_CHEST;
    private boolean reuseSchematic = false;
    private int blocksPerTick = BLOCKS_PER_TICK;

    // フィラーモード設定
    private boolean fillerMode = false;
    private FillerModule fillerModule = FillerModule.FILL;
    // フィラーモード用: 範囲指定ボードから読み取った範囲
    private BlockPos fillerPos1 = null;
    private BlockPos fillerPos2 = null;
    // (フィラーモード用アイテムはSLOT_FILLER_START～の52スロットで管理)

    // プレビュー表示/非表示設定
    private boolean previewVisible = false;

    // クライアント同期レート制限
    private int syncCounter = 0;

    // パフォーマンス: tick毎にキャッシュされるインベントリハンドラー
    private static final Direction[] DIRECTIONS = Direction.values();
    private final List<IItemHandler> cachedInventoryHandlers = new ArrayList<>(6);

    // AE2 cable_bus判定用キャッシュ（レジストリ検索を毎ブロックで回避）
    private static final ResourceLocation CABLE_BUS_ID = ResourceLocation.parse("ae2:cable_bus");


    // アニメーション用（クライアント側）
    private BlockPos currentTarget = null;
    private float prevYaw = 0, prevPitch = 40f;
    private float currentYaw = 0, currentPitch = 40f;
    private long lastAnimNano = 0;

    public enum ReplaceMode {
        DONT_REPLACE,        // 固体ブロックを置き換えない
        REPLACE_SOLID,       // 固体を固体で置き換える
        REPLACE_ANY,         // 固体を任意のブロックに置き換える
        REPLACE_EMPTY        // 固体を空気に置き換える
    }

    /** ストレージ取り出し元モード */
    public enum StorageMode {
        AE_AND_CHEST,  // AE2 + チェスト両方から取り出し
        AE_ONLY,       // AE2のみ（チェスト無視）
        CHEST_ONLY     // チェストのみ（AE2無視）
    }

    /** フィラーモジュール */
    public enum FillerModule {
        FILL,          // 埋め立て
        ERASE,         // 完全消去
        REMOVE,        // 撤去
        WALL,          // 壁作成
        TOWER,         // タワー
        BOX,           // 箱作成
        CIRCLE_WALL    // 円壁作成
    }

    public EMCSchematicCannonBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.EMC_CANNON_BE.get(), pos, state);
        try {
            Class.forName("appeng.api.networking.IInWorldGridNodeHost");
            ae2Available = true;
        } catch (ClassNotFoundException e) {
            ae2Available = false;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (ae2Available && level != null && !level.isClientSide()) {
            tryInitAe2GridNode();
        }
    }

    /**
     * AE2グリッドノードの初期化を試みる。
     * マルチプレイではチャンクロード順やAE2初期化タイミングにより
     * onLoad()時に失敗する場合があるため、リトライ可能にする。
     */
    private void tryInitAe2GridNode() {
        if (ae2GridNodeManager != null) return; // 既に初期化済み
        try {
            var manager = new com.example.emcschematicannon.integration.AE2GridNodeManager();
            manager.create(level, worldPosition);
            ae2GridNodeManager = manager;
            ae2RetryCount = 0;
            EMCSchematicCannon.LOGGER.debug("AE2 grid node created at {}", worldPosition);
        } catch (Exception | NoClassDefFoundError e) {
            // ae2Availableはfalseにしない（mod自体は存在するので、リトライで成功する可能性がある）
            EMCSchematicCannon.LOGGER.warn("Failed to create AE2 grid node at {} (attempt {}): {}",
                    worldPosition, ae2RetryCount + 1, e.getMessage());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (ae2GridNodeManager != null) {
            try {
                ((com.example.emcschematicannon.integration.AE2GridNodeManager) ae2GridNodeManager).destroy();
            } catch (Exception | NoClassDefFoundError e) { }
            ae2GridNodeManager = null;
        }
    }

    /**
     * AE2ケーブル接続用のグリッドノードホストを返す。
     * AE2が存在しない場合はnullを返す。
     */
    @Nullable
    public Object getAe2GridNodeHost() {
        return ae2GridNodeManager;
    }

    // ===== サーバーサイドtick =====
    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // AE2グリッドノード初期化リトライ（マルチプレイでのタイミング問題対策）
        if (ae2Available && ae2GridNodeManager == null && ae2RetryCount < AE2_MAX_RETRIES) {
            ae2RetryCounter++;
            if (ae2RetryCounter >= AE2_RETRY_INTERVAL) {
                ae2RetryCounter = 0;
                ae2RetryCount++;
                tryInitAe2GridNode();
            }
        }

        // スケマティック挿入検知 → 自動解析
        if (schematicSlotChanged) {
            schematicSlotChanged = false;
            ItemStack schematicStack = itemHandler.getStackInSlot(SLOT_SCHEMATIC);
            if (!schematicStack.isEmpty() && isSchematicItem(schematicStack)) {
                if (state == State.IDLE || state == State.FINISHED) {
                    parseAndLoadSchematic();
                }
            } else if (!schematicStack.isEmpty() && isRangeBoardItem(schematicStack)) {
                // 範囲指定ボード: プレビュー用に範囲を読み取り
                if (com.example.emcschematicannon.item.RangeBoardItem.hasRange(schematicStack)) {
                    fillerPos1 = com.example.emcschematicannon.item.RangeBoardItem.getPos1(schematicStack);
                    fillerPos2 = com.example.emcschematicannon.item.RangeBoardItem.getPos2(schematicStack);
                    // 撤去モード: 範囲内のブロックを即座にスキャンして材料リストに表示
                    if (fillerMode && fillerModule == FillerModule.REMOVE
                            && fillerPos1 != null && fillerPos2 != null && level != null) {
                        scanRemoveRange(fillerPos1, fillerPos2);
                    }
                    setChanged();
                    syncToClient();
                }
            } else {
                if (state != State.RUNNING && state != State.PAUSED) {
                    blockSummary.clear();
                    removeSummary.clear();
                    pendingPlacements.clear();
                    totalBlocks = 0;
                    placedBlocks = 0;
                    currentBlockRegistryName = "";
                    state = State.IDLE;
                    totalEmcUsed = 0;
                    missingBlockName = "";
                    fillerPos1 = null;
                    fillerPos2 = null;
                    syncToClient();
                }
            }
        }

        // EMC更新
        if (ownerUUID != null) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
            if (owner != null) {
                updateCachedEmc(owner);
            }
        }

        // 燃料アイテム消費 → 内部EMCバッファに変換
        consumeFuelItems();

        if (state != State.RUNNING) return;
        if (ownerUUID == null) return;

        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) return;


        // tick開始時に隣接インベントリをキャッシュ（毎ブロック×6方向の検索を回避）
        cachedInventoryHandlers.clear();
        if (storageMode != StorageMode.AE_ONLY) {
            for (Direction dir : DIRECTIONS) {
                BlockPos neighborPos = worldPosition.relative(dir);
                IItemHandler handler = level.getCapability(
                        Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
                if (handler != null) {
                    cachedInventoryHandlers.add(handler);
                }
            }
        }

        int blocksThisTick = 0;
        Iterator<BlockPlacement> it = pendingPlacements.iterator();
        boolean emcSyncNeeded = false;

        while (it.hasNext() && blocksThisTick < blocksPerTick) {
            BlockPlacement placement = it.next();

            if (energyStorage.getEnergyStored() < FE_PER_BLOCK) {
                break;
            }

            BlockPos targetPos = placement.pos();
            BlockState originalState = placement.state();
            BlockState targetState = originalState;


            // 置換モードチェック
            BlockState existingState = serverLevel.getBlockState(targetPos);
            boolean canPlace = switch (replaceMode) {
                case DONT_REPLACE -> {
                    // 空気ブロックは配置しない（既存ブロックを破壊しない）
                    if (targetState.isAir()) yield false;
                    yield existingState.isAir() || existingState.canBeReplaced();
                }
                case REPLACE_SOLID -> {
                    if (targetState.isAir()) yield false;
                    yield existingState.isAir() || existingState.canBeReplaced()
                            || !existingState.getFluidState().isEmpty()
                            || (isSolidBlock(existingState) && isSolidBlock(targetState));
                }
                case REPLACE_ANY -> true;
                case REPLACE_EMPTY -> {
                    if (targetState.isAir()) {
                        // 概略図が空気 → 既存の固体ブロックを空気に置換（削除）
                        yield !existingState.isAir();
                    } else {
                        // 概略図がブロック → 空き場所にだけ設置
                        yield existingState.isAir() || existingState.canBeReplaced()
                                || !existingState.getFluidState().isEmpty();
                    }
                }
            };

            if (!canPlace) {
                it.remove();
                placedBlocks++;
                updateSummaryCount(originalState, placement.nbt());
                continue;
            }

            // 既存BlockEntityの保護（設置先に既にBEがある場合スキップ）
            if (skipTileEntities && existingState.hasBlockEntity()) {
                it.remove();
                placedBlocks++;
                updateSummaryCount(originalState, placement.nbt());
                continue;
            }

            // 素材取得: アイテム形態チェック → チェスト → AE2 → 内部EMC → プレイヤーEMC
            boolean materialsAcquired = false;

            // AE2 cable_bus特殊処理: NBTから実際のケーブル/パーツアイテムを特定
            List<String> cableBusItemIds = null;
            boolean isCableBus = CABLE_BUS_ID.equals(BuiltInRegistries.BLOCK.getKey(targetState.getBlock()));
            if (isCableBus && placement.nbt() != null) {
                cableBusItemIds = extractAe2CableBusItems(placement.nbt());
            }

            // cable_busの場合: 各パーツアイテムを個別に素材チェック
            if (isCableBus && cableBusItemIds != null && !cableBusItemIds.isEmpty()) {
                boolean allPartsAcquired = true;
                List<ItemStack> acquiredItems = new ArrayList<>();

                for (String itemId : cableBusItemIds) {
                    ItemStack partItem = resolveItemFromRegistry(itemId);
                    if (partItem == null || partItem.isEmpty()) {
                        // レジストリにないアイテムは無料設置
                        continue;
                    }

                    boolean partAcquired = false;
                    boolean useChest = storageMode != StorageMode.AE_ONLY;
                    boolean useAe2 = ae2Available && storageMode != StorageMode.CHEST_ONLY;
                    if (useChest && tryExtractFromInventory(partItem)) {
                        partAcquired = true;
                    } else if (useAe2 && tryExtractFromAE2(partItem)) {
                        partAcquired = true;
                    } else if (useEmc) {
                        long emcCost = getEmcValue(partItem);
                        if (emcCost > 0) {
                            if (internalEmcBuffer >= emcCost) {
                                internalEmcBuffer -= emcCost;
                                totalEmcUsed += emcCost;
                                partAcquired = true;
                            } else if (consumeEmc(owner, emcCost)) {
                                totalEmcUsed += emcCost;
                                partAcquired = true;
                                emcSyncNeeded = true;
                            }
                        }
                    }

                    if (!partAcquired) {
                        // 不足アイテムを記録
                        missingBlockName = itemId;
                        allPartsAcquired = false;
                        break;
                    }
                    acquiredItems.add(partItem);
                }

                materialsAcquired = allPartsAcquired;
                if (materialsAcquired) {
                    // cable-parts acquired
                    missingBlockName = "";
                }
            } else {
                // 通常ブロック: 既存の素材取得フロー
                boolean useChest = storageMode != StorageMode.AE_ONLY;
                boolean useAe2 = ae2Available && storageMode != StorageMode.CHEST_ONLY;
                ItemStack blockItem = getItemForBlock(targetState);
                if (blockItem.isEmpty()) {
                    materialsAcquired = true;
                    // free(no-item)
                } else if (fillerMode && tryExtractFromFillerSlots(targetState)) {
                    materialsAcquired = true;
                    // filler-slots
                } else if (useChest && tryExtractFromInventory(targetState)) {
                    materialsAcquired = true;
                    // chest
                } else if (useAe2 && tryExtractFromAE2(targetState)) {
                    materialsAcquired = true;
                    // ae2
                } else if (useEmc) {
                    long emcCost = getEmcValue(targetState);
                    if (emcCost > 0) {
                        if (internalEmcBuffer >= emcCost) {
                            internalEmcBuffer -= emcCost;
                            totalEmcUsed += emcCost;
                            materialsAcquired = true;
                            // internal-emc
                        } else if (consumeEmc(owner, emcCost)) {
                            totalEmcUsed += emcCost;
                            materialsAcquired = true;
                            // player-emc
                            emcSyncNeeded = true;
                        }
                    }
                }

                if (!materialsAcquired) {
                    missingBlockName = BuiltInRegistries.BLOCK.getKey(targetState.getBlock()).toString();
                }
            }

            if (!materialsAcquired) {
                if (skipMissing) {
                    it.remove();
                    placedBlocks++;
                    updateSummaryCount(originalState, placement.nbt());
                    missingBlockName = "";
                    continue;
                }
                break;
            }
            missingBlockName = "";

            // フィラーモード撤去: 既存ブロックのアイテムをEMC変換またはストレージに搬入
            if (fillerMode && fillerModule == FillerModule.REMOVE && targetState.isAir()) {
                BlockState removedState = existingState;
                if (!removedState.isAir()) {
                    ItemStack removedItem = getItemForBlock(removedState);
                    if (!removedItem.isEmpty()) {
                        boolean handled = false;
                        // EMC変換がON: EMC値があるブロックはすべてEMCに変換
                        if (useEmc) {
                            long emcValue = getEmcValue(removedState);
                            if (emcValue > 0) {
                                try {
                                    var transmutationProxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
                                    if (transmutationProxy != null) {
                                        var provider = transmutationProxy.getKnowledgeProviderFor(owner.getUUID());
                                        if (provider != null) {
                                            provider.setEmc(provider.getEmc().add(BigInteger.valueOf(emcValue)));
                                            cachedPlayerEmc = provider.getEmc().longValue();
                                            totalEmcUsed += emcValue;
                                            emcSyncNeeded = true;
                                            handled = true;
                                        }
                                    }
                                } catch (Exception e) { }
                            }
                        }
                        // EMC変換されなかった場合（EMCオフ or EMC値なし）: ストレージに搬入
                        if (!handled) {
                            boolean inserted = false;
                            if (storageMode != StorageMode.AE_ONLY) {
                                inserted = tryInsertToInventory(removedItem);
                            }
                            if (!inserted && ae2Available && storageMode != StorageMode.CHEST_ONLY) {
                                inserted = tryInsertToAE2(removedItem);
                            }
                            // ストレージが満杯: 作業を停止
                            if (!inserted) {
                                state = State.PAUSED;
                                break;
                            }
                        }
                    }
                }
            }

            // ブロック設置: NBTがある場合はネイバー更新を遅延（AE2 cable_bus等が
            // NBT復元前にネイバーチェックで自己削除されるのを防止）
            if (placement.nbt() != null && !targetState.isAir()) {
                // 1. クライアント同期もネイバー更新もなしで設置（最小限のフラグ）
                serverLevel.setBlock(targetPos, targetState, Block.UPDATE_KNOWN_SHAPE);
                // 2. NBTデータ復元（回転済みBlockStateを渡して向きNBTを補正）
                restoreBlockEntityNbt(serverLevel, targetPos, placement.nbt(), targetState);
                // 3. NBT復元後にクライアント同期+ネイバー更新（1回で済ませる）
                serverLevel.sendBlockUpdated(targetPos, targetState, targetState, Block.UPDATE_CLIENTS);
                serverLevel.updateNeighborsAt(targetPos, targetState.getBlock());
            } else {
                serverLevel.setBlock(targetPos, targetState, Block.UPDATE_ALL);
            }

            consumeEnergy(FE_PER_BLOCK);
            currentTarget = targetPos;

            placedBlocks++;
            blocksThisTick++;
            it.remove();
            // cable_busの場合はパース済みIDリストを渡して再パースを回避
            if (isCableBus && cableBusItemIds != null) {
                for (String itemId : cableBusItemIds) {
                    blockSummary.computeIfPresent(itemId, (k, v) -> v > 1 ? v - 1 : null);
                }
            } else {
                updateSummaryCount(originalState, null);
            }
        }

        // EMC同期をtick末尾で1回だけ実行（毎ブロックsyncEmc呼出しを排除）
        if (emcSyncNeeded) {
            syncEmcToClient(owner);
        }

        // キャッシュクリア
        cachedInventoryHandlers.clear();

        if (pendingPlacements.isEmpty()) {
            state = State.FINISHED;
            currentBlockRegistryName = "";
            currentTarget = null;

            if (reuseSchematic) {
                // 再利用ON: 概略図の内容を保持、設置位置データだけクリア
                // 概略図はスキーマティックスロットに残す
            } else {
                // 再利用OFF: 概略図を出力スロットに空で移動
                ItemStack schematic = itemHandler.getStackInSlot(SLOT_SCHEMATIC);
                if (!schematic.isEmpty()) {
                    ItemStack emptySchematic = new ItemStack(schematic.getItem());
                    itemHandler.setStackInSlot(SLOT_OUTPUT, emptySchematic);
                    itemHandler.setStackInSlot(SLOT_SCHEMATIC, ItemStack.EMPTY);
                }
            }

            // 進捗リセット
            blockSummary.clear();
            removeSummary.clear();
            placedBlocks = 0;
            totalBlocks = 0;
            missingBlockName = "";

            EMCSchematicCannon.LOGGER.info("Schematic placement complete! Total EMC used: {}", totalEmcUsed);
            setChanged();
            syncToClient();
        } else if (blocksThisTick > 0) {
            setChanged();
            // レート制限: 5tickごとにクライアント同期（毎tick同期は重い）
            syncCounter++;
            if (syncCounter >= 5) {
                syncCounter = 0;
                syncToClient();
            }
        } else if (!missingBlockName.isEmpty()) {
            // 不足ブロック検出時（ブロック未設置）: クライアントに即同期
            setChanged();
            syncToClient();
        }
    }


    private static boolean isSolidBlock(BlockState state) {
        return !state.isAir() && !state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /**
     * setBlock後にBlockEntityのNBTデータを復元する。
     * AE2機械類、チェスト等の内部データ（インベントリ、設定等）を再構築。
     */
    private void restoreBlockEntityNbt(ServerLevel serverLevel, BlockPos pos, CompoundTag nbt,
                                         BlockState targetState) {
        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (be == null) return;

        try {
            CompoundTag restored = nbt.copy();
            // 位置情報を現在の設置先に更新
            restored.putInt("x", pos.getX());
            restored.putInt("y", pos.getY());
            restored.putInt("z", pos.getZ());

            // AE2等のmod向き補正: NBT内の向きデータを回転済みBlockStateに合わせる
            fixOrientationNbt(restored, targetState);

            // vanilla方式: loadWithComponents で完全復元
            be.loadWithComponents(restored, serverLevel.registryAccess());
            be.setChanged();
            // sendBlockUpdated は呼び出し元で実行（二重送信を回避）
        } catch (Exception e) {
            EMCSchematicCannon.LOGGER.debug("Failed to restore BE data at {}: {}", pos, e.getMessage());
        }
    }

    /**
     * NBT内の向きデータを回転済みBlockStateの向きに合わせて補正する。
     * AE2のAEBaseBlockEntity は myForward/myUp を保存しており、
     * loadWithComponentsで元の向きに戻ってしまうため、ここで修正する。
     */
    private void fixOrientationNbt(CompoundTag nbt, BlockState targetState) {
        // AE2: myForward / myUp（AEBaseBlockEntity）
        if (nbt.contains("myForward")) {
            // BlockStateのfacingプロパティから正しい向きを取得
            var facingProp = targetState.getBlock().getStateDefinition().getProperty("facing");
            if (facingProp != null) {
                @SuppressWarnings("unchecked")
                var dirProp = (net.minecraft.world.level.block.state.properties.Property<Direction>) facingProp;
                Direction facing = targetState.getValue(dirProp);
                byte oldForward = nbt.getByte("myForward");
                nbt.putByte("myForward", (byte) facing.ordinal());
                if (oldForward != facing.ordinal()) {
                    EMCSchematicCannon.LOGGER.debug(
                            "Fixed AE2 orientation: myForward {} -> {} for {}",
                            Direction.from3DDataValue(oldForward),
                            facing,
                            BuiltInRegistries.BLOCK.getKey(targetState.getBlock()));
                }
            }
            // myUp は通常 UP のままで回転影響なし（水平回転の場合）
        }

        // vanilla DirectionalBlockEntity互換: FacingDirection
        if (nbt.contains("FacingDirection")) {
            var facingProp = targetState.getBlock().getStateDefinition().getProperty("facing");
            if (facingProp != null) {
                @SuppressWarnings("unchecked")
                var dirProp = (net.minecraft.world.level.block.state.properties.Property<Direction>) facingProp;
                Direction facing = targetState.getValue(dirProp);
                nbt.putInt("FacingDirection", facing.ordinal());
            }
        }
    }

    // クライアントサイドtick（アニメーション更新）
    public void clientTick() {
        // 砲身照準: ブロックの向きに固定
        Direction facing = getBlockState().getValue(EMCSchematicCannonBlock.FACING);
        float defaultYaw = (float) Math.toDegrees(
                Math.atan2(facing.getStepX(), facing.getStepZ()));
        updateAimAngles(defaultYaw, 40f);
    }

    private void updateSummaryCount(BlockState targetState, @Nullable CompoundTag nbt) {
        if (targetState.isAir()) return;
        String regName = BuiltInRegistries.BLOCK.getKey(targetState.getBlock()).toString();

        // AE2 cable_bus: 個別アイテムのカウントを減らす
        if (regName.equals("ae2:cable_bus") && nbt != null) {
            List<String> items = extractAe2CableBusItems(nbt);
            for (String itemId : items) {
                blockSummary.computeIfPresent(itemId, (k, v) -> v > 1 ? v - 1 : null);
            }
            return;
        }

        blockSummary.computeIfPresent(regName, (k, v) -> v > 1 ? v - 1 : null);
    }

    private void consumeEnergy(int amount) {
        try {
            int current = ENERGY_FIELD.getInt(energyStorage);
            ENERGY_FIELD.setInt(energyStorage, Math.max(0, current - amount));
        } catch (IllegalAccessException e) {
            EMCSchematicCannon.LOGGER.warn("Failed to consume energy internally", e);
        }
    }

    // ===== クライアント同期 =====
    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        // クライアントに必要なデータのみ送信（pendingPlacementsは巨大なため除外）
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putString("State", state.name());
        tag.putInt("TotalBlocks", totalBlocks);
        tag.putInt("PlacedBlocks", placedBlocks);
        tag.putLong("CachedEmc", cachedPlayerEmc);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putString("ReplaceMode", replaceMode.name());
        tag.putBoolean("SkipMissing", skipMissing);
        tag.putBoolean("SkipTileEntities", skipTileEntities);
        tag.putBoolean("UseEmc", useEmc);
        tag.putString("StorageMode", storageMode.name());
        tag.putBoolean("ReuseSchematic", reuseSchematic);
        tag.putInt("BlocksPerTick", blocksPerTick);
        tag.putString("CurrentBlock", currentBlockRegistryName);
        tag.putString("MissingBlock", missingBlockName);
        tag.putLong("TotalEmcUsed", totalEmcUsed);
        tag.putBoolean("PreviewVisible", previewVisible);
        if (currentTarget != null) {
            tag.putInt("TargetX", currentTarget.getX());
            tag.putInt("TargetY", currentTarget.getY());
            tag.putInt("TargetZ", currentTarget.getZ());
        }

        // フィラーモード
        tag.putBoolean("FillerMode", fillerMode);
        tag.putString("FillerModule", fillerModule.name());
        if (fillerPos1 != null) {
            tag.putInt("FillerPos1X", fillerPos1.getX());
            tag.putInt("FillerPos1Y", fillerPos1.getY());
            tag.putInt("FillerPos1Z", fillerPos1.getZ());
        }
        if (fillerPos2 != null) {
            tag.putInt("FillerPos2X", fillerPos2.getX());
            tag.putInt("FillerPos2Y", fillerPos2.getY());
            tag.putInt("FillerPos2Z", fillerPos2.getZ());
        }

        CompoundTag summaryTag = new CompoundTag();
        for (var entry : blockSummary.entrySet()) {
            summaryTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("BlockSummary", summaryTag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ===== 燃料消費 =====
    /**
     * 燃料スロットのアイテムを消費し、そのEMC価値をプレイヤーのEMCに加算する。
     * ownerUUIDが設定されている場合のみ動作。
     */
    private void consumeFuelItems() {
        if (ownerUUID == null) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        ItemStack fuelStack = itemHandler.getStackInSlot(SLOT_FUEL);
        if (fuelStack.isEmpty()) return;

        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) return;

        try {
            var emcProxy = moze_intel.projecte.api.proxy.IEMCProxy.INSTANCE;
            var transmutationProxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
            if (emcProxy != null && transmutationProxy != null && emcProxy.hasValue(fuelStack)) {
                long emcValue = emcProxy.getValue(fuelStack);
                if (emcValue > 0) {
                    var provider = transmutationProxy.getKnowledgeProviderFor(ownerUUID);
                    if (provider != null) {
                        fuelStack.shrink(1);
                        if (fuelStack.isEmpty()) {
                            itemHandler.setStackInSlot(SLOT_FUEL, ItemStack.EMPTY);
                        }
                        // プレイヤーのEMCに加算
                        BigInteger current = provider.getEmc();
                        provider.setEmc(current.add(BigInteger.valueOf(emcValue)));
                        cachedPlayerEmc = provider.getEmc().longValue();
                        provider.syncEmc(owner);
                        setChanged();
                    }
                }
            }
        } catch (Exception e) { }
    }

    // ===== スケマティック解析 =====

    private static boolean isSchematicItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try {
            return stack.getItem().getClass().getName()
                    .equals("com.simibubi.create.content.schematics.SchematicItem");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isRangeBoardItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof com.example.emcschematicannon.item.RangeBoardItem;
    }

    private boolean parseAndLoadSchematic() {
        ItemStack schematicStack = itemHandler.getStackInSlot(SLOT_SCHEMATIC);
        if (schematicStack.isEmpty() || !isSchematicItem(schematicStack)) {
            return false;
        }

        try {
            String fileName = getSchematicFileName(schematicStack);
            String ownerName = getSchematicOwner(schematicStack);
            BlockPos anchor = getSchematicAnchor(schematicStack);

            if (fileName == null || ownerName == null || anchor == null) {
                EMCSchematicCannon.LOGGER.warn("Schematic item missing required data components");
                return false;
            }

            if (!fileName.endsWith(".nbt")) {
                EMCSchematicCannon.LOGGER.warn("Invalid schematic file name: {}", fileName);
                return false;
            }

            // 概略図の回転・反転を取得
            Rotation rotation = getSchematicRotation(schematicStack);
            Mirror mirror = getSchematicMirror(schematicStack);

            Path schematicFile = findSchematicFile(ownerName, fileName);
            if (schematicFile == null || !Files.exists(schematicFile)) {
                EMCSchematicCannon.LOGGER.warn("Schematic file not found: {}/{}", ownerName, fileName);
                return false;
            }

            CompoundTag nbt;
            try (InputStream is = Files.newInputStream(schematicFile)) {
                nbt = NbtIo.readCompressed(is, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            }

            List<BlockPlacement> placements = parseStructureNbt(nbt, anchor, rotation, mirror);
            if (placements.isEmpty()) {
                // blocksリストが空でもsizeタグがあれば全体を空気で埋める
                // （空気のみの概略図ではblocksに空気が含まれないことがある）
                if (nbt.contains("size")) {
                    ListTag sizeTag2 = nbt.getList("size", Tag.TAG_INT);
                    long volume = (long) sizeTag2.getInt(0) * sizeTag2.getInt(1) * sizeTag2.getInt(2);
                    if (volume > MAX_AIR_VOLUME_PLACEMENTS) {
                        // 巨大ボリューム: プレースメント生成をスキップ（メモリ/NBT保護）
                        totalBlocks = (int) Math.min(volume, Integer.MAX_VALUE);
                        placedBlocks = 0;
                        state = State.IDLE;
                        pendingPlacements.clear();
                        blockSummary.clear();
                        EMCSchematicCannon.LOGGER.info(
                                "Air schematic volume {} exceeds limit {}, skipping placement generation",
                                volume, MAX_AIR_VOLUME_PLACEMENTS);
                        setChanged();
                        syncToClient();
                        return true;
                    }
                    placements = generateAirVolume(nbt, anchor, rotation, mirror);
                }
                if (placements.isEmpty()) {
                    EMCSchematicCannon.LOGGER.warn("Schematic contains no blocks");
                    return false;
                }
            }

            loadPlacements(placements);
            return true;

        } catch (Exception e) {
            EMCSchematicCannon.LOGGER.error("Failed to parse schematic", e);
            return false;
        }
    }

    @Nullable
    private String getSchematicFileName(ItemStack stack) {
        try {
            var fileComponent = com.simibubi.create.AllDataComponents.SCHEMATIC_FILE;
            return stack.get(fileComponent);
        } catch (Exception | NoClassDefFoundError e) {
            return null;
        }
    }

    @Nullable
    private String getSchematicOwner(ItemStack stack) {
        try {
            var ownerComponent = com.simibubi.create.AllDataComponents.SCHEMATIC_OWNER;
            return stack.get(ownerComponent);
        } catch (Exception | NoClassDefFoundError e) {
            return null;
        }
    }

    @Nullable
    private BlockPos getSchematicAnchor(ItemStack stack) {
        try {
            var anchorComponent = com.simibubi.create.AllDataComponents.SCHEMATIC_ANCHOR;
            return stack.get(anchorComponent);
        } catch (Exception | NoClassDefFoundError e) {
            return null;
        }
    }

    private Rotation getSchematicRotation(ItemStack stack) {
        try {
            var rotComponent = com.simibubi.create.AllDataComponents.SCHEMATIC_ROTATION;
            Rotation rot = stack.get(rotComponent);
            return rot != null ? rot : Rotation.NONE;
        } catch (Exception | NoClassDefFoundError e) {
            return Rotation.NONE;
        }
    }

    private Mirror getSchematicMirror(ItemStack stack) {
        try {
            var mirrorComponent = com.simibubi.create.AllDataComponents.SCHEMATIC_MIRROR;
            Mirror mir = stack.get(mirrorComponent);
            return mir != null ? mir : Mirror.NONE;
        } catch (Exception | NoClassDefFoundError e) {
            return Mirror.NONE;
        }
    }

    @Nullable
    private Path findSchematicFile(String ownerName, String fileName) {
        if (!(level instanceof ServerLevel serverLevel)) return null;

        Path serverDir = serverLevel.getServer().getServerDirectory();
        Path uploadedDir = serverDir.resolve("schematics").resolve("uploaded");

        Path schematicPath = uploadedDir.resolve(ownerName).resolve(fileName).normalize();

        if (!schematicPath.startsWith(uploadedDir)) {
            EMCSchematicCannon.LOGGER.warn("Path traversal attempt blocked: {}", schematicPath);
            return null;
        }

        return schematicPath;
    }

    /**
     * NbtUtils.readBlockStateの拡張版。
     * 標準のHolderLookupで解決できないModdedブロック（AE2等）を
     * 直接レジストリ参照で解決するフォールバックを含む。
     */
    private BlockState readBlockStateSafe(CompoundTag stateTag) {
        // 1. 標準方式: NbtUtils.readBlockState
        BlockState result = NbtUtils.readBlockState(
                BuiltInRegistries.BLOCK.asLookup(), stateTag);

        if (!result.isAir() || !stateTag.contains("Name", Tag.TAG_STRING)) {
            return result;
        }

        String name = stateTag.getString("Name");
        if (name.equals("minecraft:air") || name.equals("minecraft:cave_air")
                || name.equals("minecraft:void_air") || name.isEmpty()) {
            return result;
        }

        // 2. Level経由のレジストリアクセスを試行（Modブロック解決に必要な場合あり）
        if (level != null) {
            try {
                result = NbtUtils.readBlockState(
                        level.holderLookup(Registries.BLOCK), stateTag);
                if (!result.isAir()) {
                    EMCSchematicCannon.LOGGER.debug("Resolved block {} via level registry", name);
                    return result;
                }
            } catch (Exception e) {
                EMCSchematicCannon.LOGGER.debug("Level registry lookup failed for {}: {}", name, e.getMessage());
            }
        }

        // 3. 直接レジストリ参照フォールバック（プロパティ手動解析）
        try {
            ResourceLocation blockId = ResourceLocation.parse(name);
            var blockOpt = BuiltInRegistries.BLOCK.getOptional(blockId);
            if (blockOpt.isEmpty()) {
                EMCSchematicCannon.LOGGER.warn("Block not found in registry: {} (mod not loaded?)", name);
                return Blocks.AIR.defaultBlockState();
            }

            Block block = blockOpt.get();
            result = block.defaultBlockState();

            // Properties適用
            if (stateTag.contains("Properties", Tag.TAG_COMPOUND)) {
                CompoundTag props = stateTag.getCompound("Properties");
                var stateDefinition = block.getStateDefinition();
                for (String propName : props.getAllKeys()) {
                    var property = stateDefinition.getProperty(propName);
                    if (property != null) {
                        result = applyBlockStateProperty(result, property, props.getString(propName));
                    }
                }
            }

            EMCSchematicCannon.LOGGER.info("Resolved block {} via direct registry fallback", name);
            return result;
        } catch (Exception e) {
            EMCSchematicCannon.LOGGER.warn("Failed to resolve block {}: {}", name, e.getMessage());
        }

        return Blocks.AIR.defaultBlockState();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyBlockStateProperty(
            BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
        return property.getValue(value)
                .map(v -> state.setValue(property, v))
                .orElse(state);
    }

    /**
     * Block.rotate()を実装しないmodブロック向けのフォールバック回転。
     * BlockStateのfacing / horizontal_facingプロパティを手動で回転する。
     */
    @SuppressWarnings("unchecked")
    private static BlockState manualRotateFacing(BlockState state, Rotation rotation) {
        // "facing" プロパティ（全6方向）を探す
        var prop = state.getBlock().getStateDefinition().getProperty("facing");
        if (prop != null && Direction.class.isAssignableFrom(prop.getValueClass())) {
            var dirProp = (net.minecraft.world.level.block.state.properties.Property<Direction>) prop;
            Direction current = state.getValue(dirProp);
            Direction rotated = rotateFacingDirection(current, rotation);
            if (rotated != current) {
                return state.setValue(dirProp, rotated);
            }
        }
        // "horizontal_facing" プロパティを探す
        prop = state.getBlock().getStateDefinition().getProperty("horizontal_facing");
        if (prop != null && Direction.class.isAssignableFrom(prop.getValueClass())) {
            var dirProp = (net.minecraft.world.level.block.state.properties.Property<Direction>) prop;
            Direction current = state.getValue(dirProp);
            Direction rotated = rotateFacingDirection(current, rotation);
            if (rotated != current) {
                return state.setValue(dirProp, rotated);
            }
        }
        return state;
    }

    /**
     * Block.mirror()を実装しないmodブロック向けのフォールバック反転。
     */
    @SuppressWarnings("unchecked")
    private static BlockState manualMirrorFacing(BlockState state, Mirror mirror) {
        var prop = state.getBlock().getStateDefinition().getProperty("facing");
        if (prop == null) prop = state.getBlock().getStateDefinition().getProperty("horizontal_facing");
        if (prop != null && Direction.class.isAssignableFrom(prop.getValueClass())) {
            var dirProp = (net.minecraft.world.level.block.state.properties.Property<Direction>) prop;
            Direction current = state.getValue(dirProp);
            Direction mirrored = mirrorFacingDirection(current, mirror);
            if (mirrored != current) {
                return state.setValue(dirProp, mirrored);
            }
        }
        return state;
    }

    /** 水平方向を回転させる（UP/DOWNは変更しない） */
    private static Direction rotateFacingDirection(Direction dir, Rotation rotation) {
        if (dir.getAxis() == Direction.Axis.Y) return dir; // UP/DOWNは回転しない
        return switch (rotation) {
            case CLOCKWISE_90 -> dir.getClockWise();
            case CLOCKWISE_180 -> dir.getOpposite();
            case COUNTERCLOCKWISE_90 -> dir.getCounterClockWise();
            default -> dir;
        };
    }

    /**
     * NBT内の方向キー（north, south, east, west, up, down）を回転/反転に合わせてリマップする。
     * AE2 cable_busのパーツ配置（ターミナル、バス等が方向キーで格納される）に対応。
     * また、AE2 BlockEntity内のmyForward/myUpもここで回転させる。
     */
    private static CompoundTag rotateDirectionalNbt(CompoundTag nbt, Rotation rotation, Mirror mirror) {
        // 方向キーの一覧
        String[] dirKeys = {"north", "south", "east", "west", "up", "down"};
        Direction[] directions = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.UP, Direction.DOWN
        };

        // 方向キーのCompoundTagが存在するかチェック
        boolean hasDirectionalData = false;
        for (String key : dirKeys) {
            if (nbt.contains(key, Tag.TAG_COMPOUND)) {
                hasDirectionalData = true;
                break;
            }
        }

        if (!hasDirectionalData) return nbt;

        // 各方向キーのデータを一時退避
        CompoundTag result = nbt.copy();
        Map<Direction, CompoundTag> dirData = new HashMap<>();
        for (int i = 0; i < dirKeys.length; i++) {
            if (nbt.contains(dirKeys[i], Tag.TAG_COMPOUND)) {
                dirData.put(directions[i], nbt.getCompound(dirKeys[i]).copy());
                result.remove(dirKeys[i]);
            }
        }

        // 各方向を変換して新しいキーに格納
        for (var entry : dirData.entrySet()) {
            Direction dir = entry.getKey();
            // mirror → rotation の順で適用
            if (mirror != Mirror.NONE) {
                dir = mirrorFacingDirection(dir, mirror);
            }
            if (rotation != Rotation.NONE) {
                dir = rotateFacingDirection(dir, rotation);
            }
            result.put(dir.getSerializedName(), entry.getValue());
        }

        return result;
    }

    /** 水平方向を反転させる（UP/DOWNは変更しない） */
    private static Direction mirrorFacingDirection(Direction dir, Mirror mirror) {
        if (dir.getAxis() == Direction.Axis.Y) return dir;
        return switch (mirror) {
            case FRONT_BACK -> dir.getAxis() == Direction.Axis.Z ? dir.getOpposite() : dir;
            case LEFT_RIGHT -> dir.getAxis() == Direction.Axis.X ? dir.getOpposite() : dir;
            default -> dir;
        };
    }

    private List<BlockPlacement> parseStructureNbt(CompoundTag nbt, BlockPos anchor,
                                                      Rotation rotation, Mirror mirror) {
        List<BlockPlacement> result = new ArrayList<>();

        if (!nbt.contains("palette") || !nbt.contains("blocks")) {
            EMCSchematicCannon.LOGGER.warn("Invalid structure NBT: missing palette or blocks");
            return result;
        }

        ListTag paletteTag = nbt.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>();

        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            String paletteName = stateTag.contains("Name") ? stateTag.getString("Name") : "?";
            BlockState blockState = readBlockStateSafe(stateTag);
            String resolvedName = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
            if (!paletteName.equals(resolvedName) && !blockState.isAir()) {
                EMCSchematicCannon.LOGGER.warn("Palette[{}]: {} -> resolved as {} (mismatch!)",
                        i, paletteName, resolvedName);
            } else {
                EMCSchematicCannon.LOGGER.info("Palette[{}]: {} -> {} (properties: {})",
                        i, paletteName, resolvedName, blockState);
            }
            palette.add(blockState);
        }

        ListTag blocksTag = nbt.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);

            ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
            int relX = posTag.getInt(0);
            int relY = posTag.getInt(1);
            int relZ = posTag.getInt(2);

            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.size()) continue;

            BlockState blockState = palette.get(stateIndex);
            // 空気ブロックも含める（REPLACE_EMPTYで既存ブロックを空気に置換する用途）

            // 反転適用（位置 + BlockState）
            // vanilla方式: pivot=原点で座標を反転
            if (mirror != Mirror.NONE) {
                if (mirror == Mirror.FRONT_BACK) relX = -relX;
                else if (mirror == Mirror.LEFT_RIGHT) relZ = -relZ;
                BlockState mirrored = blockState.mirror(mirror);
                if (mirrored == blockState) {
                    // mod側がmirror()未実装の場合、facingプロパティを手動で反転
                    blockState = manualMirrorFacing(blockState, mirror);
                } else {
                    blockState = mirrored;
                }
            }

            // 回転適用（位置 + BlockState）
            // vanilla方式: pivot=原点で座標を回転（Createのanchorが回転後のオフセットを含む）
            if (rotation != Rotation.NONE) {
                int newX = relX, newZ = relZ;
                switch (rotation) {
                    case CLOCKWISE_90 -> { newX = -relZ; newZ = relX; }
                    case CLOCKWISE_180 -> { newX = -relX; newZ = -relZ; }
                    case COUNTERCLOCKWISE_90 -> { newX = relZ; newZ = -relX; }
                    default -> {}
                }
                relX = newX;
                relZ = newZ;
                BlockState rotated = blockState.rotate(rotation);
                if (rotated == blockState) {
                    // mod側がrotate()未実装の場合、facingプロパティを手動で回転
                    blockState = manualRotateFacing(blockState, rotation);
                } else {
                    blockState = rotated;
                }
            }

            BlockPos absolutePos = anchor.offset(relX, relY, relZ);
            // BlockEntity NBTデータを保持（AE2等の複雑なブロックの復元に必要）
            CompoundTag blockNbt = blockTag.contains("nbt") ? blockTag.getCompound("nbt").copy() : null;

            // AE2 cable_bus等: NBT内の方向キーを回転/反転に合わせてリマップ
            if (blockNbt != null && (rotation != Rotation.NONE || mirror != Mirror.NONE)) {
                blockNbt = rotateDirectionalNbt(blockNbt, rotation, mirror);
            }

            result.add(new BlockPlacement(absolutePos, blockState, blockNbt));
        }

        EMCSchematicCannon.LOGGER.info("Parsed {} blocks from schematic (rotation={}, mirror={})",
                result.size(), rotation, mirror);
        return result;
    }

    /**
     * sizeタグから概略図ボリューム全体の空気プレースメントを生成する。
     * 空気のみの概略図でblocksリストが空の場合に使用。
     */
    private List<BlockPlacement> generateAirVolume(CompoundTag nbt, BlockPos anchor,
                                                     Rotation rotation, Mirror mirror) {
        List<BlockPlacement> result = new ArrayList<>();
        ListTag sizeTag = nbt.getList("size", Tag.TAG_INT);
        if (sizeTag.size() < 3) return result;

        int sizeX = sizeTag.getInt(0);
        int sizeY = sizeTag.getInt(1);
        int sizeZ = sizeTag.getInt(2);
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    int relX = x, relZ = z;

                    if (mirror != Mirror.NONE) {
                        if (mirror == Mirror.FRONT_BACK) relX = -relX;
                        else if (mirror == Mirror.LEFT_RIGHT) relZ = -relZ;
                    }
                    if (rotation != Rotation.NONE) {
                        int newX = relX, newZ = relZ;
                        switch (rotation) {
                            case CLOCKWISE_90 -> { newX = -relZ; newZ = relX; }
                            case CLOCKWISE_180 -> { newX = -relX; newZ = -relZ; }
                            case COUNTERCLOCKWISE_90 -> { newX = relZ; newZ = -relX; }
                            default -> {}
                        }
                        relX = newX;
                        relZ = newZ;
                    }

                    result.add(new BlockPlacement(anchor.offset(relX, y, relZ), air));
                }
            }
        }

        EMCSchematicCannon.LOGGER.info("Generated {} air placements from schematic size {}x{}x{}",
                result.size(), sizeX, sizeY, sizeZ);
        return result;
    }

    // ===== AE2 cable_bus NBT解析 =====

    /**
     * AE2 cable_busブロックのNBTから必要なアイテムIDリストを抽出する。
     * cable_busはケーブル+各面のパーツ（バス、パネル等）で構成される。
     * AE2のNBT形式: cable.id = ケーブルアイテムID、各面(up/down/north等).id = パーツアイテムID
     */
    private static List<String> extractAe2CableBusItems(@Nullable CompoundTag nbt) {
        List<String> items = new ArrayList<>();
        if (nbt == null) return items;

        // ケーブル本体
        if (nbt.contains("cable", Tag.TAG_COMPOUND)) {
            CompoundTag cableTag = nbt.getCompound("cable");
            if (cableTag.contains("id", Tag.TAG_STRING)) {
                items.add(cableTag.getString("id"));
            }
        }

        // 各面のパーツ（バス、パネル等）
        for (Direction dir : Direction.values()) {
            String dirName = dir.getSerializedName();
            if (nbt.contains(dirName, Tag.TAG_COMPOUND)) {
                CompoundTag partTag = nbt.getCompound(dirName);
                if (partTag.contains("id", Tag.TAG_STRING)) {
                    items.add(partTag.getString("id"));
                }
            }
        }

        if (items.isEmpty()) {
            // 未知のNBT形式をデバッグ出力
            EMCSchematicCannon.LOGGER.info("AE2 cable_bus NBT has unknown structure. Keys: {}", nbt.getAllKeys());
        }

        return items;
    }

    /**
     * AE2 cable_busのNBTからアイテムIDを取得し、ItemStackとして返す。
     * レジストリに存在するアイテムのみ返す。
     */
    @Nullable
    private static ItemStack resolveItemFromRegistry(String itemId) {
        try {
            ResourceLocation rl = ResourceLocation.parse(itemId);
            if (BuiltInRegistries.ITEM.containsKey(rl)) {
                var item = BuiltInRegistries.ITEM.get(rl);
                if (item != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(item);
                }
            }
        } catch (Exception e) { }
        return null;
    }

    // ===== EMC操作 =====
    private long getEmcValue(BlockState blockState) {
        try {
            var emcProxy = moze_intel.projecte.api.proxy.IEMCProxy.INSTANCE;
            if (emcProxy != null) {
                ItemStack stack = new ItemStack(blockState.getBlock().asItem());
                if (!stack.isEmpty()) {
                    return emcProxy.getValue(stack);
                }
            }
        } catch (Exception e) { }
        return 0;
    }

    private long getEmcValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        try {
            var emcProxy = moze_intel.projecte.api.proxy.IEMCProxy.INSTANCE;
            if (emcProxy != null) {
                return emcProxy.getValue(stack);
            }
        } catch (Exception e) { }
        return 0;
    }

    private static boolean hasEmcValue(ItemStack stack) {
        try {
            var emcProxy = moze_intel.projecte.api.proxy.IEMCProxy.INSTANCE;
            if (emcProxy != null) {
                return emcProxy.hasValue(stack);
            }
        } catch (Exception e) { }
        return false;
    }

    /**
     * EMCを消費する（クライアント同期なし・バッチ用）。
     * syncEmcToClient()をtick末尾で1回だけ呼ぶこと。
     */
    private boolean consumeEmc(ServerPlayer player, long amount) {
        try {
            var transmutationProxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
            if (transmutationProxy != null) {
                var provider = transmutationProxy.getKnowledgeProviderFor(player.getUUID());
                if (provider != null) {
                    BigInteger current = provider.getEmc();
                    BigInteger cost = BigInteger.valueOf(amount);
                    if (current.compareTo(cost) >= 0) {
                        provider.setEmc(current.subtract(cost));
                        cachedPlayerEmc = provider.getEmc().longValue();
                        // syncEmc は tick末尾でバッチ呼び出し（毎ブロックで呼ぶとパケット洪水になる）
                        return true;
                    }
                }
            }
        } catch (Exception e) { }
        return false;
    }

    /** tick末尾でEMC同期を1回だけ実行（バッチ） */
    private void syncEmcToClient(ServerPlayer player) {
        try {
            var transmutationProxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
            if (transmutationProxy != null) {
                var provider = transmutationProxy.getKnowledgeProviderFor(player.getUUID());
                if (provider != null) {
                    provider.syncEmc(player);
                }
            }
        } catch (Exception e) { }
    }

    private void updateCachedEmc(ServerPlayer player) {
        try {
            var transmutationProxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
            if (transmutationProxy != null) {
                var provider = transmutationProxy.getKnowledgeProviderFor(player.getUUID());
                if (provider != null) {
                    cachedPlayerEmc = provider.getEmc().longValue();
                }
            }
        } catch (Exception e) {
            cachedPlayerEmc = 0;
        }
    }

    // ===== 隣接チェスト連携 =====

    /**
     * ブロックに対応するアイテムを取得する。
     * Block.asItem()に加え、レジストリ名の一致も試す。
     */
    private ItemStack getItemForBlock(BlockState targetState) {
        Block block = targetState.getBlock();

        // 1. Block.asItem() で直接取得
        net.minecraft.world.item.Item item = block.asItem();
        if (item != net.minecraft.world.item.Items.AIR) {
            return new ItemStack(item);
        }

        // 2. レジストリ名が同じアイテムを検索（Mod製ブロックの一部はasItem()が正しく返さない）
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (BuiltInRegistries.ITEM.containsKey(blockId)) {
            item = BuiltInRegistries.ITEM.get(blockId);
            if (item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        }

        return ItemStack.EMPTY;
    }

    private boolean tryExtractFromInventory(BlockState targetState) {
        Block targetBlock = targetState.getBlock();
        ItemStack needed = getItemForBlock(targetState);
        if (needed.isEmpty()) return false;

        for (IItemHandler handler : cachedInventoryHandlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty()) continue;

                boolean matches = ItemStack.isSameItem(inSlot, needed);
                if (!matches && inSlot.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                    matches = blockItem.getBlock() == targetBlock;
                }

                if (matches) {
                    ItemStack extracted = handler.extractItem(slot, 1, false);
                    if (!extracted.isEmpty()) return true;
                }
            }
        }
        return false;
    }

    private boolean tryExtractFromInventory(ItemStack needed) {
        if (needed.isEmpty()) return false;

        for (IItemHandler handler : cachedInventoryHandlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty()) continue;
                if (ItemStack.isSameItem(inSlot, needed)) {
                    ItemStack extracted = handler.extractItem(slot, 1, false);
                    if (!extracted.isEmpty()) return true;
                }
            }
        }
        return false;
    }

    // ===== AE2連携 =====
    private boolean tryExtractFromAE2(BlockState blockState) {
        if (!ae2Available) return false;
        try {
            // 自身のグリッドノード → 隣接AE2グリッドのフォールバック付き
            return AE2Integration.tryExtractItem(ae2GridNodeManager, blockState, level, worldPosition);
        } catch (Exception e) {
            EMCSchematicCannon.LOGGER.warn("AE2 extraction exception: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 指定ItemStackをAE2 ME倉庫から1個抽出する。
     */
    private boolean tryExtractFromAE2(ItemStack needed) {
        if (!ae2Available || needed.isEmpty()) return false;
        try {
            return AE2Integration.tryExtractItem(ae2GridNodeManager, needed, level, worldPosition);
        } catch (Exception e) {
            return false;
        }
    }

    // ===== フィラースロットからの素材取得 =====

    /** フィラーアイテムスロットから一致するブロックアイテムを1個消費する */
    private boolean tryExtractFromFillerSlots(BlockState targetState) {
        var targetBlock = targetState.getBlock();
        net.minecraft.world.item.Item targetItem = targetBlock.asItem();
        if (targetItem == net.minecraft.world.item.Items.AIR) {
            var blockId = BuiltInRegistries.BLOCK.getKey(targetBlock);
            if (BuiltInRegistries.ITEM.containsKey(blockId)) {
                targetItem = BuiltInRegistries.ITEM.get(blockId);
            }
        }
        if (targetItem == net.minecraft.world.item.Items.AIR) return false;

        for (int i = SLOT_FILLER_START; i < SLOT_FILLER_START + FILLER_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            boolean matches = stack.getItem() == targetItem;
            if (!matches && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                matches = blockItem.getBlock() == targetBlock;
            }
            if (matches) {
                itemHandler.extractItem(i, 1, false);
                return true;
            }
        }
        return false;
    }

    // ===== 撤去モジュール用: アイテム搬入 =====

    /** 隣接チェストにアイテムを搬入する */
    private boolean tryInsertToInventory(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (IItemHandler handler : cachedInventoryHandlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack remainder = handler.insertItem(slot, stack.copy(), false);
                if (remainder.isEmpty()) return true;
            }
        }
        return false;
    }

    /** AE2 MEネットワークにアイテムを搬入する */
    private boolean tryInsertToAE2(ItemStack stack) {
        if (!ae2Available || stack.isEmpty()) return false;
        try {
            return AE2Integration.tryInsertItem(ae2GridNodeManager, stack, level, worldPosition);
        } catch (Exception | NoClassDefFoundError e) {
            return false;
        }
    }

    // ===== 操作 =====
    public void startPlacement(ServerPlayer player) {
        if (state == State.RUNNING || state == State.PAUSED) return;

        if (fillerMode) {
            if (pendingPlacements.isEmpty()) {
                if (!generateFillerPlacements(player)) {
                    EMCSchematicCannon.LOGGER.warn("Failed to generate filler placements");
                    state = State.ERROR;
                    setChanged();
                    syncToClient();
                    return;
                }
            }
        } else {
            if (pendingPlacements.isEmpty()) {
                if (!parseAndLoadSchematic()) {
                    EMCSchematicCannon.LOGGER.warn("Failed to parse schematic or no schematic inserted");
                    state = State.ERROR;
                    setChanged();
                    syncToClient();
                    return;
                }
            }
        }

        this.ownerUUID = player.getUUID();
        this.state = State.RUNNING;
        this.placedBlocks = 0;
        setChanged();
        syncToClient();
    }


    public void pausePlacement() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            setChanged();
            syncToClient();
        }
    }

    public void resumePlacement() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            setChanged();
            syncToClient();
        }
    }

    public void stopPlacement() {
        state = State.IDLE;
        pendingPlacements.clear();
        blockSummary.clear();
        removeSummary.clear();
        placedBlocks = 0;
        totalBlocks = 0;
        missingBlockName = "";
        totalEmcUsed = 0;
        currentTarget = null;
        fillerPos1 = null;
        fillerPos2 = null;
        setChanged();
        syncToClient();
    }

    // ===== フィラーモード: プレースメント生成 =====

    private boolean generateFillerPlacements(ServerPlayer player) {
        ItemStack rangeBoardStack = itemHandler.getStackInSlot(SLOT_SCHEMATIC);
        if (!isRangeBoardItem(rangeBoardStack)) {
            EMCSchematicCannon.LOGGER.warn("Filler mode requires a Range Board in the schematic slot");
            return false;
        }

        if (!com.example.emcschematicannon.item.RangeBoardItem.hasRange(rangeBoardStack)) {
            EMCSchematicCannon.LOGGER.warn("Range Board has no range set");
            return false;
        }

        fillerPos1 = com.example.emcschematicannon.item.RangeBoardItem.getPos1(rangeBoardStack);
        fillerPos2 = com.example.emcschematicannon.item.RangeBoardItem.getPos2(rangeBoardStack);

        if (fillerPos1 == null || fillerPos2 == null) return false;

        BlockPos min = new BlockPos(
                Math.min(fillerPos1.getX(), fillerPos2.getX()),
                Math.min(fillerPos1.getY(), fillerPos2.getY()),
                Math.min(fillerPos1.getZ(), fillerPos2.getZ()));
        BlockPos max = new BlockPos(
                Math.max(fillerPos1.getX(), fillerPos2.getX()),
                Math.max(fillerPos1.getY(), fillerPos2.getY()),
                Math.max(fillerPos1.getZ(), fillerPos2.getZ()));

        // フィラーアイテムスロットからブロック状態を取得（最初の非空スロットを使用）
        BlockState fillState = Blocks.AIR.defaultBlockState();
        for (int i = SLOT_FILLER_START; i < SLOT_FILLER_START + FILLER_SLOT_COUNT; i++) {
            ItemStack fillerItem = itemHandler.getStackInSlot(i);
            if (!fillerItem.isEmpty()) {
                var blockOpt = BuiltInRegistries.BLOCK.getOptional(
                        BuiltInRegistries.ITEM.getKey(fillerItem.getItem()));
                if (blockOpt.isPresent() && blockOpt.get() != Blocks.AIR) {
                    fillState = blockOpt.get().defaultBlockState();
                    break;
                } else if (fillerItem.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                    fillState = blockItem.getBlock().defaultBlockState();
                    break;
                }
            }
        }

        List<BlockPlacement> placements = switch (fillerModule) {
            case FILL -> generateFillPlacements(min, max, fillState);
            case ERASE -> generateErasePlacements(min, max);
            case REMOVE -> generateRemovePlacements(min, max);
            case WALL -> generateWallPlacements(min, max, fillState);
            case TOWER -> generateTowerPlacements(min, max, fillState);
            case BOX -> generateBoxPlacements(min, max, fillState);
            case CIRCLE_WALL -> generateCircleWallPlacements(min, max, fillState);
        };

        if (placements.isEmpty()) return false;

        loadPlacements(placements);
        // 撤去モード: removeSummaryをblockSummaryにコピー
        if (fillerModule == FillerModule.REMOVE && !removeSummary.isEmpty()) {
            blockSummary.clear();
            blockSummary.putAll(removeSummary);
        }
        return true;
    }

    /** 埋め立てモジュール: 範囲内をブロックで埋める。モブがいる場合は消去。 */
    private List<BlockPlacement> generateFillPlacements(BlockPos min, BlockPos max, BlockState fillState) {
        List<BlockPlacement> placements = new ArrayList<>();
        if (fillState.isAir()) return placements;

        // 範囲内のモブを消去
        if (level instanceof ServerLevel serverLevel) {
            net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
                    min.getX(), min.getY(), min.getZ(),
                    max.getX() + 1, max.getY() + 1, max.getZ() + 1);
            var entities = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area);
            for (var mob : entities) {
                mob.discard();
            }
        }

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    placements.add(new BlockPlacement(new BlockPos(x, y, z), fillState));
                }
            }
        }
        return placements;
    }

    /** 完全消去モジュール: 範囲内のブロックを消去（アイテムドロップなし） */
    private List<BlockPlacement> generateErasePlacements(BlockPos min, BlockPos max) {
        List<BlockPlacement> placements = new ArrayList<>();
        // 上から下へ消去（空気ブロックはスキップ）
        for (int y = max.getY(); y >= min.getY(); y--) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        placements.add(new BlockPlacement(pos, Blocks.AIR.defaultBlockState()));
                    }
                }
            }
        }
        return placements;
    }

    /** 撤去モジュール: 上から下に撤去（アイテムはAE/チェストに搬入可能） */
    private List<BlockPlacement> generateRemovePlacements(BlockPos min, BlockPos max) {
        List<BlockPlacement> placements = new ArrayList<>();
        // 撤去対象のブロックをサマリー用に記録
        removeSummary.clear();
        for (int y = max.getY(); y >= min.getY(); y--) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState existing = level.getBlockState(pos);
                    if (!existing.isAir()) {
                        placements.add(new BlockPlacement(pos, Blocks.AIR.defaultBlockState()));
                        String regName = BuiltInRegistries.BLOCK.getKey(existing.getBlock()).toString();
                        removeSummary.merge(regName, 1, Integer::sum);
                    }
                }
            }
        }
        return placements;
    }

    /** 撤去モード: 範囲内のブロックをスキャンしてサマリーのみ構築（配置リストは作らない） */
    private void scanRemoveRange(BlockPos p1, BlockPos p2) {
        if (level == null) return;
        BlockPos min = new BlockPos(
                Math.min(p1.getX(), p2.getX()),
                Math.min(p1.getY(), p2.getY()),
                Math.min(p1.getZ(), p2.getZ()));
        BlockPos max = new BlockPos(
                Math.max(p1.getX(), p2.getX()),
                Math.max(p1.getY(), p2.getY()),
                Math.max(p1.getZ(), p2.getZ()));
        removeSummary.clear();
        for (int y = max.getY(); y >= min.getY(); y--) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockState existing = level.getBlockState(new BlockPos(x, y, z));
                    if (!existing.isAir()) {
                        String regName = BuiltInRegistries.BLOCK.getKey(existing.getBlock()).toString();
                        removeSummary.merge(regName, 1, Integer::sum);
                    }
                }
            }
        }
        blockSummary.clear();
        blockSummary.putAll(removeSummary);
        totalBlocks = removeSummary.values().stream().mapToInt(Integer::intValue).sum();
        placedBlocks = 0;
    }

    /** 壁作成モジュール: 範囲の外周に沿って壁を作成 */
    private List<BlockPlacement> generateWallPlacements(BlockPos min, BlockPos max, BlockState fillState) {
        List<BlockPlacement> placements = new ArrayList<>();
        if (fillState.isAir()) return placements;

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    // 外周のみ配置（4面の壁）
                    if (x == min.getX() || x == max.getX() || z == min.getZ() || z == max.getZ()) {
                        placements.add(new BlockPlacement(new BlockPos(x, y, z), fillState));
                    }
                }
            }
        }
        return placements;
    }

    /** タワーモジュール: 最下段に既存ブロックがある位置のみ上へ積み上げる */
    private List<BlockPlacement> generateTowerPlacements(BlockPos min, BlockPos max, BlockState fillState) {
        List<BlockPlacement> placements = new ArrayList<>();
        if (!(level instanceof ServerLevel serverLevel)) return placements;

        for (int z = min.getZ(); z <= max.getZ(); z++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                // 最下段のブロックを基準にする（空気の場合はスキップ）
                BlockState baseState = serverLevel.getBlockState(new BlockPos(x, min.getY(), z));
                if (baseState.isAir()) continue;

                // 積み上げるブロック: フィラーアイテムが指定されていればそれ、なければ基底ブロック
                BlockState towerState = fillState.isAir() ? baseState : fillState;

                for (int y = min.getY() + 1; y <= max.getY(); y++) {
                    placements.add(new BlockPlacement(new BlockPos(x, y, z), towerState));
                }
            }
        }
        return placements;
    }

    /** 箱作成モジュール: 中が空洞の箱を作成 */
    private List<BlockPlacement> generateBoxPlacements(BlockPos min, BlockPos max, BlockState fillState) {
        List<BlockPlacement> placements = new ArrayList<>();
        if (fillState.isAir()) return placements;

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    // 6面のいずれかに接していれば外殻
                    boolean isShell = x == min.getX() || x == max.getX()
                            || y == min.getY() || y == max.getY()
                            || z == min.getZ() || z == max.getZ();
                    if (isShell) {
                        placements.add(new BlockPlacement(new BlockPos(x, y, z), fillState));
                    }
                }
            }
        }
        return placements;
    }

    /** 円壁作成モジュール: 範囲内に円筒形の壁を作成 */
    private List<BlockPlacement> generateCircleWallPlacements(BlockPos min, BlockPos max, BlockState fillState) {
        List<BlockPlacement> placements = new ArrayList<>();
        if (fillState.isAir()) return placements;

        double centerX = (min.getX() + max.getX()) / 2.0;
        double centerZ = (min.getZ() + max.getZ()) / 2.0;
        double radiusX = (max.getX() - min.getX()) / 2.0;
        double radiusZ = (max.getZ() - min.getZ()) / 2.0;

        if (radiusX < 0.5 || radiusZ < 0.5) return placements;

        for (int y = min.getY(); y <= max.getY(); y++) {
            // Y層ごとにリセット
            java.util.Set<Long> placed = new java.util.HashSet<>();

            // X軸スキャン: 各Xに対して楕円上のZ座標を計算（上下2点）
            for (int x = min.getX(); x <= max.getX(); x++) {
                double dx = (x + 0.5 - centerX) / radiusX;
                if (dx * dx > 1.0) continue;
                double zOffset = radiusZ * Math.sqrt(1.0 - dx * dx);

                int zTop = (int) Math.floor(centerZ + zOffset);
                int zBot = (int) Math.floor(centerZ - zOffset);

                for (int bz : new int[]{zTop, zBot}) {
                    if (bz < min.getZ() || bz > max.getZ()) continue;
                    long key = ((long) x << 32) | (bz & 0xFFFFFFFFL);
                    if (placed.add(key)) {
                        placements.add(new BlockPlacement(new BlockPos(x, y, bz), fillState));
                    }
                }
            }

            // Z軸スキャン: 各Zに対して楕円上のX座標を計算（左右2点）
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                double dz = (z + 0.5 - centerZ) / radiusZ;
                if (dz * dz > 1.0) continue;
                double xOffset = radiusX * Math.sqrt(1.0 - dz * dz);

                int xRight = (int) Math.floor(centerX + xOffset);
                int xLeft = (int) Math.floor(centerX - xOffset);

                for (int bx : new int[]{xRight, xLeft}) {
                    if (bx < min.getX() || bx > max.getX()) continue;
                    long key = ((long) bx << 32) | (z & 0xFFFFFFFFL);
                    if (placed.add(key)) {
                        placements.add(new BlockPlacement(new BlockPos(bx, y, z), fillState));
                    }
                }
            }
        }
        return placements;
    }

    public void loadPlacements(List<BlockPlacement> placements) {
        pendingPlacements.clear();
        pendingPlacements.addAll(placements);
        totalBlocks = placements.size();
        placedBlocks = 0;
        state = State.IDLE;
        missingBlockName = "";
        totalEmcUsed = 0;

        // プレビュー用バウンディングボックスを計算（フィラーモードでは既にfillerPos1/2が設定済み）
        if (!fillerMode && !placements.isEmpty()) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPlacement p : placements) {
                BlockPos pos = p.pos();
                minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
            }
            fillerPos1 = new BlockPos(minX, minY, minZ);
            fillerPos2 = new BlockPos(maxX, maxY, maxZ);
        }

        blockSummary.clear();
        for (BlockPlacement p : placements) {
            if (p.state().isAir()) continue; // 空気はサマリーに含めない
            String regName = BuiltInRegistries.BLOCK.getKey(p.state().getBlock()).toString();

            // AE2 cable_bus: NBTから実際のケーブル/パーツアイテムを個別にカウント
            if (regName.equals("ae2:cable_bus") && p.nbt() != null) {
                List<String> items = extractAe2CableBusItems(p.nbt());
                if (!items.isEmpty()) {
                    for (String itemId : items) {
                        blockSummary.merge(itemId, 1, Integer::sum);
                    }
                    continue; // cable_bus自体はカウントしない
                }
            }

            blockSummary.merge(regName, 1, Integer::sum);
        }

        setChanged();
        syncToClient();
    }

    // ===== 設定 =====
    public ReplaceMode getReplaceMode() { return replaceMode; }
    public boolean isSkipMissing() { return skipMissing; }
    public boolean isSkipTileEntities() { return skipTileEntities; }
    public boolean isUseEmc() { return useEmc; }
    public int getBlocksPerTick() { return blocksPerTick; }

    public void setReplaceMode(ReplaceMode mode) { this.replaceMode = mode; setChanged(); }
    public void setSkipMissing(boolean skip) { this.skipMissing = skip; setChanged(); }
    public void setSkipTileEntities(boolean skip) { this.skipTileEntities = skip; setChanged(); }
    public void setUseEmc(boolean use) { this.useEmc = use; setChanged(); }
    public StorageMode getStorageMode() { return storageMode; }
    public void setStorageMode(StorageMode mode) { this.storageMode = mode; setChanged(); }
    public boolean isReuseSchematic() { return reuseSchematic; }
    public void setReuseSchematic(boolean reuse) { this.reuseSchematic = reuse; setChanged(); }
    public void setBlocksPerTick(int speed) { this.blocksPerTick = Math.max(1, Math.min(BLOCKS_PER_TICK, speed)); setChanged(); }

    // フィラーモード設定
    public boolean isFillerMode() { return fillerMode; }
    public void setFillerMode(boolean filler) {
        this.fillerMode = filler;
        // モード切替時にスロット内の互換性のないアイテムを排出
        ItemStack schematicStack = itemHandler.getStackInSlot(SLOT_SCHEMATIC);
        if (!schematicStack.isEmpty()) {
            boolean incompatible = filler ? isSchematicItem(schematicStack) : isRangeBoardItem(schematicStack);
            if (incompatible) {
                itemHandler.setStackInSlot(SLOT_SCHEMATIC, ItemStack.EMPTY);
                if (level != null && !level.isClientSide) {
                    net.minecraft.world.entity.item.ItemEntity itemEntity =
                            new net.minecraft.world.entity.item.ItemEntity(level,
                                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                                    schematicStack);
                    level.addFreshEntity(itemEntity);
                }
            }
        }
        setChanged();
    }
    public FillerModule getFillerModule() { return fillerModule; }
    public void setFillerModule(FillerModule module) { this.fillerModule = module; setChanged(); }
    public BlockPos getFillerPos1() { return fillerPos1; }
    public BlockPos getFillerPos2() { return fillerPos2; }
    public boolean isPreviewVisible() { return previewVisible; }
    public void setPreviewVisible(boolean visible) { this.previewVisible = visible; setChanged(); }
    /** フィラースロットから最初の非空アイテムを取得（表示用） */
    public ItemStack getFirstFillerItem() {
        for (int i = SLOT_FILLER_START; i < SLOT_FILLER_START + FILLER_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    // ===== アニメーション =====
    public BlockPos getCurrentTarget() { return currentTarget; }
    public float getAnimYaw(float partialTick) { return prevYaw + (currentYaw - prevYaw) * partialTick; }
    public float getAnimPitch(float partialTick) { return prevPitch + (currentPitch - prevPitch) * partialTick; }

    /** デルタタイム基準のなめらかな補間で砲身角度を更新 */
    public void updateAimAngles(float targetYaw, float targetPitch) {
        long now = System.nanoTime();
        float dt = (now - lastAnimNano) / 1_000_000_000f;
        lastAnimNano = now;
        dt = Math.min(dt, 0.1f); // 初回や極端なラグを制限

        prevYaw = currentYaw;
        prevPitch = currentPitch;

        // 指数減衰補間: speed値が大きいほど速く追従
        float speed = 6.0f;
        float factor = 1.0f - (float) Math.exp(-speed * dt);

        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        currentYaw += yawDiff * factor;
        currentPitch += (targetPitch - currentPitch) * factor;
    }

    // ===== ゲッター =====
    public EnergyStorage getEnergyStorage() { return energyStorage; }
    public ItemStackHandler getItemHandler() { return itemHandler; }
    public State getCannonState() { return state; }
    public int getTotalBlocks() { return totalBlocks; }
    public int getPlacedBlocks() { return placedBlocks; }
    public long getCachedPlayerEmc() { return cachedPlayerEmc; }
    public List<BlockPlacement> getPendingPlacements() { return Collections.unmodifiableList(pendingPlacements); }
    public LinkedHashMap<String, Integer> getBlockSummary() { return blockSummary; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public boolean isAe2Available() { return ae2Available; }
    public String getCurrentBlockRegistryName() { return currentBlockRegistryName; }
    public long getInternalEmcBuffer() { return internalEmcBuffer; }
    public String getMissingBlockName() { return missingBlockName; }
    public long getTotalEmcUsed() { return totalEmcUsed; }

    // ===== セーブ/ロード =====
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putString("State", state.name());
        tag.putInt("TotalBlocks", totalBlocks);
        tag.putInt("PlacedBlocks", placedBlocks);
        tag.putLong("CachedEmc", cachedPlayerEmc);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);

        tag.put("Items", itemHandler.serializeNBT(registries));

        tag.putString("ReplaceMode", replaceMode.name());
        tag.putBoolean("SkipMissing", skipMissing);
        tag.putBoolean("SkipTileEntities", skipTileEntities);
        tag.putBoolean("UseEmc", useEmc);
        tag.putString("StorageMode", storageMode.name());
        tag.putBoolean("ReuseSchematic", reuseSchematic);
        tag.putInt("BlocksPerTick", blocksPerTick);
        tag.putString("CurrentBlock", currentBlockRegistryName);
        tag.putLong("InternalEmc", internalEmcBuffer);
        tag.putString("MissingBlock", missingBlockName);
        tag.putLong("TotalEmcUsed", totalEmcUsed);
        tag.putBoolean("PreviewVisible", previewVisible);
        if (currentTarget != null) {
            tag.putInt("TargetX", currentTarget.getX());
            tag.putInt("TargetY", currentTarget.getY());
            tag.putInt("TargetZ", currentTarget.getZ());
        }

        // フィラーモード
        tag.putBoolean("FillerMode", fillerMode);
        tag.putString("FillerModule", fillerModule.name());
        if (fillerPos1 != null) {
            tag.putInt("FillerPos1X", fillerPos1.getX());
            tag.putInt("FillerPos1Y", fillerPos1.getY());
            tag.putInt("FillerPos1Z", fillerPos1.getZ());
        }
        if (fillerPos2 != null) {
            tag.putInt("FillerPos2X", fillerPos2.getX());
            tag.putInt("FillerPos2Y", fillerPos2.getY());
            tag.putInt("FillerPos2Z", fillerPos2.getZ());
        }

        CompoundTag summaryTag = new CompoundTag();
        for (var entry : blockSummary.entrySet()) {
            summaryTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("BlockSummary", summaryTag);

        // プレースメント保存
        ListTag placementList = new ListTag();
        for (BlockPlacement p : pendingPlacements) {
            CompoundTag pt = new CompoundTag();
            pt.putInt("X", p.pos().getX());
            pt.putInt("Y", p.pos().getY());
            pt.putInt("Z", p.pos().getZ());
            pt.put("BlockState", NbtUtils.writeBlockState(p.state()));
            if (p.nbt() != null) {
                pt.put("TileNbt", p.nbt());
            }
            placementList.add(pt);
        }
        tag.put("Placements", placementList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("Energy")) {
            try {
                var field = EnergyStorage.class.getDeclaredField("energy");
                field.setAccessible(true);
                field.setInt(energyStorage, Math.min(tag.getInt("Energy"), MAX_ENERGY));
            } catch (Exception e) {
                energyStorage.receiveEnergy(tag.getInt("Energy"), false);
            }
        }
        if (tag.contains("State")) {
            try { state = State.valueOf(tag.getString("State")); }
            catch (Exception e) { state = State.IDLE; }
        }
        totalBlocks = tag.getInt("TotalBlocks");
        placedBlocks = tag.getInt("PlacedBlocks");
        cachedPlayerEmc = tag.getLong("CachedEmc");
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");

        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
            // 旧バージョン(4スロット)からの移行: スロット数が足りない場合は再作成
            if (itemHandler.getSlots() < TOTAL_SLOTS) {
                ItemStackHandler oldHandler = new ItemStackHandler(itemHandler.getSlots());
                oldHandler.deserializeNBT(registries, tag.getCompound("Items"));
                // サイズを強制的に拡張するため、新しいハンドラーで再初期化
                itemHandler.setSize(TOTAL_SLOTS);
                for (int i = 0; i < oldHandler.getSlots(); i++) {
                    itemHandler.setStackInSlot(i, oldHandler.getStackInSlot(i));
                }
            }
        }

        if (tag.contains("ReplaceMode")) {
            String modeName = tag.getString("ReplaceMode");
            replaceMode = switch (modeName) {
                case "AIR_ONLY" -> ReplaceMode.REPLACE_EMPTY;
                case "ANY" -> ReplaceMode.REPLACE_ANY;
                case "SOLID_ONLY" -> ReplaceMode.REPLACE_SOLID;
                default -> {
                    try { yield ReplaceMode.valueOf(modeName); }
                    catch (Exception e) { yield ReplaceMode.REPLACE_ANY; }
                }
            };
        }
        skipMissing = tag.getBoolean("SkipMissing");
        skipTileEntities = tag.getBoolean("SkipTileEntities");
        if (tag.contains("UseEmc")) useEmc = tag.getBoolean("UseEmc");
        if (tag.contains("StorageMode")) {
            try { storageMode = StorageMode.valueOf(tag.getString("StorageMode")); }
            catch (Exception e) { storageMode = StorageMode.AE_AND_CHEST; }
        }
        if (tag.contains("ReuseSchematic")) reuseSchematic = tag.getBoolean("ReuseSchematic");
        if (tag.contains("BlocksPerTick")) blocksPerTick = tag.getInt("BlocksPerTick");
        if (tag.contains("CurrentBlock")) currentBlockRegistryName = tag.getString("CurrentBlock");
        if (tag.contains("InternalEmc")) internalEmcBuffer = tag.getLong("InternalEmc");
        if (tag.contains("MissingBlock")) missingBlockName = tag.getString("MissingBlock");
        if (tag.contains("TotalEmcUsed")) totalEmcUsed = tag.getLong("TotalEmcUsed");

        // プレビュー表示設定
        if (tag.contains("PreviewVisible")) previewVisible = tag.getBoolean("PreviewVisible");

        // 照準ターゲット
        if (tag.contains("TargetX")) {
            currentTarget = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        } else {
            currentTarget = null;
        }

        // フィラーモード
        if (tag.contains("FillerMode")) fillerMode = tag.getBoolean("FillerMode");
        if (tag.contains("FillerModule")) {
            try { fillerModule = FillerModule.valueOf(tag.getString("FillerModule")); }
            catch (Exception e) { fillerModule = FillerModule.FILL; }
        }
        if (tag.contains("FillerPos1X")) {
            fillerPos1 = new BlockPos(tag.getInt("FillerPos1X"), tag.getInt("FillerPos1Y"), tag.getInt("FillerPos1Z"));
        } else { fillerPos1 = null; }
        if (tag.contains("FillerPos2X")) {
            fillerPos2 = new BlockPos(tag.getInt("FillerPos2X"), tag.getInt("FillerPos2Y"), tag.getInt("FillerPos2Z"));
        } else { fillerPos2 = null; }

        blockSummary.clear();
        if (tag.contains("BlockSummary")) {
            CompoundTag summaryTag = tag.getCompound("BlockSummary");
            for (String key : summaryTag.getAllKeys()) {
                blockSummary.put(key, summaryTag.getInt(key));
            }
        }


        // 通常モードのプレースメント復元
        pendingPlacements.clear();
        if (tag.contains("Placements")) {
            ListTag list = tag.getList("Placements", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag pt = list.getCompound(i);
                BlockPos pos = new BlockPos(pt.getInt("X"), pt.getInt("Y"), pt.getInt("Z"));
                BlockState bs = readBlockStateSafe(pt.getCompound("BlockState"));
                CompoundTag tileNbt = pt.contains("TileNbt") ? pt.getCompound("TileNbt") : null;
                pendingPlacements.add(new BlockPlacement(pos, bs, tileNbt));
            }
        }
    }

    // ===== MenuProvider =====
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.emcschematicannon.emc_schematic_cannon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            this.ownerUUID = serverPlayer.getUUID();
            updateCachedEmc(serverPlayer);
            syncToClient();
        }
        return new EMCSchematicCannonMenu(containerId, inventory, this);
    }

    public record BlockPlacement(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
        public BlockPlacement(BlockPos pos, BlockState state) {
            this(pos, state, null);
        }
    }
}
