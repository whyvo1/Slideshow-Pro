package org.teacon.slides.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.slides.Slideshow;
import org.teacon.slides.cache.ImageCache;
import org.teacon.slides.texture.*;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;


public final class SlideState {

	private static final Executor RENDER_EXECUTOR = r -> RenderSystem.recordRenderCall(r::run);

	private static final int RECYCLE_SECONDS = 120; // 2min
	private static final int RETRY_INTERVAL_SECONDS = 30; // 30s
	private static long sAnimationTick;

	private static final int CLEANER_INTERVAL_SECONDS = 720; // 12min
	private static int sCleanerTimer;

	private static final AtomicReference<ConcurrentHashMap<String, SlideState>> sCache;
	private static final AtomicReference<ConcurrentHashMap<String, SlideState>> sCacheID;

	static {
		sCache = new AtomicReference<>(new ConcurrentHashMap<>());
		sCacheID = new AtomicReference<>(new ConcurrentHashMap<>());
	}

	public static void clearCacheID() {
		sCacheID.getAcquire().clear();
	}

	public static void tick(MinecraftClient client) {
		if (!client.isPaused()) {
			if (++sAnimationTick % 20 == 0) {
				ConcurrentHashMap<String, SlideState> map = sCache.getAcquire();
				if (!map.isEmpty()) {
					map.entrySet().removeIf(entry -> entry.getValue().update());
				}
				map = sCacheID.getAcquire();
				if (!map.isEmpty()) {
					map.entrySet().removeIf(entry -> entry.getValue().update());
				}
				if (++sCleanerTimer > CLEANER_INTERVAL_SECONDS) {
					int n = ImageCache.getInstance().cleanResources();
					if (n != 0) {
						Slideshow.LOGGER.debug("Cleanup {} http cache image resources", n);
					}
					sCleanerTimer = 0;
				}
			}
		}
	}

	public static void onPlayerLeft(MinecraftClient minecraft) {
		RenderSystem.recordRenderCall(() -> {
			ConcurrentHashMap<String, SlideState> map = sCache.getAndSet(new ConcurrentHashMap<>());
			int size = map.size();
			map.values().forEach(s -> s.mSlide.close());
			map.clear();
			map = sCacheID.getAndSet(new ConcurrentHashMap<>());
			size += map.size();
			map.values().forEach(s -> s.mSlide.close());
			Slideshow.LOGGER.debug("Release {} slide images", size);
			map.clear();
		});
	}

	public static long getAnimationTick() {
		return sAnimationTick;
	}

	@Nullable
	public static Slide getSlide(String location, boolean fromID) {
		if (location == null || location.isEmpty()) {
			return null;
		}
		if(fromID) {
			return sCacheID.getAcquire().computeIfAbsent(location, SlideState::getSlideStateFromID).getWithUpdate();
		}
		return sCache.getAcquire().computeIfAbsent(location, SlideState::new).getWithUpdate();
	}

	public static void cacheSlide(String location, boolean fromID) {
		if (location == null || location.isEmpty()) {
			return;
		}
		if(fromID) {
			sCacheID.getAcquire().computeIfAbsent(location, SlideState::getSlideStateFromID);
			return;
		}
		sCache.getAcquire().computeIfAbsent(location, SlideState::new);
	}

	/**
	 * Current slide and state.
	 */
	private Slide mSlide;
	private State mState;

	private int mCounter;

	private SlideState() {}

	@NotNull
	private static SlideState getSlideStateFromID(String location) {
		SlideState s = new SlideState();
		Identifier ID = Identifier.tryParse(location);
		if (ID == null) {
			s.mSlide = Slide.empty();
			s.mState = State.FAILED_OR_EMPTY;
			s.mCounter = RETRY_INTERVAL_SECONDS;
		} else {
			s.mSlide = Slide.loading();
			s.mState = State.LOADING;
			s.mCounter = RECYCLE_SECONDS;
			ImageCache.getInstance().getResourceFromPack(ID).thenCompose(data -> createTexture(location, data)).thenAccept(textureProvider -> {
				if (s.mState == State.LOADING) {
					s.mSlide = Slide.make(textureProvider);
					s.mState = State.LOADED;
				} else {
					// timeout
					assert s.mState == State.LOADED;
					textureProvider.close();
				}
			}).exceptionally(e -> {
				RenderSystem.recordRenderCall(() -> {
					assert s.mState == State.LOADING;
					s.mSlide = Slide.failed();
					s.mState = State.FAILED_OR_EMPTY;
					s.mCounter = RETRY_INTERVAL_SECONDS;
				});
				return null;
			});
		}
		return s;
	}

	private SlideState(String location) {
		URI uri = createURI(location);
		if (uri == null) {
			mSlide = Slide.empty();
			mState = State.FAILED_OR_EMPTY;
			mCounter = RETRY_INTERVAL_SECONDS;
		} else {
			mSlide = Slide.loading();
			mState = State.LOADING;
			mCounter = RECYCLE_SECONDS;
			ImageCache.getInstance().getResource(uri, true).thenCompose(data -> createTexture(location, data)).thenAccept(textureProvider -> {
				if (mState == State.LOADING) {
					mSlide = Slide.make(textureProvider);
					mState = State.LOADED;
				} else {
					// timeout
					assert mState == State.LOADED;
					textureProvider.close();
				}
			}).exceptionally(e -> {
				RenderSystem.recordRenderCall(() -> {
					assert mState == State.LOADING;
					mSlide = Slide.failed();
					mState = State.FAILED_OR_EMPTY;
					mCounter = RETRY_INTERVAL_SECONDS;
				});
				return null;
			});
		}
	}

	@NotNull
	private Slide getWithUpdate() {
		if (mState != State.FAILED_OR_EMPTY) {
			mCounter = RECYCLE_SECONDS;
		}
		return mSlide;
	}

	private boolean update() {
		if (--mCounter < 0) {
			RenderSystem.recordRenderCall(() -> {
				if (mState == State.LOADED) {
					assert mSlide instanceof Slide.Image;
					mSlide.close();
				} else if (mState == State.LOADING) {
					assert mSlide == Slide.loading();
					// timeout
					mState = State.LOADED;
				} else {
					assert mSlide instanceof Slide.Icon;
					assert mState == State.FAILED_OR_EMPTY;
				}
			});
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "SlideState{" +
				"slide=" + mSlide +
				", state=" + mState +
				", counter=" + mCounter +
				'}';
	}

	private static CompletableFuture<TextureProvider> createTexture(String name, byte[] data) {
		boolean isGif = name.endsWith(".gif") || GIFDecoder.checkMagic(data);
		boolean isWebP = name.endsWith(".webp") || WebPDecoder.checkMagic(data);
		return CompletableFuture.supplyAsync(
				isGif ? () -> new AnimatedTextureProvider(data) :
						() -> new StaticTextureProvider(data, isWebP), RENDER_EXECUTOR);
	}

	@Nullable
	public static URI createURI(String location) {
		if (StringUtils.isNotBlank(location)) {
			try {
				return URI.create(location);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	public enum State {
		/**
		 * States that will be changed at the next tick
		 * <p>
		 * NOTHING: the slide is newly created and ready for loading.
		 * LOADING: a slide is loading and a loading image is displayed (expired after {@link #RECYCLE_SECONDS}).
		 */
		NOTHING, LOADING,
		/**
		 *
		 * States that will not be changed but can be expired
		 * <p>
		 * LOADED: a network resource is succeeded to retrieve (no expiration if the slide is rendered).
		 * FAILED_OR_EMPTY: it is empty or failed to retrieve the network resource (expired after {@link
		 * #RETRY_INTERVAL_SECONDS}).
		 */
		LOADED, FAILED_OR_EMPTY
	}
}
