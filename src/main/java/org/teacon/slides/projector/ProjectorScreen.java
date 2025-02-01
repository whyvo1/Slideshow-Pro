package org.teacon.slides.projector;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;
import org.teacon.slides.Slideshow;
import org.teacon.slides.network.ProjectorAfterUpdateC2SPacket;
import org.teacon.slides.network.ProjectorExportC2SPacket;
import org.teacon.slides.util.RenderUtils;
import org.teacon.slides.renderer.SlideState;

@SuppressWarnings("ConstantConditions")
public final class ProjectorScreen extends HandledScreen<ProjectorScreenHandler> {

	private static final Identifier
			GUI_TEXTURE = new Identifier(Slideshow.ID, "textures/gui/projector.png");

	private static final net.minecraft.text.Text
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

	private TexturedButtonWidget mSwitchURL;
	private TexturedButtonWidget mSwitchID;
	private TexturedButtonWidget mSwitchContainer;
	private TexturedButtonWidget mButtonExport;
	private TexturedButtonWidget mSwitchSingleSided;
	private TexturedButtonWidget mSwitchDoubleSided;

	private SourceType mSourceType;
	private boolean mDoubleSided;
	private int mImageColor = ~0;
	private Vec2f mImageSize = Vec2f.SOUTH_EAST_UNIT;
	private Vec3f mImageOffset = new Vec3f();

	private ProjectorBlock.InternalRotation mRotation = ProjectorBlock.InternalRotation.NONE;

	private boolean mInvalidURL = true;
	private boolean mInvalidColor = true;
	private boolean mInvalidWidth = true, mInvalidHeight = true;
	private boolean mInvalidOffsetX = true, mInvalidOffsetY = true, mInvalidOffsetZ = true;

	private final ProjectorBlockEntity mEntity;
	private final int imageWidth;
	private final int imageHeight;

	public ProjectorScreen(ProjectorScreenHandler handler, PlayerInventory inventory, net.minecraft.text.Text title) {
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
		client.keyboard.setRepeatEvents(true);

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
				mImageOffset = new Vec3f(parseFloat(input), mImageOffset.getY(), mImageOffset.getZ());
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
				mImageOffset = new Vec3f(mImageOffset.getX(), parseFloat(input), mImageOffset.getZ());
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
				mImageOffset = new Vec3f(mImageOffset.getX(), mImageOffset.getY(), parseFloat(input));
				mInvalidOffsetZ = false;
			} catch (Exception e) {
				mInvalidOffsetZ = true;
			}
			mOffsetZInput.setEditableColor(mInvalidOffsetZ ? 0xE04B4B : 0xE0E0E0);
		});
		mOffsetZInput.setText(floatToString(mEntity.mOffsetZ));
		addDrawableChild(mOffsetZInput);

		addDrawableChild(new TexturedButtonWidget(leftPos + 117, topPos + 153, 18, 19, 179, 153, 0, GUI_TEXTURE, button -> {
			ProjectorBlock.InternalRotation newRotation = mRotation.flip();
			updateRotation(newRotation);
		}));
		addDrawableChild(new TexturedButtonWidget(leftPos + 142, topPos + 153, 18, 19, 179, 173, 0, GUI_TEXTURE, button -> {
			ProjectorBlock.InternalRotation newRotation = mRotation.compose(BlockRotation.CLOCKWISE_90);
			updateRotation(newRotation);
		}));
		mRotation = mEntity.getCachedState().get(ProjectorBlock.ROTATION);

		mSwitchURL = new TexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 53, 0, GUI_TEXTURE, button -> {
			mSourceType = SourceType.ResourceID;
			mSwitchID.visible = true;
			mSwitchURL.visible = false;
		});
		mSwitchID = new TexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 73, 0, GUI_TEXTURE, button -> {
			mSourceType = SourceType.ContainerBlock;
			mSwitchContainer.visible = true;
			mSwitchID.visible = false;
			mButtonExport.visible = false;
			mURLInput.setEditable(false);
		});
		mSwitchContainer = new TexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 93, 0, GUI_TEXTURE, button -> {
			mSourceType = SourceType.URL;
			mSwitchURL.visible = true;
			mSwitchContainer.visible = false;
			mButtonExport.visible = true;
			mURLInput.setEditable(true);
		});

		mButtonExport = new TexturedButtonWidget(leftPos + 149, topPos + 7, 18, 19, 179, 33, 0, GUI_TEXTURE, button -> {
			this.sendExport();
		});

		mSwitchSingleSided = new TexturedButtonWidget(leftPos + 9, topPos + 153, 18, 19, 179, 113, 0, GUI_TEXTURE, button -> {
			mDoubleSided = false;
			mSwitchDoubleSided.visible = true;
			mSwitchSingleSided.visible = false;
		});
		mSwitchDoubleSided = new TexturedButtonWidget(leftPos + 9, topPos + 153, 18, 19, 179, 133, 0, GUI_TEXTURE, button -> {
			mDoubleSided = true;
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
			Vec3f absolute = relativeToAbsolute(mImageOffset, mImageSize, mRotation);
			Vec3f newRelative = absoluteToRelative(absolute, mImageSize, newRotation);
			mOffsetXInput.setText(floatToString(newRelative.getX()));
			mOffsetYInput.setText(floatToString(newRelative.getY()));
			mOffsetZInput.setText(floatToString(newRelative.getZ()));
		}
		mRotation = newRotation;
	}

	private void updateSize(Vec2f newSize) {
		if (!mInvalidOffsetX && !mInvalidOffsetY && !mInvalidOffsetZ) {
			Vec3f absolute = relativeToAbsolute(mImageOffset, mImageSize, mRotation);
			Vec3f newRelative = absoluteToRelative(absolute, newSize, mRotation);
			mOffsetXInput.setText(floatToString(newRelative.getX()));
			mOffsetYInput.setText(floatToString(newRelative.getY()));
			mOffsetZInput.setText(floatToString(newRelative.getZ()));
		}
		mImageSize = newSize;
	}

	@Override
	public void handledScreenTick() {
		if (mEntity == null) {
			client.player.closeHandledScreen();
			return;
		}
		mURLInput.tick();
		mColorInput.tick();
		mWidthInput.tick();
		mHeightInput.tick();
		mOffsetXInput.tick();
		mOffsetYInput.tick();
		mOffsetZInput.tick();
	}

	private void sendExport() {
		if(this.mSourceType == SourceType.ContainerBlock) {
			return;
		}
		new ProjectorExportC2SPacket(mSourceType == SourceType.ResourceID, mURLInput.getText()).sendToServer();
	}

	@Override
	public void removed() {
		super.removed();
		client.keyboard.setRepeatEvents(false);
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
			mEntity.mOffsetX = mImageOffset.getX();
			mEntity.mOffsetY = mImageOffset.getY();
			mEntity.mOffsetZ = mImageOffset.getZ();
		}
		mEntity.needInitContainer = mEntity.mSourceType == mSourceType;
		mEntity.mSourceType = mSourceType;
		mEntity.mDoubleSided = mDoubleSided;
		new ProjectorAfterUpdateC2SPacket(mEntity, mRotation).sendToServer();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifier) {
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
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);
	}

	@Override
	protected void drawBackground(MatrixStack matrixStack, float f, int i, int j) {
		renderBackground(matrixStack);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, GUI_TEXTURE);
		drawTexture(matrixStack, (width - imageWidth) / 2, (height - imageHeight) / 2, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void drawForeground(MatrixStack matrixStack, int mouseX, int mouseY) {
		if(mEntity == null) {
			return;
		}
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderTexture(0, GUI_TEXTURE);

		int alpha = mImageColor >>> 24;
		if (alpha > 0) {
			int red = (mImageColor >> 16) & 255, green = (mImageColor >> 8) & 255, blue = mImageColor & 255;
			RenderUtils.setShaderColor(red / 255.0F, green / 255.0F, blue / 255.0F, alpha / 255.0F);
			drawTexture(matrixStack, 38, 131, 180, 194, 10, 10);
			drawTexture(matrixStack, 82, 159, 180, 194, 17, 17);
		}

		RenderUtils.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		drawTexture(matrixStack, 82, 159, 202, 194 - mRotation.ordinal() * 20, 17, 17);

		drawCenteredStringWithoutShadow(matrixStack, textRenderer, IMAGE_TEXT, imageWidth / 2F, -14);
		drawCenteredStringWithoutShadow(matrixStack, textRenderer, OFFSET_TEXT, imageWidth / 2F, 60);
		drawCenteredStringWithoutShadow(matrixStack, textRenderer, OTHERS_TEXT, imageWidth / 2F, 112);

		int offsetX = mouseX - (width - imageWidth) / 2, offsetY = mouseY - (height - imageHeight) / 2;
		if (offsetX >= 9 && offsetY >= 27 && offsetX < 27 && offsetY < 46) {
			renderTooltip(matrixStack, switch (mSourceType) {
				case ResourceID -> ID_TEXT;
				case ContainerBlock -> CONTAINER_TEXT;
				default -> URL_TEXT;
			}, offsetX, offsetY);
		} else if (offsetX >= 149 && offsetY >= 7 && offsetX < 167 && offsetY < 26) {
			if(mSourceType != SourceType.ContainerBlock) {
				renderTooltip(matrixStack, EXPORT_TEXT, offsetX, offsetY);
			}
		} else if (offsetX >= 34 && offsetY >= 153 && offsetX < 52 && offsetY < 172) {
			renderTooltip(matrixStack, COLOR_TEXT, offsetX, offsetY);
		} else if (offsetX >= 9 && offsetY >= 49 && offsetX < 27 && offsetY < 68) {
			renderTooltip(matrixStack, WIDTH_TEXT, offsetX, offsetY);
		} else if (offsetX >= 90 && offsetY >= 49 && offsetX < 108 && offsetY < 68) {
			renderTooltip(matrixStack, HEIGHT_TEXT, offsetX, offsetY);
		} else if (offsetX >= 9 && offsetY >= 101 && offsetX < 27 && offsetY < 120) {
			renderTooltip(matrixStack, OFFSET_X_TEXT, offsetX, offsetY);
		} else if (offsetX >= 63 && offsetY >= 101 && offsetX < 81 && offsetY < 120) {
			renderTooltip(matrixStack, OFFSET_Y_TEXT, offsetX, offsetY);
		} else if (offsetX >= 117 && offsetY >= 101 && offsetX < 135 && offsetY < 120) {
			renderTooltip(matrixStack, OFFSET_Z_TEXT, offsetX, offsetY);
		} else if (offsetX >= 117 && offsetY >= 153 && offsetX < 135 && offsetY < 172) {
			renderTooltip(matrixStack, FLIP_TEXT, offsetX, offsetY);
		} else if (offsetX >= 142 && offsetY >= 153 && offsetX < 160 && offsetY < 172) {
			renderTooltip(matrixStack, ROTATE_TEXT, offsetX, offsetY);
		} else if (offsetX >= 9 && offsetY >= 153 && offsetX < 27 && offsetY < 172) {
			renderTooltip(matrixStack, SINGLE_DOUBLE_SIDED_TEXT, offsetX, offsetY);
		}
	}

	private static void drawCenteredStringWithoutShadow(MatrixStack stack, TextRenderer renderer, net.minecraft.text.Text string, float x, float y) {
		renderer.draw(stack, string, x - renderer.getWidth(string) / 2F, y, 0x404040);
	}

	private static float parseFloat(String text) {
		return Math.round(Float.parseFloat(text) * 10000) / 10000F;
	}

	private static String floatToString(float value) {
		return String.valueOf(Math.round(value * 10000) / 10000F);
	}

	private static Vec3f relativeToAbsolute(Vec3f relatedOffset, Vec2f size,
											ProjectorBlock.InternalRotation rotation) {
		Vector4f center = new Vector4f(0.5F * size.x, 0.0F, 0.5F * size.y, 1.0F);
		// matrix 6: offset for slide (center[new] = center[old] + offset)
		center.transform(Matrix4f.translate(relatedOffset.getX(), -relatedOffset.getZ(), relatedOffset.getY()));
		// matrix 5: translation for slide
		center.transform(Matrix4f.translate(-0.5F, 0.0F, 0.5F - size.y));
		// matrix 4: internal rotation
		rotation.transform(center);
		// ok, that's enough
		return new Vec3f(center.getX(), center.getY(), center.getZ());
	}

	private static Vec3f absoluteToRelative(Vec3f absoluteOffset, Vec2f size,
											ProjectorBlock.InternalRotation rotation) {
		Vector4f center = new Vector4f(absoluteOffset);
		// inverse matrix 4: internal rotation
		rotation.invert().transform(center);
		// inverse matrix 5: translation for slide
		center.transform(Matrix4f.translate(0.5F, 0.0F, -0.5F + size.y));
		// subtract (offset = center[new] - center[old])
		center.transform(Matrix4f.translate(-0.5F * size.x, 0.0F, -0.5F * size.y));
		// ok, that's enough (remember it is (a, -c, b) => (a, b, c))
		return new Vec3f(center.getX(), center.getZ(), -center.getY());
	}
}
