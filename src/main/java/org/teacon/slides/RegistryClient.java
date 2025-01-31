package org.teacon.slides;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.teacon.slides.util.NetworkUtilities;

import java.util.function.Consumer;
import java.util.function.Function;

public class RegistryClient {

	public static void registerBlockRenderType(RenderLayer type, Block block) {
		BlockRenderLayerMap.INSTANCE.putBlock(block, type);
	}

	public static <T extends BlockEntity> void registerTileEntityRenderer(BlockEntityType<T> type, Function<BlockEntityRenderDispatcher, BlockEntityRenderer<? super T>> function) {
		BlockEntityRendererRegistry.INSTANCE.register(type, function);
	}

	public static void registerNetworkReceiver(Identifier resourceLocation, NetworkUtilities.S2CPacketCallback packetCallback) {
		ClientPlayNetworking.registerGlobalReceiver(resourceLocation, (client, handler, buf, sender) -> {
			packetCallback.packetCallback(client, buf);
		});
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
