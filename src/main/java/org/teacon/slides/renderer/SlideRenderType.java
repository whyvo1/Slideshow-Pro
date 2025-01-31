package org.teacon.slides.renderer;

import com.google.common.collect.ImmutableList;
import org.teacon.slides.Slideshow;
import org.teacon.slides.util.UtilitiesClient;

import java.util.Objects;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class SlideRenderType extends RenderLayer {

	private static final ImmutableList<RenderPhase> GENERAL_STATES = RenderUtils.getRenderStateShards();

	private final int mHashCode;

	public SlideRenderType(int texture) {
		super(Slideshow.ID, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
				RenderUtils.getMode(), 256, false, true,
				() -> {
					GENERAL_STATES.forEach(RenderPhase::startDrawing);
					RenderUtils.startDrawingTexture(texture);
				},
				() -> GENERAL_STATES.forEach(RenderPhase::endDrawing));
		mHashCode = Objects.hash(super.hashCode(), GENERAL_STATES, texture);
	}

	SlideRenderType(Identifier texture) {
		super(Slideshow.ID, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
				RenderUtils.getMode(), 256, false, true,
				() -> {
					GENERAL_STATES.forEach(RenderPhase::startDrawing);
					UtilitiesClient.beginDrawingTexture(texture);
				},
				() -> GENERAL_STATES.forEach(RenderPhase::endDrawing));
		mHashCode = Objects.hash(super.hashCode(), GENERAL_STATES, texture);
	}

	@Override
	public int hashCode() {
		return mHashCode;
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}
}
