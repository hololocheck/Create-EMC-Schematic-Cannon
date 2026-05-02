package com.example.advancedschematicannon.integration;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AE2連携ヘルパー。
 * キャノン自身のグリッドノード、または隣接AE2ブロックのグリッドを使用してME倉庫からアイテムを引き出す。
 */
public class AE2Integration {

    private AE2Integration() {}

    /**
     * ME倉庫からアイテムを引き出す。
     * 1. キャノン自身のグリッドノードを試す
     * 2. 失敗時、隣接AE2ブロックのグリッドからフォールバック抽出
     */
    public static boolean tryExtractItem(Object gridNodeManager, BlockState targetState,
                                          Level level, BlockPos cannonPos) {
        try {
            return tryExtractItemInternal(gridNodeManager, targetState, level, cannonPos);
        } catch (NoClassDefFoundError | Exception e) {
            AdvancedSchematicCannon.LOGGER.warn("AE2 extraction failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ME倉庫からItemStackを引き出す（cable_bus等用）。
     */
    public static boolean tryExtractItem(Object gridNodeManager, net.minecraft.world.item.ItemStack needed,
                                          Level level, BlockPos cannonPos) {
        if (needed.isEmpty()) return false;
        try {
            return tryExtractItemByStackInternal(gridNodeManager, needed, level, cannonPos);
        } catch (NoClassDefFoundError | Exception e) {
            AdvancedSchematicCannon.LOGGER.warn("AE2 ItemStack extraction failed: {}", e.getMessage());
            return false;
        }
    }

    // ========== 旧API互換（gridNodeManagerのみ） ==========

    public static boolean tryExtractItem(Object gridNodeManager, BlockState targetState) {
        if (gridNodeManager == null) return false;
        try {
            return tryExtractFromGrid(getActiveGrid(gridNodeManager), targetState,
                    getActionSource(gridNodeManager));
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    public static boolean tryExtractItem(Object gridNodeManager, net.minecraft.world.item.ItemStack needed) {
        if (gridNodeManager == null || needed.isEmpty()) return false;
        try {
            return tryExtractFromGridByStack(getActiveGrid(gridNodeManager), needed,
                    getActionSource(gridNodeManager));
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    // ========== 内部実装 ==========

    private static boolean tryExtractItemInternal(Object gridNodeManager, BlockState targetState,
                                                    Level level, BlockPos cannonPos) {
        // 1. キャノン自身のグリッドノードを試す
        if (gridNodeManager != null) {
            var grid = getActiveGrid(gridNodeManager);
            if (grid != null) {
                var source = getActionSource(gridNodeManager);
                if (tryExtractFromGrid(grid, targetState, source)) return true;
            }
        }

        // 2. フォールバック: 隣接AE2ブロックのグリッドを使用
        return tryExtractViaAdjacentGrid(level, cannonPos, targetState);
    }

    private static boolean tryExtractItemByStackInternal(Object gridNodeManager,
                                                           net.minecraft.world.item.ItemStack needed,
                                                           Level level, BlockPos cannonPos) {
        // 1. キャノン自身のグリッドノードを試す
        if (gridNodeManager != null) {
            var grid = getActiveGrid(gridNodeManager);
            if (grid != null) {
                var source = getActionSource(gridNodeManager);
                if (tryExtractFromGridByStack(grid, needed, source)) return true;
            }
        }

        // 2. フォールバック: 隣接AE2ブロックのグリッドを使用
        return tryExtractByStackViaAdjacentGrid(level, cannonPos, needed);
    }

    /**
     * gridNodeManagerからグリッドを取得。
     * isActive()チェックをスキップ: マルチプレイではAE2電力源がチャンク未ロードで
     * powered=falseになる場合があるが、ストレージ自体はアクセス可能な場合がある。
     */
    private static appeng.api.networking.IGrid getActiveGrid(Object gridNodeManager) {
        if (gridNodeManager == null) return null;
        var manager = (AE2GridNodeManager) gridNodeManager;
        var gridNode = manager.getNode();
        if (gridNode == null) {
            AdvancedSchematicCannon.LOGGER.info("[AE2] Own grid node is null");
            return null;
        }
        var grid = gridNode.getGrid();
        if (grid == null) {
            AdvancedSchematicCannon.LOGGER.info("[AE2] Own grid node has no grid (powered={}, channels={})",
                    gridNode.isPowered(), gridNode.meetsChannelRequirements());
        }
        return grid;
    }

    private static appeng.api.networking.security.IActionSource getActionSource(Object gridNodeManager) {
        return appeng.api.networking.security.IActionSource.ofMachine((AE2GridNodeManager) gridNodeManager);
    }

    /**
     * 隣接ブロックからAE2のグリッドを探索する。
     * キャノン自身のグリッドノードが接続できない場合のフォールバック。
     * isActive()不要: powered=falseでもストレージアクセスを試みる。
     */
    private static boolean tryExtractViaAdjacentGrid(Level level, BlockPos cannonPos, BlockState targetState) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = cannonPos.relative(dir);
            var host = level.getCapability(
                    appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, neighborPos);
            if (host == null) continue;

            var neighborNode = host.getGridNode(dir.getOpposite());
            if (neighborNode == null) continue;

            var grid = neighborNode.getGrid();
            if (grid == null) continue;

            var source = appeng.api.networking.security.IActionSource.empty();
            if (tryExtractFromGrid(grid, targetState, source)) {
                AdvancedSchematicCannon.LOGGER.info("[AE2] Extracted via adjacent grid at {} ({})",
                        neighborPos, dir);
                return true;
            }
        }
        AdvancedSchematicCannon.LOGGER.info("[AE2] No adjacent AE2 grid found for block extraction");
        return false;
    }

    private static boolean tryExtractByStackViaAdjacentGrid(Level level, BlockPos cannonPos,
                                                              net.minecraft.world.item.ItemStack needed) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = cannonPos.relative(dir);
            var host = level.getCapability(
                    appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, neighborPos);
            if (host == null) continue;

            var neighborNode = host.getGridNode(dir.getOpposite());
            if (neighborNode == null) continue;

            var grid = neighborNode.getGrid();
            if (grid == null) continue;

            var source = appeng.api.networking.security.IActionSource.empty();
            if (tryExtractFromGridByStack(grid, needed, source)) {
                AdvancedSchematicCannon.LOGGER.info("[AE2] Extracted ItemStack via adjacent grid at {} ({})",
                        neighborPos, dir);
                return true;
            }
        }
        return false;
    }

    // ========== アイテム搬入（撤去モジュール用） ==========

    /**
     * ME倉庫にアイテムを搬入する。
     */
    public static boolean tryInsertItem(Object gridNodeManager, net.minecraft.world.item.ItemStack stack,
                                         Level level, BlockPos cannonPos) {
        if (stack.isEmpty()) return false;
        try {
            return tryInsertItemInternal(gridNodeManager, stack, level, cannonPos);
        } catch (NoClassDefFoundError | Exception e) {
            AdvancedSchematicCannon.LOGGER.warn("AE2 insertion failed: {}", e.getMessage());
            return false;
        }
    }

    private static boolean tryInsertItemInternal(Object gridNodeManager, net.minecraft.world.item.ItemStack stack,
                                                   Level level, BlockPos cannonPos) {
        // 1. キャノン自身のグリッドノードを試す
        if (gridNodeManager != null) {
            var grid = getActiveGrid(gridNodeManager);
            if (grid != null) {
                var source = getActionSource(gridNodeManager);
                if (tryInsertToGrid(grid, stack, source)) return true;
            }
        }

        // 2. フォールバック: 隣接AE2ブロックのグリッドを使用
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = cannonPos.relative(dir);
            var host = level.getCapability(
                    appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, neighborPos);
            if (host == null) continue;
            var neighborNode = host.getGridNode(dir.getOpposite());
            if (neighborNode == null) continue;
            var grid = neighborNode.getGrid();
            if (grid == null) continue;
            var source = appeng.api.networking.security.IActionSource.empty();
            if (tryInsertToGrid(grid, stack, source)) return true;
        }
        return false;
    }

    private static boolean tryInsertToGrid(appeng.api.networking.IGrid grid,
                                             net.minecraft.world.item.ItemStack stack,
                                             appeng.api.networking.security.IActionSource actionSource) {
        if (grid == null) return false;
        var storageService = grid.getService(appeng.api.networking.storage.IStorageService.class);
        if (storageService == null) return false;
        var inventory = storageService.getInventory();
        if (inventory == null) return false;

        var aeKey = appeng.api.stacks.AEItemKey.of(stack);
        if (aeKey != null) {
            long inserted = inventory.insert(aeKey, stack.getCount(),
                    appeng.api.config.Actionable.MODULATE, actionSource);
            return inserted > 0;
        }
        return false;
    }

    // ========== グリッドからの抽出共通処理 ==========

    private static boolean tryExtractFromGrid(appeng.api.networking.IGrid grid, BlockState targetState,
                                                appeng.api.networking.security.IActionSource actionSource) {
        var storageService = grid.getService(appeng.api.networking.storage.IStorageService.class);
        if (storageService == null) return false;

        var inventory = storageService.getInventory();
        if (inventory == null) return false;

        // アイテム取得: Block.asItem() + レジストリ名フォールバック
        var block = targetState.getBlock();
        net.minecraft.world.item.Item item = block.asItem();
        if (item == net.minecraft.world.item.Items.AIR) {
            var blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(blockId)) {
                item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(blockId);
            }
        }
        if (item == net.minecraft.world.item.Items.AIR) return false;

        var targetItem = new net.minecraft.world.item.ItemStack(item);
        var targetBlock = targetState.getBlock();

        // まず完全一致で試す
        var aeKey = appeng.api.stacks.AEItemKey.of(targetItem);
        if (aeKey != null) {
            long stored = inventory.extract(aeKey, 1,
                    appeng.api.config.Actionable.SIMULATE, actionSource);
            if (stored <= 0) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] extract miss: key={} stored=0", aeKey);
            }
            long extracted = inventory.extract(aeKey, 1,
                    appeng.api.config.Actionable.MODULATE, actionSource);
            if (extracted > 0) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] extracted crafted/available item: key={} amount={}", aeKey, extracted);
                return true;
            }
        }

        // 完全一致で失敗した場合、全アイテムを走査してBlockItem逆引きも試す
        var availableStacks = inventory.getAvailableStacks();
        for (var key : availableStacks.keySet()) {
            if (key instanceof appeng.api.stacks.AEItemKey itemKey) {
                boolean matches = itemKey.getItem() == targetItem.getItem();
                if (!matches && itemKey.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                    matches = blockItem.getBlock() == targetBlock;
                }
                if (matches) {
                    long extracted = inventory.extract(key, 1,
                            appeng.api.config.Actionable.MODULATE, actionSource);
                    if (extracted > 0) {
                        AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] extracted by fallback scan: key={} amount={}", key, extracted);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean tryExtractFromGridByStack(appeng.api.networking.IGrid grid,
                                                       net.minecraft.world.item.ItemStack needed,
                                                       appeng.api.networking.security.IActionSource actionSource) {
        if (grid == null) return false;

        var storageService = grid.getService(appeng.api.networking.storage.IStorageService.class);
        if (storageService == null) return false;

        var inventory = storageService.getInventory();
        if (inventory == null) return false;

        var aeKey = appeng.api.stacks.AEItemKey.of(needed);
        if (aeKey != null) {
            long stored = inventory.extract(aeKey, 1,
                    appeng.api.config.Actionable.SIMULATE, actionSource);
            if (stored <= 0) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] extract miss: key={} stored=0", aeKey);
            }
            long extracted = inventory.extract(aeKey, 1,
                    appeng.api.config.Actionable.MODULATE, actionSource);
            if (extracted > 0) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] extracted crafted/available stack: key={} amount={}", aeKey, extracted);
                return true;
            }
        }
        return false;
    }

    // ========== 自動クラフト ==========

    /**
     * ME network に craft pattern があるかチェックし、あれば craft リクエストを投入する。
     * @return true if a pattern exists AND a job was requested (item not yet available — caller must wait).
     *         false if no pattern exists or the grid is unreachable (caller should treat as missing).
     */
    public static boolean tryAutoCraft(Object gridNodeManager, BlockState targetState,
                                         Level level, BlockPos cannonPos) {
        return tryAutoCraft(gridNodeManager, targetState, level, cannonPos, 1L);
    }

    public static boolean tryAutoCraft(Object gridNodeManager, BlockState targetState,
                                         Level level, BlockPos cannonPos, long amount) {
        if (targetState == null || targetState.isAir()) return false;
        var block = targetState.getBlock();
        net.minecraft.world.item.Item item = block.asItem();
        if (item == net.minecraft.world.item.Items.AIR) {
            var blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(blockId)) {
                item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(blockId);
            }
        }
        if (item == net.minecraft.world.item.Items.AIR) return false;
        return tryAutoCraft(gridNodeManager, new net.minecraft.world.item.ItemStack(item), level, cannonPos, amount);
    }

    public static boolean tryAutoCraft(Object gridNodeManager, net.minecraft.world.item.ItemStack needed,
                                         Level level, BlockPos cannonPos) {
        return tryAutoCraft(gridNodeManager, needed, level, cannonPos, Math.max(1, needed.getCount()));
    }

    public static boolean tryAutoCraft(Object gridNodeManager, net.minecraft.world.item.ItemStack needed,
                                         Level level, BlockPos cannonPos, long amount) {
        if (needed.isEmpty() || amount <= 0) return false;
        try {
            // 1. キャノン自身のグリッドノードを優先
            if (gridNodeManager != null) {
                var grid = getActiveGrid(gridNodeManager);
                if (grid != null && requestCraft(grid, needed, amount, level,
                        (AE2GridNodeManager) gridNodeManager, null)) {
                    return true;
                }
            }
            // 2. フォールバック: 隣接AE2グリッド
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = cannonPos.relative(dir);
                var host = level.getCapability(
                        appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, neighborPos);
                if (host == null) continue;
                var neighborNode = host.getGridNode(dir.getOpposite());
                if (neighborNode == null) continue;
                var grid = neighborNode.getGrid();
                if (grid == null) continue;
                if (requestCraft(grid, needed, amount, level, null, neighborNode)) return true;
            }
        } catch (NoClassDefFoundError | Exception e) {
            AdvancedSchematicCannon.LOGGER.debug("AE2 autocraft error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Tracks in-flight AE2 craft requests so the cannon does not submit the same
     * job every tick, while still allowing the next request as soon as AE2 finishes.
     */
    private static final java.util.concurrent.ConcurrentHashMap<PendingCraftKey, PendingCraft> PENDING_CRAFTS =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CRAFT_CALCULATION_TIMEOUT_MS = 30_000L;
    private static final long STANDALONE_JOB_RETRY_MS = 1_000L;

    private record PendingCraftKey(appeng.api.networking.IGrid grid, appeng.api.stacks.AEKey what) {}

    private static final class PendingCraft {
        private volatile long retryAfterMs;
        private volatile appeng.api.networking.crafting.ICraftingLink link;

        private PendingCraft(long startedAtMs) {
            this.retryAfterMs = startedAtMs + CRAFT_CALCULATION_TIMEOUT_MS;
        }

        private boolean isActive(long nowMs) {
            var currentLink = link;
            if (currentLink != null) {
                try {
                    return !currentLink.isDone() && !currentLink.isCanceled();
                } catch (Throwable ignored) {
                    return nowMs < retryAfterMs;
                }
            }
            return nowMs < retryAfterMs;
        }

        private void markSubmitted(appeng.api.networking.crafting.ICraftingLink link) {
            this.link = link;
            this.retryAfterMs = link == null
                    ? System.currentTimeMillis() + STANDALONE_JOB_RETRY_MS
                    : Long.MAX_VALUE;
        }
    }

    private static PendingCraft reservePendingCraft(PendingCraftKey key) {
        while (true) {
            long now = System.currentTimeMillis();
            var existing = PENDING_CRAFTS.get(key);
            if (existing != null) {
                if (existing.isActive(now)) return null;
                if (!PENDING_CRAFTS.remove(key, existing)) continue;
            }

            var pending = new PendingCraft(now);
            if (PENDING_CRAFTS.putIfAbsent(key, pending) == null) {
                return pending;
            }
        }
    }

    private static void clearPendingCraft(PendingCraftKey key, PendingCraft pending) {
        PENDING_CRAFTS.remove(key, pending);
    }

    private static void schedulePendingCleanup(PendingCraftKey key, PendingCraft pending) {
        java.util.concurrent.CompletableFuture.delayedExecutor(
                5L, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
            if (!pending.isActive(System.currentTimeMillis())) {
                clearPendingCraft(key, pending);
            } else if (pending.link != null) {
                schedulePendingCleanup(key, pending);
            }
        });
    }

    private static long getStoredAmount(appeng.api.networking.IGrid grid,
                                          appeng.api.stacks.AEKey key,
                                          appeng.api.networking.security.IActionSource source) {
        try {
            var storageService = grid.getService(appeng.api.networking.storage.IStorageService.class);
            if (storageService == null || storageService.getInventory() == null) return -1L;
            return storageService.getInventory().extract(
                    key, Long.MAX_VALUE, appeng.api.config.Actionable.SIMULATE, source);
        } catch (Throwable t) {
            return -1L;
        }
    }

    private static String formatKeyCounter(appeng.api.stacks.KeyCounter counter) {
        if (counter == null) return "null";
        if (counter.isEmpty()) return "none";

        StringBuilder builder = new StringBuilder();
        int shown = 0;
        for (var entry : counter) {
            if (shown > 0) builder.append(", ");
            var key = entry.getKey();
            builder.append(key == null ? "null" : key.getId())
                    .append(" x")
                    .append(entry.getLongValue());
            shown++;
            if (shown >= 16) {
                int remaining = counter.size() - shown;
                if (remaining > 0) builder.append(", ... +").append(remaining).append(" more");
                break;
            }
        }
        return builder.toString();
    }

    private static boolean requestCraft(appeng.api.networking.IGrid grid,
                                          net.minecraft.world.item.ItemStack needed,
                                          long amount,
                                          Level level,
                                          AE2GridNodeManager gridNodeManager,
                                          appeng.api.networking.IGridNode fallbackRequesterNode) {
        try {
            var craftingService = grid.getService(appeng.api.networking.crafting.ICraftingService.class);
            if (craftingService == null) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 autocraft] no ICraftingService on grid");
                return false;
            }

            var aeKey = appeng.api.stacks.AEItemKey.of(needed);
            if (aeKey == null) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 autocraft] AEItemKey.of returned null for {}", needed);
                return false;
            }

            // レシピが登録されているかチェック
            if (!craftingService.isCraftable(aeKey)) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] pattern not craftable: key={} item={} amount={}",
                        aeKey, needed.getItem(), amount);
                return false;
            }

            // ゲーム内のServerLevelでのみ submitJob 可能
            if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;

            final var source = gridNodeManager != null
                    ? appeng.api.networking.security.IActionSource.ofMachine(gridNodeManager)
                    : appeng.api.networking.security.IActionSource.empty();
            // gridNodeManager が ICraftingSimulationRequester を実装しているなら、それを使う。
            // 隣接グリッド(gridNodeManager==null)の場合は一時 Requester を使う。
            final appeng.api.networking.crafting.ICraftingSimulationRequester simRequester;
            final appeng.api.networking.crafting.ICraftingRequester craftingRequester;
            if (gridNodeManager != null) {
                gridNodeManager.setSimulationSource(source);
                simRequester = gridNodeManager;
                craftingRequester = gridNodeManager;
            } else if (fallbackRequesterNode != null) {
                var requester = new StaticCraftingRequester(serverLevel, source, fallbackRequesterNode);
                simRequester = requester;
                craftingRequester = requester;
            } else {
                simRequester = new StaticCraftingRequester(serverLevel, source, null);
                craftingRequester = null;
            }

            final var pendingKey = new PendingCraftKey(grid, aeKey);
            final var pendingCraft = reservePendingCraft(pendingKey);
            if (pendingCraft == null) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] request already pending: key={} amount={}", aeKey, amount);
                return true;
            }

            long storedBefore = getStoredAmount(grid, aeKey, source);
            AdvancedSchematicCannon.LOGGER.info(
                    "[AE2 missing craft] request start: key={} item={} amount={} storedBefore={} requester={} fallbackNode={}",
                    aeKey, needed.getItem(), amount, storedBefore,
                    gridNodeManager != null ? "own-node" : "adjacent-node",
                    fallbackRequesterNode != null);

            // CPU一覧を診断ログ出力
            try {
                int cpuCount = 0;
                int busyCount = 0;
                long maxStorage = 0;
                for (var cpu : craftingService.getCpus()) {
                    cpuCount++;
                    if (cpu.isBusy()) busyCount++;
                    long storage = cpu.getAvailableStorage();
                    if (storage > maxStorage) maxStorage = storage;
                }
                AdvancedSchematicCannon.LOGGER.info(
                        "[AE2 autocraft] CPUs: total={}, busy={}, maxAvailableBytes={}",
                        cpuCount, busyCount, maxStorage);
            } catch (Throwable t) {
                AdvancedSchematicCannon.LOGGER.info("[AE2 autocraft] CPU enum error: {}", t.toString());
            }

            // 計算(AE2は独自のバックグラウンドスレッドで実行) — Future<ICraftingPlan>
            final java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan> planFuture;
            try {
                planFuture = craftingService.beginCraftingCalculation(
                        serverLevel, simRequester, aeKey, amount,
                        appeng.api.networking.crafting.CalculationStrategy.REPORT_MISSING_ITEMS);
            } catch (RuntimeException | Error e) {
                clearPendingCraft(pendingKey, pendingCraft);
                throw e;
            }

            AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] calculation dispatched: key={} amount={}", aeKey, amount);

            // バックグラウンドでfuture.get()待機 → サーバースレッドでsubmitJob
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                appeng.api.networking.crafting.ICraftingPlan plan;
                try {
                    plan = planFuture.get(30L, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Throwable t) {
                    AdvancedSchematicCannon.LOGGER.warn("[AE2 missing craft] calculation failed: key={} error={}",
                            aeKey, t.toString());
                    clearPendingCraft(pendingKey, pendingCraft);
                    return;
                }
                if (plan == null) {
                    AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] plan is null: key={}", aeKey);
                    clearPendingCraft(pendingKey, pendingCraft);
                    return;
                }

                // plan の詳細をログ出力
                try {
                    AdvancedSchematicCannon.LOGGER.info(
                            "[AE2 missing craft] plan result: key={} simulation={} bytes={} finalOutput={} missing=[{}] emitted=[{}] used=[{}]",
                            aeKey,
                            plan.simulation(),
                            plan.bytes(),
                            plan.finalOutput(),
                            formatKeyCounter(plan.missingItems()),
                            formatKeyCounter(plan.emittedItems()),
                            formatKeyCounter(plan.usedItems()));
                } catch (Throwable t) {
                    AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] plan details unavailable: {}", t.toString());
                }

                if (plan.simulation()) {
                    AdvancedSchematicCannon.LOGGER.info(
                            "[AE2 missing craft] NOT submitted because AE reported missing ingredients: key={} missing=[{}]",
                            aeKey, formatKeyCounter(plan.missingItems()));
                    clearPendingCraft(pendingKey, pendingCraft);
                    return;
                }
                // submitJobはサーバースレッドで実行
                serverLevel.getServer().execute(() -> {
                    try {
                        var result = craftingService.submitJob(plan, craftingRequester, null, false, source);
                        if (result == null) {
                            AdvancedSchematicCannon.LOGGER.warn("[AE2 missing craft] submitJob returned null: key={}", aeKey);
                            clearPendingCraft(pendingKey, pendingCraft);
                            return;
                        }
                        // 結果詳細
                        String detail;
                        try {
                            detail = String.format("successful=%s, errorCode=%s, errorDetail=%s, link=%s",
                                    result.successful(), result.errorCode(), result.errorDetail(),
                                    result.link());
                        } catch (Throwable t) {
                            detail = "result=" + result;
                        }
                        if (!result.successful()) {
                            AdvancedSchematicCannon.LOGGER.warn("[AE2 missing craft] submitJob FAILED: key={} detail={}",
                                    aeKey, detail);
                            clearPendingCraft(pendingKey, pendingCraft);
                        } else {
                            AdvancedSchematicCannon.LOGGER.info("[AE2 missing craft] submitJob OK: key={} detail={}",
                                    aeKey, detail);
                            final var link = result.link();
                            pendingCraft.markSubmitted(link);
                            if (gridNodeManager != null && link != null) {
                                gridNodeManager.addLink(link);
                            }
                            if (craftingRequester instanceof StaticCraftingRequester staticRequester && link != null) {
                                staticRequester.addLink(link);
                            }
                            schedulePendingCleanup(pendingKey, pendingCraft);
                        }
                    } catch (Throwable t) {
                        AdvancedSchematicCannon.LOGGER.error("[AE2 autocraft] submitJob exception for " + aeKey, t);
                        clearPendingCraft(pendingKey, pendingCraft);
                    }
                });
            });
            return true;
        } catch (NoClassDefFoundError | Exception e) {
            AdvancedSchematicCannon.LOGGER.error("[AE2 autocraft] requestCraft exception", e);
            return false;
        }
    }

    /**
     * AE2自動クラフト要求用requester（level参照ヘルパー）。
     * AE2の ICraftingSimulationRequester#getActionSource() を実装。
     */
    private static class StaticCraftingRequester implements appeng.api.networking.crafting.ICraftingSimulationRequester,
            appeng.api.networking.crafting.ICraftingRequester {
        @SuppressWarnings("unused")
        private final net.minecraft.server.level.ServerLevel level;
        private final appeng.api.networking.security.IActionSource source;
        private final appeng.api.networking.IGridNode actionableNode;
        private final java.util.Set<appeng.api.networking.crafting.ICraftingLink> activeLinks =
                java.util.concurrent.ConcurrentHashMap.newKeySet();

        StaticCraftingRequester(net.minecraft.server.level.ServerLevel level,
                                appeng.api.networking.security.IActionSource source,
                                appeng.api.networking.IGridNode actionableNode) {
            this.level = level;
            this.source = source;
            this.actionableNode = actionableNode;
        }

        @Override public appeng.api.networking.security.IActionSource getActionSource() { return source; }

        @Override public appeng.api.networking.IGridNode getActionableNode() { return actionableNode; }

        @Override
        public com.google.common.collect.ImmutableSet<appeng.api.networking.crafting.ICraftingLink> getRequestedJobs() {
            return com.google.common.collect.ImmutableSet.copyOf(activeLinks);
        }

        @Override
        public long insertCraftedItems(appeng.api.networking.crafting.ICraftingLink link,
                                       appeng.api.stacks.AEKey what,
                                       long amount,
                                       appeng.api.config.Actionable mode) {
            return 0;
        }

        @Override
        public void jobStateChange(appeng.api.networking.crafting.ICraftingLink link) {
            activeLinks.remove(link);
        }

        private void addLink(appeng.api.networking.crafting.ICraftingLink link) {
            if (link != null) activeLinks.add(link);
        }
    }

    // ========== 電力供給 (AE → FE) ==========

    /**
     * AE2グリッドからエネルギーを抽出してFE換算値を返す。1 AE = 1 FE として変換。
     * グリッドが接続されていない、またはエネルギーが不足している場合は 0 を返す。
     */
    public static int transferEnergyFromAE2(Object gridNodeManager, int maxReceive) {
        if (gridNodeManager == null || maxReceive <= 0) return 0;
        try {
            var grid = getActiveGrid(gridNodeManager);
            if (grid == null) return 0;
            var energyService = grid.getService(appeng.api.networking.energy.IEnergyService.class);
            if (energyService == null) return 0;
            double extracted = energyService.extractAEPower(
                    maxReceive,
                    appeng.api.config.Actionable.MODULATE,
                    appeng.api.config.PowerMultiplier.ONE);
            return (int) Math.floor(extracted);
        } catch (NoClassDefFoundError | Exception e) {
            return 0;
        }
    }
}
