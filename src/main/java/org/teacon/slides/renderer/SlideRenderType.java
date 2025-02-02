package org.teacon.slides.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexFormat;
import org.teacon.slides.Slideshow;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class SlideRenderType extends RenderLayer.MultiPhase {

	public SlideRenderType(int texture) {
		super(Slideshow.ID, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true,
				MultiPhaseParameters.builder()
						.program(CUTOUT_PROGRAM)
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
						.colorLogic(NO_COLOR_LOGIC)
						.build(true));
		Runnable baseAction = this.beginAction;
		this.beginAction = () -> {
			baseAction.run();
			RenderSystem.setShaderTexture(0, texture);
		};
	}

	SlideRenderType(Identifier texture) {
		super(Slideshow.ID + "_icon", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true,
				MultiPhaseParameters.builder()
						.program(CUTOUT_PROGRAM)
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
						.colorLogic(NO_COLOR_LOGIC)
						.build(true));
		Runnable baseAction = this.beginAction;
		this.beginAction = () -> {
			baseAction.run();
			RenderSystem.setShaderTexture(0, texture);
		};
	}
}
