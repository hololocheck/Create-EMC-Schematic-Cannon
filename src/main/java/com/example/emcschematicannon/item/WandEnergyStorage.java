package com.example.emcschematicannon.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 空中設置杖のFEエネルギーストレージ実装。
 * DataComponentベースでItemStackにエネルギーを保存する。
 */
public class WandEnergyStorage implements IEnergyStorage {

    private final ItemStack stack;

    public WandEnergyStorage(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int current = AirPlacementWandItem.getEnergy(stack);
        int accepted = Math.min(maxReceive, AirPlacementWandItem.MAX_ENERGY - current);
        if (!simulate && accepted > 0) {
            AirPlacementWandItem.setEnergy(stack, current + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0; // 外部からの抽出は不可
    }

    @Override
    public int getEnergyStored() {
        return AirPlacementWandItem.getEnergy(stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return AirPlacementWandItem.MAX_ENERGY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
