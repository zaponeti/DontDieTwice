package com.diehardpractice.client;

import com.diehardpractice.network.PracticePackets;
import com.diehardpractice.practice.PracticeType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
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

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof SelectWorldScreen selectWorldScreen) {
				List<AbstractWidget> buttons = Screens.getButtons(screen);
				// Join vanilla's "Play Selected World"/"Create New World" row instead of adding a new one below
				// it - there's barely any vertical gap between that row, the scrollable world list above it, and
				// the "Edit"/"Delete"/"Back" row below it, so an extra row collides with one or the other.
				// Everything here is measured at runtime (not a hardcoded guess) so it tracks whatever space
				// vanilla's own row actually leaves free, in this version or any other.
				List<AbstractWidget> bottomRow = buttons.stream().filter(widget -> widget.getY() > screen.height / 2).toList();
				int rowY = bottomRow.stream().mapToInt(AbstractWidget::getY).min().orElse(screen.height - 52);
				List<AbstractWidget> rowButtons = bottomRow.stream()
					.filter(widget -> widget.getY() == rowY)
					.sorted(Comparator.comparingInt(AbstractWidget::getX))
					.toList();
				int rowRightEdge = rowButtons.stream().mapToInt(widget -> widget.getX() + widget.getWidth()).max().orElse(screen.width / 2);
				List<AbstractWidget> lowerRow = bottomRow.stream().filter(widget -> widget.getY() > rowY).toList();
				if (rowButtons.size() == 2 && !lowerRow.isEmpty()) {
					// Keep vanilla's original left-to-right order ("Play Selected World", "Create New World",
					// then Practice) but shrink all three to equal widths, sized so the whole row spans exactly
					// as wide as the "Edit"/"Delete"/"Re-Create"/"Back" row below it - measured at runtime from
					// that row's own bounds, not a hardcoded width. These are plain public widgets already
					// handed to us, so this is just calling their own setX()/setWidth(), not touching anything
					// private or vanilla-internal.
					AbstractWidget playButton = rowButtons.get(0);
					AbstractWidget createButton = rowButtons.get(1);
					int lowerRowLeft = lowerRow.stream().mapToInt(AbstractWidget::getX).min().orElse(playButton.getX());
					int lowerRowRight = lowerRow.stream().mapToInt(widget -> widget.getX() + widget.getWidth()).max().orElse(rowRightEdge);
					int gap = 4;
					int compactWidth = (lowerRowRight - lowerRowLeft - 2 * gap) / 3;
					int slot0X = lowerRowLeft;
					int slot1X = slot0X + compactWidth + gap;
					int slot2X = slot1X + compactWidth + gap;
					playButton.setX(slot0X);
					playButton.setWidth(compactWidth);
					createButton.setX(slot1X);
					createButton.setWidth(compactWidth);
					buttons.add(
						Button.builder(Component.translatable("diehardpractice.tab_title"), b -> client.setScreen(new PracticeScreen(selectWorldScreen)))
							.bounds(slot2X, rowY, compactWidth, 20)
							.build()
					);
				} else {
					// Fill whatever room is actually left in that row (clamped to a sane range) instead of a
					// fixed width, so it reads more like a third column rather than a narrow bolted-on button.
					int x = rowRightEdge + 4;
					int buttonWidth = Math.max(80, Math.min(200, screen.width - 4 - x));
					buttons.add(
						Button.builder(Component.translatable("diehardpractice.tab_title"), b -> client.setScreen(new PracticeScreen(selectWorldScreen)))
							.bounds(x, rowY, buttonWidth, 20)
							.build()
					);
				}
			} else if (screen instanceof TitleScreen titleScreen) {
				// Reachable from the title screen too: if no worlds exist yet, vanilla skips SelectWorldScreen
				// and jumps straight to CreateWorldScreen, so the button there would never even be seen.
				List<AbstractWidget> buttons = Screens.getButtons(screen);
				// Excludes the "Copyright Mojang AB..." corner link (a 10-tall PlainTextButton sitting right at
				// the bottom of the screen) - without this filter it looked like the "last" widget and pushed
				// our button below the visible screen entirely. Real menu buttons are all 20 tall.
				List<AbstractWidget> realButtons = buttons.stream()
					.filter(widget -> widget.getHeight() >= 20)
					.sorted(Comparator.comparingInt(AbstractWidget::getY))
					.toList();
				if (realButtons.size() >= 2) {
					// Insert as the 2nd entry, right under "Singleplayer", by nudging every button below it down
					// one row - these are plain public widgets already handed to us, so this is just calling
					// their own setY(), not touching anything private or vanilla-internal.
					int singleplayerY = realButtons.get(0).getY();
					int rowHeight = realButtons.get(1).getY() - singleplayerY;
					for (AbstractWidget widget : realButtons) {
						if (widget.getY() > singleplayerY) {
							widget.setY(widget.getY() + rowHeight);
						}
					}
					buttons.add(
						Button.builder(Component.translatable("diehardpractice.tab_title"), b -> client.setScreen(new PracticeScreen(titleScreen)))
							.bounds(screen.width / 2 - 100, singleplayerY + rowHeight, 200, 20)
							.build()
					);
				} else {
					int belowLastButton = realButtons.stream().mapToInt(widget -> widget.getY() + widget.getHeight()).max().orElse(screen.height / 4) + 4;
					buttons.add(
						Button.builder(Component.translatable("diehardpractice.tab_title"), b -> client.setScreen(new PracticeScreen(titleScreen)))
							.bounds(screen.width / 2 - 100, belowLastButton, 200, 20)
							.build()
					);
				}
			} else if (screen instanceof PauseScreen pauseScreen && pauseScreen.showsPauseMenu() && PracticeClientState.hasPractice) {
				Screens.getButtons(screen)
					.add(
						Button.builder(Component.translatable("diehardpractice.restart_button"), b -> PracticeRestart.perform(client))
							.bounds(screen.width / 2 - 100, screen.height - 28, 200, 20)
							.build()
					);
			}
		});
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

		PracticeType type = PracticeClientState.currentType;
		int centerX = graphics.guiWidth() / 2;
		int boxHeight = 24;
		int timeBoxWidth = 105;
		int iconBoxWidth = boxHeight;
		int totalWidth = timeBoxWidth + iconBoxWidth;
		int left = centerX - totalWidth / 2;
		int top = HUD_TOP_Y;
		int right = left + totalWidth;
		int bottom = top + boxHeight;
		int dividerX = left + timeBoxWidth;
		int borderColor = 0xFF3F3F3F;

		graphics.fill(left, top, right, bottom, 0xB0000000);
		graphics.fill(left, top, right, top + 1, borderColor);
		graphics.fill(left, bottom - 1, right, bottom, borderColor);
		graphics.fill(left, top, left + 1, bottom, borderColor);
		graphics.fill(right - 1, top, right, bottom, borderColor);
		graphics.fill(dividerX, top, dividerX + 1, bottom, borderColor);

		int timeColor = PracticeClientState.timerRunning ? 0x55FF55 : 0xAAAAAA;
		graphics.pose().pushPose();
		graphics.pose().translate(left + timeBoxWidth / 2.0F, top + boxHeight / 2.0F - 4, 0);
		graphics.pose().scale(1.5F, 1.5F, 1.5F);
		graphics.drawCenteredString(minecraft.font, timeText, 0, 0, timeColor);
		graphics.pose().popPose();

		graphics.renderItem(new ItemStack(iconFor(type)), dividerX + (iconBoxWidth - 16) / 2, top + (boxHeight - 16) / 2);

		Component infoText = Component.empty().append(type.displayName()).append(" · ").append(PracticeClientState.currentTier.displayName());
		graphics.drawCenteredString(minecraft.font, infoText, centerX, bottom + 4, 0xDDDDDD);
	}

	private static Item iconFor(PracticeType type) {
		return switch (type) {
			case BASTION -> Items.PIGLIN_HEAD;
			case FORTRESS -> Items.BLAZE_ROD; // vanilla has no Blaze head item
			case DRAGON -> Items.DRAGON_HEAD;
			case END_CITY -> Items.ELYTRA;
		};
	}
}
