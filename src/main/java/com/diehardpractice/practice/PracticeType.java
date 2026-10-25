package com.diehardpractice.practice;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum PracticeType {
	BASTION("diehardpractice.practice_type.bastion"),
	FORTRESS("diehardpractice.practice_type.fortress"),
	DRAGON("diehardpractice.practice_type.dragon"),
	END_CITY("diehardpractice.practice_type.end_city");

	private final String translationKey;

	PracticeType(String translationKey) {
		this.translationKey = translationKey;
	}

	public Component displayName() {
		return Component.translatable(this.translationKey);
	}

	/** Lowercase key used in the gear config file, e.g. "end_city". */
	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
