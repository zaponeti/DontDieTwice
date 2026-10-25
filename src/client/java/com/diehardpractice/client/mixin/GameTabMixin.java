package com.diehardpractice.client.mixin;

import com.diehardpractice.client.GameTabStateAccessor;
import com.diehardpractice.practice.BastionSubtype;
import com.diehardpractice.practice.GearTier;
import com.diehardpractice.practice.PracticeType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a Practice-structure / Equipment section (single column, same width as the vanilla fields above) to the Game tab. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class GameTabMixin extends GridLayoutTab implements GameTabStateAccessor {
	private static final int FIELD_WIDTH = 210;

	// Synthetic outer-instance reference; not part of the official mappings, so it keeps its raw intermediary name.
	@Shadow(remap = false)
	@Final
	CreateWorldScreen field_42174;

	// The real GameTab constructor only takes the synthetic outer-instance parameter (its title is a static
	// constant passed to super() internally) - this stub's body is discarded by Mixin, only the signature matters.
	protected GameTabMixin(CreateWorldScreen createWorldScreen) {
		super(Component.empty());
	}

	private final Map<PracticeType, Button> diehardpractice$typeButtons = new EnumMap<>(PracticeType.class);
	private final Map<GearTier, Button> diehardpractice$tierButtons = new EnumMap<>(GearTier.class);
	private Button diehardpractice$bastionButton;
	@Nullable
	private PracticeType diehardpractice$selectedType;
	@Nullable
	private GearTier diehardpractice$selectedTier;
	private BastionSubtype diehardpractice$selectedBastionSubtype = BastionSubtype.ANY;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void diehardpractice$addPracticeSection(CallbackInfo ci) {
		GridLayout grid = this.layout;
		Minecraft minecraft = Minecraft.getInstance();

		int row = 5;
		grid.addChild(new StringWidget(Component.translatable("diehardpractice.structure_label"), minecraft.font), row++, 0);

		for (PracticeType type : PracticeType.values()) {
			Button button = Button.builder(type.displayName(), b -> this.diehardpractice$selectType(type)).width(FIELD_WIDTH).build();
			grid.addChild(button, row++, 0);
			this.diehardpractice$typeButtons.put(type, button);
		}

		grid.addChild(new StringWidget(Component.translatable("diehardpractice.equipment_label"), minecraft.font), row++, 0);

		for (GearTier tier : GearTier.values()) {
			Button button = Button.builder(tier.displayName(), b -> this.diehardpractice$selectTier(tier)).width(FIELD_WIDTH).build();
			grid.addChild(button, row++, 0);
			this.diehardpractice$tierButtons.put(tier, button);
		}

		this.diehardpractice$bastionButton = Button.builder(this.diehardpractice$bastionButtonLabel(), b -> this.diehardpractice$cycleBastionSubtype())
			.width(FIELD_WIDTH)
			.build();
		this.diehardpractice$bastionButton.visible = false;
		grid.addChild(this.diehardpractice$bastionButton, row, 0);
	}

	private void diehardpractice$selectType(PracticeType type) {
		this.diehardpractice$selectedType = type;
		this.diehardpractice$typeButtons.forEach((candidate, button) -> button.setMessage(diehardpractice$label(candidate.displayName(), candidate == type)));
		this.diehardpractice$bastionButton.visible = type == PracticeType.BASTION;
	}

	private void diehardpractice$selectTier(GearTier tier) {
		this.diehardpractice$selectedTier = tier;
		this.diehardpractice$tierButtons.forEach((candidate, button) -> button.setMessage(diehardpractice$label(candidate.displayName(), candidate == tier)));
	}

	private void diehardpractice$cycleBastionSubtype() {
		BastionSubtype[] values = BastionSubtype.values();
		this.diehardpractice$selectedBastionSubtype = values[(this.diehardpractice$selectedBastionSubtype.ordinal() + 1) % values.length];
		this.diehardpractice$bastionButton.setMessage(this.diehardpractice$bastionButtonLabel());
	}

	private Component diehardpractice$bastionButtonLabel() {
		return Component.translatable("diehardpractice.bastion_subtype_label").append(": ").append(this.diehardpractice$selectedBastionSubtype.displayName());
	}

	@Override
	@Nullable
	public PracticeType diehardpractice$getSelectedType() {
		return this.diehardpractice$selectedType;
	}

	@Override
	@Nullable
	public GearTier diehardpractice$getSelectedTier() {
		return this.diehardpractice$selectedTier;
	}

	@Override
	public BastionSubtype diehardpractice$getSelectedBastionSubtype() {
		return this.diehardpractice$selectedBastionSubtype;
	}

	private static Component diehardpractice$label(Component base, boolean selected) {
		return selected ? Component.literal("» ").append(base).append(" «") : base;
	}
}
