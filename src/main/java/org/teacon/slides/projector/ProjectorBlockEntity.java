package org.teacon.slides.projector;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Tickable;
import org.jetbrains.annotations.Nullable;
import org.teacon.slides.Slideshow;
import org.teacon.slides.item.ImageItem;
import org.teacon.slides.network.ProjectorImageInfoS2CPacket;
import org.teacon.slides.util.Text;

@SuppressWarnings("ConstantConditions")
public final class ProjectorBlockEntity extends BlockEntity implements Tickable, ExtendedScreenHandlerFactory, BlockEntityClientSerializable {
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

	public ProjectorBlockEntity() {
		super(Slideshow.PROJECTOR_BLOCK_ENTITY);
	}

	private boolean tryReadImageItem(ItemStack item, boolean next) {
		if(item.getItem() instanceof ImageItem && item.hasTag() && item.getTag().contains("location")) {
			if(next) {
				this.mCNextFromID = item.getTag().getBoolean("from_id");
				this.mCNextLocation = item.getTag().getString("location");
			}
			else {
				this.mCFromID = item.getTag().getBoolean("from_id");
				this.mCLocation = item.getTag().getString("location");
			}
			return true;
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
	public NbtCompound writeNbt(NbtCompound compoundTag) {
		this.saveCompound(compoundTag);
		return super.writeNbt(compoundTag);
	}

	@Override
	public void fromTag(BlockState blockState, NbtCompound compoundTag) {
		this.loadCompound(compoundTag);
		super.fromTag(blockState, compoundTag);
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

	public void synch() {
		new ProjectorImageInfoS2CPacket(this).sendToClient((ServerWorld) this.world);
	}

	@Override
	public void tick() {
		if(world.isClient()) {
			return;
		}
		if(this.mSourceType != SourceType.ContainerBlock) {
			return;
		}
		this.mContainer = HopperBlockEntity.getInventoryAt(world, this.getPos().down(1));
		if(this.mContainer == null) {
			this.mCLocation = "";
			this.mCNextLocation = "";
			this.scanIndex = -1;
			return;
		}
		if(this.needInitContainer) {
			this.scanIndex = -1;
			this.handleReadImage(false);
			this.needInitContainer = false;
			this.markDirty();
			this.synch();
			return;
		}
		if(this.needHandleReadImage) {
			if(this.scanIndex < 0) {
				this.scanIndex = -1;
				this.handleReadImage(false);
				return;
			}
			this.handleReadImage(this.flipBack);
			this.markDirty();
			this.synch();
			this.needHandleReadImage = false;
			this.flipBack = false;
		}
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		return this.writeNbt(new NbtCompound());
	}

	@Override
	public boolean copyItemDataRequiresOperator() {
		return true;
	}

	@Override
	public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
		packetByteBuf.writeBlockPos(this.pos);
	}

	@Override
	public net.minecraft.text.Text getDisplayName() {
		return Text.literal("");
	}

	@Override
	public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeBlockPos(this.pos);
		return new ProjectorScreenHandler(syncId, buf);
	}

	@Override
	public void fromClientTag(NbtCompound tag) {
		this.loadCompound(tag);
	}

	@Override
	public NbtCompound toClientTag(NbtCompound tag) {
		this.saveCompound(tag);
		return tag;
	}
}
