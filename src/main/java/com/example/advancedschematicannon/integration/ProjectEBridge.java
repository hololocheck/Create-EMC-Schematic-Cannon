package com.example.advancedschematicannon.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.math.BigInteger;

/**
 * Centralised bridge for ProjectE API access.
 *
 * ProjectE is now an optional dependency. All ProjectE class references live in this class so
 * that the EMC cannon block entity can be compiled and instantiated even when ProjectE is absent.
 * Every public method short-circuits to a safe default when ProjectE is not loaded.
 */
public final class ProjectEBridge {

    private static final boolean LOADED = computeLoaded();

    private static boolean computeLoaded() {
        try {
            return ModList.get() != null && ModList.get().isLoaded("projecte");
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    private ProjectEBridge() {}

    public static long getEmcValue(ItemStack stack) {
        if (!LOADED || stack.isEmpty()) return 0L;
        try {
            var proxy = moze_intel.projecte.api.proxy.IEMCProxy.INSTANCE;
            return proxy == null ? 0L : proxy.getValue(stack);
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static boolean hasEmcValue(ItemStack stack) {
        if (!LOADED || stack.isEmpty()) return false;
        try {
            var proxy = moze_intel.projecte.api.proxy.IEMCProxy.INSTANCE;
            return proxy != null && proxy.hasValue(stack);
        } catch (Throwable t) {
            return false;
        }
    }

    public static long getPlayerEmc(ServerPlayer player) {
        if (!LOADED || player == null) return 0L;
        try {
            var proxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
            if (proxy == null) return 0L;
            var provider = proxy.getKnowledgeProviderFor(player.getUUID());
            return provider == null ? 0L : provider.getEmc().longValue();
        } catch (Throwable t) {
            return 0L;
        }
    }

    /** Consume amount from player EMC. Returns true if fully paid. Does not sync. */
    public static boolean consumePlayerEmc(ServerPlayer player, long amount) {
        if (!LOADED || player == null || amount <= 0) return false;
        try {
            var proxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
            if (proxy == null) return false;
            var provider = proxy.getKnowledgeProviderFor(player.getUUID());
            if (provider == null) return false;
            BigInteger current = provider.getEmc();
            BigInteger cost = BigInteger.valueOf(amount);
            if (current.compareTo(cost) < 0) return false;
            provider.setEmc(current.subtract(cost));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Add amount to player EMC. Returns true on success. Does not sync. */
    public static boolean addPlayerEmc(ServerPlayer player, long amount) {
        if (!LOADED || player == null || amount <= 0) return false;
        try {
            var proxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
            if (proxy == null) return false;
            var provider = proxy.getKnowledgeProviderFor(player.getUUID());
            if (provider == null) return false;
            provider.setEmc(provider.getEmc().add(BigInteger.valueOf(amount)));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void syncPlayerEmc(ServerPlayer player) {
        if (!LOADED || player == null) return;
        try {
            var proxy = moze_intel.projecte.api.proxy.ITransmutationProxy.INSTANCE;
            if (proxy == null) return;
            var provider = proxy.getKnowledgeProviderFor(player.getUUID());
            if (provider != null) provider.syncEmc(player);
        } catch (Throwable t) {
            // ignored
        }
    }
}
