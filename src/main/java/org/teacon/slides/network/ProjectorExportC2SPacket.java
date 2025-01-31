package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.teacon.slides.RegistryClient;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.util.Utilities;

public class ProjectorExportC2SPacket {

    private final boolean mFromID;
    private final String mLocation;

    public ProjectorExportC2SPacket(boolean fromID, String location) {
        mFromID = fromID;
        mLocation = location;
    }

    public ProjectorExportC2SPacket(PacketByteBuf buf){
        mFromID = buf.readBoolean();
        mLocation = buf.readString();
    }

    public void sendToServer() {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeBoolean(mFromID);
        buffer.writeString(mLocation);
        RegistryClient.sendToServer(Slideshow.PACKET_EXPORT, buffer);
    }

    private ItemStack getImageItem() {
        ItemStack itemStack = new ItemStack(Slideshow.IMAGE_ITEM, 1);
        itemStack.putSubTag("from_id", NbtByte.of(this.mFromID));
        itemStack.putSubTag("location", NbtString.of(this.mLocation));
        return itemStack;
    }

    public static void handle(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayer, PacketByteBuf packet){
        ProjectorExportC2SPacket projectorExportPacket = new ProjectorExportC2SPacket(packet);
        minecraftServer.execute(() -> {
            if (ProjectorBlock.hasPermission(serverPlayer)) {
                ItemStack itemStack = projectorExportPacket.getImageItem();
                boolean bl = serverPlayer.inventory.insertStack(itemStack);
                if (bl && itemStack.isEmpty()) {
                    itemStack.setCount(1);
                    ItemEntity itemEntity = serverPlayer.dropItem(itemStack, false);
                    if (itemEntity != null) {
                        itemEntity.setDespawnImmediately();
                    }

                    serverPlayer.world.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((serverPlayer.getRandom().nextFloat() - serverPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    serverPlayer.playerScreenHandler.sendContentUpdates();
                } else {
                    ItemEntity itemEntity = serverPlayer.dropItem(itemStack, false);
                    if (itemEntity != null) {
                        itemEntity.resetPickupDelay();
                        itemEntity.setOwner(serverPlayer.getUuid());
                    }
                }
                return;
            }
            GameProfile profile = serverPlayer.getGameProfile();
            Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for projector export: player = {}", profile);
        });
    }
}
