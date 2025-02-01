package org.teacon.slides.util;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class RegistryServer {

	public static void registerNetworkReceiver(Identifier resourceLocation, NetworkUtilities.C2SPacketCallback packetCallback) {
		ServerPlayNetworking.registerGlobalReceiver(resourceLocation, (server, player, handler, buf, sender) -> packetCallback.packetCallback(server, player, buf));
	}

	public static void sendToPlayer(ServerPlayerEntity player, Identifier id, PacketByteBuf packet) {
		ServerPlayNetworking.send(player, id, packet);
	}
}
