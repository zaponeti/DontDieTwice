package com.diehardpractice.practice;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** Per-world practice selection, applied-once flag and raid timer, persisted in the overworld's save data. */
public class PracticeWorldState extends SavedData {
	private static final String ID = "diehardpractice";

	@Nullable
	private PracticeType type;
	@Nullable
	private GearTier tier;
	private BastionSubtype bastionSubtype = BastionSubtype.ANY;
	private boolean setupDone;
	private long timerTicks;
	private boolean timerRunning;

	public static SavedData.Factory<PracticeWorldState> factory() {
		return new SavedData.Factory<>(PracticeWorldState::new, PracticeWorldState::load, DataFixTypes.LEVEL);
	}

	public static PracticeWorldState get(ServerLevel overworld) {
		return overworld.getDataStorage().computeIfAbsent(factory(), ID);
	}

	@Nullable
	public PracticeType getType() {
		return this.type;
	}

	@Nullable
	public GearTier getTier() {
		return this.tier;
	}

	public boolean hasPractice() {
		return this.type != null && this.tier != null;
	}

	public BastionSubtype getBastionSubtype() {
		return this.bastionSubtype;
	}

	public void setSelection(PracticeType type, GearTier tier, BastionSubtype bastionSubtype) {
		this.type = type;
		this.tier = tier;
		this.bastionSubtype = bastionSubtype;
		this.setDirty();
	}

	public boolean isSetupDone() {
		return this.setupDone;
	}

	public void markSetupDone() {
		this.setupDone = true;
		this.setDirty();
	}

	public long getTimerTicks() {
		return this.timerTicks;
	}

	public boolean isTimerRunning() {
		return this.timerRunning;
	}

	public void resetAndStartTimer() {
		this.timerTicks = 0L;
		this.timerRunning = true;
		this.setDirty();
	}

	public void stopTimer() {
		this.timerRunning = false;
		this.setDirty();
	}

	public void tickTimer() {
		if (this.timerRunning) {
			this.timerTicks++;
			this.setDirty();
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
		if (this.type != null) {
			tag.putString("Type", this.type.name());
		}
		if (this.tier != null) {
			tag.putString("Tier", this.tier.name());
		}
		tag.putString("BastionSubtype", this.bastionSubtype.name());
		tag.putBoolean("SetupDone", this.setupDone);
		tag.putLong("TimerTicks", this.timerTicks);
		tag.putBoolean("TimerRunning", this.timerRunning);
		return tag;
	}

	private static PracticeWorldState load(CompoundTag tag, HolderLookup.Provider provider) {
		PracticeWorldState state = new PracticeWorldState();
		if (tag.contains("Type")) {
			state.type = PracticeType.valueOf(tag.getString("Type"));
		}
		if (tag.contains("Tier")) {
			state.tier = GearTier.valueOf(tag.getString("Tier"));
		}
		if (tag.contains("BastionSubtype")) {
			state.bastionSubtype = BastionSubtype.valueOf(tag.getString("BastionSubtype"));
		}
		state.setupDone = tag.getBoolean("SetupDone");
		state.timerTicks = tag.getLong("TimerTicks");
		state.timerRunning = tag.getBoolean("TimerRunning");
		return state;
	}
}
