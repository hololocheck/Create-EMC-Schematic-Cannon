package com.example.advancedschematicannon.client;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.ModRegistry;
import com.example.advancedschematicannon.item.AirPlacementWandItem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.advancedschematicannon.network.WandDistancePacket;

/**
 * 空中設置杖を持っている時、設置先ブロック位置に枠を表示する。
 */
@EventBusSubscriber(modid = AdvancedSchematicCannon.MOD_ID, value = Dist.CLIENT)
public class WandPlacementRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // メインハンドまたはオフハンドに空中設置杖を持っているか
        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();
        boolean holdingWand = mainHand.is(ModRegistry.AIR_PLACEMENT_WAND.get())
                || offHand.is(ModRegistry.AIR_PLACEMENT_WAND.get());
        if (!holdingWand) return;

        // 設置先位置を計算
        BlockPos placePos;
        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            // ブロックに当たっている場合: 隣の空気ブロック
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            // 空中を見ている場合: 視線方向の固定距離
            placePos = AirPlacementWandItem.getTargetPos(mc.player);
        }

        // 設置先が空気でなければ表示しない
        if (!mc.level.getBlockState(placePos).isAir()) return;

        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // 白色ワイヤーフレーム
        AABB box = new AABB(
                placePos.getX() - camera.x, placePos.getY() - camera.y, placePos.getZ() - camera.z,
                placePos.getX() + 1 - camera.x, placePos.getY() + 1 - camera.y, placePos.getZ() + 1 - camera.z);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(event.getPoseStack(), consumer, box, 1.0f, 1.0f, 1.0f, 0.6f);

        bufferSource.endBatch();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!Screen.hasShiftDown()) return;

        double delta = event.getScrollDeltaY();
        if (delta == 0) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!stack.is(ModRegistry.AIR_PLACEMENT_WAND.get())) {
            stack = mc.player.getOffhandItem();
            if (!stack.is(ModRegistry.AIR_PLACEMENT_WAND.get())) return;
        }

        int current = AirPlacementWandItem.getDistance(stack);
        int newDist = current + (delta > 0 ? 1 : -1);
        newDist = Math.max(AirPlacementWandItem.MIN_DISTANCE, Math.min(newDist, AirPlacementWandItem.MAX_DISTANCE));

        if (newDist != current) {
            AirPlacementWandItem.setDistance(stack, newDist);
            PacketDistributor.sendToServer(new WandDistancePacket(newDist));
            mc.player.displayClientMessage(
                    Component.translatable("message.advancedschematicannon.wand_distance", newDist)
                            .withStyle(ChatFormatting.GOLD), true);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        // ミドルクリック(ボタン2) + Shift
        if (event.getButton() != 2 || event.getAction() != 1) return;
        if (!Screen.hasShiftDown()) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!stack.is(ModRegistry.AIR_PLACEMENT_WAND.get())) {
            stack = mc.player.getOffhandItem();
            if (!stack.is(ModRegistry.AIR_PLACEMENT_WAND.get())) return;
        }

        int current = AirPlacementWandItem.getDistance(stack);
        if (current != AirPlacementWandItem.DEFAULT_DISTANCE) {
            AirPlacementWandItem.setDistance(stack, AirPlacementWandItem.DEFAULT_DISTANCE);
            PacketDistributor.sendToServer(new WandDistancePacket(AirPlacementWandItem.DEFAULT_DISTANCE));
            mc.player.displayClientMessage(
                    Component.translatable("message.advancedschematicannon.wand_distance_reset", AirPlacementWandItem.DEFAULT_DISTANCE)
                            .withStyle(ChatFormatting.GOLD), true);
        }
        event.setCanceled(true);
    }
}
