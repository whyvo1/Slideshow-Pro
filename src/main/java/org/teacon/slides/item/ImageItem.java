package org.teacon.slides.item;

import net.minecraft.client.item.TooltipType;
import net.minecraft.text.Text;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import org.teacon.slides.Slideshow;

public class ImageItem extends Item {

    public ImageItem(Settings properties) {
        super(properties);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext context, List<Text> list, TooltipType type) {
        String location = itemStack.get(Slideshow.LOCATION_COMPONENT);
        if(location == null) {
            list.add(Text.translatable("item.slide_show.image.tooltip.no_properties").formatted(Formatting.DARK_RED));
            super.appendTooltip(itemStack, context, list, type);
            return;
        }
        boolean bl = Boolean.TRUE.equals(itemStack.get(Slideshow.FROM_ID_COMPONENT));
        list.add(Text.translatable(bl ? "item.slide_show.image.tooltip.id" : "item.slide_show.image.tooltip.url").formatted(Formatting.AQUA));
        list.add(Text.literal(location).formatted(Formatting.AQUA));
        super.appendTooltip(itemStack, context, list, type);
    }
}
