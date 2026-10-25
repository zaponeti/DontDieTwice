package com.diehardpractice.practice;

import com.diehardpractice.DieHardPractice;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Starting loadouts for every (PracticeType, GearTier) combination, loaded from the editable
 * config/diehardpractice_gear.json file (created from bundled defaults on first run).
 */
public final class GearPresets {
	private static final String CONFIG_FILE_NAME = "diehardpractice_gear.json";
	private static final String DEFAULT_RESOURCE_PATH = "/diehardpractice_gear.default.json";

	private static JsonObject root;

	private GearPresets() {
	}

	public record Loadout(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet, ItemStack offhand, List<ItemStack> hotbar) {
	}

	public static Loadout get(RegistryAccess registryAccess, PracticeType type, GearTier tier) {
		JsonObject tierObject = ensureLoaded().getAsJsonObject(type.id()).getAsJsonObject(tier.id());
		return new Loadout(
			parseSlot(registryAccess, tierObject, "head"),
			parseSlot(registryAccess, tierObject, "chest"),
			parseSlot(registryAccess, tierObject, "legs"),
			parseSlot(registryAccess, tierObject, "feet"),
			parseSlot(registryAccess, tierObject, "offhand"),
			parseHotbar(registryAccess, tierObject)
		);
	}

	private static ItemStack parseSlot(RegistryAccess registryAccess, JsonObject tierObject, String key) {
		return tierObject.has(key) ? parseItemEntry(registryAccess, tierObject.get(key)) : ItemStack.EMPTY;
	}

	private static List<ItemStack> parseHotbar(RegistryAccess registryAccess, JsonObject tierObject) {
		List<ItemStack> hotbar = new ArrayList<>();
		if (tierObject.has("hotbar")) {
			for (JsonElement element : tierObject.getAsJsonArray("hotbar")) {
				hotbar.add(parseItemEntry(registryAccess, element));
			}
		}
		return hotbar;
	}

	private static ItemStack parseItemEntry(RegistryAccess registryAccess, JsonElement element) {
		if (element.isJsonPrimitive()) {
			return new ItemStack(itemFromId(element.getAsString()));
		}

		JsonObject object = element.getAsJsonObject();
		ItemStack stack;
		if (object.has("potion")) {
			Holder<Potion> potion = BuiltInRegistries.POTION.getHolderOrThrow(ResourceKey.create(Registries.POTION, ResourceLocation.parse(object.get("potion").getAsString())));
			stack = PotionContents.createItemStack(Items.POTION, potion);
		} else {
			stack = new ItemStack(itemFromId(object.get("item").getAsString()));
		}

		if (object.has("count")) {
			stack.setCount(object.get("count").getAsInt());
		}

		if (object.has("enchantments")) {
			JsonObject enchantments = object.getAsJsonObject("enchantments");
			for (Map.Entry<String, JsonElement> entry : enchantments.entrySet()) {
				Holder<Enchantment> enchantment = registryAccess.registryOrThrow(Registries.ENCHANTMENT)
					.getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(entry.getKey())));
				stack.enchant(enchantment, entry.getValue().getAsInt());
			}
		}

		return stack;
	}

	private static Item itemFromId(String id) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
	}

	private static JsonObject ensureLoaded() {
		if (root == null) {
			root = load();
		}
		return root;
	}

	private static JsonObject load() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
		if (!Files.exists(configFile)) {
			copyDefaultTo(configFile);
		}

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			DieHardPractice.LOGGER.error("Failed to read {}, falling back to bundled defaults", configFile, e);
			return loadBundledDefault();
		}
	}

	private static void copyDefaultTo(Path configFile) {
		try (InputStream in = GearPresets.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
			Files.createDirectories(configFile.getParent());
			Files.copy(in, configFile);
		} catch (IOException e) {
			DieHardPractice.LOGGER.error("Failed to create default gear config at {}", configFile, e);
		}
	}

	private static JsonObject loadBundledDefault() {
		try (Reader reader = new java.io.InputStreamReader(GearPresets.class.getResourceAsStream(DEFAULT_RESOURCE_PATH), StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException("Bundled default gear config is missing or unreadable", e);
		}
	}
}
