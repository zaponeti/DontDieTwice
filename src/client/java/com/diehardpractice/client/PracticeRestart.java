package com.diehardpractice.client;

import com.diehardpractice.client.mixin.CreateWorldScreenInvoker;
import com.diehardpractice.practice.GearTier;
import com.diehardpractice.practice.PracticeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

/**
 * Quits the current world and immediately creates a brand new one with the same practice selection and the
 * same seed. A simple re-teleport isn't enough: the structure itself (loot, mobs, broken blocks) stays however
 * the previous attempt left it, so a fresh world is needed - but it has to be the SAME seed, or the search
 * just lands on a different, randomly-placed structure instead of a clean copy of the one just practiced.
 */
public final class PracticeRestart {
	private PracticeRestart() {
	}

	public static void perform(Minecraft minecraft) {
		PracticeType type = PracticeClientState.currentType;
		GearTier tier = PracticeClientState.currentTier;
		if (type == null || tier == null) {
			return;
		}

		//? if >=26.1 {
		/*Long seed = minecraft.getSingleplayerServer() != null ? minecraft.getSingleplayerServer().overworld().getSeed() : null;
		*///?} else {
		Long seed = minecraft.getSingleplayerServer() != null ? minecraft.getSingleplayerServer().getWorldData().worldGenOptions().seed() : null;
		//?}

		if (minecraft.level != null) {
			//? if >=26.1 {
			/*minecraft.level.disconnect(Component.translatable("menu.savingLevel"));
			minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")), false);
			*///?} else {
			minecraft.level.disconnect();
			minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
			//?}
		}

		TitleScreen titleScreen = new TitleScreen();
		//? if >=26.1 {
		/*minecraft.gui.setScreen(titleScreen);

		CreateWorldScreen.openFresh(minecraft, () -> minecraft.gui.setScreen(titleScreen));
		CreateWorldScreen screen = (CreateWorldScreen) minecraft.gui.screen();
		*///?} else {
		minecraft.setScreen(titleScreen);

		CreateWorldScreen.openFresh(minecraft, titleScreen);
		CreateWorldScreen screen = (CreateWorldScreen) minecraft.screen;
		//?}
		if (seed != null) {
			screen.getUiState().setSeed(String.valueOf(seed));
		}

		((CreateWorldScreenInvoker) screen).diehardpractice$onCreate();
		PracticeClientState.setPending(type, tier, PracticeClientState.currentBastionSubtype);
	}
}
