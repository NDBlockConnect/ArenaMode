<p style="font-size:28px;text-align:center;"><b>ArenaMode</b></p>

> Turn any spot into a gladiator arena: configure waves of entities with a count and a duration,
> rotate them endlessly and/or in a fair random order, and fight inside a square boundary nothing
> can walk through.

> [!NOTE]
> Built for Minecraft **1.21.11** on Fabric, and designed as a companion to
> **[BetterPeaceMode](https://github.com/NDBlockConnect/BetterPeaceMode)** - it reuses that mod's
> grudge ledger and contributes its settings to that mod's shared configuration window, so both mods
> together still mean one settings screen and one key binding.

---

## 1 Requirements

| | |
| :--- | :--- |
| Minecraft | 1.21.11 |
| Loader | Fabric Loader 0.19.3+ |
| Requires | Fabric API 0.141.6+1.21.11 **and** BetterPeaceMode 26.0.0-Alpha.5+ |

## 2 Playing a duel

Craft an **Arena Rod** (a fishing rod with two diamonds) or take one from the Tools & Utilities
creative tab, then:

| Action | Effect |
| :--- | :--- |
| Right-click | Start the duel, centred on where you stand |
| Sneak + right-click | Stop the duel and clear whatever it still has alive |

The same two actions exist as `/arena start` and `/arena stop`, and the Arena/Waves pages of the
BetterPeaceMode settings screen (default key `B`) edit every number without any permissions.

## 3 How a duel runs

- **Waves.** Each configured entry is an entity id, a count and a duration, e.g.
  `minecraft:zombie 5 180s`. A wave lasts its duration and then the rotation moves on - the entities
  you did not kill are **not** removed.
- **Leftovers come back with interest.** When the rotation reaches a wave again, it summons its
  configured count *on top of* the survivors from last time, so an unfinished wave grows exactly as
  the operator described: `spawned = configured + remaining`.
- **Endless.** Without it the duel ends after the last wave of the cycle and everything it summoned
  is removed. With it the rotation starts a new cycle instead.
- **Fair random order.** `randomOrder` shuffles the wave list every cycle with Fisher-Yates, so each
  wave appears exactly once per cycle - random, never unfair.
- **Entity cap.** `maxEntities` bounds how many arena entities may be alive at once. A wave that
  would push past the cap only summons what fits, and says so in chat.
- **The cap is a hard cap, splits included.** A slime that splits while the arena is full would
  otherwise push the population past the configured number and leave its children behind forever.
  Any arena-tagged entity no wave owns is therefore claimed on sight - by entity type when a wave
  uses that type, otherwise as a stray - so it stays inside the wall, counts against the cap and is
  removed with the rest when the duel ends. Anything that shows up while the cap is already reached
  is discarded instead.
- **The square.** The arena is a square column centred on the player, `radius` blocks from the
  centre to each edge. Nothing crosses an edge in either direction: walking, flying and pushed
  movement are clamped at the wall, and anything that teleports out - including the player - is
  pulled back to the nearest inside point within a second. Every arena entity is tagged
  `arenamode.arena`, kept out of vanilla despawning, and never calls BetterPeaceMode reinforcements
  (`bpm.no_reinforcements`), so a wave keeps the size you asked for.
- **Boundary particles.** With `boundaryParticles` on, the edge is drawn for the duellist as a ring
  of end-rod sparks.

## 4 Commands

Permission level 2 (gamemasters), so single-player worlds and server operators both work. In
single-player the GUI is usually faster.

| Command | Effect |
| :--- | :--- |
| `/arena start` / `/arena stop` | Begin or end a duel for the executing player. |
| `/arena status` | Show whether a duel is running, which wave, how many entities. |
| `/arena radius <4-128>` | Half-size of the square. |
| `/arena max <1-512>` | Arena entity cap. |
| `/arena endless <true\|false>` | Keep rotating after the last wave. |
| `/arena random <true\|false>` | Shuffle each cycle fairly. |
| `/arena wave add <entity> <count> <seconds>` | Append a wave (entity ids complete as you type). |
| `/arena wave remove <index>` | Remove one wave. |
| `/arena wave list` / `clear` | Show or reset the list. |
| `/arena reload` / `save` | Read or write `config/arenamode.json`. |

## 5 Configuration

`config/arenamode.json`:

```json
{
  "waves": [
    { "entity": "minecraft:zombie", "count": 5, "seconds": 180 }
  ],
  "radius": 16.0,
  "maxEntities": 48,
  "endless": false,
  "randomOrder": false,
  "boundaryParticles": true
}
```

## 6 Notes and limits

- Arena mobs are pointed at the duellist, so they fight in Real Peace and in vanilla difficulty
  alike. In BetterPeaceMode's Better Peace the world is Peaceful, which nullifies damage to the
  player: the duel still spawns and rotates, but the mobs cannot hurt you there.
- Creative and spectator players are never chased by BetterPeaceMode's rules, so a duel run in
  creative is a safe way to preview waves.
- Stopping or finishing a duel removes the entities it summoned. Survivors are only kept between
  waves of the same duel.
- Sessions live in memory: restarting the server ends them (the entities left in the world stay).

## 7 Build

```powershell
$env:JAVA_HOME = "<a JDK 25>"      # Loom 1.18 needs JDK 25 to run
.\gradlew.bat build                # the mod itself still targets Java 21
```

The build compiles against the BetterPeaceMode artifact in `libs/` (it is not bundled into the jar;
players install BetterPeaceMode themselves).

Output: `build/libs/ArenaMode-v26.0-Alpha.1-JE-1.21.11-Fabric.jar`.

---

License: Apache-2.0

GitHub@NDBlockConnect
