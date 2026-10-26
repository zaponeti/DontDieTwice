package com.diehardpractice.practice;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The 4 bastion remnant variants vanilla can generate, identified by the path of their starting jigsaw piece. */
public enum BastionSubtype {
	ANY("diehardpractice.bastion_subtype.any", null),
	HOUSING("diehardpractice.bastion_subtype.housing", "bastion/units/"),
	STABLES("diehardpractice.bastion_subtype.stables", "bastion/hoglin_stable/"),
	TREASURE("diehardpractice.bastion_subtype.treasure", "bastion/treasure/"),
	BRIDGE("diehardpractice.bastion_subtype.bridge", "bastion/bridge/");

	public static final Codec<BastionSubtype> CODEC = Codec.STRING.xmap(BastionSubtype::valueOf, BastionSubtype::name);

	private final String translationKey;
	private final String startingPiecePrefix;

	BastionSubtype(String translationKey, String startingPiecePrefix) {
		this.translationKey = translationKey;
		this.startingPiecePrefix = startingPiecePrefix;
	}

	public Component displayName() {
		return Component.translatable(this.translationKey);
	}

	/** Identifies the subtype of a generated bastion from the resource location of its starting piece template. */
	public static BastionSubtype fromStartingPiece(ResourceLocation startingPiece) {
		String path = startingPiece.getPath();
		for (BastionSubtype subtype : values()) {
			if (subtype.startingPiecePrefix != null && path.startsWith(subtype.startingPiecePrefix)) {
				return subtype;
			}
		}
		return ANY;
	}
}
