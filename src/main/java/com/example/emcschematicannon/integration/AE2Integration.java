package com.example.emcschematicannon.integration;

import com.example.emcschematicannon.EMCSchematicCannon;
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
            EMCSchematicCannon.LOGGER.warn("AE2 extraction failed: {}", e.getMessage());
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
            EMCSchematicCannon.LOGGER.warn("AE2 ItemStack extraction failed: {}", e.getMessage());
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
            EMCSchematicCannon.LOGGER.info("[AE2] Own grid node is null");
            return null;
        }
        var grid = gridNode.getGrid();
        if (grid == null) {
            EMCSchematicCannon.LOGGER.info("[AE2] Own grid node has no grid (powered={}, channels={})",
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
                EMCSchematicCannon.LOGGER.info("[AE2] Extracted via adjacent grid at {} ({})",
                        neighborPos, dir);
                return true;
            }
        }
        EMCSchematicCannon.LOGGER.info("[AE2] No adjacent AE2 grid found for block extraction");
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
                EMCSchematicCannon.LOGGER.info("[AE2] Extracted ItemStack via adjacent grid at {} ({})",
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
            EMCSchematicCannon.LOGGER.warn("AE2 insertion failed: {}", e.getMessage());
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
            long extracted = inventory.extract(aeKey, 1,
                    appeng.api.config.Actionable.MODULATE, actionSource);
            if (extracted > 0) return true;
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
                    if (extracted > 0) return true;
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
            long extracted = inventory.extract(aeKey, 1,
                    appeng.api.config.Actionable.MODULATE, actionSource);
            if (extracted > 0) return true;
        }
        return false;
    }
}
