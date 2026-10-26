package com.diehardpractice.network;

import com.diehardpractice.practice.BastionSubtype;
import com.diehardpractice.practice.GearTier;
import com.diehardpractice.practice.PracticeType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class PracticePackets {
	private PracticePackets() {
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("diehardpractice", path);
	}

	/** C2S: sent once, right after the player's first join, if a practice selection is pending on the client. */
	public record SelectPracticePayload(PracticeType practiceType, GearTier tier, BastionSubtype bastionSubtype) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SelectPracticePayload> TYPE = new CustomPacketPayload.Type<>(id("select_practice"));
		public static final StreamCodec<FriendlyByteBuf, SelectPracticePayload> STREAM_CODEC = CustomPacketPayload.codec(
			SelectPracticePayload::write, SelectPracticePayload::new
		);

		private SelectPracticePayload(FriendlyByteBuf buf) {
			this(buf.readEnum(PracticeType.class), buf.readEnum(GearTier.class), buf.readEnum(BastionSubtype.class));
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeEnum(this.practiceType);
			buf.writeEnum(this.tier);
			buf.writeEnum(this.bastionSubtype);
		}

		@Override
		public CustomPacketPayload.Type<SelectPracticePayload> type() {
			return TYPE;
		}
	}

	/**
	 * S2C: sent in response to the /practice restart command (the pause-menu button acts client-side directly,
	 * without a round trip). Recreating the world is inherently a client-driven action (quit to title, create a
	 * fresh world), so the server can only ask the client to do it - never do it itself.
	 */
	public record RequestClientRestartPayload() implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<RequestClientRestartPayload> TYPE = new CustomPacketPayload.Type<>(id("request_client_restart"));
		public static final StreamCodec<FriendlyByteBuf, RequestClientRestartPayload> STREAM_CODEC = CustomPacketPayload.codec(
			RequestClientRestartPayload::write, RequestClientRestartPayload::new
		);

		private RequestClientRestartPayload(FriendlyByteBuf buf) {
			this();
		}

		private void write(FriendlyByteBuf buf) {
		}

		@Override
		public CustomPacketPayload.Type<RequestClientRestartPayload> type() {
			return TYPE;
		}
	}

	/** S2C: sent on join and whenever the stored practice state or timer changes, so the client can show the HUD / Restart button. */
	public record PracticeStateSyncPayload(
		boolean hasPractice, PracticeType practiceType, GearTier tier, BastionSubtype bastionSubtype, boolean timerRunning, long timerTicks
	) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<PracticeStateSyncPayload> TYPE = new CustomPacketPayload.Type<>(id("state_sync"));
		public static final StreamCodec<FriendlyByteBuf, PracticeStateSyncPayload> STREAM_CODEC = CustomPacketPayload.codec(
			PracticeStateSyncPayload::write, PracticeStateSyncPayload::new
		);

		private PracticeStateSyncPayload(FriendlyByteBuf buf) {
			this(
				buf.readBoolean(),
				buf.readEnum(PracticeType.class),
				buf.readEnum(GearTier.class),
				buf.readEnum(BastionSubtype.class),
				buf.readBoolean(),
				buf.readVarLong()
			);
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeBoolean(this.hasPractice);
			buf.writeEnum(this.practiceType != null ? this.practiceType : PracticeType.BASTION);
			buf.writeEnum(this.tier != null ? this.tier : GearTier.NEWBIE);
			buf.writeEnum(this.bastionSubtype != null ? this.bastionSubtype : BastionSubtype.ANY);
			buf.writeBoolean(this.timerRunning);
			buf.writeVarLong(this.timerTicks);
		}

		@Override
		public CustomPacketPayload.Type<PracticeStateSyncPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		//? if >=26.1 {
		/*PayloadTypeRegistry.serverboundPlay().register(SelectPracticePayload.TYPE, SelectPracticePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PracticeStateSyncPayload.TYPE, PracticeStateSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RequestClientRestartPayload.TYPE, RequestClientRestartPayload.STREAM_CODEC);
		*///?} else {
		PayloadTypeRegistry.playC2S().register(SelectPracticePayload.TYPE, SelectPracticePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PracticeStateSyncPayload.TYPE, PracticeStateSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(RequestClientRestartPayload.TYPE, RequestClientRestartPayload.STREAM_CODEC);
		//?}
	}
}
