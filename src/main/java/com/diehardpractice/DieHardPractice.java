package com.diehardpractice;

import com.diehardpractice.network.PracticePackets;
import com.diehardpractice.practice.PracticeService;
import com.diehardpractice.practice.PracticeWorldState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DieHardPractice implements ModInitializer {
	public static final String MOD_ID = "diehardpractice";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PracticePackets.register();

		ServerPlayNetworking.registerGlobalReceiver(PracticePackets.SelectPracticePayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				PracticeWorldState state = PracticeWorldState.get(context.server().overworld());
				if (!state.isSetupDone()) {
					state.setSelection(payload.practiceType(), payload.tier(), payload.bastionSubtype());
					state.markSetupDone();
					PracticeService.applySetup(context.server(), player, payload.practiceType(), payload.tier(), payload.bastionSubtype());
				}
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			PracticeWorldState state = PracticeWorldState.get(server.overworld());
			PracticeService.syncState(player, state);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			PracticeWorldState state = PracticeWorldState.get(server.overworld());
			state.tickTimer();
			if (server.getTickCount() % 20 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (state.hasPractice()) {
						PracticeService.checkCompletion(player, state.getType(), state);
					}

					PracticeService.syncState(player, state);
				}
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("practice")
				.then(
					Commands.literal("stop")
						.executes(ctx -> {
							PracticeWorldState state = PracticeWorldState.get(ctx.getSource().getServer().overworld());
							state.stopTimer();
							for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
								PracticeService.syncState(player, state);
							}
							ctx.getSource().sendSuccess(() -> Component.translatable("diehardpractice.timer_stopped"), false);
							return 1;
						})
				)
				.then(
					Commands.literal("restart")
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							PracticeWorldState state = PracticeWorldState.get(ctx.getSource().getServer().overworld());
							if (!state.hasPractice()) {
								ctx.getSource().sendFailure(Component.translatable("diehardpractice.no_practice_data"));
								return 0;
							}

							// Recreating the world only the client can do (quit to title, create a fresh world) - ask it to.
							ServerPlayNetworking.send(player, new PracticePackets.RequestClientRestartPayload());
							return 1;
						})
				)
		));
	}
}
