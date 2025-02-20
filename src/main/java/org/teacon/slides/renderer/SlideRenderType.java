package org.teacon.slides.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexFormat;
import org.teacon.slides.Slideshow;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class SlideRenderType extends RenderLayer.MultiPhase {

	private static final String ID_ICON = Slideshow.ID + "icon";

	public SlideRenderType(int texture) {
		super(Slideshow.ID, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true,
				MultiPhaseParameters.builder()
						.shader(CUTOUT_SHADER)
						.transparency(TRANSLUCENT_TRANSPARENCY)
						.depthTest(LEQUAL_DEPTH_TEST)
						.cull(ENABLE_CULLING)
						.lightmap(ENABLE_LIGHTMAP)
						.overlay(DISABLE_OVERLAY_COLOR)
						.layering(NO_LAYERING)
						.target(MAIN_TARGET)
						.texturing(DEFAULT_TEXTURING)
						.writeMaskState(ALL_MASK)
						.lineWidth(FULL_LINE_WIDTH)
						.build(true));
		Runnable action = this.beginAction;
		this.beginAction = () -> {
			action.run();
			RenderSystem.setShaderTexture(0, texture);
		};
	}

	SlideRenderType(Identifier texture) {
		super(ID_ICON, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true,
				MultiPhaseParameters.builder()
						.shader(CUTOUT_SHADER)
						.transparency(TRANSLUCENT_TRANSPARENCY)
						.depthTest(LEQUAL_DEPTH_TEST)
						.cull(ENABLE_CULLING)
						.lightmap(ENABLE_LIGHTMAP)
						.overlay(DISABLE_OVERLAY_COLOR)
						.layering(NO_LAYERING)
						.target(MAIN_TARGET)
						.texturing(DEFAULT_TEXTURING)
						.writeMaskState(ALL_MASK)
						.lineWidth(FULL_LINE_WIDTH)
						.build(true));
		Runnable action = this.beginAction;
		this.beginAction = () -> {
			action.run();
			RenderSystem.setShaderTexture(0, texture);
		};
	}
}
