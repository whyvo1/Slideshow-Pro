package org.teacon.slides.util;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

public class UtilitiesClient {

	public static void beginDrawingTexture(Identifier textureId) {
		MinecraftClient.getInstance().getTextureManager().bindTexture(textureId);
	}
}
