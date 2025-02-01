package org.teacon.slides.projector;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.teacon.slides.Slideshow;

public class ProjectorScreenHandler extends ScreenHandler {

    private final BlockPos pos;

    public ProjectorScreenHandler(int syncId, PacketByteBuf packetByteBuf) {
        super(Slideshow.PROJECTOR_SCREEN_HANDLER, syncId);
        this.pos = packetByteBuf.readBlockPos();
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if(player instanceof ServerPlayerEntity serverPlayer) {
            return switch (serverPlayer.interactionManager.getGameMode()) {
                case CREATIVE, SPECTATOR -> true;
                default -> false;
            };
        }
        return false;
    }
}
