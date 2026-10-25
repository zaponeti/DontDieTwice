package com.diehardpractice.client.mixin;

import com.diehardpractice.client.GameTabStateAccessor;
import com.diehardpractice.client.PracticeClientState;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Simplifies the Create World screen down to just the Game tab (with its Practice section), and renames the create button. */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {
	@Unique
	private Tab diehardpractice$gameTab;

	protected CreateWorldScreenMixin(Component component) {
		super(component);
	}

	@ModifyArg(
		method = "init",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;"
		)
	)
	private Tab[] diehardpractice$onlyGameTab(Tab[] tabs) {
		this.diehardpractice$gameTab = tabs[0];
		return new Tab[] {tabs[0]};
	}

	@ModifyArg(
		method = "init",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;",
			ordinal = 0
		),
		index = 0
	)
	private Component diehardpractice$renameCreateButton(Component original) {
		return Component.translatable("diehardpractice.start_button");
	}

	@Inject(method = "onCreate", at = @At("HEAD"))
	private void diehardpractice$stashPracticeSelection(CallbackInfo ci) {
		// Always reflects THIS screen's current state (including "nothing picked" -> null), so stale selections
		// from an earlier visit to this screen never leak into a world the user didn't mean to practice-ify.
		// PracticeRestart's automated flow re-applies its own selection right after onCreate() runs, so it wins here.
		if (this.diehardpractice$gameTab instanceof GameTabStateAccessor state) {
			PracticeClientState.setPending(state.diehardpractice$getSelectedType(), state.diehardpractice$getSelectedTier(), state.diehardpractice$getSelectedBastionSubtype());
		}
	}
}
