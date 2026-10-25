package com.diehardpractice.client.mixin;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes CreateWorldScreen's private onCreate() so the Practice tab can trigger world creation directly. */
@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenInvoker {
	@Invoker("onCreate")
	void diehardpractice$onCreate();
}
