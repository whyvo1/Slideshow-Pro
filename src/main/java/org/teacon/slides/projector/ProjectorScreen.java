package org.teacon.slides.projector;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import net.minecraft.util.math.Vec2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;
import org.teacon.slides.Slideshow;
import org.teacon.slides.network.ProjectorAfterUpdateC2SPayload;
import org.teacon.slides.network.ProjectorExportC2SPayload;
import net.minecraft.text.Text;
import org.teacon.slides.util.RegistryClient;
import org.teacon.slides.renderer.SlideState;


@SuppressWarnings("ConstantConditions")
public final class ProjectorScreen extends HandledScreen<ProjectorScreenHandler> {

	private static final Identifier
			GUI_TEXTURE = Identifier.of(Slideshow.ID, "textures/gui/projector.png");

	private static final Text
			IMAGE_TEXT = Text.translatable("gui.slide_show.section.image"),
			OFFSET_TEXT = Text.translatable("gui.slide_show.section.offset"),
			OTHERS_TEXT = Text.translatable("gui.slide_show.section.others"),
			URL_TEXT = Text.translatable("gui.slide_show.url"),
			ID_TEXT = Text.translatable("gui.slide_show.id"),
			EXPORT_TEXT = Text.translatable("gui.slide_show.export"),
			CONTAINER_TEXT = Text.translatable("gui.slide_show.container"),
			COLOR_TEXT = Text.translatable("gui.slide_show.color"),
			WIDTH_TEXT = Text.translatable("gui.slide_show.width"),
			HEIGHT_TEXT = Text.translatable("gui.slide_show.height"),
			OFFSET_X_TEXT = Text.translatable("gui.slide_show.offset_x"),
			OFFSET_Y_TEXT = Text.translatable("gui.slide_show.offset_y"),
			OFFSET_Z_TEXT = Text.translatable("gui.slide_show.offset_z"),
			FLIP_TEXT = Text.translatable("gui.slide_show.flip"),
			ROTATE_TEXT = Text.translatable("gui.slide_show.rotate"),
			SINGLE_DOUBLE_SIDED_TEXT = Text.translatable("gui.slide_show.single_double_sided");

	private TextFieldWidget mURLInput;
	private TextFieldWidget mColorInput;
	private TextFieldWidget mWidthInput;
	private TextFieldWidget mHeightInput;
	private TextFieldWidget mOffsetXInput;
	private TextFieldWidget mOffsetYInput;
	private TextFieldWidget mOffsetZInput;

	private ScreenTexturedButtonWidget mSwitchURL;
	private ScreenTexturedButtonWidget mSwitchID;
	private ScreenTexturedButtonWidget mSwitchContainer;
	private ScreenTexturedButtonWidget mButtonExport;
	private ScreenTexturedButtonWidget mSwitchSingleSided;
	private ScreenTexturedButtonWidget mSwitchDoubleSided;

	private SourceType mSourceType;
	private boolean mDoubleSided;
	private int mImageColor = ~0;
	private Vec2f mImageSize = Vec2f.SOUTH_EAST_UNIT;
	private Vector3f mImageOffset = new Vector3f();

	private ProjectorBlock.InternalRotation mRotation = ProjectorBlock.InternalRotation.NONE;

	private boolean mInvalidURL = true;
	private boolean mInvalidColor = true;
	private boolean mInvalidWidth = true, mInvalidHeight = true;
	private boolean mInvalidOffsetX = true, mInvalidOffsetY = true, mInvalidOffsetZ = true;

	private final ProjectorBlockEntity mEntity;
	private final int imageWidth;
	private final int imageHeight;

	public ProjectorScreen(ProjectorScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		BlockEntity blockEntity = MinecraftClient.getInstance().world.getBlockEntity(handler.getPos());
		mEntity = blockEntity instanceof ProjectorBlockEntity ? (ProjectorBlockEntity) blockEntity : null;
		imageWidth = 176;
		imageHeight = 217;
	}

	@Override
	protected void init() {
		super.init();
		if (mEntity == null) {
			return;
		}
		mSourceType = mEntity.mSourceType;

		final int leftPos = (width - imageWidth) / 2;
		final int topPos = (height - imageHeight) / 2;

		mURLInput = new TextFieldWidget(textRenderer, leftPos + 30, topPos + 29, 137, 16,
				Text.translatable("gui.slide_show.url"));
		mURLInput.setMaxLength(512);
		mURLInput.setChangedListener(text -> {
			if (StringUtils.isNotBlank(text)) {
				mInvalidURL = SlideState.createURI(text) == null;
			} else {
				mInvalidURL = false;
			}
			mURLInput.setEditableColor(mInvalidURL ? 0xE04B4B : 0xE0E0E0);
		});
		mURLInput.setText(mEntity.mLocation);
		mURLInput.setEditable(mSourceType != SourceType.ContainerBlock);
		addDrawableChild(mURLInput);
		if(mSourceType != SourceType.ContainerBlock) {
			setInitialFocus(mURLInput);
		}

		mColorInput = new TextFieldWidget(textRenderer, leftPos + 55, topPos + 155, 56, 16,
				Text.translatable("gui.slide_show.color"));
		mColorInput.setMaxLength(8);
		mColorInput.setChangedListener(text -> {
			try {
				mImageColor = Integer.parseUnsignedInt(text, 16);
				mInvalidColor = false;
			} catch (Exception e) {
				mInvalidColor = true;
			}
			mColorInput.setEditableColor(mInvalidColor ? 0xE04B4B : 0xE0E0E0);
		});
		mColorInput.setText(String.format("%08X", mEntity.mColor));
		addDrawableChild(mColorInput);

		mWidthInput = new TextFieldWidget(textRenderer, leftPos + 30, topPos + 51, 56, 16,
				Text.translatable("gui.slide_show.width"));
		mWidthInput.setChangedListener(text -> {
			try {
				Vec2f newSize = new Vec2f(parseFloat(text), mImageSize.y);
				updateSize(newSize);
				mInvalidWidth = false;
			} catch (Exception e) {
				mInvalidWidth = true;
			}
			mWidthInput.setEditableColor(mInvalidWidth ? 0xE04B4B : 0xE0E0E0);
		});
		mWidthInput.setText(floatToString(mEntity.mWidth));
		addDrawableChild(mWidthInput);

		mHeightInput = new TextFieldWidget(textRenderer, leftPos + 111, topPos + 51, 56, 16,
				Text.translatable("gui.slide_show.height"));
		mHeightInput.setChangedListener(input -> {
			try {
				Vec2f newSize = new Vec2f(mImageSize.x, parseFloat(input));
				updateSize(newSize);
				mInvalidHeight = false;
			} catch (Exception e) {
				mInvalidHeight = true;
			}
			mHeightInput.setEditableColor(mInvalidHeight ? 0xE04B4B : 0xE0E0E0);
		});
		mHeightInput.setText(floatToString(mEntity.mHeight));
		addDrawableChild(mHeightInput);

		mOffsetXInput = new TextFieldWidget(textRenderer, leftPos + 30, topPos + 103, 29, 16,
				Text.translatable("gui.slide_show.offset_x"));
		mOffsetXInput.setChangedListener(input -> {
			try {
				mImageOffset = new Vector3f(parseFloat(input), mImageOffset.y(), mImageOffset.z());
				mInvalidOffsetX = false;
			} catch (Exception e) {
				mInvalidOffsetX = true;
			}
			mOffsetXInput.setEditableColor(mInvalidOffsetX ? 0xE04B4B : 0xE0E0E0);
		});
		mOffsetXInput.setText(floatToString(mEntity.mOffsetX));
		addDrawableChild(mOffsetXInput);

		mOffsetYInput = new TextFieldWidget(textRenderer, leftPos + 84, topPos + 103, 29, 16,
				Text.translatable("gui.slide_show.offset_y"));
		mOffsetYInput.setChangedListener(input -> {
			try {
				mImageOffset = new Vector3f(mImageOffset.x(), parseFloat(input), mImageOffset.z());
				mInvalidOffsetY = false;
			} catch (Exception e) {
				mInvalidOffsetY = true;
			}
			mOffsetYInput.setEditableColor(mInvalidOffsetY ? 0xE04B4B : 0xE0E0E0);
		});
		mOffsetYInput.setText(floatToString(mEntity.mOffsetY));
		addDrawableChild(mOffsetYInput);

		mOffsetZInput = new TextFieldWidget(textRenderer, leftPos + 138, topPos + 103, 29, 16,
				Text.translatable("gui.slide_show.offset_z"));
		mOffsetZInput.setChangedListener(input -> {
			try {
				mImageOffset = new Vector3f(mImageOffset.x(), mImageOffset.y(), parseFloat(input));
				mInvalidOffsetZ = false;
			} catch (Exception e) {
				mInvalidOffsetZ = true;
			}
			mOffsetZInput.setEditableColor(mInvalidOffsetZ ? 0xE04B4B : 0xE0E0E0);
		});
		mOffsetZInput.setText(floatToString(mEntity.mOffsetZ));
		addDrawableChild(mOffsetZInput);

		addDrawableChild(new ScreenTexturedButtonWidget(leftPos + 117, topPos + 153, 18, 19, 179, 153, GUI_TEXTURE, button -> {
            ProjectorBlock.InternalRotation newRotation = mRotation.flip();
            updateRotation(newRotation);
        }));
		addDrawableChild(new ScreenTexturedButtonWidget(leftPos + 142, topPos + 153, 18, 19, 179, 173, GUI_TEXTURE, button -> {
			ProjectorBlock.InternalRotation newRotation = mRotation.compose(BlockRotation.CLOCKWISE_90);
			updateRotation(newRotation);
		}));
		mRotation = mEntity.getCachedState().get(ProjectorBlock.ROTATION);

		mSwitchURL = new ScreenTexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 53, GUI_TEXTURE, button -> {
			mSourceType = SourceType.ResourceID;
			mSwitchID.visible = true;
			mSwitchURL.visible = false;
		});
		mSwitchID = new ScreenTexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 73, GUI_TEXTURE, button -> {
			mSourceType = SourceType.ContainerBlock;
			mSwitchContainer.visible = true;
			mSwitchID.visible = false;
			mButtonExport.visible = false;
			mURLInput.setEditable(false);
		});
		mSwitchContainer = new ScreenTexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 93, GUI_TEXTURE, button -> {
			mSourceType = SourceType.URL;
			mSwitchURL.visible = true;
			mSwitchContainer.visible = false;
			mButtonExport.visible = true;
			mURLInput.setEditable(true);
		});

		mButtonExport = new ScreenTexturedButtonWidget(leftPos + 149, topPos + 7, 18, 19, 179, 33, GUI_TEXTURE, button -> this.sendExport());

		mSwitchSingleSided = new ScreenTexturedButtonWidget(leftPos + 9, topPos + 153, 18, 19, 179, 113, GUI_TEXTURE, button -> {
			mDoubleSided = true;
			mSwitchDoubleSided.visible = true;
			mSwitchSingleSided.visible = false;
		});
		mSwitchDoubleSided = new ScreenTexturedButtonWidget(leftPos + 9, topPos + 153, 18, 19, 179, 133, GUI_TEXTURE, button -> {
			mDoubleSided = false;
			mSwitchSingleSided.visible = true;
			mSwitchDoubleSided.visible = false;
		});

		mSwitchURL.visible = mSourceType == SourceType.URL;
		mSwitchID.visible = mSourceType == SourceType.ResourceID;
		mSwitchContainer.visible = mSourceType == SourceType.ContainerBlock;
		mButtonExport.visible = mSourceType != SourceType.ContainerBlock;
		mDoubleSided = mEntity.mDoubleSided;
		mSwitchDoubleSided.visible = mDoubleSided;
		mSwitchSingleSided.visible = !mDoubleSided;
		addDrawableChild(mSwitchURL);
		addDrawableChild(mSwitchID);
		addDrawableChild(mSwitchContainer);
		addDrawableChild(mButtonExport);
		addDrawableChild(mSwitchSingleSided);
		addDrawableChild(mSwitchDoubleSided);
	}

	private void updateRotation(ProjectorBlock.InternalRotation newRotation) {
		if (!mInvalidOffsetX && !mInvalidOffsetY && !mInvalidOffsetZ) {
			Vector3f absolute = relativeToAbsolute(mImageOffset, mImageSize, mRotation);
			Vector3f newRelative = absoluteToRelative(absolute, mImageSize, newRotation);
			mOffsetXInput.setText(floatToString(newRelative.x()));
			mOffsetYInput.setText(floatToString(newRelative.y()));
			mOffsetZInput.setText(floatToString(newRelative.z()));
		}
		mRotation = newRotation;
	}

	private void updateSize(Vec2f newSize) {
		if (!mInvalidOffsetX && !mInvalidOffsetY && !mInvalidOffsetZ) {
			Vector3f absolute = relativeToAbsolute(mImageOffset, mImageSize, mRotation);
			Vector3f newRelative = absoluteToRelative(absolute, newSize, mRotation);
			mOffsetXInput.setText(floatToString(newRelative.x()));
			mOffsetYInput.setText(floatToString(newRelative.y()));
			mOffsetZInput.setText(floatToString(newRelative.z()));
		}
		mImageSize = newSize;
	}


	@Override
	protected void handledScreenTick() {
		if (mEntity == null) {
			client.player.closeHandledScreen();
		}
	}

	private void sendExport() {
		if(this.mSourceType == SourceType.ContainerBlock) {
			return;
		}
		ClientPlayNetworking.send(new ProjectorExportC2SPayload(mSourceType == SourceType.ResourceID, mURLInput.getText()));
	}

	@Override
	public void removed() {
		super.removed();
		if (mEntity == null) {
			return;
		}
		final boolean invalidSize = mInvalidWidth || mInvalidHeight;
		final boolean invalidOffset = mInvalidOffsetX || mInvalidOffsetY || mInvalidOffsetZ;
		if (mSourceType != SourceType.ContainerBlock && !mInvalidURL) {
			mEntity.mLocation = mURLInput.getText();
		}
		if (!mInvalidColor) {
			mEntity.mColor = mImageColor;
		}
		if (!invalidSize) {
			mEntity.mWidth = mImageSize.x;
			mEntity.mHeight = mImageSize.y;
		}
		if (!invalidOffset) {
			mEntity.mOffsetX = mImageOffset.x();
			mEntity.mOffsetY = mImageOffset.y();
			mEntity.mOffsetZ = mImageOffset.z();
		}
		mEntity.needInitContainer = mEntity.mSourceType != mSourceType;
		mEntity.mSourceType = mSourceType;
		mEntity.mDoubleSided = mDoubleSided;
		RegistryClient.sendToServer(new ProjectorAfterUpdateC2SPayload(mEntity, mRotation));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifier) {
		if(this.mEntity == null) {
			return super.keyPressed(keyCode, scanCode, modifier);
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			client.player.closeHandledScreen();
			return true;
		}

		return mURLInput.keyPressed(keyCode, scanCode, modifier) || mURLInput.isActive()
				|| mColorInput.keyPressed(keyCode, scanCode, modifier) || mColorInput.isActive()
				|| mWidthInput.keyPressed(keyCode, scanCode, modifier) || mWidthInput.isActive()
				|| mHeightInput.keyPressed(keyCode, scanCode, modifier) || mHeightInput.isActive()
				|| mOffsetXInput.keyPressed(keyCode, scanCode, modifier) || mOffsetXInput.isActive()
				|| mOffsetYInput.keyPressed(keyCode, scanCode, modifier) || mOffsetYInput.isActive()
				|| mOffsetZInput.keyPressed(keyCode, scanCode, modifier) || mOffsetZInput.isActive()
				|| super.keyPressed(keyCode, scanCode, modifier);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.drawTexture(RenderLayer::getGuiTexturedOverlay, GUI_TEXTURE, (width - imageWidth) / 2, (height - imageHeight) / 2, 0, 0, imageWidth, imageHeight, 256, 256);
	}

	@Override
	protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
		if(mEntity == null) {
			return;
		}
		int alpha = mImageColor >>> 24;
		if (alpha > 0) {
			int red = (mImageColor >> 16) & 255, green = (mImageColor >> 8) & 255, blue = mImageColor & 255;
			RenderSystem.setShaderColor(red / 255.0F, green / 255.0F, blue / 255.0F, alpha / 255.0F);
			ctx.drawTexture(RenderLayer::getGuiTexturedOverlay, GUI_TEXTURE, 38, 131, 180, 194, 10, 10, 256, 256);
			ctx.drawTexture(RenderLayer::getGuiTexturedOverlay, GUI_TEXTURE, 82, 159, 180, 194, 17, 17, 256, 256);
		}

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		ctx.drawTexture(RenderLayer::getGuiTexturedOverlay, GUI_TEXTURE, 82, 159, 202, 194 - mRotation.ordinal() * 20, 17, 17, 256, 256);

		drawCenteredStringWithoutShadow(ctx, textRenderer, IMAGE_TEXT, imageWidth / 2, -14);
		drawCenteredStringWithoutShadow(ctx, textRenderer, OFFSET_TEXT, imageWidth / 2, 60);
		drawCenteredStringWithoutShadow(ctx, textRenderer, OTHERS_TEXT, imageWidth / 2, 112);

		int offsetX = mouseX - (width - imageWidth) / 2, offsetY = mouseY - (height - imageHeight) / 2;
		if (offsetX >= 9 && offsetY >= 27 && offsetX < 27 && offsetY < 46) {
			ctx.drawTooltip(textRenderer, switch (mSourceType) {
				case ResourceID -> ID_TEXT;
				case ContainerBlock -> CONTAINER_TEXT;
				default -> URL_TEXT;
			}, offsetX, offsetY);
		} else if (offsetX >= 149 && offsetY >= 7 && offsetX < 167 && offsetY < 26) {
			if(mSourceType != SourceType.ContainerBlock) {
				ctx.drawTooltip(textRenderer, EXPORT_TEXT, offsetX, offsetY);
			}
		} else if (offsetX >= 34 && offsetY >= 153 && offsetX < 52 && offsetY < 172) {
			ctx.drawTooltip(textRenderer, COLOR_TEXT, offsetX, offsetY);
		} else if (offsetX >= 9 && offsetY >= 49 && offsetX < 27 && offsetY < 68) {
			ctx.drawTooltip(textRenderer, WIDTH_TEXT, offsetX, offsetY);
		} else if (offsetX >= 90 && offsetY >= 49 && offsetX < 108 && offsetY < 68) {
			ctx.drawTooltip(textRenderer, HEIGHT_TEXT, offsetX, offsetY);
		} else if (offsetX >= 9 && offsetY >= 101 && offsetX < 27 && offsetY < 120) {
			ctx.drawTooltip(textRenderer, OFFSET_X_TEXT, offsetX, offsetY);
		} else if (offsetX >= 63 && offsetY >= 101 && offsetX < 81 && offsetY < 120) {
			ctx.drawTooltip(textRenderer, OFFSET_Y_TEXT, offsetX, offsetY);
		} else if (offsetX >= 117 && offsetY >= 101 && offsetX < 135 && offsetY < 120) {
			ctx.drawTooltip(textRenderer, OFFSET_Z_TEXT, offsetX, offsetY);
		} else if (offsetX >= 117 && offsetY >= 153 && offsetX < 135 && offsetY < 172) {
			ctx.drawTooltip(textRenderer, FLIP_TEXT, offsetX, offsetY);
		} else if (offsetX >= 142 && offsetY >= 153 && offsetX < 160 && offsetY < 172) {
			ctx.drawTooltip(textRenderer, ROTATE_TEXT, offsetX, offsetY);
		} else if (offsetX >= 9 && offsetY >= 153 && offsetX < 27 && offsetY < 172) {
			ctx.drawTooltip(textRenderer, SINGLE_DOUBLE_SIDED_TEXT, offsetX, offsetY);
		}
	}

	private static void drawCenteredStringWithoutShadow(DrawContext ctx, TextRenderer textRenderer, Text text, int centerX, int y) {
		OrderedText orderedText = text.asOrderedText();
		ctx.drawText(textRenderer, text, centerX - textRenderer.getWidth(orderedText) / 2, y, 0x404040, false);
	}

	private static float parseFloat(String text) {
		return Math.round(Float.parseFloat(text) * 10000) / 10000F;
	}

	private static String floatToString(float value) {
		return String.valueOf(Math.round(value * 10000) / 10000F);
	}

	private static Vector3f relativeToAbsolute(Vector3f relatedOffset, Vec2f size,
											   ProjectorBlock.InternalRotation rotation) {
		Vector4f center = new Vector4f(0.5F * size.x, 0.0F, 0.5F * size.y, 1.0F);
		// matrix 6: offset for slide (center[new] = center[old] + offset)
		center.mul(new Matrix4f().translate(relatedOffset.x(), -relatedOffset.z(), relatedOffset.y()));
		// matrix 5: translation for slide
		center.mul(new Matrix4f().translate(-0.5F, 0.0F, 0.5F - size.y));
		// matrix 4: internal rotation
		rotation.transform(center);
		// ok, that's enough
		return new Vector3f(center.x(), center.y(), center.z());
	}

	private static Vector3f absoluteToRelative(Vector3f absoluteOffset, Vec2f size,
											   ProjectorBlock.InternalRotation rotation) {
		Vector4f center = new Vector4f(absoluteOffset, 1.0F);
		// inverse matrix 4: internal rotation
		rotation.invert().transform(center);
		// inverse matrix 5: translation for slide
		center.mul(new Matrix4f().translate(0.5F, 0.0F, -0.5F + size.y));
		// subtract (offset = center[new] - center[old])
		center.mul(new Matrix4f().translate(-0.5F * size.x, 0.0F, -0.5F * size.y));
		// ok, that's enough (remember it is (a, -c, b) => (a, b, c))
		return new Vector3f(center.x(), center.z(), -center.y());
	}

	public static class ScreenTexturedButtonWidget extends ButtonWidget {
		public int u;
		public int v;
		public Identifier texture;

		public ScreenTexturedButtonWidget(int x, int y, int width, int height, int u, int v, Identifier texture, ButtonWidget.PressAction pressAction) {
			super(x, y, width, height, ScreenTexts.EMPTY, pressAction, DEFAULT_NARRATION_SUPPLIER);
			this.u = u;
			this.v = v;
			this.texture = texture;
		}

		public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			context.drawTexture(RenderLayer::getGuiTexturedOverlay, this.texture, this.getX(), this.getY(), this.u, this.v, this.width, this.height, 256, 256);
		}

	}

}
