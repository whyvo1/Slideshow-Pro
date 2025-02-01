package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.util.Utilities;

public class ProjectorExportC2SPayload implements CustomPayload {

    public static final Id<ProjectorExportC2SPayload> ID = new CustomPayload.Id<>(Slideshow.PACKET_EXPORT);

    private final boolean mFromID;
    private final String mLocation;

    public ProjectorExportC2SPayload(boolean fromID, String location) {
        mFromID = fromID;
        mLocation = location;
    }

    public ProjectorExportC2SPayload(PacketByteBuf buf){
        mFromID = buf.readBoolean();
        mLocation = buf.readString();
    }

    public static void writeBuffer(ProjectorExportC2SPayload payload, PacketByteBuf buffer) {
        buffer.writeBoolean(payload.mFromID);
        buffer.writeString(payload.mLocation);
    }

    private ItemStack getImageItem() {
        ItemStack itemStack = new ItemStack(Slideshow.IMAGE_ITEM, 1);
        itemStack.set(Slideshow.FROM_ID_COMPONENT, mFromID);
        itemStack.set(Slideshow.LOCATION_COMPONENT, mLocation);
        return itemStack;
    }

    public static void handle(ProjectorExportC2SPayload payload, ServerPlayNetworking.Context context){
        ServerPlayerEntity serverPlayer = context.player();
        MinecraftServer server = context.server();
        server.execute(() -> {
            if (ProjectorBlock.hasPermission(serverPlayer)) {
                ItemStack itemStack = payload.getImageItem();
                boolean bl = serverPlayer.getInventory().insertStack(itemStack);
                if (bl && itemStack.isEmpty()) {
                    itemStack.setCount(1);
                    ItemEntity itemEntity = serverPlayer.dropItem(itemStack, false);
                    if (itemEntity != null) {
                        itemEntity.setDespawnImmediately();
                    }

                    serverPlayer.getWorld().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((serverPlayer.getRandom().nextFloat() - serverPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
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

    @Override
    public Id<ProjectorExportC2SPayload> getId() {
        return ID;
    }
}
