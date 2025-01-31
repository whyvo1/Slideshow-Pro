package org.teacon.slides.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.projector.SourceType;

public class ProjectorRenderer extends BlockEntityRenderer<ProjectorBlockEntity> {

	public ProjectorRenderer(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(ProjectorBlockEntity blockEntity, float partialTick, MatrixStack pStack,
					   VertexConsumerProvider source, int packedLight, int packedOverlay) {

		final Slide slide = SlideState.getSlide(blockEntity.getLocation(), blockEntity.getFromID());
		if(blockEntity.mSourceType == SourceType.ContainerBlock) {
			SlideState.cacheSlide(blockEntity.mCNextLocation, blockEntity.mCNextFromID);
		}
		if (slide == null) {
			return;
		}
		if (!blockEntity.getCachedState().get(Properties.POWERED)) {
			int color = blockEntity.mColor;
			if ((color & 0xFF000000) == 0) {
				return;
			}

			pStack.push();

			MatrixStack.Entry last = pStack.peek();
			Matrix4f pose = last.getModel();
			Matrix3f normal = last.getNormal();

			BlockState state = blockEntity.getCachedState();
			Direction direction = state.get(Properties.FACING);
			ProjectorBlock.InternalRotation rotation = state.get(ProjectorBlock.ROTATION);
			pStack.translate(0.5f, 0.5f, 0.5f);
			pose.multiply(direction.getRotationQuaternion());
			normal.multiply(direction.getRotationQuaternion());
			pStack.translate(0.0f, 0.5f, 0.0f);
			rotation.transform(pose);
			rotation.transform(normal);
			pStack.translate(-0.5F, 0.0F, 0.5F - blockEntity.mHeight);
			pStack.translate(blockEntity.mOffsetX, -blockEntity.mOffsetZ, blockEntity.mOffsetY);
			pose.multiply(Matrix4f.scale(blockEntity.mWidth, 1.0F, blockEntity.mHeight));

			final boolean flipped = blockEntity.getCachedState().get(ProjectorBlock.ROTATION).isFlipped();

			slide.render(source, last.getModel(), last.getNormal(), blockEntity.mWidth, blockEntity.mHeight, color, packedLight,
					OverlayTexture.DEFAULT_UV, flipped || blockEntity.mDoubleSided, !flipped || blockEntity.mDoubleSided,
					SlideState.getAnimationTick(), partialTick);

			pStack.pop();
		}
	}

	@Override
	public boolean rendersOutsideBoundingBox(ProjectorBlockEntity tile) {
		return true;
	}


}
