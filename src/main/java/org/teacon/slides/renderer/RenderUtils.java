package org.teacon.slides.renderer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;

public abstract class RenderUtils extends RenderLayer {

	public RenderUtils() {
		super(null, null, getMode(), 0, false, false, null, null);
	}

	public static int getMode() {
		return 7;
	}

	public static void setShaderColor(float r, float g, float b, float a) {
		RenderSystem.color4f(r, g, b, a);
	}

	public static void startDrawingTexture(int textureId) {
		RenderSystem.enableTexture();
		if (RenderSystem.isOnRenderThread()) {
			GlStateManager.bindTexture(textureId);
		} else {
			RenderSystem.recordRenderCall(() -> GlStateManager.bindTexture(textureId));
		}
	}

	public static ImmutableList<RenderPhase> getRenderStateShards() {
		return ImmutableList.of(
				TRANSLUCENT_TRANSPARENCY,
				LEQUAL_DEPTH_TEST,
				ENABLE_CULLING,
				ENABLE_LIGHTMAP,
				DISABLE_OVERLAY_COLOR,
				NO_LAYERING,
				MAIN_TARGET,
				DEFAULT_TEXTURING,
				ALL_MASK,
				FULL_LINE_WIDTH
		);
	}
}
