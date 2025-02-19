package org.teacon.slides.util;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class Utilities {
    public static final Marker MARKER = MarkerManager.getMarker("Network");

    public static void sendOverLayMessage(PlayerEntity player, Text message) {
        if(player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(new OverlayMessageS2CPacket( message));
        }
    }

    public static int forPlayersTacking(BlockEntity entity, Consumer<ServerPlayerEntity> consumer) {
        int i = 0;
        for(ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
            consumer.accept(player);
            i++;
        }
        return i;
    }
}
