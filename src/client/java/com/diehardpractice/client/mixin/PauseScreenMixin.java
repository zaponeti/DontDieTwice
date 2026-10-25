package com.diehardpractice.client.mixin;

import com.diehardpractice.client.PracticeClientState;
import com.diehardpractice.client.PracticeRestart;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a "Restart" button to the pause menu when the current save has stored practice data. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	protected PauseScreenMixin(Component component) {
		super(component);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void diehardpractice$addRestartButton(CallbackInfo ci) {
		PauseScreen self = (PauseScreen) (Object) this;
		if (self.showsPauseMenu() && PracticeClientState.hasPractice) {
			this.addRenderableWidget(
				Button.builder(Component.translatable("diehardpractice.restart_button"), button -> PracticeRestart.perform(this.minecraft))
					.bounds(this.width / 2 - 100, this.height - 28, 200, 20)
					.build()
			);
		}
	}
}
