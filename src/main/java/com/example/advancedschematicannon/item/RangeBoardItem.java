package com.example.advancedschematicannon.item;

import com.example.advancedschematicannon.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 範囲指定ボード - station sound systemから移植。
 * 右クリックでブロック位置を記録し、2点で範囲を指定する。
 * シフト+スクロールで編集する点を選択、シフト+左クリックで解除。
 */
public class RangeBoardItem extends Item {

    public RangeBoardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        // シフト+右クリック
        if (player.isShiftKeyDown()) {
            int editMode = getEditMode(stack);
            if (editMode == 0) {
                // 通常モード: 全範囲クリア
                stack.remove(ModDataComponents.RANGE_POS1.get());
                stack.remove(ModDataComponents.RANGE_POS2.get());
                player.displayClientMessage(
                        Component.translatable("message.advancedschematicannon.range_cleared")
                                .withStyle(ChatFormatting.YELLOW), true);
            } else if (editMode == 1) {
                // 編集モード: Pos1のみクリア
                stack.remove(ModDataComponents.RANGE_POS1.get());
                player.displayClientMessage(
                        Component.translatable("message.advancedschematicannon.pos1_cleared")
                                .withStyle(ChatFormatting.YELLOW), true);
            } else if (editMode == 2) {
                // 編集モード: Pos2のみクリア
                stack.remove(ModDataComponents.RANGE_POS2.get());
                player.displayClientMessage(
                        Component.translatable("message.advancedschematicannon.pos2_cleared")
                                .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.SUCCESS;
        }

        setPosition(stack, player, clickedPos);
        return InteractionResult.SUCCESS;
    }

    private static final double MAX_RANGE = 64.0;

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);

        // シフト+右クリック: クリア処理
        if (player.isShiftKeyDown()) {
            int editMode = getEditMode(stack);
            if (editMode == 0) {
                stack.remove(ModDataComponents.RANGE_POS1.get());
                stack.remove(ModDataComponents.RANGE_POS2.get());
                player.displayClientMessage(
                        Component.translatable("message.advancedschematicannon.range_cleared")
                                .withStyle(ChatFormatting.YELLOW), true);
            } else if (editMode == 1) {
                stack.remove(ModDataComponents.RANGE_POS1.get());
                player.displayClientMessage(
                        Component.translatable("message.advancedschematicannon.pos1_cleared")
                                .withStyle(ChatFormatting.YELLOW), true);
            } else if (editMode == 2) {
                stack.remove(ModDataComponents.RANGE_POS2.get());
                player.displayClientMessage(
                        Component.translatable("message.advancedschematicannon.pos2_cleared")
                                .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.success(stack);
        }

        // 通常右クリック（空中）: 遠距離レイキャストで視線先のブロックを選択
        BlockPos targetPos = getLookTargetBlock(player, level);
        if (targetPos != null) {
            setPosition(stack, player, targetPos);
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * 遠距離レイキャストでプレイヤーの視線先のブロック位置を取得する。
     */
    private static BlockPos getLookTargetBlock(Player player, Level level) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(MAX_RANGE));
        BlockHitResult hitResult = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }
        return null;
    }

    /**
     * 編集モードに応じてPos1またはPos2を設定する。
     */
    private static void setPosition(ItemStack stack, Player player, BlockPos pos) {
        int editMode = getEditMode(stack);
        if (editMode == 1) {
            stack.set(ModDataComponents.RANGE_POS1.get(), pos);
            player.displayClientMessage(
                    Component.translatable("message.advancedschematicannon.pos1_set",
                            pos.getX(), pos.getY(), pos.getZ())
                            .withStyle(ChatFormatting.GREEN), true);
        } else if (editMode == 2) {
            stack.set(ModDataComponents.RANGE_POS2.get(), pos);
            player.displayClientMessage(
                    Component.translatable("message.advancedschematicannon.pos2_set",
                            pos.getX(), pos.getY(), pos.getZ())
                            .withStyle(ChatFormatting.GREEN), true);
        } else {
            // 通常モード: Pos1未設定→Pos1、設定済み→Pos2
            BlockPos pos1 = stack.get(ModDataComponents.RANGE_POS1.get());
            if (pos1 == null) {
                stack.set(ModDataComponents.RANGE_POS1.get(), pos);
                player.displayClientMessage(
                        Component.translatable("message.advancedschematicannon.pos1_set",
                                pos.getX(), pos.getY(), pos.getZ())
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                stack.set(ModDataComponents.RANGE_POS2.get(), pos);
                player.displayClientMessage(
                        Component.translatable("message.advancedschematicannon.pos2_set",
                                pos.getX(), pos.getY(), pos.getZ())
                                .withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    public static int getEditMode(ItemStack stack) {
        Integer mode = stack.get(ModDataComponents.RANGE_EDIT_MODE.get());
        return mode != null ? mode : 0;
    }

    public static boolean hasRange(ItemStack stack) {
        return stack.has(ModDataComponents.RANGE_POS1.get())
                && stack.has(ModDataComponents.RANGE_POS2.get());
    }

    public static BlockPos getPos1(ItemStack stack) {
        return stack.get(ModDataComponents.RANGE_POS1.get());
    }

    public static BlockPos getPos2(ItemStack stack) {
        return stack.get(ModDataComponents.RANGE_POS2.get());
    }

    /**
     * プレイヤーが手に持っている範囲指定ボードを取得する。
     */
    public static ItemStack findHeldRangeBoard(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(ModRegistry.RANGE_BOARD_ITEM.get())) return mainHand;
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(ModRegistry.RANGE_BOARD_ITEM.get())) return offHand;
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos pos1 = stack.get(ModDataComponents.RANGE_POS1.get());
        BlockPos pos2 = stack.get(ModDataComponents.RANGE_POS2.get());

        if (pos1 != null && pos2 != null) {
            tooltip.add(Component.literal("Pos1: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ())
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Pos2: " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ())
                    .withStyle(ChatFormatting.AQUA));
        } else if (pos1 != null) {
            tooltip.add(Component.literal("Pos1: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ())
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.advancedschematicannon.range_board.set_pos2")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.advancedschematicannon.range_board.no_range")
                    .withStyle(ChatFormatting.GRAY));
        }

        int editMode = getEditMode(stack);
        if (editMode == 1) {
            tooltip.add(Component.translatable("tooltip.advancedschematicannon.range_board.editing_pos1")
                    .withStyle(ChatFormatting.YELLOW));
        } else if (editMode == 2) {
            tooltip.add(Component.translatable("tooltip.advancedschematicannon.range_board.editing_pos2")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
