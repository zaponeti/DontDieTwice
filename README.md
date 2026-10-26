# Don't Die Twice - Practice mode

A Fabric mod for practicing Minecraft speedrun structures — Bastion Remnants, Nether Fortresses, the Ender Dragon
fight, and End Cities — without the overworld grind. Pick a target and a gear tier from the world-creation screen,
and the mod drops you right next to it with a matching loadout and a raid timer running.

**Minecraft 1.21.1 · Fabric Loader 0.20.1+ · Fabric API · Java 21+**

## Features

- **One-click setup** — a "Practice" button sits next to "Create New World" on the world-select screen. Pick a
  structure, a gear tier, and (for Bastions) a specific variant, then hit *Start Practice* — the world is created
  and you're teleported immediately.
- **Four practice targets** — Bastion Remnant (with Housing / Stables / Treasure / Bridge variant selection),
  Nether Fortress, the Ender Dragon fight, and an End City.
- **Four gear tiers** — Newbie, Experienced, Pro, and Speedrunner, from fully-enchanted diamond gear down to a
  couple of wooden tools and an apple. Every loadout is fully editable (see [Configuration](#configuration)).
- **Safe landing, not auto-arrival** — the mod teleports you *near* the target, scanning real generated terrain
  for an open, solid spot instead of guessing a height, so you don't spawn inside a wall, in lava, or straight
  into a room full of mobs. You still have to spot and reach the structure itself, same as on a real run.
- **Raid timer** — a HUD clock starts the moment you land. It stops automatically once you've met the structure's
  goal (the dragon-kill or *Sky's the Limit* advancement, or enough Blaze Rods / Ender Pearls), or any time with
  `/practice stop`.
- **Instant restart** — the pause-menu button (or `/practice restart`) quits and recreates the world with the
  *same seed and the same selection*, so you land back on an untouched copy of the exact same structure instead of
  a random new one.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.1 and drop it in your `mods` folder.
3. Download the latest Don't Die Twice - Practice mode release and drop it in the same `mods` folder.

## Usage

1. From the title screen, open **Singleplayer**.
2. Click **Practice** next to *Create New World*.
3. On the Game tab, pick a **Practice structure**, an **Equipment** tier, and (if you picked Bastion) a
   **Bastion type**.
4. Click **Start Practice**. A fresh world is generated and you're teleported near the target — not directly
   into it — with your gear and the timer already running. Finding and reaching the structure from there is
   still on you.
5. When you're done (or want a clean redo), open the pause menu and hit **Restart** — or type `/practice restart`.

Starting gear for every structure/tier combination is fully customizable — see [Configuration](#configuration).

### Commands

| Command             | Effect                                                                 |
|----------------------|------------------------------------------------------------------------|
| `/practice stop`     | Stops the raid timer where it is.                                     |
| `/practice restart`  | Recreates the current world with the same seed and the same practice selection. |

## Configuration

Starting loadouts live in `config/diehardpractice_gear.json`, generated from the mod's built-in defaults the first
time you start a practice run. It's plain JSON — edit items, counts, potions, and enchantments freely; changes take
effect the next time the mod loads (restart the game after editing).

```json
"bastion": {
  "newbie": {
    "head": { "item": "minecraft:diamond_helmet", "enchantments": { "minecraft:protection": 4 } },
    "feet": "minecraft:golden_boots",
    "offhand": "minecraft:shield",
    "items": [
      { "item": "minecraft:diamond_sword", "enchantments": { "minecraft:sharpness": 5 } },
      { "item": "minecraft:arrow", "count": 1 },
      { "potion": "minecraft:fire_resistance", "count": 2 }
    ]
  }
}
```

- Each of `head` / `chest` / `legs` / `feet` / `offhand` is either a plain item ID string or an object with
  `item`, `count`, and `enchantments`.
- `items` is a flat list filling the player's inventory (36 slots); entries exceeding an item's real max stack
  size are split across multiple stacks automatically.
- A potion entry uses `"potion"` instead of `"item"` (e.g. `{"potion": "minecraft:fire_resistance", "count": 4}`).

## Building from source

```bash
./gradlew build
```

The distributable jar is written to `build/libs/`. To try it out directly:

```bash
./gradlew runClient
```

Requires JDK 21 to compile the mod itself; the Fabric Loom build tooling currently needs JDK 25 to run (set via
`org.gradle.java.home` in `gradle.properties`).

## Roadmap

Don't Die Twice - Practice mode currently covers Bastion Remnants, Nether Fortresses, the Ender Dragon fight, and End Cities.
More raid-practice targets are planned for future releases — likely candidates include the Ancient City, Trial
Chambers, the Woodland Mansion, and Ocean Monuments. Suggestions are welcome.

## License

MIT — see [`LICENSE`](LICENSE). Use, modify, and redistribute freely.
