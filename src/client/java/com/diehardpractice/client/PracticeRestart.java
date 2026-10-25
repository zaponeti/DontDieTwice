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
 * Quits the current world and immediately creates a brand new one with the same practice selection.
 * A simple re-teleport isn't enough: the structure itself (loot, mobs, broken blocks) stays however the
 * previous attempt left it, so only a fresh world gives a genuinely clean attempt.
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

		if (minecraft.level != null) {
			minecraft.level.disconnect();
			minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
		}

		TitleScreen titleScreen = new TitleScreen();
		minecraft.setScreen(titleScreen);

		CreateWorldScreen.openFresh(minecraft, titleScreen);
		((CreateWorldScreenInvoker) minecraft.screen).diehardpractice$onCreate();
		// Set after onCreate() runs: that call also stashes the (blank, freshly-opened) screen's own selection,
		// which would otherwise clobber this restart's values before the new world's first join consumes them.
		PracticeClientState.setPending(type, tier, PracticeClientState.currentBastionSubtype);
	}
}
