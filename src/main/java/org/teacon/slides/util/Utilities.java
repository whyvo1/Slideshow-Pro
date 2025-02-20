package org.teacon.slides.util;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Collections;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class Utilities {
    public static final Marker MARKER = MarkerManager.getMarker("Network");

    public static <T extends BlockEntity> BlockEntityType<T> getBlockEntityType(TileEntitySupplier<T> supplier, Block block) {
        return new BlockEntityType<>(supplier::supplier, Collections.singleton(block), null);
    }

    @FunctionalInterface
    public interface TileEntitySupplier<T extends BlockEntity> {
        T supplier();
    }

    public static void sendOverLayMessage(PlayerEntity player, Text message) {
        if(player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, message));
        }
    }

    public static int forPlayersTacking(ServerWorld world, BlockPos pos, Consumer<ServerPlayerEntity> consumer) {
        int i = 0;
        for(ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
            consumer.accept(player);
            i++;
        }
        return i;
    }
}
