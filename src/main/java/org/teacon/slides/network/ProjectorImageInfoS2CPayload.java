package org.teacon.slides.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.util.Utilities;

public class ProjectorImageInfoS2CPayload implements CustomPayload {

    public static final Id<ProjectorImageInfoS2CPayload> ID = new CustomPayload.Id<>(Slideshow.PACKET_TAG_UPDATE);

    private final BlockPos mPos;
    private final boolean bl0;
    private final String str0;
    private final boolean bl1;
    private final String str1;

    public ProjectorImageInfoS2CPayload(ProjectorBlockEntity entity) {
        this.mPos = entity.getPos();
        this.bl0 = entity.mCFromID;
        this.str0 = entity.mCLocation;
        this.bl1 = entity.mCNextFromID;
        this.str1 = entity.mCNextLocation;
    }

    public ProjectorImageInfoS2CPayload(PacketByteBuf buffer) {
        this.mPos = buffer.readBlockPos();
        this.bl0 = buffer.readBoolean();
        this.str0 = buffer.readString();
        this.bl1 = buffer.readBoolean();
        this.str1 = buffer.readString();
    }

    public static void writeBuffer(ProjectorImageInfoS2CPayload payload, PacketByteBuf buffer) {
        buffer.writeBlockPos(payload.mPos);
        buffer.writeBoolean(payload.bl0);
        buffer.writeString(payload.str0);
        buffer.writeBoolean(payload.bl1);
        buffer.writeString(payload.str1);
    }

    public static void handle(ProjectorImageInfoS2CPayload payload, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        client.execute(() -> {
            if (client.world != null) {
                BlockEntity entity = client.world.getBlockEntity(payload.mPos);
                if (entity instanceof ProjectorBlockEntity entity1) {
                    entity1.mCFromID = payload.bl0;
                    entity1.mCLocation = payload.str0;
                    entity1.mCNextFromID = payload.bl1;
                    entity1.mCNextLocation = payload.str1;
                    return;
                }
            }
            Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for image info in {}.", payload.mPos);
        });
    }

    @Override
    public Id<ProjectorImageInfoS2CPayload> getId() {
        return ID;
    }
}
