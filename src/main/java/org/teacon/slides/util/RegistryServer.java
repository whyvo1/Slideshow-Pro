package org.teacon.slides.util;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketDecoder;
import net.minecraft.network.codec.ValueFirstEncoder;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public class RegistryServer {

	public static <V extends CustomPayload> void registerNetworkReceiver(
			CustomPayload.Id<V> id,
			final ValueFirstEncoder<PacketByteBuf, V> encoder,
			final PacketDecoder<PacketByteBuf, V> decoder,
			ServerPlayNetworking.PlayPayloadHandler<V> handler
	) {
		PayloadTypeRegistry.playC2S().register(id, PacketCodec.of(encoder, decoder));
		ServerPlayNetworking.registerGlobalReceiver(id, handler);
	}

	public static <V extends CustomPayload> void sendToPlayer(ServerPlayerEntity player, V payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
