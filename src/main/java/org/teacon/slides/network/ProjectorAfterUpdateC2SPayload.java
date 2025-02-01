package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.util.Utilities;

public final class ProjectorAfterUpdateC2SPayload implements CustomPayload {

	public static final Id<ProjectorAfterUpdateC2SPayload> ID = new CustomPayload.Id<>(Slideshow.PACKET_UPDATE);

	private final BlockPos mPos;
	private final ProjectorBlock.InternalRotation mRotation;
	private final NbtCompound mTag;
	private final boolean mIC;

	public ProjectorAfterUpdateC2SPayload(ProjectorBlockEntity entity, ProjectorBlock.InternalRotation rotation) {
		mPos = entity.getPos();
		mRotation = rotation;
		mTag = new NbtCompound();
		entity.saveCompound(mTag);
		mIC = entity.needInitContainer;
	}

	public ProjectorAfterUpdateC2SPayload(PacketByteBuf buf) {
		mPos = buf.readBlockPos();
		mRotation = ProjectorBlock.InternalRotation.VALUES[buf.readVarInt()];
		mTag = buf.readNbt();
		mIC = buf.readBoolean();
	}

	public static void writeBuffer(ProjectorAfterUpdateC2SPayload payload, PacketByteBuf buffer) {
		buffer.writeBlockPos(payload.mPos);
		buffer.writeVarInt(payload.mRotation.ordinal());
		buffer.writeNbt(payload.mTag);
		buffer.writeBoolean(payload.mIC);
	}

	public static void handle(ProjectorAfterUpdateC2SPayload payload, ServerPlayNetworking.Context context) {
		ServerPlayerEntity player = context.player();
		MinecraftServer server;
		try {
			server = context.server();
		}
		catch (NoSuchMethodError e) {
			server = player.getServer();
		}
        if(server == null) {
			return;
		}
        server.execute(() -> {
			ServerWorld level = player.getServerWorld();
			BlockEntity blockEntity = level.getBlockEntity(payload.mPos);
			// prevent remote chunk loading
			if (ProjectorBlock.hasPermission(player) && level.canSetBlock(payload.mPos) && blockEntity instanceof ProjectorBlockEntity blockEntity1) {
				BlockState state = blockEntity.getCachedState().with(ProjectorBlock.ROTATION, payload.mRotation);
				blockEntity1.loadCompound(payload.mTag);
				blockEntity1.needInitContainer = payload.mIC;
				level.setBlockState(payload.mPos, state);
				// mark chunk unsaved
				blockEntity.markDirty();
				return;
			}
			GameProfile profile = player.getGameProfile();
			Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for projector update: player = {}, pos = {}", profile, payload.mPos);
		});

	}

	@Override
	public Id<ProjectorAfterUpdateC2SPayload> getId() {
		return ID;
	}
}
