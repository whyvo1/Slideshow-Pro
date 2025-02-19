package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.teacon.slides.util.RegistryClient;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.util.Utilities;

public final class ProjectorAfterUpdateC2SPacket {

	private final BlockPos mPos;
	private final ProjectorBlock.InternalRotation mRotation;
	private final NbtCompound mTag;
	private final boolean mBoolean;

	public ProjectorAfterUpdateC2SPacket(ProjectorBlockEntity entity, ProjectorBlock.InternalRotation rotation) {
		mPos = entity.getPos();
		mRotation = rotation;
		mTag = new NbtCompound();
		entity.saveCompound(mTag);
		mBoolean = entity.needInitContainer;
	}

	public ProjectorAfterUpdateC2SPacket(PacketByteBuf buf) {
		mPos = buf.readBlockPos();
		mRotation = ProjectorBlock.InternalRotation.VALUES[buf.readVarInt()];
		mTag = buf.readNbt();
		mBoolean = buf.readBoolean();
	}

	public void sendToServer() {
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		buffer.writeBlockPos(mPos);
		buffer.writeVarInt(mRotation.ordinal());
		buffer.writeNbt(mTag);
		buffer.writeBoolean(mBoolean);
		RegistryClient.sendToServer(Slideshow.PACKET_UPDATE, buffer);
	}

	public static void handle(MinecraftServer minecraftServer, ServerPlayerEntity player, PacketByteBuf packet) {
		ProjectorAfterUpdateC2SPacket projectorAfterUpdatePacket = new ProjectorAfterUpdateC2SPacket(packet);
		minecraftServer.execute(() -> {
			ServerWorld level = player.getServerWorld();
			BlockEntity blockEntity = level.getBlockEntity(projectorAfterUpdatePacket.mPos);
			BlockPos pos = projectorAfterUpdatePacket.mPos;
			// prevent remote chunk loading
			if (ProjectorBlock.hasPermission(player) && level.canSetBlock(pos) && blockEntity instanceof ProjectorBlockEntity blockEntity1) {
				BlockState state = blockEntity.getCachedState().with(ProjectorBlock.ROTATION, projectorAfterUpdatePacket.mRotation);
                blockEntity1.loadCompound(projectorAfterUpdatePacket.mTag);
				blockEntity1.needInitContainer = projectorAfterUpdatePacket.mBoolean;
				level.setBlockState(pos, state);

				if(!level.setBlockState(pos, state, Block.NOTIFY_ALL)) {
					level.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				}

				// mark chunk unsaved
				blockEntity.markDirty();
				return;
			}
			GameProfile profile = player.getGameProfile();
			Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for projector update: player = {}, pos = {}", profile, projectorAfterUpdatePacket.mPos);
		});
	}

}
