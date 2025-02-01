package org.teacon.slides.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class NetworkUtilities {

	@FunctionalInterface
	public interface C2SPacketCallback {
		void packetCallback(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf packet);
	}

	@FunctionalInterface
	public interface S2CPacketCallback {
		void packetCallback(MinecraftClient client, PacketByteBuf packet);
	}
}
