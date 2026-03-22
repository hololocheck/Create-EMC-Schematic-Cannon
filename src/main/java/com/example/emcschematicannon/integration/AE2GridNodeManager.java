package com.example.emcschematicannon.integration;

import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * AE2グリッドノードを管理するクラス。
 * 1チャンネルを消費するデバイスとしてAE2ネットワークに認識される。
 */
public class AE2GridNodeManager implements IInWorldGridNodeHost, IActionHost {

    private final IManagedGridNode managedNode;

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
}
