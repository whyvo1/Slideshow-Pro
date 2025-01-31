import me.shedaniel.architectury.extensions.BlockEntityExtension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockEntityClientSerializableMapper extends BlockEntityMapper implements BlockEntityExtension {

	public BlockEntityClientSerializableMapper(BlockEntityType<?> type) {
		super(type);
	}

	@Override
	public void loadClientData(BlockState pos, CompoundTag tag) {
		load(getBlockState(), tag);
	}

	@Override
	public CompoundTag saveClientData(CompoundTag tag) {
		save(tag);
		return tag;
	}

	public final void fromClientTag(CompoundTag tag) {
		load(getBlockState(), tag);
	}

	public final CompoundTag toClientTag(CompoundTag tag) {
		save(tag);
		return tag;
	}
}
