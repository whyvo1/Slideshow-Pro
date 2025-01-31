package org.teacon.slides.projector;

import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.*;
import org.jetbrains.annotations.NotNull;
import org.teacon.slides.item.FlipperItem;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Locale;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vector4f;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.teacon.slides.util.Text;
import org.teacon.slides.util.Utilities;

import static net.minecraft.state.property.Properties.FACING;
import static net.minecraft.state.property.Properties.POWERED;


@SuppressWarnings("deprecation")
public final class ProjectorBlock extends Block implements BlockEntityProvider {

	public static final EnumProperty<InternalRotation>
			ROTATION = EnumProperty.of("rotation", InternalRotation.class);
	public static final EnumProperty<Direction>
			BASE = EnumProperty.of("base", Direction.class, Direction.Type.VERTICAL);

	private static final VoxelShape SHAPE_WITH_BASE_UP = Block.createCuboidShape(0.0, 4.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape SHAPE_WITH_BASE_DOWN = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

	public ProjectorBlock() {
		super(AbstractBlock.Settings.of(Material.METAL)
				.strength(20F)
				.luminance(state -> 15) // TODO Configurable
				.noCollision());
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
		Direction horizontalFacing = context.getPlayerFacing().getOpposite();
		Direction base = Arrays.stream(context.getPlacementDirections())
				.filter(Direction.Type.VERTICAL)
				.findFirst()
				.orElse(Direction.DOWN);
		InternalRotation rotation =
				InternalRotation.VALUES[4 + Math.floorMod(facing.getOffsetY() * horizontalFacing.getHorizontal(), 4)];
		return getDefaultState()
				.with(BASE, base)
				.with(FACING, facing)
				.with(POWERED, Boolean.FALSE)
				.with(ROTATION, rotation);
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
								boolean isMoving) {
		boolean powered = worldIn.isReceivingRedstonePower(pos);
		if (powered != state.get(POWERED)) {
			worldIn.setBlockState(pos, state.with(POWERED, powered));
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

	@NotNull
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if(!world.isClient()) {
			ItemStack itemStack = player.getStackInHand(hand);
			if(itemStack.getItem() instanceof FlipperItem) {
				FlipperItem.setProjectorPos(itemStack, pos);
				Utilities.sendOverLayMessage(player, Text.translatable("info.slide_show.bound_projector").formatted(Formatting.AQUA));
				return ActionResult.CONSUME;
			}
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if(blockEntity instanceof NamedScreenHandlerFactory factory) {
				player.openHandledScreen(factory);
			}
		}
		return ActionResult.SUCCESS;
	}

	public static boolean hasPermission(ServerPlayerEntity serverPlayer) {
		return hasPermission(serverPlayer.interactionManager.getGameMode());
	}

	private static boolean hasPermission(GameMode gameType) {
		return gameType == GameMode.CREATIVE || gameType == GameMode.SURVIVAL;
	}

	@Override
	public BlockEntity createBlockEntity(BlockView blockGetter) {
		return new ProjectorBlockEntity();
	}

	public enum InternalRotation implements StringIdentifiable {
		NONE(new float[]{1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F}),
		CLOCKWISE_90(new float[]{0F, 0F, -1F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 1F}),
		CLOCKWISE_180(new float[]{-1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 1F}),
		COUNTERCLOCKWISE_90(new float[]{0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 1F}),
		HORIZONTAL_FLIPPED(new float[]{-1F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F}),
		DIAGONAL_FLIPPED(new float[]{0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 1F}),
		VERTICAL_FLIPPED(new float[]{1F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 1F}),
		ANTI_DIAGONAL_FLIPPED(new float[]{0F, 0F, 1F, 0F, 0F, -1F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 1F});

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

		InternalRotation(float[] matrix) {
			mSerializedName = name().toLowerCase(Locale.ROOT);
			mMatrix = new Matrix4f();
			load(mMatrix, FloatBuffer.wrap(matrix));
			mNormal = new Matrix3f(mMatrix);
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
			vector.transform(mMatrix);
		}

		public void transform(Matrix4f poseMatrix) {
			poseMatrix.multiply(mMatrix);
		}

		public void transform(Matrix3f normalMatrix) {
			normalMatrix.multiply(mNormal);
		}

		@NotNull
		@Override
		public final String asString() {
			return mSerializedName;
		}

		private static void load(Matrix4f matrix4f, FloatBuffer floatBuffer) {
			matrix4f.a00 = floatBuffer.get(bufferIndex(0, 0));
			matrix4f.a01 = floatBuffer.get(bufferIndex(0, 1));
			matrix4f.a02 = floatBuffer.get(bufferIndex(0, 2));
			matrix4f.a03 = floatBuffer.get(bufferIndex(0, 3));
			matrix4f.a10 = floatBuffer.get(bufferIndex(1, 0));
			matrix4f.a11 = floatBuffer.get(bufferIndex(1, 1));
			matrix4f.a12 = floatBuffer.get(bufferIndex(1, 2));
			matrix4f.a13 = floatBuffer.get(bufferIndex(1, 3));
			matrix4f.a20 = floatBuffer.get(bufferIndex(2, 0));
			matrix4f.a21 = floatBuffer.get(bufferIndex(2, 1));
			matrix4f.a22 = floatBuffer.get(bufferIndex(2, 2));
			matrix4f.a23 = floatBuffer.get(bufferIndex(2, 3));
			matrix4f.a30 = floatBuffer.get(bufferIndex(3, 0));
			matrix4f.a31 = floatBuffer.get(bufferIndex(3, 1));
			matrix4f.a32 = floatBuffer.get(bufferIndex(3, 2));
			matrix4f.a33 = floatBuffer.get(bufferIndex(3, 3));
		}

		private static int bufferIndex(int i, int j) {
			return j * 4 + i;
		}
	}
}
