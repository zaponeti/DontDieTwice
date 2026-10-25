package com.diehardpractice.client.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Adds a "Practice" entry point into the same row as the vanilla "Play Selected World" / "Create New World" buttons. */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {
	private static final String BOUNDS_TARGET = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;";

	protected SelectWorldScreenMixin(Component component) {
		super(component);
	}

	@ModifyArgs(method = "init", at = @At(value = "INVOKE", target = BOUNDS_TARGET, ordinal = 0))
	private void diehardpractice$resizePlaySelectedWorld(Args args) {
		args.set(0, this.width / 2 - 154);
		args.set(2, 100);
	}

	@ModifyArgs(method = "init", at = @At(value = "INVOKE", target = BOUNDS_TARGET, ordinal = 1))
	private void diehardpractice$resizeCreateNewWorld(Args args) {
		args.set(0, this.width / 2 - 50);
		args.set(2, 100);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void diehardpractice$addPracticeButton(CallbackInfo ci) {
		this.addRenderableWidget(
			Button.builder(Component.translatable("diehardpractice.tab_title"), button -> CreateWorldScreen.openFresh(this.minecraft, this))
				.bounds(this.width / 2 + 54, this.height - 52, 100, 20)
				.build()
		);
	}
}
