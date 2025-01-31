package org.teacon.slides.item;

import org.jetbrains.annotations.Nullable;
import org.teacon.slides.util.Text;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class ImageItem extends Item {

    public ImageItem(Settings properties) {
        super(properties);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, @Nullable World level, List<net.minecraft.text.Text> list, TooltipContext tooltipFlag) {
        if(!itemStack.hasTag() || itemStack.getTag().getString("location") == null) {
            list.add(Text.translatable("item.slide_show.image.tooltip.no_properties").formatted(Formatting.DARK_RED));
            super.appendTooltip(itemStack, level, list, tooltipFlag);
            return;
        }
        list.add(Text.translatable(itemStack.getTag().getBoolean("from_id") ? "item.slide_show.image.tooltip.id" : "item.slide_show.image.tooltip.url").formatted(Formatting.AQUA));
        list.add(Text.literal(itemStack.getTag().getString("location")).formatted(Formatting.AQUA));
        super.appendTooltip(itemStack, level, list, tooltipFlag);
    }
}
