import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockEntityMapper extends BlockEntity {

	public BlockEntityMapper(BlockEntityType<?> type) {
		super(type);
	}

	@Override
	public final void load(BlockState blockState, CompoundTag compoundTag) {
		super.load(blockState, compoundTag);
		readCompoundTag(compoundTag);
	}

	@Override
	public final CompoundTag save(CompoundTag compoundTag) {
		super.save(compoundTag);
		writeCompoundTag(compoundTag);
		return compoundTag;
	}

	public void readCompoundTag(CompoundTag compoundTag) {
	}

	public void writeCompoundTag(CompoundTag compoundTag) {
	}
}
