package org.teacon.slides.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Collections;
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
}
