package com.diehardpractice.client;

import com.diehardpractice.practice.BastionSubtype;
import com.diehardpractice.practice.GearTier;
import com.diehardpractice.practice.PracticeType;
import org.jetbrains.annotations.Nullable;

/** Client-only cache: a pending pre-world selection, plus the latest state synced from the server. */
public final class PracticeClientState {
	private PracticeClientState() {
	}

	// Set by the practice create-world screen before any world exists; consumed on the first join.
	@Nullable
	public static PracticeType pendingType;
	@Nullable
	public static GearTier pendingTier;
	public static BastionSubtype pendingBastionSubtype = BastionSubtype.ANY;

	// Synced from the server (on join, and whenever the practice state or timer changes).
	public static boolean hasPractice;
	@Nullable
	public static PracticeType currentType;
	@Nullable
	public static GearTier currentTier;
	public static BastionSubtype currentBastionSubtype = BastionSubtype.ANY;
	public static boolean timerRunning;
	public static long timerTicks;
	/** System.nanoTime() when timerTicks was last synced, so the HUD can interpolate smooth sub-tick time. */
	public static long timerSyncedAtNanos;

	public static void setPending(PracticeType type, GearTier tier, BastionSubtype bastionSubtype) {
		pendingType = type;
		pendingTier = tier;
		pendingBastionSubtype = bastionSubtype;
	}

	public static void clearPending() {
		pendingType = null;
		pendingTier = null;
		pendingBastionSubtype = BastionSubtype.ANY;
	}
}
