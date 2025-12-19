package peedog.funnyfauna;

import net.minecraft.client.render.EntityRenderDispatcher;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.block.color.BlockColorDispatcher;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelStandard;
import peedog.funnyfauna.client.render.entity.*;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.emu.MobEmu;
import peedog.funnyfauna.entity.horse.MobHorse;
import peedog.funnyfauna.entity.horse.MobRendererHorse;
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.entity.penguin.MobPenguin;
import peedog.funnyfauna.item.FunnyFaunaItems;
import turniplabs.halplibe.helper.ModelHelper;
import turniplabs.halplibe.util.ModelEntrypoint;

public class FunnyFaunaModels implements ModelEntrypoint {
	@Override
	public void initBlockModels(BlockModelDispatcher blockModelDispatcher) {

	}

	@Override
	public void initItemModels(ItemModelDispatcher dispatcher) {
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.FOOD_LIZARDTAIL, null).setIcon("funnyfauna:item/food_lizardtail"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.COARSEHIDE, null).setIcon("funnyfauna:item/coarsehide"));
		dispatcher.addDispatch(new ItemModelStandard(FunnyFaunaItems.BIRDFOOT, null).setIcon("funnyfauna:item/birdfoot"));
	}

	@Override
	public void initEntityModels(EntityRenderDispatcher dispatcher) {
		ModelHelper.setEntityModel(MobPenguin.class, () -> new MobRendererPenguin(new ModelPenguin(), 0.6F));
		ModelHelper.setEntityModel(MobLizard.class, () -> new MobRendererLizard(new ModelLizard(), 0.6F));
		ModelHelper.setEntityModel(MobEmu.class, () -> new MobRendererEmu(new ModelEmu(), 0.6F));
		ModelHelper.setEntityModel(MobCamel.class, () -> new MobRendererCamel(new ModelCamel(), 0.6F));
		ModelHelper.setEntityModel(MobHorse.class, MobRendererHorse::new);

	}

	@Override
	public void initTileEntityModels(TileEntityRenderDispatcher tileEntityRenderDispatcher) {

	}

	@Override
	public void initBlockColors(BlockColorDispatcher blockColorDispatcher) {

	}
}
