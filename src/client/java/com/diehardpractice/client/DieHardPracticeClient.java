package com.diehardpractice.client;

import com.diehardpractice.network.PracticePackets;
import com.diehardpractice.practice.PracticeType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DieHardPracticeClient implements ClientModInitializer {
	// Vanilla's boss-event bar occupies roughly y=3..31 at the top-center of the screen (see BossHealthOverlay);
	// the HUD used to sit at y=8 and get visually buried under the dragon's health bar during the fight.
	private static final int HUD_TOP_Y = 42;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(PracticePackets.PracticeStateSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			PracticeClientState.hasPractice = payload.hasPractice();
			PracticeClientState.currentType = payload.practiceType();
			PracticeClientState.currentTier = payload.tier();
			PracticeClientState.currentBastionSubtype = payload.bastionSubtype();
			PracticeClientState.timerRunning = payload.timerRunning();
			PracticeClientState.timerTicks = payload.timerTicks();
			PracticeClientState.timerSyncedAtNanos = System.nanoTime();
		}));

		ClientPlayNetworking.registerGlobalReceiver(
			PracticePackets.RequestClientRestartPayload.TYPE, (payload, context) -> context.client().execute(() -> PracticeRestart.perform(context.client()))
		);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (PracticeClientState.pendingType != null && PracticeClientState.pendingTier != null) {
				ClientPlayNetworking.send(
					new PracticePackets.SelectPracticePayload(PracticeClientState.pendingType, PracticeClientState.pendingTier, PracticeClientState.pendingBastionSubtype)
				);
				PracticeClientState.clearPending();
			}
		});

		HudRenderCallback.EVENT.register(DieHardPracticeClient::renderHud);
	}

	private static void renderHud(GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
		if (!PracticeClientState.hasPractice) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		long displayTicks = PracticeClientState.timerTicks;
		if (PracticeClientState.timerRunning) {
			long elapsedNanos = System.nanoTime() - PracticeClientState.timerSyncedAtNanos;
			displayTicks += elapsedNanos / 50_000_000L; // 50ms per tick
		}

		long totalMillis = displayTicks * 50L;
		long hours = totalMillis / 3_600_000L;
		long minutes = totalMillis / 60_000L % 60L;
		long seconds = totalMillis / 1000L % 60L;
		long centis = totalMillis / 10L % 100L;
		String timeText = hours > 0 ? String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, centis) : String.format("%d:%02d.%02d", minutes, seconds, centis);

		int centerX = graphics.guiWidth() / 2;
		int boxWidth = 150;
		int boxHeight = 20;
		int boxLeft = centerX - boxWidth / 2;

		graphics.fill(boxLeft, HUD_TOP_Y, boxLeft + boxWidth, HUD_TOP_Y + boxHeight, 0xB0000000);
		graphics.fill(boxLeft, HUD_TOP_Y, boxLeft + boxWidth, HUD_TOP_Y + 1, 0xFF3F3F3F);
		graphics.fill(boxLeft, HUD_TOP_Y + boxHeight - 1, boxLeft + boxWidth, HUD_TOP_Y + boxHeight, 0xFF3F3F3F);

		int timeColor = PracticeClientState.timerRunning ? 0x55FF55 : 0xAAAAAA;
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, HUD_TOP_Y + boxHeight / 2.0F - 4, 0);
		graphics.pose().scale(1.5F, 1.5F, 1.5F);
		graphics.drawCenteredString(minecraft.font, timeText, 0, 0, timeColor);
		graphics.pose().popPose();

		PracticeType type = PracticeClientState.currentType;
		Component infoText = Component.empty().append(type.displayName()).append(" · ").append(PracticeClientState.currentTier.displayName());
		int infoWidth = minecraft.font.width(infoText);
		int iconSize = 16;
		int groupWidth = infoWidth + 4 + iconSize;
		int groupLeft = centerX - groupWidth / 2;
		int infoY = HUD_TOP_Y + boxHeight + 4;

		graphics.renderItem(new ItemStack(iconFor(type)), groupLeft, infoY - 4);
		graphics.drawString(minecraft.font, infoText, groupLeft + iconSize + 4, infoY, 0xDDDDDD);
	}

	private static Item iconFor(PracticeType type) {
		return switch (type) {
			case BASTION -> Items.GILDED_BLACKSTONE;
			case FORTRESS -> Items.NETHER_BRICKS;
			case DRAGON -> Items.DRAGON_HEAD;
			case END_CITY -> Items.ELYTRA;
		};
	}
}
