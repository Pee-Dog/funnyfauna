package peedog.funnyfauna;

import net.minecraft.client.render.EntityRenderDispatcher;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.block.color.BlockColorDispatcher;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.camel.MobRendererCamel;
import peedog.funnyfauna.entity.camel.ModelCamel;
import peedog.funnyfauna.entity.emu.MobEmu;
import peedog.funnyfauna.entity.emu.MobRendererEmu;
import peedog.funnyfauna.entity.emu.ModelEmu;
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.entity.lizard.MobRendererLizard;
import peedog.funnyfauna.entity.lizard.ModelLizard;
import peedog.funnyfauna.entity.penguin.MobPenguin;
import peedog.funnyfauna.entity.penguin.MobRendererPenguin;
import peedog.funnyfauna.entity.penguin.ModelPenguin;
import turniplabs.halplibe.helper.ModelHelper;
import turniplabs.halplibe.util.ModelEntrypoint;

public class FunnyFaunaModels implements ModelEntrypoint {
	@Override
	public void initBlockModels(BlockModelDispatcher blockModelDispatcher) {

	}

	@Override
	public void initItemModels(ItemModelDispatcher itemModelDispatcher) {

	}

	@Override
	public void initEntityModels(EntityRenderDispatcher dispatcher) {
		ModelHelper.setEntityModel(MobPenguin.class, () -> new MobRendererPenguin(new ModelPenguin(), 0.6F));
		ModelHelper.setEntityModel(MobLizard.class, () -> new MobRendererLizard(new ModelLizard(), 0.6F));
		ModelHelper.setEntityModel(MobEmu.class, () -> new MobRendererEmu(new ModelEmu(), 0.6F));
		ModelHelper.setEntityModel(MobCamel.class, () -> new MobRendererCamel(new ModelCamel(), 0.6F));
	}

	@Override
	public void initTileEntityModels(TileEntityRenderDispatcher tileEntityRenderDispatcher) {

	}

	@Override
	public void initBlockColors(BlockColorDispatcher blockColorDispatcher) {

	}
}
