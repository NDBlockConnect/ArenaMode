<p style="font-size:28px;text-align:center;"><b>ArenaMode FACT</b></p>

> Development facts for ArenaMode, newest first. Each entry records what was checked, how it was
> checked, and what the evidence was.

---

## {FACTTime: 2026.10.04-05:35:00} ArenaCoreVerified {FACTNum 2}

GitCommitHashRange: initial (unmerged)

Files:
```
.\src\main\java\dev\blockconnect\arenamode\core\ArenaManager.java
.\src\main\java\dev\blockconnect\arenamode\core\ArenaSession.java
.\src\main\java\dev\blockconnect\arenamode\core\ArenaBoundary.java
.\src\main\java\dev\blockconnect\arenamode\item\ArenaRodItem.java
```

### What's Happened?
Every rule the owner asked for was measured on a live Fabric 0.19.3 dedicated server with a real
1.21.11 client, driven through RCON and MDL/Despotes.

### Any evidence?

| Rule | Setup | Result |
|---|---|---|
| Rotation keeps leftovers | wave 1 = 3 zombies (20 s), wave 2 = 2 cows (20 s), endless | wave 2 log: `spawned=2 ... aliveTotal=5` - the zombies were still there |
| `configured + remaining` | one zombie killed during wave 1, then wait for the cycle to return | log: `wave 1/2 minecraft:zombie x3 for 20s started: spawned=3 survivors=2 aliveTotal=7`; counts: 5 zombies and 2 cows |
| Entity cap | wave of 10 zombies, `maxEntities = 4` | exactly 4 zombies |
| Wall, movement | zombie given `Motion [10,0,0]` | stopped at the east edge (x = 12 with the centre at 0.5 and radius 12) |
| Wall, teleport | player teleported to (200, 111, 200) | pulled back to the inside corner (12, -60, 12) |
| Rod, start | `give arenamode:arena_rod` then right-click | `arena status` flips to running and the wave summons |
| Rod, stop | sneak + right-click | `arena status` idle and 0 entities carry the arena tag |
| Endless off | two waves of 20 s | after the second wave the session reports `Duel complete` and cleans up |
| Fair random | 3 waves, `randomOrder = true`, endless | each cycle contains each wave exactly once; consecutive cycles differed (order changed on the third cycle) |
| Staged size protected | arena zombie hit while carrying `bpm.no_reinforcements` | group stays at 3, no BetterPeaceMode helpers |

### Any Founds?
A session ends the moment its owner is not alive, which is correct for a duel but made three early
runs look like "nothing spawned": the test player had been killed by the previous arena and was
sitting on the death screen. The wave log line (`spawned=...`) is what made that visible, and it
stays in the mod because it is equally useful for operators.

### FACTs
A game feature that spawns entities must log what it spawned; without that line a dead test subject
and a broken spawn look identical.

version: v26.0-Alpha.1

---

## {FACTTime: 2026.10.04-05:25:00} SharedSettingsScreenAndQuoting {FACTNum 1}

GitCommitHashRange: initial (unmerged)

Files:
```
.\src\main\java\dev\blockconnect\arenamode\client\ArenaPages.java
.\src\main\java\dev\blockconnect\arenamode\command\ArenaCommand.java
```

### What's Happened?
The owner asked for the settings GUI to be shared with BetterPeaceMode and for a trigger item, and
the first command syntax turned out to be a trap.

### Any evidence?
1. BetterPeaceMode Alpha.5 added a client page API; the client log confirms
   `[ArenaMode] registered 2 settings pages with BetterPeaceMode`, and screenshots of the shared
   window show `Page: Rules (1/4)`, `Reinforcements (2/4)`, `Arena (3/4)`, `Waves (4/4)` with the
   arena's own controls on the last two.
2. Screenshots also exposed three GUI defects that were fixed in BetterPeaceMode: full-width
   controls overlapping the row above, a doubled label on every cycle button, and an `EditBox` that
   rendered nothing (a zero-width construction left `displayPos` at the end of the value, and the
   first colour used to fix it had alpha 0).
3. `StringArgumentType.string()` refused `minecraft:zombie`: a colon is not allowed in Brigadier's
   unquoted string form, so every wave had to be quoted. The argument is now the vanilla entity-type
   argument (`ResourceArgument.resource(...)`), which accepts the id unquoted and completes it.

### FACTs
For a namespaced id, use the registry-backed argument type; a plain string argument forces players
to quote their ids.

version: v26.0-Alpha.1

---

License: Apache-2.0

GitHub@NDBlockConnect
