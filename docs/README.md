# BeastOfBurden 文档索引（给 AI）

本目录是 **策划 + 技术** 的权威文字源。代码是运行真相；文档描述意图、规则与接线状态。二者冲突时：**以代码为准，并回写文档**。

阅读顺序见下文。不要把 `AGENTS.md` / `CONTEXT.md` 当策划书——它们只负责环境与工程约束。

## 文档约定

| 标记 | 含义 |
|------|------|
| **已实现** | 当前代码按此规则运行 |
| **部分实现** | 有 UI / NBT / 配置，但逻辑不完整 |
| **遗留** | 枚举、NBT、文案仍在，规划逻辑已停用 |
| **未接线** | 类或配置存在，规划/工作循环未调用 |

改功能时：先读对应 **策划** 页，再读 **技术** 页，改完同步两边（尤其是公式、默认计划、NBT 键、网络协议）。

## 策划 `design/`

```
design/
  00-overview/          总览：定位、术语、核心循环
  01-systems/           系统：职业、补给、规划、选址、农田
  02-economy/           数值：生成公式、物品价值、容量与冷却
  03-ui/                界面：市政厅页、计划编辑器、配置屏
  04-content/           内容：建筑目录、默认 12 步计划
  05-config.md          全部 Forge 配置项与接线状态
```

| 何时读 | 文件 |
|--------|------|
| 不了解这个 mod 是什么 | [00-overview/vision.md](design/00-overview/vision.md) |
| 遇到术语 / 枚举名 | [00-overview/glossary.md](design/00-overview/glossary.md) |
| 改玩家流程 | [00-overview/player-loop.md](design/00-overview/player-loop.md) |
| 雇佣 / 容量 / 技能 | [01-systems/job.md](design/01-systems/job.md) |
| 卡住请求、生成、配送 | [01-systems/supply.md](design/01-systems/supply.md) |
| 自治规划总览与门控 | [01-systems/planning.md](design/01-systems/planning.md) |
| 固定式计划 | [01-systems/planning-scripted.md](design/01-systems/planning-scripted.md) |
| 启发式打分 | [01-systems/planning-heuristic.md](design/01-systems/planning-heuristic.md) |
| 选址 / 占地 / 道路 | [01-systems/placement.md](design/01-systems/placement.md) |
| 农田 | [01-systems/fields.md](design/01-systems/fields.md) |
| 改 tick / 价值 / 冷却 | [02-economy/numbers.md](design/02-economy/numbers.md) |
| 改 GUI 文案或布局 | [03-ui/ui.md](design/03-ui/ui.md) |
| 增删可规划建筑 | [04-content/buildings.md](design/04-content/buildings.md) |
| 改默认建造顺序 | [04-content/default-plan.md](design/04-content/default-plan.md) |
| 增删 config 项 | [05-config.md](design/05-config.md) |

## 技术 `tech/`

```
tech/
  architecture.md       运行时管线与 tick 职责
  packages.md           包 / 类地图
  townhall-module.md    市政厅模块、雇佣、NBT 同步
  ai-work.md            AI 状态机、请求队列、生成配送
  planning.md           规划管线、策略、执行器
  protocols.md          网络协议 + NBT 契约
  minecolonies.md       对模拟殖民地内部 API 的依赖
  gaps.md               已知洞：遗留、未接线、不要误修
```

| 何时读 | 文件 |
|--------|------|
| 加 tick / 驱动 / 副作用顺序 | [architecture.md](tech/architecture.md) |
| 找不到该改哪个类 | [packages.md](tech/packages.md) |
| 改模块容量、持久化、快照 | [townhall-module.md](tech/townhall-module.md) |
| 改生成/配送/卡住判定 | [ai-work.md](tech/ai-work.md) |
| 改规划策略或放置 | [planning.md](tech/planning.md) |
| 加网络消息或改 NBT 键 | [protocols.md](tech/protocols.md) |
| 升 MineColonies 版本 | [minecolonies.md](tech/minecolonies.md) |
| 「这个配置为什么没效果」 | [gaps.md](tech/gaps.md) |

## 不在本目录的东西

- 工程环境、Gradle、包名约束 → 仓库根 [CONTEXT.md](../CONTEXT.md)、[AGENTS.md](../AGENTS.md)
- 玩家安装说明 → [README.md](../README.md)
- 模拟殖民地源码参考 → `minecolonies-release-1.20/`（只读，禁止改）
