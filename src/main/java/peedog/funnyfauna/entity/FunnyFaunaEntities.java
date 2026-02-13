package peedog.funnyfauna.entity;

import net.minecraft.core.util.collection.NamespaceID;
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
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.entity.mouse.MobMouse;
import peedog.funnyfauna.entity.penguin.MobPenguin;
import peedog.funnyfauna.entity.projectile.ProjectileBigEgg;
import peedog.funnyfauna.entity.test.MobRideableTest;
import peedog.funnyfauna.entity.tumbleweed.EntityTumbleweed;
import peedog.funnyfauna.entity.worm.EntityWorm;
import turniplabs.halplibe.helper.EntityHelper;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

public class FunnyFaunaEntities {
	public static boolean hasInit = false;

	public static void init() {
		if (!hasInit) {
			hasInit = true;
			initializeEntities();
		}

	}

	public static String entityKey(String string) {
		return MOD_ID + ".entity." + string;
	}

	public static void initializeEntities() {
		EntityHelper.createEntity(MobPenguin.class, NamespaceID.getPermanent(MOD_ID, "penguin"), entityKey("penguin"));
		EntityHelper.createEntity(MobLizard.class, NamespaceID.getPermanent(MOD_ID, "gecko"), entityKey("gecko"));
		EntityHelper.createEntity(MobEmu.class, NamespaceID.getPermanent(MOD_ID, "emu"), entityKey("emu"));
		EntityHelper.createEntity(MobCamel.class, NamespaceID.getPermanent(MOD_ID, "camel"), entityKey("camel"));
		EntityHelper.createEntity(MobHorse.class, NamespaceID.getPermanent(MOD_ID, "horse"), entityKey("horse"));
		EntityHelper.createEntity(MobArmadillo.class, NamespaceID.getPermanent(MOD_ID, "armadillo"), entityKey("armadillo"));
		EntityHelper.createEntity(ProjectileBigEgg.class, NamespaceID.getPermanent(MOD_ID, "egg_emu"), entityKey("egg_emu"));
		EntityHelper.createEntity(MobRideableTest.class, NamespaceID.getPermanent(MOD_ID, "mobrideable"), entityKey("mobrideable"));
		EntityHelper.createEntity(EntityAnt.class, NamespaceID.getPermanent(MOD_ID, "ant"), entityKey("ant"));
		EntityHelper.createEntity(EntityTumbleweed.class, NamespaceID.getPermanent(MOD_ID, "tumbleweed"), entityKey("tumbleweed"));
		EntityHelper.createEntity(EntityCricket.class, NamespaceID.getPermanent(MOD_ID, "cricket"), entityKey("cricket"));
		EntityHelper.createEntity(EntityWorm.class, NamespaceID.getPermanent(MOD_ID, "worm"), entityKey("worm"));
		EntityHelper.createEntity(MobBoar.class, NamespaceID.getPermanent(MOD_ID, "boar"), entityKey("boar"));
		EntityHelper.createEntity(MobBird.class, NamespaceID.getPermanent(MOD_ID, "bird"), entityKey("bird"));
		EntityHelper.createEntity(MobFox.class, NamespaceID.getPermanent(MOD_ID, "fox"), entityKey("fox"));
		EntityHelper.createEntity(MobMouse.class, NamespaceID.getPermanent(MOD_ID, "mouse"), entityKey("mouse"));
		EntityHelper.createEntity(MobBunny.class, NamespaceID.getPermanent(MOD_ID, "bunny"), entityKey("bunny"));




	}
}
