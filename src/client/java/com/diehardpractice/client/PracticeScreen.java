package com.diehardpractice.client;

import com.diehardpractice.client.mixin.CreateWorldScreenInvoker;
import com.diehardpractice.practice.BastionSubtype;
import com.diehardpractice.practice.GearTier;
import com.diehardpractice.practice.PracticeType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

/**
 * Standalone practice-setup screen: pick a structure, gear tier, and (for Bastion) a variant, then hand off to
 * vanilla's own (unmodified) Create World screen. Kept as a plain Screen - not a Mixin into any vanilla class -
 * so it stays stable across Minecraft versions instead of depending on internal screen layout/field names.
 */
public final class PracticeScreen extends Screen {
	private static final int FIELD_WIDTH = 210;
	private static final int ROW_HEIGHT = 22;

	private final Screen parent;
	private final Map<PracticeType, Button> typeButtons = new EnumMap<>(PracticeType.class);
	private final Map<GearTier, Button> tierButtons = new EnumMap<>(GearTier.class);
	private Button bastionButton;
	private Button startButton;
	private EditBox nameBox;
	private Button difficultyButton;
	@Nullable
	private PracticeType selectedType;
	@Nullable
	private GearTier selectedTier;
	private BastionSubtype selectedBastionSubtype = BastionSubtype.ANY;
	private Difficulty selectedDifficulty = Difficulty.NORMAL;

	private int nameLabelY;
	private int structureLabelY;
	private int equipmentLabelY;

	public PracticeScreen(Screen parent) {
		super(Component.translatable("diehardpractice.tab_title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int left = centerX - FIELD_WIDTH / 2;
		int y = 24;

		this.nameLabelY = y;
		y += ROW_HEIGHT;

		this.nameBox = this.addRenderableWidget(new EditBox(this.font, left, y, FIELD_WIDTH, 20, Component.translatable("selectWorld.enterName")));
		this.nameBox.setValue("New Practice");
		y += ROW_HEIGHT;

		this.difficultyButton = this.addRenderableWidget(
			Button.builder(this.difficultyButtonLabel(), b -> this.cycleDifficulty()).bounds(left, y, FIELD_WIDTH, 20).build()
		);
		y += ROW_HEIGHT + ROW_HEIGHT / 2;

		this.structureLabelY = y;
		y += ROW_HEIGHT;

		for (PracticeType type : PracticeType.values()) {
			Button button = this.addRenderableWidget(Button.builder(type.displayName(), b -> this.selectType(type)).bounds(left, y, FIELD_WIDTH, 20).build());
			this.typeButtons.put(type, button);
			y += ROW_HEIGHT;
		}

		this.equipmentLabelY = y;
		y += ROW_HEIGHT;

		for (GearTier tier : GearTier.values()) {
			Button button = this.addRenderableWidget(Button.builder(tier.displayName(), b -> this.selectTier(tier)).bounds(left, y, FIELD_WIDTH, 20).build());
			this.tierButtons.put(tier, button);
			y += ROW_HEIGHT;
		}

		this.bastionButton = this.addRenderableWidget(Button.builder(this.bastionButtonLabel(), b -> this.cycleBastionSubtype()).bounds(left, y, FIELD_WIDTH, 20).build());
		this.bastionButton.visible = false;
		y += ROW_HEIGHT + ROW_HEIGHT / 2;

		this.startButton = this.addRenderableWidget(
			Button.builder(Component.translatable("diehardpractice.start_button"), b -> this.startPractice()).bounds(left, y, FIELD_WIDTH, 20).build()
		);
		this.startButton.active = false;
		y += ROW_HEIGHT;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).bounds(left, y, FIELD_WIDTH, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("selectWorld.enterName"), this.width / 2, this.nameLabelY + 6, 0xFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("diehardpractice.structure_label"), this.width / 2, this.structureLabelY + 6, 0xFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("diehardpractice.equipment_label"), this.width / 2, this.equipmentLabelY + 6, 0xFFFFFF);
	}

	private void selectType(PracticeType type) {
		this.selectedType = type;
		this.typeButtons.forEach((candidate, button) -> button.setMessage(label(candidate.displayName(), candidate == type)));
		this.bastionButton.visible = type == PracticeType.BASTION;
		this.updateStartButton();
	}

	private void selectTier(GearTier tier) {
		this.selectedTier = tier;
		this.tierButtons.forEach((candidate, button) -> button.setMessage(label(candidate.displayName(), candidate == tier)));
		this.updateStartButton();
	}

	private void cycleBastionSubtype() {
		BastionSubtype[] values = BastionSubtype.values();
		this.selectedBastionSubtype = values[(this.selectedBastionSubtype.ordinal() + 1) % values.length];
		this.bastionButton.setMessage(this.bastionButtonLabel());
	}

	private Component bastionButtonLabel() {
		return Component.translatable("diehardpractice.bastion_subtype_label").append(": ").append(this.selectedBastionSubtype.displayName());
	}

	private void cycleDifficulty() {
		Difficulty[] values = Difficulty.values();
		this.selectedDifficulty = values[(this.selectedDifficulty.ordinal() + 1) % values.length];
		this.difficultyButton.setMessage(this.difficultyButtonLabel());
	}

	private Component difficultyButtonLabel() {
		return Component.translatable("options.difficulty").append(": ").append(this.selectedDifficulty.getDisplayName());
	}

	private void updateStartButton() {
		this.startButton.active = this.selectedType != null && this.selectedTier != null;
	}

	private void startPractice() {
		if (this.selectedType == null || this.selectedTier == null) {
			return;
		}
		PracticeClientState.setPending(this.selectedType, this.selectedTier, this.selectedBastionSubtype);
		// Skip the vanilla Create World screen entirely - create the world immediately with the name/
		// difficulty picked here, same trick PracticeRestart uses to recreate a world without re-prompting.
		CreateWorldScreen.openFresh(this.minecraft, this.parent);
		if (this.minecraft.screen instanceof CreateWorldScreen createWorldScreen) {
			String name = this.nameBox.getValue().isBlank() ? "New Practice" : this.nameBox.getValue();
			createWorldScreen.getUiState().setName(name);
			createWorldScreen.getUiState().setDifficulty(this.selectedDifficulty);
			((CreateWorldScreenInvoker) createWorldScreen).diehardpractice$onCreate();
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private static Component label(Component base, boolean selected) {
		return selected ? Component.literal("» ").append(base).append(" «") : base;
	}
}
