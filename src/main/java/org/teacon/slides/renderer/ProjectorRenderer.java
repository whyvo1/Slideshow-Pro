package org.teacon.slides.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.teacon.slides.config.Config;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.projector.SourceType;

public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {

	public ProjectorRenderer() {
		super();
	}

	@Override
	public void render(ProjectorBlockEntity blockEntity, float partialTick, MatrixStack matrices,
					   VertexConsumerProvider source, int packedLight, int packedOverlay) {

		BlockState state = blockEntity.getCachedState();
		final Slide slide = SlideState.getSlide(blockEntity.getLocation(), blockEntity.getFromID());
		if(blockEntity.mSourceType == SourceType.ContainerBlock) {
			SlideState.cacheSlide(blockEntity.mCNextLocation, blockEntity.mCNextFromID);
		}
		if (slide == null) {
			return;
		}
		float width = blockEntity.mWidth;
		float height = blockEntity.mHeight;
		int color = blockEntity.mColor;
		boolean isTransparent = (color & 0xFF000000) == 0;
		boolean isPowered = state.get(Properties.POWERED);
		boolean doubleSided = blockEntity.mDoubleSided;
		if (!isTransparent && !isPowered) {
			matrices.push();
			MatrixStack.Entry lastPose = matrices.peek();
			Matrix4f pose = new Matrix4f(lastPose.getPositionMatrix());
			Matrix3f normal = new Matrix3f(lastPose.getNormalMatrix());
			blockEntity.transformToSlideSpace(pose, normal);
			boolean flipped = state.get(ProjectorBlock.ROTATION).isFlipped();
			slide.render(source, pose, normal, width, height, color, packedLight, OverlayTexture.DEFAULT_UV, flipped || doubleSided, !flipped || doubleSided, SlideState.getAnimationTick(), partialTick);
			matrices.pop();
		}
	}

	@Override
	public boolean rendersOutsideBoundingBox(ProjectorBlockEntity tile) {
		return true;
	}

	@Override
	public int getRenderDistance() {
		return Config.getRenderDistance();
	}
}
