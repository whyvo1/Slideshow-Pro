package org.teacon.slides.texture;

import org.jetbrains.annotations.NotNull;
import org.teacon.slides.renderer.SlideRenderType;

public interface TextureProvider extends AutoCloseable {

	@NotNull
	SlideRenderType updateAndGet(long tick, float partialTick);

	int getWidth();

	int getHeight();

	@Override
	void close();
}
