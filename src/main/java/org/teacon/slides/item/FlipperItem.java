package org.teacon.slides.item;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.teacon.slides.util.RegistryClient;
import org.teacon.slides.Slideshow;
import org.teacon.slides.util.Text;
import org.teacon.slides.util.Utilities;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;

import java.util.List;

public class FlipperItem extends Item {
    private static boolean attackKeyDown = false;

    public FlipperItem(Settings properties) {
        super(properties);
    }

    public static int[] getProjectorPos(ItemStack stack) {
        if(!stack.isOf(Slideshow.FLIPPER_ITEM) || !stack.hasNbt() || stack.getNbt().getIntArray("projector") == null) {
            return null;
        }
        int[] intArray = stack.getNbt().getIntArray("projector");
        if(intArray == null || intArray.length < 3) {
            return null;
        }
        return intArray;
    }

    public static void setProjectorPos(ItemStack stack, @Nullable BlockPos pos) {
        if(!stack.isOf(Slideshow.FLIPPER_ITEM)) {
            return;
        }
        if(pos == null) {
            stack.removeSubNbt("projector");
            return;
        }
        stack.setSubNbt("projector", new NbtIntArray(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
    }

    public static boolean trySendFlip(World world, PlayerEntity player, ItemStack itemStack, boolean back, boolean init) {
        int[] pos = getProjectorPos(itemStack);
        if(pos == null) {
            Utilities.sendOverLayMessage(player, Text.translatable("info.slide_show.need_bound").formatted(Formatting.DARK_RED));
            return false;
        }
        BlockEntity entity = world.getBlockEntity(new BlockPos(pos[0], pos[1], pos[2]));
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand interactionHand) {
        ItemStack itemStack = player.getStackInHand(interactionHand);
        if(world.isClient()) {
            return TypedActionResult.success(itemStack, world.isClient());
        }
        if(trySendFlip(world, player, itemStack, false, player.isSneaking())){
            return TypedActionResult.success(itemStack, true);
        }

        return TypedActionResult.fail(itemStack);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemStack, world, entity, slot, selected);
        if(world.isClient() && selected) {
            if(attackKeyDown != MinecraftClient.getInstance().options.keyAttack.isPressed()) {
                attackKeyDown = !attackKeyDown;
                if(attackKeyDown) {
                    sendServerFlipBack(slot);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, @Nullable World world, List<net.minecraft.text.Text> list, TooltipContext tooltipFlag) {
        int[] pos = getProjectorPos(itemStack);
        if(pos == null) {
            list.add(Text.translatable("item.slide_show.flipper.tooltip.not_bound").formatted(Formatting.RED));
            list.add(Text.translatable("item.slide_show.flipper.tooltip.not_bound1"));
            super.appendTooltip(itemStack, world, list, tooltipFlag);
            return;
        }
        list.add(Text.translatable("item.slide_show.flipper.tooltip.bound", pos[0], pos[1], pos[2]).formatted(Formatting.AQUA));
        super.appendTooltip(itemStack, world, list, tooltipFlag);
    }

    private static void sendServerFlipBack(int i) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeInt(i);
        RegistryClient.sendToServer(Slideshow.PACKET_FLIP_BACK, buffer);
    }

    public static void handleServerFlipBack(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayer, PacketByteBuf packet) {
        int i = packet.readInt();
        minecraftServer.execute(() -> {
            ItemStack itemStack = serverPlayer.getInventory().getStack(i);
            if(itemStack.isOf(Slideshow.FLIPPER_ITEM) && trySendFlip(serverPlayer.world, serverPlayer, itemStack, true, false)){
                GameProfile profile = serverPlayer.getGameProfile();
                Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for flip back: player = {}", profile);
            }
        });
    }

    @Override
    public boolean canMine(BlockState blockState, World world, BlockPos blockPos, PlayerEntity player) {
        return false;
    }
}
