package org.teacon.slides;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.teacon.slides.item.FlipperItem;
import org.teacon.slides.item.ImageItem;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.network.ProjectorExportC2SPacket;
import org.teacon.slides.network.ProjectorAfterUpdateC2SPacket;
import org.teacon.slides.projector.ProjectorScreenHandler;
import org.teacon.slides.util.RegistryServer;

public class Slideshow implements ModInitializer {
	public static final String ID = "slide_show";
	public static final Logger LOGGER = LogManager.getLogger(ID);
	public static final Identifier PACKET_UPDATE = Identifier.of(ID, "update");
	public static final Identifier PACKET_EXPORT = Identifier.of(ID, "export");
	public static final Identifier PACKET_FLIP_BACK = Identifier.of(ID, "flip_back");
	public static final Identifier PACKET_TAG_UPDATE = Identifier.of(ID, "tag_update");


	public final static Item IMAGE_ITEM = registerItem("image", new ImageItem(new Item.Settings()
			.maxCount(1)
	), ItemGroups.TOOLS);
	public final static Item FLIPPER_ITEM = registerItem("flipper", new FlipperItem(new Item.Settings()
			.maxCount(1)
	), ItemGroups.TOOLS);
	public static final Block PROJECTOR_BLOCK = registerBlockAndItem("projector", new ProjectorBlock(AbstractBlock.Settings.create()
			.sounds(BlockSoundGroup.METAL)
			.strength(20F)
			.luminance(state -> 15)
			.noCollision()));
	public final static BlockEntityType<ProjectorBlockEntity> PROJECTOR_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(ID, "projector"), BlockEntityType.Builder.create(ProjectorBlockEntity::new, PROJECTOR_BLOCK).build(null));

	public static final ExtendedScreenHandlerType<ProjectorScreenHandler> PROJECTOR_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(ID, "projector_screen_handler"),
			new ExtendedScreenHandlerType<>((syncId, inventory, data) -> new ProjectorScreenHandler(syncId, data))
	);

	@Override
	public void onInitialize() {
		RegistryServer.registerNetworkReceiver(PACKET_UPDATE, ProjectorAfterUpdateC2SPacket::handle);
		RegistryServer.registerNetworkReceiver(PACKET_EXPORT, ProjectorExportC2SPacket::handle);
		RegistryServer.registerNetworkReceiver(PACKET_FLIP_BACK, FlipperItem::handleServerFlipBack);
	}

	private static Item registerItem(String path, Item item, RegistryKey<ItemGroup> group) {
		Item result = Registry.register(Registries.ITEM, Identifier.of(Slideshow.ID, path), item);
		ItemGroupEvents.modifyEntriesEvent(group).register(content -> content.add(result));
		return result;
	}

	private static Block registerBlockAndItem(String path, Block block) {
		Block block0 = Registry.register(Registries.BLOCK, Identifier.of(Slideshow.ID, path), block);
		registerItem(path, new BlockItem(block0, new Item.Settings()), ItemGroups.TOOLS);
		return block0;
	}
}