# Store listing copy (Modrinth / CurseForge)

Copy the pieces below into the project page when creating the Modrinth and CurseForge listings. Both platforms
render standard Markdown, so the body works as-is on either one.

## Summary (Modrinth "Summary" field, CurseForge "Short description")

> Practice Bastions, Fortresses, the Ender Dragon, and End Cities instantly — pick a target and a gear tier, get
> teleported in with a raid timer running.

## Tags / categories

`Utility` · `Adventure` · `Game Mechanics` — Fabric, Client+Server (singleplayer-focused)

## Body description

---

### Jump straight into the fight

Don't Die Twice - Practice mode turns Minecraft's world-creation screen into a structure-practice launcher. Pick what you want to
work on, pick how much of a head start you want, and go — no seed-hunting, no overworld walk, no re-gearing by
hand.

### How it works

1. On the world-select screen, click **Practice** next to *Create New World*.
2. Choose a **Practice structure**: Bastion Remnant, Nether Fortress, the Ender Dragon fight, or an End City.
   Picking a Bastion also lets you cycle a specific variant — Housing, Stables, Treasure, Bridge, or any.
3. Choose an **Equipment** tier: Newbie, Experienced, Pro, or Speedrunner.
4. Click **Start Practice**. A new world generates and you're teleported near the structure immediately, geared
   up and ready — not dropped directly inside it, so you'll still spot and approach it yourself like on a real
   run.

### What you get

- ⚔️ **Four practice targets**, each with their own safe-landing logic (the mod scans real generated terrain for
  an open spot outside the structure instead of guessing a height — no more spawning in lava or straight into a
  mob room).
- 🎒 **Four gear tiers**, from fully enchanted diamond gear with building blocks and utility items (Newbie) down
  to two wooden tools and an apple (Speedrunner). Every one of the 16 loadouts is fully customizable in a plain
  JSON config file — change items, counts, potions, and enchantments however you like.
- ⏱️ **An automatic raid timer** that starts the moment you land and stops itself once you've hit the structure's
  goal (defeating the dragon, picking up an Elytra, or gathering enough Blaze Rods / Ender Pearls) — or stop it
  yourself any time with `/practice stop`.
- 🔁 **One-click restart** — the pause-menu button or `/practice restart` quits and recreates the world with the
  *exact same seed*, so you land back on a fresh, unexplored copy of the exact same structure for another attempt,
  instead of a random new one.

### Commands

- `/practice stop` — stop the raid timer where it is.
- `/practice restart` — recreate the current world with the same seed and practice selection.

### Configuration

Starting gear lives in `config/diehardpractice_gear.json` (created from the built-in defaults the first time you
start a run). Edit items, stack counts, potions, and enchantments freely — see the README on the project's source
repository for the exact format.

### Requirements

- Minecraft 1.21.1
- [Fabric Loader](https://fabricmc.net/use/) 0.20.1+
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 21+

### Roadmap

Bastion Remnant, Nether Fortress, the Ender Dragon fight, and End City are the four targets today. More
raid-practice structures are planned for future updates — likely candidates include the Ancient City, Trial
Chambers, the Woodland Mansion, and Ocean Monuments.

### Notes

Don't Die Twice - Practice mode is built around singleplayer practice runs. It doesn't need an existing world or any particular
seed to work — just click Practice and go.

---

## Gallery suggestions

Add screenshots of: the Practice tab on the Create World screen, the raid timer HUD mid-run, and the pause-menu
Restart button, before publishing.
