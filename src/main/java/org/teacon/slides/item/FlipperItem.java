package org.teacon.slides.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.teacon.slides.network.FlipperFlipBackC2SPayload;
import org.teacon.slides.util.RegistryClient;
import org.teacon.slides.Slideshow;
import org.teacon.slides.util.Utilities;
import net.minecraft.text.Text;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;

import java.util.Arrays;
import java.util.List;

public class FlipperItem extends Item {
    private static boolean attackKeyDown = false;

    public FlipperItem(Settings properties) {
        super(properties);
    }

    public static List<Integer> getProjectorPos(ItemStack stack) {
        if(!stack.isOf(Slideshow.FLIPPER_ITEM)) {
            return null;
        }
        List<Integer> intArray = stack.get(Slideshow.PROJECTOR_COMPONENT);
        if(intArray == null || intArray.size() < 3) {
            return null;
        }
        return intArray;
    }

    public static void setProjectorPos(ItemStack stack, @Nullable BlockPos pos) {
        if(!stack.isOf(Slideshow.FLIPPER_ITEM)) {
            return;
        }
        if(pos == null) {
            stack.remove(Slideshow.PROJECTOR_COMPONENT);
            return;
        }
        stack.set(Slideshow.PROJECTOR_COMPONENT, Arrays.asList(pos.getX(), pos.getY(), pos.getZ()));
    }

    public static boolean trySendFlip(World world, PlayerEntity player, ItemStack itemStack, boolean back, boolean init) {
        List<Integer> pos = getProjectorPos(itemStack);
        if(pos == null) {
            Utilities.sendOverLayMessage(player, Text.translatable("info.slide_show.need_bound").formatted(Formatting.DARK_RED));
            return false;
        }
        BlockEntity entity = world.getBlockEntity(new BlockPos(pos.get(0), pos.get(1), pos.get(2)));
        if(!(entity instanceof ProjectorBlockEntity entity1)) {
            Utilities.sendOverLayMessage(player, Text.translatable("info.slide_show.binding_lost").formatted(Formatting.DARK_RED));
            setProjectorPos(itemStack, null);
            return false;
        }
        if(!ProjectorBlock.hasPermission((ServerPlayerEntity) player)) {
            return false;
        }
        if(!entity1.canFlip()) {
            return false;
        }
        if(init) {
            entity1.needInitContainer = true;
            Utilities.sendOverLayMessage(player, Text.translatable("info.slide_show.initialized").formatted(Formatting.AQUA));
            return true;
        }
        entity1.needHandleReadImage = true;
        if(back) {
            entity1.flipBack = true;
        }
        Utilities.sendOverLayMessage(player, Text.translatable("info.slide_show.slide_flipped").formatted(Formatting.AQUA));
        return true;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if(world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if(trySendFlip(world, user, itemStack, false, user.isSneaking())){
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if(world.isClient()) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = context.getStack();
        if (stack.isOf(Slideshow.FLIPPER_ITEM)) {
            BlockPos pos = context.getBlockPos();
            PlayerEntity player = context.getPlayer();
            if(player == null) {
                return ActionResult.FAIL;
            }
            if(world.getBlockEntity(pos) instanceof ProjectorBlockEntity) {
                setProjectorPos(stack, pos);
                Utilities.sendOverLayMessage(player, Text.translatable("info.slide_show.bound_projector").formatted(Formatting.AQUA));
                return ActionResult.CONSUME;
            }
            return trySendFlip(world, player, stack, false, player.isSneaking()) ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack itemStack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemStack, world, entity, slot, selected);
        if(world.isClient() && selected) {
            if(attackKeyDown != MinecraftClient.getInstance().options.attackKey.isPressed()) {
                attackKeyDown = !attackKeyDown;
                if(attackKeyDown) {
                    sendServerFlipBack(slot);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext context, List<Text> list, TooltipType type) {
        List<Integer> pos = getProjectorPos(itemStack);
        if(pos == null) {
            list.add(Text.translatable("item.slide_show.flipper.tooltip.not_bound").formatted(Formatting.RED));
            list.add(Text.translatable("item.slide_show.flipper.tooltip.not_bound1"));
            super.appendTooltip(itemStack, context, list, type);
            return;
        }
        list.add(Text.translatable("item.slide_show.flipper.tooltip.bound", pos.get(0), pos.get(1), pos.get(2)).formatted(Formatting.AQUA));
        super.appendTooltip(itemStack, context, list, type);
    }

    private static void sendServerFlipBack(int i) {
        RegistryClient.sendToServer(new FlipperFlipBackC2SPayload(i));
    }

    @Override
    public boolean canMine(BlockState blockState, World world, BlockPos blockPos, PlayerEntity player) {
        return false;
    }
}
