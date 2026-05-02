package com.example.advancedschematicannon.block;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.client.RangeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * EMC概略図砲のBlockEntityRenderer。
 * 動的照準（砲身回転）を行う。
 */
public class EMCSchematicCannonRenderer implements BlockEntityRenderer<EMCSchematicCannonBlockEntity> {

    public static final ModelResourceLocation PIPE_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AdvancedSchematicCannon.MOD_ID,
                    "block/emc_schematic_cannon/pipe"));
    public static final ModelResourceLocation CONNECTOR_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AdvancedSchematicCannon.MOD_ID,
                    "block/emc_schematic_cannon/connector"));

    public EMCSchematicCannonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EMCSchematicCannonBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        var blockState = be.getBlockState();
        var block = blockState.getBlock();
        if (!(block instanceof EMCSchematicCannonBlock) && !(block instanceof EnhancedSchematicCannonBlock)) return;

        // 照準角度の更新: ターゲットがある（作業中）ときのみ砲身を回転
        BlockPos target = be.getCurrentTarget();
        if (target != null) {
            BlockPos cannonPos = be.getBlockPos();
            double dx = target.getX() + 0.5 - (cannonPos.getX() + 0.5);
            double dy = target.getY() + 0.5 - (cannonPos.getY() + 1.0);
            double dz = target.getZ() + 0.5 - (cannonPos.getZ() + 0.5);
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, dz));
            float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);
            float targetPitch = (float) Math.toDegrees(Math.atan2(dy, horizontalDist));
            be.updateAimAngles(targetYaw, targetPitch);
        }

        // 動的照準: 補間されたyaw/pitchを使用
        float yaw = be.getAnimYaw(partialTick);
        float pitch = be.getAnimPitch(partialTick);
        float yRot = 90f + yaw;

        var mc = Minecraft.getInstance();
        var modelManager = mc.getModelManager();
        var modelRenderer = mc.getBlockRenderer().getModelRenderer();

        BakedModel connectorModel = modelManager.getModel(CONNECTOR_MODEL);
        BakedModel pipeModel = modelManager.getModel(PIPE_MODEL);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());

        // コネクタ描画: Y回転（照準方位角に追従）
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, 0, -0.5);
        modelRenderer.renderModel(poseStack.last(), buffer, null, connectorModel,
                1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
        poseStack.popPose();

        // パイプ描画: Y回転 + Z回転（仰角）
        poseStack.pushPose();
        poseStack.translate(0.5, 15.0 / 16.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.translate(-0.5, -15.0 / 16.0, -0.5);
        modelRenderer.renderModel(poseStack.last(), buffer, null, pipeModel,
                1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
        poseStack.popPose();

        // プレビュー範囲枠の登録/解除
        BlockPos p1 = be.getFillerPos1();
        BlockPos p2 = be.getFillerPos2();
        if (be.isPreviewVisible() && p1 != null && p2 != null) {
            RangeRenderer.updatePreviewRange(be.getBlockPos(), p1, p2);
        } else {
            RangeRenderer.removePreviewRange(be.getBlockPos());
        }
    }

    @Override
    public boolean shouldRenderOffScreen(EMCSchematicCannonBlockEntity be) {
        return true;
    }
}
