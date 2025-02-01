package org.teacon.slides.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class Utilities {
    public static final Marker MARKER = MarkerManager.getMarker("Network");

    public static void sendOverLayMessage(PlayerEntity player, Text message) {
        if(player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(new OverlayMessageS2CPacket(message));
        }
    }
}
