package org.teacon.slides.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.teacon.slides.Slideshow;
import org.teacon.slides.renderer.SlideRenderType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionException;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;

public final class AnimatedTextureProvider implements TextureProvider {

	private static final LZWDecoder gRenderThreadDecoder = new LZWDecoder();

	private static float sMaxAnisotropic = -1;

	private final GIFDecoder mDecoder;

	private int mTexture;
	private final SlideRenderType mRenderType;

	private long mFrameStartTime;
	private long mFrameDelayTime;

	public AnimatedTextureProvider(byte[] data) {
		if (sMaxAnisotropic < 0) {
			GLCapabilities caps = GL.getCapabilities();
			if (caps.OpenGL46 ||
					caps.GL_ARB_texture_filter_anisotropic ||
					caps.GL_EXT_texture_filter_anisotropic) {
				sMaxAnisotropic = Math.max(1, glGetFloat(GL46C.GL_MAX_TEXTURE_MAX_ANISOTROPY));
				Slideshow.LOGGER.info("Max anisotropic: {}", sMaxAnisotropic);
			} else {
				sMaxAnisotropic = 0;
			}
		}
		ByteBuffer buffer = null;
		try {
			mDecoder = new GIFDecoder(data, gRenderThreadDecoder, false);
			final int width = mDecoder.getScreenWidth();
			final int height = mDecoder.getScreenHeight();

			buffer = MemoryUtil.memAlloc(width * height * 4);
			mFrameDelayTime = mDecoder.decodeNextFrame(buffer);

			mTexture = glGenTextures();
			RenderSystem.bindTexture(mTexture);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

			if (sMaxAnisotropic > 0) {
				glTexParameterf(GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAX_ANISOTROPY, sMaxAnisotropic);
			}

			glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

			glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
			glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
			mRenderType = new SlideRenderType(mTexture);
		} catch (Throwable t) {
			close();
			throw new CompletionException(t);
		} finally {
			MemoryUtil.memFree(buffer);
		}
	}

	@NotNull
	@Override
	public SlideRenderType updateAndGet(long tick, float partialTick) {
		long timeMillis = (long) ((tick + partialTick) * 50);
		if (mFrameStartTime == 0) {
			mFrameStartTime = timeMillis;
		} else if (mFrameStartTime + mFrameDelayTime <= timeMillis) {
			ByteBuffer buffer = null;
			try {
				final int width = getWidth();
				final int height = getHeight();
				buffer = MemoryUtil.memAlloc(width * height * 4);
				mFrameDelayTime = mDecoder.decodeNextFrame(buffer);
				RenderSystem.bindTexture(mTexture);
				glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
				glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
				glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
				glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
				glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
			} catch (IOException e) {
				mFrameDelayTime = Integer.MAX_VALUE;
			} finally {
				MemoryUtil.memFree(buffer);
			}
			mFrameStartTime = timeMillis;
		}
		return mRenderType;
	}

	@Override
	public int getWidth() {
		return mDecoder.getScreenWidth();
	}

	@Override
	public int getHeight() {
		return mDecoder.getScreenHeight();
	}

	@Override
	public void close() {
		if (mTexture != 0) {
			RenderSystem.deleteTexture(mTexture);
		}
		mTexture = 0;
	}
}
