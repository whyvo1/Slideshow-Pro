package org.teacon.slides;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.teacon.slides.config.Config;
import org.teacon.slides.projector.ProjectorScreen;
import org.teacon.slides.network.ProjectorImageInfoS2CPacket;
import org.teacon.slides.renderer.ProjectorRenderer;
import org.teacon.slides.renderer.SlideState;
import org.teacon.slides.util.RegistryClient;

public class SlideshowClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		RegistryClient.registerBlockEntityRenderer(Slideshow.PROJECTOR_BLOCK_ENTITY, (ctx) -> new ProjectorRenderer());
		RegistryClient.registerBlockRenderType(Slideshow.PROJECTOR_BLOCK, RenderLayer.getCutout());
		RegistryClient.registerTickEvent(SlideState::tick);
		RegistryClient.registerClientStoppingEvent(SlideState::onPlayerLeft);
		RegistryClient.registerNetworkReceiver(Slideshow.PACKET_TAG_UPDATE, ProjectorImageInfoS2CPacket::handle);
		HandledScreens.register(Slideshow.PROJECTOR_SCREEN_HANDLER, ProjectorScreen::new);

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			private final Identifier id = Identifier.of(Slideshow.ID, "client_reload");
			@Override
			public void reload(ResourceManager resourceManager) {
				SlideState.clearCacheID();
				Config.refreshProperties();
			}

			@Override
			public Identifier getFabricId() {
				return this.id;
			}
		});
	}
}
