# 数值

> 状态：**已实现**（标明未接线项）  
> 代码：`Config.java`、`ItemGenerationTask.calculateDuration`、`ItemValueRegistry`、`TownHallBeastofburdenModule.getModuleMax`

改这些数字时同步 `Config` 默认值、本页、以及配置屏文案。

## 生成时间

```
capability = 1.0 + strengthLevel * strengthSpeedBonus
rawTicks   = baseGenerationTicks + round(stackValue * ticksPerItemValue)
duration   = max(minGenerationTicks, ceil(rawTicks / capability))
```

`stackValue = perItemValue * count`。

| 配置键 | 默认 | 范围 |
|--------|------|------|
| `baseGenerationTicks` | 60 | 1–20000 |
| `minGenerationTicks` | 40 | 1–20000 |
| `ticksPerItemValue` | 3.0 | 0–10000 |
| `strengthSpeedBonus` | 0.05 | 0–10 |

例：钻石×1、力量 0 → value 100 → raw = 60+300 = 360 → 18 秒（20 tps）。力量 10 → capability 1.5 → 240 tick。

## 物品价值解析顺序

1. 配置热加载后的 `RESOLVED` 缓存
2. `itemValues` 显式表（`namespace:path=value`）
3. 代码内 `BAKED_DEFAULTS`（与 Config 默认列表一致的一组原版物品）
4. 若 `deriveItemValuesFromRecipes`：按合成/熔炼/爆破/烟熏/切石/锻造反推，最多 256 轮，取最便宜路径 `ceil(原料总价 / 产出数)`
5. `defaultItemValue` 默认 **5**

服务端启动 `ItemValueBootstrap` 全量 reload。配置变更走 `ItemValueRegistry.onConfigReloaded`。

### 默认显式表（Config.defaultItemValueEntries）

| 物品 | 价值 | 物品 | 价值 |
|------|------|------|------|
| dirt / grass_block | 1 | oak_log | 8 |
| sand | 1 | oak_planks | 2 |
| gravel / cobblestone / stone | 2 | stick | 1 |
| deepslate | 3 | coal / charcoal | 5 |
| iron_ingot | 20 | gold_ingot | 50 |
| copper_ingot | 8 | redstone | 3 |
| lapis_lazuli | 6 | quartz | 8 |
| diamond | 100 | emerald | 120 |
| obsidian | 15 | flint | 3 |
| clay_ball | 2 | brick | 4 |
| wheat | 2 | bread | 6 |
| leather | 8 | string / feather | 2 |
| bone | 3 | gunpowder | 10 |
| blaze_rod | 25 | ender_pearl | 40 |
| nether_star | 500 | ancient_debris | 150 |
| netherite_scrap | 200 | netherite_ingot | 800 |

未列出的物品走配方推导或默认 5。配置屏可增删改；多人游戏保存需权限 2。

## 职业容量

`max(1, min(3, (townHallLevel + 1) / 2))` → 1 / 2 / 3 人。

## 工作日志

| 键 | 默认 | 含义 |
|----|------|------|
| `workLogMaxEntries` | 500 | 每市政厅最多存条数 |
| `workLogHistoryDays` | 100 | UI 显示最近 N 天；**0 = 全部** |

## 规划空间 / 建造者

| 键 | 默认 | 含义 |
|----|------|------|
| `planningSearchRadius` | 96 | 水平搜点半径 |
| `planningMaxCandidates` | 500 | 每建筑类型最多候选 |
| `planningBuilderRadius` | 100 | 建造者接单最大距离 |
| `planningMaxBuilderQueue` | 3 | 每建造者小屋队列 |
| `planningMinBlueprintSeparation` | 4 | 足迹最小间隙 |

## 规划冷却（模块 tick）

| 键 | 默认 | 接线 |
|----|------|------|
| `planningRetryCooldown` | 1 | 已接线；成功时还与 2 取 max |
| `planningColdStartCooldown` | 1 | 已接线；成功时与 2 取 max |
| `planningPlacementFailureCooldown` | 2 | **未接线** |

## 农田目标田数

`min(3, 1 + farmerLevel/2)`，与 Config 无关，写在 `FieldPlanner`。

## 未接线数值（不要当成现行规则）

- `planningPhaseEvaluationInterval` 默认 24000 — 阶段评估已删除
- `planningResearchTickInterval` 默认 600 — 研究循环未挂
- `planningRequireRoadAccess` 默认 false — 未做硬过滤
