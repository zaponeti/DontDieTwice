package com.diehardpractice.practice;

import com.diehardpractice.mixin.PoolElementStructurePieceAccessor;
import com.diehardpractice.mixin.SinglePoolElementAccessor;
import com.diehardpractice.network.PracticePackets;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Core practice logic: gearing up, teleporting to the chosen target, and (re)starting the raid timer. */
public final class PracticeService {
	private PracticeService() {
	}

	private static final int STRUCTURE_SEARCH_RADIUS = 100;
	private static final int MAX_BASTION_ATTEMPTS = 6;
	private static final int BASTION_SEARCH_JUMP = 2500;

	// Completion goals: how much of the structure's signature loot counts as "done" and auto-stops the timer.
	private static final int BASTION_ENDER_PEARLS_GOAL = 16;
	private static final int FORTRESS_BLAZE_RODS_GOAL = 8;

	/** Runs the full practice setup: clear inventory, give gear, teleport, (re)start timer. Used for the initial join and for restarts. */
	public static void applySetup(MinecraftServer server, ServerPlayer player, PracticeType type, GearTier tier, BastionSubtype bastionSubtype) {
		giveGear(server, player, type, tier);
		teleport(server, player, type, bastionSubtype);
		PracticeWorldState state = PracticeWorldState.get(server.overworld());
		state.resetAndStartTimer();
		syncState(player, state);
	}

	private static void giveGear(MinecraftServer server, ServerPlayer player, PracticeType type, GearTier tier) {
		player.getInventory().clearContent();
		GearPresets.Loadout loadout = GearPresets.get(server.registryAccess(), type, tier);
		player.setItemSlot(EquipmentSlot.HEAD, loadout.head());
		player.setItemSlot(EquipmentSlot.CHEST, loadout.chest());
		player.setItemSlot(EquipmentSlot.LEGS, loadout.legs());
		player.setItemSlot(EquipmentSlot.FEET, loadout.feet());
		player.setItemSlot(EquipmentSlot.OFFHAND, loadout.offhand());
		List<ItemStack> items = loadout.items();
		int inventorySize = player.getInventory().getContainerSize();
		for (int i = 0; i < items.size() && i < inventorySize; i++) {
			player.getInventory().setItem(i, items.get(i));
		}
	}

	private static void teleport(MinecraftServer server, ServerPlayer player, PracticeType type, BastionSubtype bastionSubtype) {
		switch (type) {
			case BASTION -> teleportToBastion(server, player, bastionSubtype);
			case FORTRESS -> teleportToStructure(server, player, Level.NETHER, BuiltinStructures.FORTRESS, true);
			case END_CITY -> teleportToStructure(server, player, Level.END, BuiltinStructures.END_CITY, false);
			case DRAGON -> teleportToDragonPlatform(server, player);
		}
	}

	/**
	 * Locates a bastion remnant of the requested variant. Vanilla's locate API only finds "the nearest bastion",
	 * with no way to ask for a specific variant directly - the variant is only knowable after inspecting the
	 * generated structure's starting piece. So this tries up to {@link #MAX_BASTION_ATTEMPTS} candidates, jumping
	 * the search point well past each rejected one, and settles for the last candidate if none matched in time.
	 */
	private static void teleportToBastion(MinecraftServer server, ServerPlayer player, BastionSubtype desiredSubtype) {
		ServerLevel targetLevel = server.getLevel(Level.NETHER);
		if (targetLevel == null) {
			return;
		}

		//? if >=26.1 {
		/*Registry<Structure> registry = targetLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		Holder<Structure> holder = registry.getOrThrow(BuiltinStructures.BASTION_REMNANT);
		*///?} else {
		Registry<Structure> registry = targetLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
		Holder<Structure> holder = registry.getHolderOrThrow(BuiltinStructures.BASTION_REMNANT);
		//?}
		HolderSet<Structure> holderSet = HolderSet.direct(holder);

		//? if >=26.1 {
		/*BlockPos origin = targetLevel.getRespawnData().pos();
		*///?} else {
		BlockPos origin = targetLevel.getSharedSpawnPos();
		//?}
		StructureStart matchedStart = null;
		StructureStart lastStart = null;

		for (int attempt = 0; attempt < MAX_BASTION_ATTEMPTS; attempt++) {
			Pair<BlockPos, Holder<Structure>> result = targetLevel.getChunkSource()
				.getGenerator()
				.findNearestMapStructure(targetLevel, holderSet, origin, STRUCTURE_SEARCH_RADIUS, false);
			if (result == null) {
				break;
			}

			BlockPos pos = result.getFirst();
			//? if >=26.1 {
			/*SectionPos sectionPos = SectionPos.of(pos);
			List<StructureStart> starts = targetLevel.structureManager().startsForStructure(sectionPos.x(), sectionPos.z(), holder.value());
			*///?} else {
			List<StructureStart> starts = targetLevel.structureManager().startsForStructure(SectionPos.of(pos), holder.value());
			//?}
			if (starts.isEmpty()) {
				break;
			}

			StructureStart start = starts.get(0);
			lastStart = start;
			if (desiredSubtype == BastionSubtype.ANY || detectBastionSubtype(start) == desiredSubtype) {
				matchedStart = start;
				break;
			}

			origin = jitterPast(origin, pos);
		}

		StructureStart chosen = matchedStart != null ? matchedStart : lastStart;
		if (chosen == null) {
			player.sendSystemMessage(Component.translatable("diehardpractice.structure_not_found"));
			return;
		}

		if (matchedStart == null && desiredSubtype != BastionSubtype.ANY) {
			player.sendSystemMessage(Component.translatable("diehardpractice.bastion_subtype_not_found", desiredSubtype.displayName()));
		}

		landOnStructure(targetLevel, player, chosen.getBoundingBox(), true);
	}

	private static BastionSubtype detectBastionSubtype(StructureStart start) {
		List<StructurePiece> pieces = start.getPieces();
		if (pieces.isEmpty() || !(pieces.get(0) instanceof PoolElementStructurePieceAccessor poolPieceAccessor)) {
			return BastionSubtype.ANY;
		}

		StructurePoolElement element = poolPieceAccessor.diehardpractice$getElement();
		if (!(element instanceof SinglePoolElementAccessor singleElementAccessor)) {
			return BastionSubtype.ANY;
		}

		return singleElementAccessor.diehardpractice$getTemplate().left().map(BastionSubtype::fromStartingPiece).orElse(BastionSubtype.ANY);
	}

	/** Jumps the search origin well past a rejected candidate so the next nearest-structure search finds a different one. */
	private static BlockPos jitterPast(BlockPos origin, BlockPos rejected) {
		double dx = rejected.getX() - origin.getX();
		double dz = rejected.getZ() - origin.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0) {
			dx = 1.0;
			dz = 0.0;
			length = 1.0;
		}

		return new BlockPos(rejected.getX() + (int) (dx / length * BASTION_SEARCH_JUMP), 0, rejected.getZ() + (int) (dz / length * BASTION_SEARCH_JUMP));
	}

	private static void teleportToStructure(
		MinecraftServer server, ServerPlayer player, ResourceKey<Level> dimension, ResourceKey<Structure> structureKey, boolean fireResistance
	) {
		ServerLevel targetLevel = server.getLevel(dimension);
		if (targetLevel == null) {
			return;
		}

		//? if >=26.1 {
		/*Registry<Structure> registry = targetLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		Holder<Structure> holder = registry.getOrThrow(structureKey);
		*///?} else {
		Registry<Structure> registry = targetLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
		Holder<Structure> holder = registry.getHolderOrThrow(structureKey);
		//?}
		HolderSet<Structure> holderSet = HolderSet.direct(holder);
		//? if >=26.1 {
		/*BlockPos origin = targetLevel.getRespawnData().pos();
		*///?} else {
		BlockPos origin = targetLevel.getSharedSpawnPos();
		//?}
		Pair<BlockPos, Holder<Structure>> result = targetLevel.getChunkSource()
			.getGenerator()
			.findNearestMapStructure(targetLevel, holderSet, origin, STRUCTURE_SEARCH_RADIUS, false);
		if (result == null) {
			player.sendSystemMessage(Component.translatable("diehardpractice.structure_not_found"));
			return;
		}

		BlockPos approx = result.getFirst();
		//? if >=26.1 {
		/*SectionPos sectionPos = SectionPos.of(approx);
		List<StructureStart> starts = targetLevel.structureManager().startsForStructure(sectionPos.x(), sectionPos.z(), holder.value());
		*///?} else {
		List<StructureStart> starts = targetLevel.structureManager().startsForStructure(SectionPos.of(approx), holder.value());
		//?}
		if (starts.isEmpty()) {
			player.sendSystemMessage(Component.translatable("diehardpractice.structure_not_found"));
			return;
		}

		landOnStructure(targetLevel, player, starts.get(0).getBoundingBox(), fireResistance);
	}

	/**
	 * Teleports the player to a safe spot near the given structure's bounding box and grants the usual
	 * safety-net effects. Shared by every structure-based practice target (bastion, fortress, end city).
	 */
	private static void landOnStructure(ServerLevel targetLevel, ServerPlayer player, BoundingBox box, boolean fireResistance) {
		// findNearestMapStructure()'s own Y coordinate is a placement-search placeholder (always ~0), not the
		// structure's real height - the bounding box (forced to generate by the locate call above) has the truth.
		BlockPos landing = findSafeLanding(targetLevel, box);
		if (landing == null) {
			int centerX = (box.minX() + box.maxX()) / 2;
			int centerZ = (box.minZ() + box.maxZ()) / 2;
			//? if >=26.1 {
			/*landing = new BlockPos(centerX, Math.min(100, targetLevel.getMaxY() - 10), centerZ);
			*///?} else {
			landing = new BlockPos(centerX, Math.min(100, targetLevel.getMaxBuildHeight() - 10), centerZ);
			//?}
			player.sendSystemMessage(Component.translatable("diehardpractice.unsafe_landing"));
		}

		//? if >=26.1 {
		/*player.teleportTo(targetLevel, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
		*///?} else {
		player.teleportTo(targetLevel, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, player.getYRot(), player.getXRot());
		//?}
		if (fireResistance) {
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 30, 0));
		}
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 10, 0));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 1));
	}

	/**
	 * Scans real block data (forcing chunks to generate first) for a safe, open landing spot just outside the
	 * structure's bounding box - landing inside it risks dropping straight into a room full of hostile mobs.
	 * Falls back to scanning on/inside the structure only if nothing safe is found around its perimeter.
	 */
	@Nullable
	private static BlockPos findSafeLanding(ServerLevel level, BoundingBox box) {
		int centerX = (box.minX() + box.maxX()) / 2;
		int centerZ = (box.minZ() + box.maxZ()) / 2;
		//? if >=26.1 {
		/*int top = Math.min(box.maxY() + 20, level.getMaxY() - 2);
		int bottom = Math.max(box.minY() - 15, level.getMinY() + 1);
		*///?} else {
		int top = Math.min(box.maxY() + 20, level.getMaxBuildHeight() - 2);
		int bottom = Math.max(box.minY() - 15, level.getMinBuildHeight() + 1);
		//?}

		int buffer = 12;
		int[][] outsideColumns = {
			{centerX, box.maxZ() + buffer},
			{centerX, box.minZ() - buffer},
			{box.maxX() + buffer, centerZ},
			{box.minX() - buffer, centerZ},
			{box.maxX() + buffer, box.maxZ() + buffer},
			{box.minX() - buffer, box.minZ() - buffer},
			{box.maxX() + buffer, box.minZ() - buffer},
			{box.minX() - buffer, box.maxZ() + buffer}
		};
		BlockPos landing = scanColumns(level, outsideColumns, top, bottom);
		if (landing != null) {
			return landing;
		}

		// Nothing safe just outside - fall back to scanning on top of / inside the structure itself.
		int[][] insideColumns = {{centerX, centerZ}, {centerX + 4, centerZ}, {centerX - 4, centerZ}, {centerX, centerZ + 4}, {centerX, centerZ - 4}};
		return scanColumns(level, insideColumns, top, bottom);
	}

	@Nullable
	private static BlockPos scanColumns(ServerLevel level, int[][] columns, int top, int bottom) {
		for (int[] column : columns) {
			int x = column[0];
			int z = column[1];
			level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL);

			for (int y = top; y >= bottom; y--) {
				BlockPos feet = new BlockPos(x, y, z);
				if (isOpen(level, feet) && isOpen(level, feet.above()) && isSafeFloor(level, feet.below())) {
					return feet;
				}
			}
		}

		return null;
	}

	private static boolean isOpen(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || (!state.isSolid() && state.getFluidState().isEmpty());
	}

	private static boolean isSafeFloor(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isSolid() && state.getFluidState().isEmpty() && !state.is(Blocks.MAGMA_BLOCK) && !state.is(Blocks.FIRE);
	}

	private static void teleportToDragonPlatform(MinecraftServer server, ServerPlayer player) {
		ServerLevel end = server.getLevel(Level.END);
		if (end == null) {
			return;
		}

		// Mirrors vanilla's own obsidian-platform arrival logic (see EndPortalBlock#getPortalDestination).
		EndPlatformFeature.createEndPlatform(end, ServerLevel.END_SPAWN_POINT.below(), true);
		//? if >=26.1 {
		/*Vec3 landing = Vec3.atBottomCenterOf(ServerLevel.END_SPAWN_POINT).subtract(0.0, 1.0, 0.0);
		*///?} else {
		Vec3 landing = ServerLevel.END_SPAWN_POINT.getBottomCenter().subtract(0.0, 1.0, 0.0);
		//?}
		//? if >=26.1 {
		/*player.teleportTo(end, landing.x(), landing.y(), landing.z(), java.util.Set.of(), net.minecraft.core.Direction.WEST.toYRot(), 0.0F, true);
		*///?} else {
		player.teleportTo(end, landing.x(), landing.y(), landing.z(), net.minecraft.core.Direction.WEST.toYRot(), 0.0F);
		//?}
	}

	/**
	 * Auto-stops the raid timer once the player has the structure's signature loot (or, for the dragon, the kill
	 * advancement) - same idea as a speedrun split, so you don't have to remember to run /practice stop yourself.
	 */
	public static void checkCompletion(ServerPlayer player, PracticeType type, PracticeWorldState state) {
		if (!state.isTimerRunning()) {
			return;
		}

		boolean complete = switch (type) {
			case DRAGON -> hasAdvancement(player, "end/kill_dragon");
			case BASTION -> countItem(player, Items.ENDER_PEARL) >= BASTION_ENDER_PEARLS_GOAL;
			case FORTRESS -> countItem(player, Items.BLAZE_ROD) >= FORTRESS_BLAZE_RODS_GOAL;
			case END_CITY -> hasAdvancement(player, "end/elytra"); // "Sky's the Limit" - picking up an Elytra
		};

		if (complete) {
			state.stopTimer();
			syncState(player, state);
			player.sendSystemMessage(Component.translatable("diehardpractice.practice_complete"));
		}
	}

	private static boolean hasAdvancement(ServerPlayer player, String path) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(ResourceLocation.withDefaultNamespace(path));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	// Container.getItem(int) covers every slot (hotbar, main, armor, offhand) via the plain public index-based
	// API - avoids depending on Inventory's internal item/armor/offhand field layout, which changed between
	// versions (armor and offhand moved out of Inventory entirely and into an EntityEquipment component).
	private static int countItem(ServerPlayer player, Item item) {
		int count = 0;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			count += stack.is(item) ? stack.getCount() : 0;
		}
		return count;
	}

	public static void syncState(ServerPlayer player, PracticeWorldState state) {
		ServerPlayNetworking.send(
			player,
			new PracticePackets.PracticeStateSyncPayload(
				state.hasPractice(),
				state.getType() != null ? state.getType() : PracticeType.BASTION,
				state.getTier() != null ? state.getTier() : GearTier.NEWBIE,
				state.getBastionSubtype(),
				state.isTimerRunning(),
				state.getTimerTicks()
			)
		);
	}
}
