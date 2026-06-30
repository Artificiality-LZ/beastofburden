# BeastOfBurden（牛马）

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.20-orange.svg)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE.txt)

**BeastOfBurden** is a [MineColonies](https://www.curseforge.com/minecraft/mc-mods/minecolonies) addon for Minecraft **1.20.1** (Forge). It adds a new Town Hall job — the *Beast of Burden* (牛马) — that helps keep your colony running when logistics stall and can autonomously plan new construction when builders are idle.

[English](#english) · [中文](#中文)

---

## English

### Features

**Item generation & delivery** — Scans colony requests that are stuck and cannot be fulfilled by normal logistics. Assigned beasts spend time generating the required items, then deliver them to the requester. Generation duration depends on item value; higher Strength speeds up work.

**Autonomous colony planning** — When enabled at the Town Hall, beasts plan the next hut, field, or upgrade while builders are available. Two modes are supported:

- **Scripted** — Follows a fixed build order (customizable via the plan editor).
- **Heuristic** — Adapts to the colony's current development phase (experimental).

**Town Hall module** — No dedicated hut required. Hire beasts from the Town Hall tab, view live work status, progress bars, and work history. Capacity scales with Town Hall level (1 / 2 / 3 beasts at levels 1–2 / 3–4 / 5).

### Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 1.20.1 |
| Forge | 47.4.20+ |
| MineColonies | 1.1.1214+ |

MineColonies pulls in its own dependencies (Structurize, BlockUI, Domum Ornamentum, Multi-Piston). Install them via your launcher or mod pack; do not distribute their jars from this repository.

### Installation

1. Install **Minecraft 1.20.1**, **Forge 47+**, and **MineColonies** (and its dependencies).
2. Download the latest `beastofburden-*.jar` from [Releases](https://github.com/Artificiality-LZ/beastofburden/releases) (or build from source).
3. Place the jar in your `mods` folder.
4. Start a world, build a Town Hall, and open the **Beast of Burden** tab to hire workers.

### Configuration

In-game: **Mods** → **BeastOfBurden** → config screen (generation timing, item values, work log limits).

Server config file: `world/serverconfig/beastofburden-server.toml` (after first run).

### Building from Source

```bash
git clone https://github.com/Artificiality-LZ/beastofburden.git
cd beastofburden
./gradlew build          # output: build/libs/beastofburden-*.jar
./gradlew runClient      # dev client (working dir: ./run)
```

**IDE:** Java 17 required. Run `./gradlew genIntellijRuns` for IntelliJ run configurations.

See [AGENTS.md](AGENTS.md) for a detailed project overview for contributors.

### Contributing

Issues and pull requests are welcome on [GitHub](https://github.com/Artificiality-LZ/beastofburden).

This mod uses MineColonies internals and Mixins; upstream MineColonies updates may require compatibility fixes.

### License

[GNU General Public License v3.0](LICENSE.txt) — Copyright (c) 2026 Artificiality-LZ

If you distribute modified versions of this mod, you must also release the corresponding source code under GPL-3.0.

---

## 中文

### 功能简介

**资源生成与配送** — 检测殖民地内卡住、无法正常满足的物资请求。牛马会花费时间凭空生成对应物品，并亲自配送到需求方。生成耗时与物品「价值」相关；力量属性越高，生成越快。

**自主规划殖民地** — 在市政厅开启后，当建筑工空闲时，牛马会自动规划下一座小屋、农田或升级。支持两种模式：

- **固定式** — 按预设顺序建造（可通过「编辑建造计划」自定义）。
- **启发式** — 根据殖民地发展阶段智能决策（实验性功能）。

**市政厅模块** — 无需单独盖小屋。在市政厅「牛马」标签页雇佣、查看当前工作与进度条、浏览工作记录。可雇佣数量随市政厅等级提升（1–2 级 1 名，3–4 级 2 名，5 级 3 名）。

### 安装

1. 安装 **Minecraft 1.20.1**、**Forge 47+** 及 **模拟殖民地（MineColonies）** 及其依赖。
2. 从 [Releases](https://github.com/Artificiality-LZ/beastofburden/releases) 下载 jar，或自行编译。
3. 将 jar 放入 `mods` 文件夹，进游戏建造市政厅，在「牛马」标签页雇佣即可。

### 从源码构建

```bash
git clone https://github.com/Artificiality-LZ/beastofburden.git
cd beastofburden
./gradlew build
./gradlew runClient
```

需要 **Java 17**。更多开发说明见 [AGENTS.md](AGENTS.md)。

### 许可证

[GNU 通用公共许可证 v3.0（GPL-3.0）](LICENSE.txt) — 版权所有 (c) 2026 Artificiality-LZ

若你分发本 mod 的修改版本，须以 GPL-3.0 同步公开对应源代码。
