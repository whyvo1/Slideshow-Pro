package org.teacon.slides.projector;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.teacon.slides.Slideshow;
import org.teacon.slides.network.ProjectorImageInfoS2CPayload;
import org.teacon.slides.network.ProjectorOpenScreenPayload;
import org.teacon.slides.util.RegistryServer;

@SuppressWarnings("ConstantConditions")
public final class ProjectorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<ProjectorOpenScreenPayload> {

	public SourceType mSourceType = SourceType.URL;
	public String mLocation = "";
	public int mColor = ~0;
	public float mWidth = 1;
	public float mHeight = 1;
	public float mOffsetX = 0;
	public float mOffsetY = 0;
	public float mOffsetZ = 0;
	public boolean mDoubleSided = true;

	public Inventory mContainer = null;
	public int scanIndex = -1;

	public boolean needInitContainer = false;
	public boolean needHandleReadImage = false;
	public boolean flipBack = false;

	public boolean mCFromID = false;
	public String mCLocation = "";

	public boolean mCNextFromID = false;
	public String mCNextLocation = "";

	public ProjectorBlockEntity(BlockPos pos, BlockState state) {
		super(Slideshow.PROJECTOR_BLOCK_ENTITY, pos, state);
	}

	public void transformToSlideSpace(Matrix4f pose, Matrix3f normal) {
		var state = this.getCachedState();
		var direction = state.get(Properties.FACING);
		var rotation = state.get(ProjectorBlock.ROTATION);
		pose.translate(0.5f, 0.5f, 0.5f);
		pose.rotate(direction.getRotationQuaternion());
		normal.rotate(direction.getRotationQuaternion());
		pose.translate(0.0f, 0.5f, 0.0f);
		rotation.transform(pose);
		rotation.transform(normal);
		pose.translate(-0.5F, 0.0F, 0.5F - this.mHeight);
		pose.translate(this.mOffsetX, -this.mOffsetZ, mOffsetY);
		pose.scale(this.mWidth, 1.0F, this.mHeight);
	}

	private boolean tryReadImageItem(ItemStack item, boolean next) {
		if(item.isOf(Slideshow.IMAGE_ITEM)) {
			String loc = item.get(Slideshow.LOCATION_COMPONENT);
			if(loc != null) {
				if (next) {
					this.mCNextFromID = Boolean.TRUE.equals(item.get(Slideshow.FROM_ID_COMPONENT));
					this.mCNextLocation = loc;
				} else {
					this.mCFromID = Boolean.TRUE.equals(item.get(Slideshow.FROM_ID_COMPONENT));
					this.mCLocation = loc;
				}
				return true;
			}
		}
		return false;
	}

	private void handleReadImage(boolean back) {
		int size = this.mContainer.size();
		if(size <= 0) {
			return;
		}
		int start = back ? size + this.scanIndex - 1 : this.scanIndex + 1;
		int end = back ? this.scanIndex - 1 : size + this.scanIndex + 1;
		boolean found = false;
		if(back) {
			for (int j = start; j > end; j--) {
				int i = j % size;
				ItemStack item = this.mContainer.getStack(i);
				if (tryReadImageItem(item, false)) {
					this.scanIndex = i;
					found = true;
					start = i + 1;
					end = start + size;
					break;
				}
			}
		}
		else {
			for (int j = start; j < end; j++) {
				int i = j % size;
				ItemStack item = this.mContainer.getStack(i);
				if (tryReadImageItem(item, false)) {
					this.scanIndex = i;
					found = true;
					start = i + 1;
					end = start + size;
					break;
				}
			}
		}
		if(!found) {
			this.mCNextLocation = "";
			return;
		}
		for(int j = start; j < end; j ++) {
			int i = j % size;
			ItemStack item = this.mContainer.getStack(i);
			if(tryReadImageItem(item, true)) {
				return;
			}
		}
	}

	public boolean canFlip() {
		return this.mSourceType == SourceType.ContainerBlock && this.mContainer != null;
	}

	public boolean getFromID() {
		if(this.mSourceType != SourceType.ContainerBlock) {
			return this.mSourceType == SourceType.ResourceID;
		}
		return this.mCFromID;
	}

	public String getLocation() {
		if(this.mSourceType != SourceType.ContainerBlock) {
			return this.mLocation;
		}
		return this.mCLocation;
	}

	@Override
	public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		this.saveCompound(nbt);
		super.writeNbt(nbt, registryLookup);
	}

	@Override
	public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		this.loadCompound(nbt);
		super.readNbt(nbt, registryLookup);
	}

	public void saveCompound(NbtCompound compoundTag) {
		compoundTag.putString("SourceType", switch(mSourceType) {
			case ResourceID -> "resource_id";
			case ContainerBlock -> "container";
			default -> "url";
		});
		compoundTag.putString("ImageLocation", mLocation);
		compoundTag.putInt("Color", mColor);
		compoundTag.putFloat("Width", mWidth);
		compoundTag.putFloat("Height", mHeight);
		compoundTag.putFloat("OffsetX", mOffsetX);
		compoundTag.putFloat("OffsetY", mOffsetY);
		compoundTag.putFloat("OffsetZ", mOffsetZ);
		compoundTag.putBoolean("DoubleSided", mDoubleSided);
		compoundTag.putInt("ScanIndex", scanIndex);
		compoundTag.putBoolean("CFromID", mCFromID);
		compoundTag.putString("CLocation", mCLocation);
		compoundTag.putBoolean("CNextFromID", mCNextFromID);
		compoundTag.putString("CNextLocation", mCNextLocation);
	}

	public void loadCompound(NbtCompound compoundTag) {
		mSourceType = switch (compoundTag.getString("SourceType")) {
			case "resource_id" -> SourceType.ResourceID;
			case "container" -> SourceType.ContainerBlock;
			default -> SourceType.URL;
		};
		mLocation = compoundTag.getString("ImageLocation");
		mColor = compoundTag.getInt("Color");
		mWidth = compoundTag.getFloat("Width");
		mHeight = compoundTag.getFloat("Height");
		mOffsetX = compoundTag.getFloat("OffsetX");
		mOffsetY = compoundTag.getFloat("OffsetY");
		mOffsetZ = compoundTag.getFloat("OffsetZ");
		mDoubleSided = compoundTag.getBoolean("DoubleSided");
		scanIndex = compoundTag.getInt("ScanIndex");
		mCFromID = compoundTag.getBoolean("CFromID");
		mCLocation = compoundTag.getString("CLocation");
		mCNextFromID = compoundTag.getBoolean("CNextFromID");
		mCNextLocation = compoundTag.getString("CNextLocation");
	}

	public void sync() {
		for(ServerPlayerEntity player : PlayerLookup.tracking(this)) {
			RegistryServer.sendToPlayer(player, new ProjectorImageInfoS2CPayload(this));
		}
	}

	public static void tick(World world, BlockPos pos, ProjectorBlockEntity entity) {
		if(world.isClient()) {
			return;
		}
		if(entity.mSourceType != SourceType.ContainerBlock) {
			return;
		}
		entity.mContainer = HopperBlockEntity.getInventoryAt(world, pos.down(1));
		if(entity.mContainer == null) {
			entity.mCLocation = "";
			entity.mCNextLocation = "";
			entity.scanIndex = -1;
			return;
		}
		if(entity.needInitContainer) {
			entity.scanIndex = -1;
			entity.handleReadImage(false);
			entity.needInitContainer = false;
			entity.markDirty();
			entity.sync();
			return;
		}
		if(entity.needHandleReadImage) {
			if(entity.scanIndex < 0) {
				entity.scanIndex = -1;
				entity.handleReadImage(false);
				return;
			}
			entity.handleReadImage(entity.flipBack);
			entity.markDirty();
			entity.sync();
			entity.needHandleReadImage = false;
			entity.flipBack = false;
		}
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
		NbtCompound compoundTag = new NbtCompound();
		this.writeNbt(compoundTag, registryLookup);
		return compoundTag;
	}

	@Override
	public boolean copyItemDataRequiresOperator() {
		return true;
	}

	@Override
	public Text getDisplayName() {
		return Text.literal("");
	}

	@Override
	public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		return new ProjectorScreenHandler(syncId, new ProjectorOpenScreenPayload(this.pos));
	}

	@Override
	public ProjectorOpenScreenPayload getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
		return new ProjectorOpenScreenPayload(this.pos);
	}
}
