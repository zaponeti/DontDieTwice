package com.diehardpractice.client;

import com.diehardpractice.practice.BastionSubtype;
import com.diehardpractice.practice.GearTier;
import com.diehardpractice.practice.PracticeType;
import org.jetbrains.annotations.Nullable;

/** Plain (non-Mixin) interface GameTabMixin implements, so CreateWorldScreenMixin can read its current selection. */
public interface GameTabStateAccessor {
	@Nullable
	PracticeType diehardpractice$getSelectedType();

	@Nullable
	GearTier diehardpractice$getSelectedTier();

	BastionSubtype diehardpractice$getSelectedBastionSubtype();
}
