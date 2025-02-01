package org.teacon.slides.util;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class RegistryClient {

	public static void registerBlockRenderType(Block block, RenderLayer type) {
		BlockRenderLayerMap.INSTANCE.putBlock(block, type);
	}

	public static <T extends BlockEntity> void registerBlockEntityRenderer(BlockEntityType<T> type, BlockEntityRendererFactory<? super T> function) {
		BlockEntityRendererFactories.register(type, function);
	}

	public static void registerNetworkReceiver(Identifier resourceLocation, NetworkUtilities.S2CPacketCallback packetCallback) {
		ClientPlayNetworking.registerGlobalReceiver(resourceLocation, (client, handler, buf, sender) -> packetCallback.packetCallback(client, buf));
	}

	public static void registerClientStoppingEvent(Consumer<MinecraftClient> consumer) {
		ClientLifecycleEvents.CLIENT_STOPPING.register(consumer::accept);
	}

	public static void registerTickEvent(Consumer<MinecraftClient> consumer) {
		ClientTickEvents.START_CLIENT_TICK.register(consumer::accept);
	}

	public static void sendToServer(Identifier id, PacketByteBuf packet) {
		ClientPlayNetworking.send(id, packet);
	}
}
