<p style="font-size:28px;text-align:center;"><b>ArenaMode CHANGELOG</b></p>

> Newest first. Records change sets against the version they belong to.

> [!NOTE]
> ArenaMode starts at `v26.0-Alpha.1`; the first alpha contains the complete feature set requested
> for the gladiator arena.

---

## {ChangeTime: 2026.10.04-05:40:00} GladiatorArena

GitCommitHash: initial

ChangedFiles:
```
.\settings.gradle +16 -0
.\build.gradle +73 -0
.\gradle.properties +25 -0
.\src\main\resources\fabric.mod.json +35 -0
.\src\main\resources\arenamode.mixins.json +14 -0
.\src\main\java\dev\blockconnect\arenamode\ArenaMode.java +36 -0
.\src\main\java\dev\blockconnect\arenamode\config\ArenaConfig.java +109 -0
.\src\main\java\dev\blockconnect\arenamode\config\ArenaConfigManager.java +76 -0
.\src\main\java\dev\blockconnect\arenamode\core\ArenaSession.java +201 -0
.\src\main\java\dev\blockconnect\arenamode\core\ArenaManager.java +307 -0
.\src\main\java\dev\blockconnect\arenamode\core\ArenaBoundary.java +63 -0
.\src\main\java\dev\blockconnect\arenamode\command\ArenaCommand.java +268 -0
.\src\main\java\dev\blockconnect\arenamode\item\ArenaRodItem.java +62 -0
.\src\main\java\dev\blockconnect\arenamode\item\ArenaItems.java +37 -0
.\src\main\java\dev\blockconnect\arenamode\client\ArenaModeClient.java +20 -0
.\src\main\java\dev\blockconnect\arenamode\client\ArenaPages.java +168 -0
.\src\main\java\dev\blockconnect\arenamode\mixin\EntityMoveMixin.java +48 -0
.\src\main\java\dev\blockconnect\arenamode\mixin\MobDespawnMixin.java +27 -0
```

ChangeLog:
Num|File Name|Change Description|Change Time|Changer
----|----|----|----|----
1|ArenaManager.java ArenaSession.java|New: wave rotation with survivors kept between visits, so a wave summons `configured + remaining` the next time it comes around|2026.10.04-05:40:00|StarsailsClover
2|ArenaManager.java|New: chunk-free square arena anchored on the player, entity cap, endless cycles and a fair Fisher-Yates shuffle per cycle|2026.10.04-05:40:00|StarsailsClover
3|EntityMoveMixin.java ArenaBoundary.java|New: the square wall - movement is compared before and after each step, so nothing crosses an edge in either direction; teleports are caught by a periodic containment pass|2026.10.04-05:40:00|StarsailsClover
4|ArenaRodItem.java ArenaItems.java|New: Arena Rod trigger item with right-click start and sneak-right-click stop, plus its model, texture and crafting recipe|2026.10.04-05:40:00|StarsailsClover
5|ArenaPages.java ArenaModeClient.java|New: the arena contributes Arena and Waves pages to BetterPeaceMode's shared settings screen instead of shipping its own screen and key binding|2026.10.04-05:40:00|StarsailsClover
6|ArenaCommand.java|New: `/arena` command surface for start/stop/status, radius, cap, endless, random and wave list editing|2026.10.04-05:40:00|StarsailsClover
7|ArenaConfig.java ArenaConfigManager.java|New: `config/arenamode.json` with waves, radius, cap and the two rotation switches|2026.10.04-05:40:00|StarsailsClover

version: v26.0-Alpha.1

---

License: Apache-2.0

GitHub@NDBlockConnect
