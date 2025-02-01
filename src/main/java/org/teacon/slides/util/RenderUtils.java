package org.teacon.slides.util;

import com.mojang.blaze3d.systems.RenderSystem;

public class RenderUtils {
	public static void setShaderColor(float r, float g, float b, float a) {
		RenderSystem.setShaderColor(r, g, b, a);
	}

//	public static ImmutableList<RenderPhase> getRenderStateShards() {
//		return ImmutableList.of(
//				TRANSLUCENT_TRANSPARENCY,
//				LEQUAL_DEPTH_TEST,
//				ENABLE_CULLING,
//				ENABLE_LIGHTMAP,
//				DISABLE_OVERLAY_COLOR,
//				NO_LAYERING,
//				MAIN_TARGET,
//				DEFAULT_TEXTURING,
//				ALL_MASK,
//				FULL_LINE_WIDTH
//		);
//	}
}
