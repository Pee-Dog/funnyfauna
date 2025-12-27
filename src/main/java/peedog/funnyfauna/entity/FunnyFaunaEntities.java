package peedog.funnyfauna.entity;

import net.minecraft.core.util.collection.NamespaceID;
import peedog.funnyfauna.entity.ant.EntityAnt;
import peedog.funnyfauna.entity.armadillo.MobArmadillo;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.entity.emu.MobEmu;
import peedog.funnyfauna.entity.horse.MobHorse;
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.entity.penguin.MobPenguin;
import peedog.funnyfauna.entity.projectile.ProjectileBigEgg;
import peedog.funnyfauna.entity.test.MobRideableTest;
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
		EntityHelper.createEntity(MobLizard.class, NamespaceID.getPermanent(MOD_ID, "lizard"), entityKey("lizard"));
		EntityHelper.createEntity(MobEmu.class, NamespaceID.getPermanent(MOD_ID, "emu"), entityKey("emu"));
		EntityHelper.createEntity(MobCamel.class, NamespaceID.getPermanent(MOD_ID, "camel"), entityKey("camel"));
		EntityHelper.createEntity(MobHorse.class, NamespaceID.getPermanent(MOD_ID, "horse"), entityKey("horse"));
		EntityHelper.createEntity(MobArmadillo.class, NamespaceID.getPermanent(MOD_ID, "armadillo"), entityKey("armadillo"));
		EntityHelper.createEntity(ProjectileBigEgg.class, NamespaceID.getPermanent(MOD_ID, "egg_emu"), entityKey("egg_emu"));
		EntityHelper.createEntity(MobRideableTest.class, NamespaceID.getPermanent(MOD_ID, "mobrideable"), entityKey("mobrideable"));
		EntityHelper.createEntity(EntityAnt.class, NamespaceID.getPermanent(MOD_ID, "ant"), entityKey("ant"));
		EntityHelper.createEntity(EntityCricket.class, NamespaceID.getPermanent(MOD_ID, "cricket"), entityKey("cricket"));




	}
}
