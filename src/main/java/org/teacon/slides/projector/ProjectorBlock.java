package org.teacon.slides.projector;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.slides.Slideshow;

import java.util.Arrays;
import java.util.Locale;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import static net.minecraft.state.property.Properties.FACING;
import static net.minecraft.state.property.Properties.POWERED;


public final class ProjectorBlock extends BlockWithEntity implements BlockEntityProvider {

	public static final EnumProperty<InternalRotation>
			ROTATION = EnumProperty.of("rotation", InternalRotation.class);
	public static final EnumProperty<Direction>
			BASE = EnumProperty.of("base", Direction.class, Direction.Type.VERTICAL);

	private static final VoxelShape SHAPE_WITH_BASE_UP = Block.createCuboidShape(0.0, 4.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape SHAPE_WITH_BASE_DOWN = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

	public ProjectorBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState()
				.with(BASE, Direction.DOWN)
				.with(FACING, Direction.EAST)
				.with(POWERED, Boolean.FALSE)
				.with(ROTATION, InternalRotation.NONE));
	}

	@NotNull
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(BASE)) {
            case DOWN -> SHAPE_WITH_BASE_DOWN;
            case UP -> SHAPE_WITH_BASE_UP;
            default -> throw new AssertionError();
        };
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(BASE, FACING, POWERED, ROTATION);
	}

	@NotNull
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction facing = context.getPlayerLookDirection().getOpposite();
		Direction horizontalFacing = context.getVerticalPlayerLookDirection().getOpposite();
		Direction base = Arrays.stream(context.getPlacementDirections())
				.filter(Direction.Type.VERTICAL)
				.findFirst()
				.orElse(Direction.DOWN);
		InternalRotation rotation =
				InternalRotation.VALUES[4 + Math.floorMod(facing.getOffsetY() * horizontalFacing.getHorizontalQuarterTurns(), 4)];
		return getDefaultState()
				.with(BASE, base)
				.with(FACING, facing)
				.with(POWERED, Boolean.FALSE)
				.with(ROTATION, rotation);
	}

	@Override
	protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
		boolean powered = world.isReceivingRedstonePower(pos);
		if (powered != state.get(POWERED)) {
			world.setBlockState(pos, state.with(POWERED, powered));
		}
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (!oldState.isOf(state.getBlock())) {
			boolean powered = worldIn.isReceivingRedstonePower(pos);
			if (powered != state.get(POWERED)) {
				worldIn.setBlockState(pos, state.with(POWERED, powered));
			}
		}
	}

	@Override
	public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? null : validateTicker(type, Slideshow.PROJECTOR_BLOCK_ENTITY, (world1, pos, state1, entity) -> ProjectorBlockEntity.tick(world1, pos, entity));
	}

	@NotNull
	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		Direction direction = state.get(FACING);
        return switch (direction) {
            case DOWN, UP -> state.with(ROTATION, state.get(ROTATION).compose(BlockRotation.CLOCKWISE_180));
            default -> state.with(FACING, mirror.getRotation(direction).rotate(direction));
        };
	}

	@NotNull
	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		Direction direction = state.get(FACING);
        return switch (direction) {
            case DOWN, UP -> state.with(ROTATION, state.get(ROTATION).compose(rotation));
            default -> state.with(FACING, rotation.rotate(direction));
        };
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if(!world.isClient()) {
			if(player.getMainHandStack().isOf(Slideshow.FLIPPER_ITEM) || player.getOffHandStack().isOf(Slideshow.FLIPPER_ITEM)) {
				return ActionResult.PASS;
			}
			NamedScreenHandlerFactory factory = this.createScreenHandlerFactory(state, world, pos);
			if(factory != null) {
				player.openHandledScreen(factory);
			}
		}
		return ActionResult.SUCCESS;
	}

	public static boolean hasPermission(ServerPlayerEntity serverPlayer) {
		return hasPermission(serverPlayer.interactionManager.getGameMode());
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return createCodec(ProjectorBlock::new);
	}

	@Override
	public BlockRenderType getRenderType(BlockState blockState) {
		return BlockRenderType.MODEL;
	}

	private static boolean hasPermission(GameMode gameType) {
		return gameType == GameMode.CREATIVE || gameType == GameMode.SURVIVAL;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ProjectorBlockEntity(pos, state);
	}

	public enum InternalRotation implements StringIdentifiable {
		NONE(1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F),
		CLOCKWISE_90(0F, 0F, -1F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 0F),
		CLOCKWISE_180(-1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, -1F, 0F),
		COUNTERCLOCKWISE_90(0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, -1F, 0F, 0F, 0F),
		HORIZONTAL_FLIPPED(-1F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 1F, 0F),
		DIAGONAL_FLIPPED(0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F),
		VERTICAL_FLIPPED(1F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, -1F, 0F),
		ANTI_DIAGONAL_FLIPPED(0F, 0F, 1F, 0F, 0F, -1F, 0F, 0F, 1F, 0F, 0F, 0F);

		public static final InternalRotation[] VALUES = values();

		private static final int[]
				INV_INDICES = {0, 3, 2, 1, 4, 5, 6, 7},
				FLIP_INDICES = {4, 7, 6, 5, 0, 3, 2, 1};
		private static final int[][] ROTATION_INDICES = {
				{0, 1, 2, 3, 4, 5, 6, 7},
				{1, 2, 3, 0, 5, 6, 7, 4},
				{2, 3, 0, 1, 6, 7, 4, 5},
				{3, 0, 1, 2, 7, 4, 5, 6}
		};

		private final String mSerializedName;
		private final Matrix4f mMatrix;
		private final Matrix3f mNormal;

		InternalRotation(float m00, float m10, float m20, float m30,
						 float m01, float m11, float m21, float m31,
						 float m02, float m12, float m22, float m32) {
			mMatrix = new Matrix4f(m00, m01, m02, 0F, m10, m11, m12, 0F, m20, m21, m22, 0F, m30, m31, m32, 1F);
			mNormal = new Matrix3f(m00, m01, m02, m10, m11, m12, m20, m21, m22);
			mSerializedName = name().toLowerCase(Locale.ROOT);
		}

		public InternalRotation compose(BlockRotation rotation) {
			return VALUES[ROTATION_INDICES[rotation.ordinal()][ordinal()]];
		}

		public InternalRotation flip() {
			return VALUES[FLIP_INDICES[ordinal()]];
		}

		public InternalRotation invert() {
			return VALUES[INV_INDICES[ordinal()]];
		}

		public boolean isFlipped() {
			return ordinal() >= 4;
		}

		public void transform(Vector4f vector) {
			vector.mul(mMatrix);
		}

		public void transform(Matrix4f poseMatrix) {
			poseMatrix.mul(mMatrix);
		}

		public void transform(Matrix3f normalMatrix) {
			normalMatrix.mul(mNormal);
		}

		@NotNull
		@Override
		public final String asString() {
			return mSerializedName;
		}

	}
}
