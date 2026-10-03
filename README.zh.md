<p style="font-size:28px;text-align:center;"><b>ArenaMode 角斗场</b></p>

> 把任何一块地方变成角斗场：配置一波波实体（实体 ID + 数量 + 时长），可无尽轮换、可公平随机轮换，并在一个"任何东西都穿不过去"的方形边界里决斗。

> [!NOTE]
> 面向 Minecraft **1.21.11**（Fabric），并且是 **[BetterPeaceMode](https://github.com/NDBlockConnect/BetterPeaceMode)** 的配套模组：复用它的仇恨账本，并把设置页面交给它的共享配置窗口，因此两个模组一起装也只有一个设置界面、一个按键。

---

## 1 依赖

| | |
| :--- | :--- |
| Minecraft | 1.21.11 |
| 加载器 | Fabric Loader 0.19.3+ |
| 依赖 | Fabric API 0.141.6+1.21.11 **以及** BetterPeaceMode 26.0.0-Alpha.5+ |

## 2 怎么开始决斗

合成一把 **角斗钓竿**（钓鱼竿 + 两颗钻石），或在创造模式"工具与实用物品"栏里直接取，然后：

| 操作 | 效果 |
| :--- | :--- |
| 右键 | 以你所在位置为中心开始决斗 |
| 潜行 + 右键 | 结束决斗并清掉场上剩余的角斗实体 |

同样两个动作也有指令版本 `/arena start` 与 `/arena stop`；所有数值都可以在 BetterPeaceMode 设置界面（默认按键 `B`）的 Arena / Waves 两页里改，不需要任何权限。

## 3 决斗规则

- **波次。** 每个配置项是"实体 ID + 数量 + 时长"，例如 `minecraft:zombie 5 180s`。一波持续到时间结束，然后轮换到下一波——**没杀掉的实体不会被清除**。
- **遗留会累加。** 轮换再次回到这一波时，会在上次幸存者之上召唤本波配置的数量，即 `本次生成 = 配置数量 + 剩余数量`，与需求完全一致。
- **无尽模式。** 关闭时，走完一轮后决斗结束并清理它召唤的所有实体；打开时直接开始下一轮。
- **公平随机。** 打开 `randomOrder` 后，每一轮都用 Fisher-Yates 重排波次列表，因此每个波次在一轮内恰好出现一次——随机，但不不公平。
- **实体上限。** `maxEntities` 限制场上角斗实体总数；会超出的部分不再召唤，并在聊天栏提示。
- **方形边界。** 以玩家为中心、半径 `radius` 格的方形柱体。任何实体都无法从任意方向穿过：行走、飞行、被推动都会被墙面截停；靠传送跑出去的东西（包括玩家）会在 1 秒内被拉回最近的场内位置。所有角斗实体带有 `arenamode.arena` 标签、不会被原版清除，也不会呼叫 BetterPeaceMode 的增援（`bpm.no_reinforcements`），因此一波就是你要的数量。
- **边界粒子。** 打开 `boundaryParticles` 时，会给决斗者画出末地烛粒子构成的边界线。

## 4 指令

需要权限等级 2（管理员）。单机下建议直接用图形界面。

| 指令 | 作用 |
| :--- | :--- |
| `/arena start` / `/arena stop` | 为执行者开始 / 结束决斗。 |
| `/arena status` | 显示是否进行中、第几波、场上实体数。 |
| `/arena radius <4-128>` | 方形半径。 |
| `/arena max <1-512>` | 角斗场实体上限。 |
| `/arena endless <true\|false>` | 无尽轮换。 |
| `/arena random <true\|false>` | 每轮公平随机。 |
| `/arena wave add <实体> <数量> <秒>` | 追加一波（实体 ID 可自动补全）。 |
| `/arena wave remove <序号>` | 删除某一波。 |
| `/arena wave list` / `clear` | 查看 / 重置列表。 |
| `/arena reload` / `save` | 读取 / 写入 `config/arenamode.json`。 |

## 5 配置文件

`config/arenamode.json`：

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

## 6 说明与限制

- 角斗实体会被指向决斗者，因此在"真正的和平"与原版难度下都会真的打起来；在"更好的和平"（和平难度）下，原版会把玩家受到的伤害清零——波次照样刷新轮换，但生物伤不到你。
- 按照 BetterPeaceMode 的规则，创造/旁观模式玩家不会被追击，所以用创造模式预览波次是安全的。
- 结束或走完决斗会清除它召唤的实体；幸存者只在同一场决斗的波次之间保留。
- 会话保存在内存中：服务器重启会结束会话（留在世界里的实体会保留）。

## 7 构建

```powershell
$env:JAVA_HOME = "<一个 JDK 25>"    # Loom 1.18 需要 JDK 25 运行
.\gradlew.bat build                 # 模组本身仍以 Java 21 为目标
```

构建会针对 `libs/` 里的 BetterPeaceMode 构件编译（该 jar 不会被打包进产物；玩家需自行安装 BetterPeaceMode）。

产物：`build/libs/ArenaMode-v26.0-Alpha.1-JE-1.21.11-Fabric.jar`。

---

License: Apache-2.0

GitHub@NDBlockConnect
