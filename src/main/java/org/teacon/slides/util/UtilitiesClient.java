package org.teacon.slides.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class UtilitiesClient {

	public static void beginDrawingTexture(Identifier textureId) {
		MinecraftClient.getInstance().getTextureManager().bindTexture(textureId);
	}
}
