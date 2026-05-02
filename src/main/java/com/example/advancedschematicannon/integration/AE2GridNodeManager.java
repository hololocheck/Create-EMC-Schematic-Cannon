package com.example.advancedschematicannon.integration;

import appeng.api.config.Actionable;
import appeng.api.networking.*;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * AE2グリッドノードを管理するクラス。
 * 1チャンネルを消費するデバイスとしてAE2ネットワークに認識される。
 * ICraftingRequester も実装するため、自動クラフトジョブは通常のCPU/アセンブラパイプラインを通る。
 */
public class AE2GridNodeManager implements IInWorldGridNodeHost, IActionHost,
        ICraftingRequester, ICraftingSimulationRequester {

    private final IManagedGridNode managedNode;
    /** 送信中のクラフトリンクを保持。CPUはここからジョブを取り出して実行する。 */
    private final Set<ICraftingLink> activeLinks = new HashSet<>();

    public AE2GridNodeManager() {
        this.managedNode = GridHelper.createManagedNode(this, new IGridNodeListener<AE2GridNodeManager>() {
            @Override
            public void onSaveChanges(AE2GridNodeManager nodeOwner, IGridNode node) {
            }

            @Override
            public void onStateChanged(AE2GridNodeManager nodeOwner, IGridNode node, IGridNodeListener.State state) {
            }
        })
          .setFlags(GridFlags.REQUIRE_CHANNEL)
          .setIdlePowerUsage(0.5)
          .setInWorldNode(true)
          .setExposedOnSides(EnumSet.allOf(Direction.class));
    }

    public void create(Level level, BlockPos pos) {
        managedNode.create(level, pos);
    }

    public void destroy() {
        managedNode.destroy();
        synchronized (activeLinks) {
            for (var link : activeLinks) {
                try { link.cancel(); } catch (Throwable ignored) {}
            }
            activeLinks.clear();
        }
    }

    public void addLink(ICraftingLink link) {
        if (link == null) return;
        synchronized (activeLinks) { activeLinks.add(link); }
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction direction) {
        return managedNode.getNode();
    }

    @Nullable
    public IGridNode getNode() {
        return managedNode.getNode();
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return managedNode.getNode();
    }

    // ===== ICraftingRequester =====
    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        synchronized (activeLinks) {
            return ImmutableSet.copyOf(activeLinks);
        }
    }

    /**
     * CPUが完成したアイテムを渡してくる際に呼ばれる。
     * 0を返すと AE2 はアイテムをME storageへ自動格納する。
     */
    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        return 0;
    }

    /** ジョブ状態変化(完了・キャンセル等)で呼ばれる。 */
    @Override
    public void jobStateChange(ICraftingLink link) {
        synchronized (activeLinks) {
            activeLinks.remove(link);
        }
    }

    // ===== ICraftingSimulationRequester =====
    private IActionSource simulationSource = IActionSource.empty();
    public void setSimulationSource(IActionSource source) { this.simulationSource = source; }

    @Override
    public IActionSource getActionSource() {
        return simulationSource;
    }
}
