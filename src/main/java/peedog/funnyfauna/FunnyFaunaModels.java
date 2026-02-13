package peedog.funnyfauna;

import net.minecraft.client.render.EntityRenderDispatcher;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.block.color.BlockColorDispatcher;
import net.minecraft.client.render.block.model.*;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Side;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.block.BlockModelJarClosed;
import peedog.funnyfauna.block.FunnyFaunaBlocks;
import peedog.funnyfauna.block.entity.TileEntityEmuEgg;
import peedog.funnyfauna.client.render.entity.*;
import peedog.funnyfauna.client.render.tileentity.TileRendererEmuEgg;
import peedog.funnyfauna.entity.ant.EntityAnt;
import peedog.funnyfauna.entity.armadillo.MobArmadillo;
import peedog.funnyfauna.entity.bird.MobBird;
import peedog.funnyfauna.entity.boar.MobBoar;
import peedog.funnyfauna.entity.bunny.MobBunny;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.entity.emu.MobEmu;
import peedog.funnyfauna.entity.fox.MobFox;
import peedog.funnyfauna.entity.horse.MobHorse;
import peedog.funnyfauna.client.render.entity.MobRendererHorse;
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.entity.mouse.MobMouse;
import peedog.funnyfauna.entity.penguin.MobPenguin;
import peedog.funnyfauna.entity.projectile.ProjectileBigEgg;
import peedog.funnyfauna.entity.tumbleweed.EntityTumbleweed;
import peedog.funnyfauna.entity.worm.EntityWorm;
import peedog.funnyfauna.item.FunnyFaunaItems;
import peedog.funnyfauna.item.ItemSturdyShell;
import peedog.funnyfauna.item.ItemToggleable;
import turniplabs.halplibe.helper.ModelHelper;
import turniplabs.halplibe.util.ModelEntrypoint;

public class FunnyFaunaModels implements ModelEntrypoint {
	@Override
	public void initBlockModels(BlockModelDispatcher blockModelDispatcher) {
		ModelHelper.setBlockModel(FunnyFaunaBlocks.ANT_HILL, () -> new BlockModelStandard<>(FunnyFaunaBlocks.ANT_HILL).setAllTextures(0, "minecraft:block/sand"));
		ModelHelper.setBlockModel(FunnyFaunaBlocks.JAR_CRICKET, () -> new BlockModelJarClosed(FunnyFaunaBlocks.JAR_CRICKET));
		ModelHelper.setBlockModel(FunnyFaunaBlocks.HAYBALE, () -> new BlockModelAxisAligned(Blocks.LOG_OAK).setTex(0, "funnyfauna:block/haybale_top", new Side[]{Side.TOP, Side.BOTTOM}).setTex(0, "funnyfauna:block/haybale_side", new Side[]{Side.NORTH, Side.EAST, Side.SOUTH, Side.WEST}));
	}

	@Override
	public void initItemModels(ItemModelDispatcher dispatcher) {
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.FOOD_LIZARDTAIL, null).setIcon("funnyfauna:item/food_lizardtail"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.COARSEHIDE, null).setIcon("funnyfauna:item/coarsehide"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.BIRDFOOT, null).setIcon("funnyfauna:item/birdfoot"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.POCKET, null).setIcon("funnyfauna:item/pocket"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.OIL, null).setIcon("funnyfauna:item/oil"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.FOOD_BLUBBER, null).setIcon("funnyfauna:item/food_blubber"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.FOOD_EGGEMU_COOKED, null).setIcon("funnyfauna:item/food_eggemu_cooked"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.EGG_EMU, null).setIcon("funnyfauna:item/egg_emu"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.JAR_CRICKET, null).setIcon("funnyfauna:item/jar_cricket"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.JAR_WORM, null).setIcon("funnyfauna:item/jar_cricket"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.SCALES, null).setIcon("funnyfauna:item/scales"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.SCALES_REINFORCED, null).setIcon("funnyfauna:item/scales_reinforced"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.SCUTE, null).setIcon("funnyfauna:item/scute"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.TUMBLEWEED, null).setIcon("funnyfauna:item/tumbleweed"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.TUMBLEWEED, null).setIcon("funnyfauna:item/tumbleweed"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.BIOME_COMPASS, null).setIcon("funnyfauna:item/biome_compass"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.ARM_EXTENSION, null) {
			private final IconCoordinate OFF =
				TextureRegistry.getTexture("funnyfauna:item/arm_extension_off");

			private final IconCoordinate ON =
				TextureRegistry.getTexture("funnyfauna:item/arm_extension_on");

			@Override
			public IconCoordinate getIcon(Entity entity, ItemStack stack) {
				// Use the common toggleable helper
				return ItemToggleable.isToggled(stack) ? ON : OFF;
			}
		});

		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.STURDY_SHELL, null) {
			private final IconCoordinate OFF =
				TextureRegistry.getTexture("funnyfauna:item/sturdy_shell_off");

			private final IconCoordinate ON =
				TextureRegistry.getTexture("funnyfauna:item/sturdy_shell_on");

			@Override
			public IconCoordinate getIcon(Entity entity, ItemStack stack) {
				return ItemToggleable.isToggled(stack) ? ON : OFF;
			}
		});
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.CLIMBING_CLAWS, null) {
			private final IconCoordinate OFF =
				TextureRegistry.getTexture("funnyfauna:item/climbing_claws_off");

			private final IconCoordinate ON =
				TextureRegistry.getTexture("funnyfauna:item/climbing_claws_on");

			@Override
			public IconCoordinate getIcon(Entity entity, ItemStack stack) {
				// Use the common toggleable helper
				return ItemToggleable.isToggled(stack) ? ON : OFF;
			}
		});

	}

	@Override
	public void initEntityModels(EntityRenderDispatcher dispatcher) {
		ModelHelper.setEntityModel(MobPenguin.class, () -> new MobRendererPenguin(new ModelPenguin(), 0.6F));
		ModelHelper.setEntityModel(MobLizard.class, () -> new MobRendererLizard(new ModelLizard(), 0.6F));
		ModelHelper.setEntityModel(MobEmu.class, () -> new MobRendererEmu(new ModelEmu(), 0.6F));
		ModelHelper.setEntityModel(MobCamel.class, MobRendererCamel::new);
		ModelHelper.setEntityModel(MobHorse.class, MobRendererHorse::new);
		ModelHelper.setEntityModel(MobArmadillo.class, MobRendererArmadillo::new);
		ModelHelper.setEntityModel(MobBoar.class, MobRendererBoar::new);
		ModelHelper.setEntityModel(ProjectileBigEgg.class, EntityRendererBigEgg::new);
		ModelHelper.setEntityModel(EntityAnt.class, EntityRendererAnt::new);
		ModelHelper.setEntityModel(EntityCricket.class, EntityRendererCricket::new);
		ModelHelper.setEntityModel(EntityWorm.class, EntityRendererWorm::new);
		ModelHelper.setEntityModel(EntityTumbleweed.class, EntityRendererTumbleweed::new);
		ModelHelper.setEntityModel(MobBird.class, MobRendererBird::new);
		ModelHelper.setEntityModel(MobFox.class, MobRendererFox::new);
		ModelHelper.setEntityModel(MobMouse.class, MobRendererMouse::new);
		ModelHelper.setEntityModel(MobBunny.class, MobRendererBunny::new);


	}

	@Override
	public void initTileEntityModels(TileEntityRenderDispatcher tileEntityRenderDispatcher) {
		ModelHelper.setTileEntityModel(TileEntityEmuEgg.class, TileRendererEmuEgg::new);

	}

	@Override
	public void initBlockColors(BlockColorDispatcher blockColorDispatcher) {

	}
}
