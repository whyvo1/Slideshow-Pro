package org.teacon.slides.util;

import com.mojang.blaze3d.systems.RenderSystem;

public class RenderUtils {
	public static void setShaderColor(float r, float g, float b, float a) {
		RenderSystem.setShaderColor(r, g, b, a);
	}

}
