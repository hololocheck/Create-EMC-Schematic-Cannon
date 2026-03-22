package com.example.emcschematicannon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 砲台から発射されたブロックの飛行データ。
 * クライアント側でのアニメーション描画に使用。
 */
public class LaunchedItem {
    public final BlockPos from;
    public final BlockPos target;
    public final BlockState blockState;
    public final int totalTicks;
    public int ticksRemaining;

    public LaunchedItem(BlockPos from, BlockPos target, BlockState blockState) {
        this.from = from;
        this.target = target;
        this.blockState = blockState;

        double dx = target.getX() - from.getX();
        double dy = target.getY() - from.getY();
        double dz = target.getZ() - from.getZ();
        double distSqr = dx * dx + dy * dy + dz * dz;
        this.totalTicks = Math.max(10, (int) (Math.sqrt(Math.sqrt(distSqr)) * 4));
        this.ticksRemaining = totalTicks;
    }

    /**
     * 飛行中の補間位置を計算する。
     * XZ: 線形補間、Y: 二次ベジェ曲線（放物弧）
     */
    public double[] getPosition(float partialTicks) {
        float progress = 1.0f - (ticksRemaining - partialTicks) / totalTicks;
        progress = Math.max(0, Math.min(1, progress));

        double dx = target.getX() - from.getX();
        double dy = target.getY() - from.getY();
        double dz = target.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        double x = from.getX() + 0.5 + dx * progress;
        double z = from.getZ() + 0.5 + dz * progress;

        double arcHeight = Math.sqrt(Math.max(dist, 1)) * 0.6 + Math.abs(dy);
        double yStart = from.getY() + 1.5;
        double yEnd = target.getY() + 0.5;
        double yMid = Math.max(yStart, yEnd) + arcHeight;

        double oneMinusT = 1.0 - progress;
        double y = oneMinusT * oneMinusT * yStart
                + 2 * oneMinusT * progress * yMid
                + progress * progress * yEnd;

        return new double[]{x, y, z};
    }

    public float getRotation(float partialTicks) {
        float progress = 1.0f - (ticksRemaining - partialTicks) / totalTicks;
        return progress * 360.0f;
    }

    /** @return true if this item has finished its flight */
    public boolean tick() {
        ticksRemaining--;
        return ticksRemaining <= 0;
    }
}
