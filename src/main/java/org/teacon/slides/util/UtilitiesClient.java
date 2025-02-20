package org.teacon.slides.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class UtilitiesClient {

	public static void beginDrawingTexture(Identifier textureId) {
		RenderSystem.enableTexture();
		MinecraftClient.getInstance().getTextureManager().bindTexture(textureId);
	}
}
