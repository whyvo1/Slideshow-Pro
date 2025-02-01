package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.teacon.slides.Slideshow;
import org.teacon.slides.util.Utilities;

import org.teacon.slides.item.FlipperItem;

public final class FlipperFlipBackC2SPayload implements CustomPayload  {
    public static final Id<FlipperFlipBackC2SPayload> ID = new CustomPayload.Id<>(Slideshow.PACKET_FLIP_BACK);

    private final int slot;

    public FlipperFlipBackC2SPayload(int slot) {
        this.slot = slot;
    }

    public FlipperFlipBackC2SPayload(PacketByteBuf buf) {
        slot = buf.readInt();
    }

    public static void writeBuffer(FlipperFlipBackC2SPayload payload, PacketByteBuf buffer) {
        buffer.writeInt(payload.slot);
    }

    public static void handle(FlipperFlipBackC2SPayload payload, ServerPlayNetworking.Context context) {
        int i = payload.slot;
        ServerPlayerEntity serverPlayer = context.player();
        MinecraftServer server;
        try {
            server = context.server();
        }
        catch (NoSuchMethodError e) {
            server = serverPlayer.getServer();
        }
        if(server == null) {
            return;
        }
        server.execute(() -> {
            ItemStack itemStack = serverPlayer.getInventory().getStack(i);
            if (itemStack.isOf(Slideshow.FLIPPER_ITEM) && FlipperItem.trySendFlip(serverPlayer.getServerWorld(), serverPlayer, itemStack, true, false)) {
                return;
            }
            GameProfile profile = serverPlayer.getGameProfile();
            Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for flip back: player = {}", profile);
        });
    }

    @Override
    public Id<FlipperFlipBackC2SPayload> getId() {
        return ID;
    }
}
