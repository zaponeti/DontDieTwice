package com.diehardpractice.practice;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum GearTier {
	NEWBIE("diehardpractice.gear_tier.newbie"),
	EXPERIENCED("diehardpractice.gear_tier.experienced"),
	PRO("diehardpractice.gear_tier.pro"),
	SPEEDRUNNER("diehardpractice.gear_tier.speedrunner");

	public static final Codec<GearTier> CODEC = Codec.STRING.xmap(GearTier::valueOf, GearTier::name);

	private final String translationKey;

	GearTier(String translationKey) {
		this.translationKey = translationKey;
	}

	public Component displayName() {
		return Component.translatable(this.translationKey);
	}

	/** Lowercase key used in the gear config file, e.g. "experienced". */
	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
