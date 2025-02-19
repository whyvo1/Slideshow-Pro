package org.teacon.slides.network;

import io.netty.buffer.Unpooled;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.teacon.slides.util.RegistryServer;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.util.Utilities;

public class ProjectorImageInfoS2CPacket {
    private final BlockPos mPos;
    private final boolean bl0;
    private final String str0;
    private final boolean bl1;
    private final String str1;

    public ProjectorImageInfoS2CPacket(ProjectorBlockEntity entity) {
        this.mPos = entity.getPos();
        this.bl0 = entity.mCFromID;
        this.str0 = entity.mCLocation;
        this.bl1 = entity.mCNextFromID;
        this.str1 = entity.mCNextLocation;
    }

    public ProjectorImageInfoS2CPacket(PacketByteBuf buffer) {
        this.mPos = buffer.readBlockPos();
        this.bl0 = buffer.readBoolean();
        this.str0 = buffer.readString();
        this.bl1 = buffer.readBoolean();
        this.str1 = buffer.readString();
    }

    public void sendToClient(ServerWorld world) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeBlockPos(this.mPos);
        buffer.writeBoolean(this.bl0);
        buffer.writeString(this.str0);
        buffer.writeBoolean(this.bl1);
        buffer.writeString(this.str1);
        Utilities.forPlayersTacking(world, mPos, player -> RegistryServer.sendToPlayer(player, Slideshow.PACKET_TAG_UPDATE, buffer));

    }

    public static void handle(MinecraftClient client, PacketByteBuf buffer) {
        ProjectorImageInfoS2CPacket packet = new ProjectorImageInfoS2CPacket(buffer);
        client.execute(() -> {
            if (client.world != null) {
                BlockEntity entity = client.world.getBlockEntity(packet.mPos);
                if(entity instanceof ProjectorBlockEntity entity1) {
                    entity1.mCFromID = packet.bl0;
                    entity1.mCLocation = packet.str0;
                    entity1.mCNextFromID = packet.bl1;
                    entity1.mCNextLocation = packet.str1;
                    return;
                }
            }
            Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for image info in {}.", packet.mPos);
        });
    }
}
