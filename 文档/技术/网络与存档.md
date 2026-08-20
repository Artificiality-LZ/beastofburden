# 网络与 NBT 契约

改字段必须：双端可读、旧档可降级、必要时升 `ModNetwork` 协议号。

## 网络频道

- 频道：`beastofburden:main`
- 协议字符串：**`"2"`**（精确相等，不匹配则拒绝连接）
- 注册顺序即 packet id 0–3，**不要在中间插入**，只能追加并升协议

| ID | 类 | 方向 | 权限 |
|----|----|------|------|
| 0 | `SaveBeastConfigMessage` | C→S | 单人放行；多人 `hasPermissions(2)` |
| 1 | `ToggleAutonomousPlanningMessage` | C→S | 殖民地 `Action.MANAGE_HUTS` |
| 2 | `CyclePlanningModeMessage` | C→S | 同上 |
| 3 | `SaveColonyPlanMessage` | C→S | 同上；payload = BlockPos + 脚本 CompoundTag |

S2C 工作数据走建筑模块 `serializeToView`，无独立包。

### SaveBeastConfig 字段顺序

`baseTicks, minTicks, ticksPerValue(double), strengthBonus(double), defaultValue, deriveRecipes, logMax, logDays, itemCount + (itemId, value)*`

## 模块 NBT（市政厅建筑）

模块键：`beastofburden:townhall_beastofburden`

| 键 | 类型 | 内容 |
|----|------|------|
| MineColonies `TAG_HIRING_MODE` | int | 雇佣模式 ordinal |
| MineColonies `TAG_WORKING_RESIDENTS` | int[] | 市民 id |
| `workLog` | ListTag | `BeastWorkLogEntry` |
| `autonomousPlanning` | bool | |
| `colonyPlanner` | Compound | 见下 |

### colonyPlanner

| 键 | 说明 |
|----|------|
| `phase` | 遗留 ordinal，写 0 |
| `emergencyDays` `recoveryDays` `phaseCooldown` `researchCooldown` | 遗留 stub 0 |
| `retryCooldown` | 现用；读档可回退 `tacticalCooldown` |
| `planningMode` | ordinal |
| `scripted` | FixedPlanScript + `stepIndex` |
| `debug` | PlanningReport |
| `blocklist` | 空 compound（世界可用时） |

### PlanningReport

`decision`、`detail`、`intent`、`action`、`reason`、`location`、`builder`、`note`

### BeastWorkLogEntry

`day`、`citizen`、`name`、`action`、`item`、`count`、`duration`、可选 `detail`

## BeastWorkSnapshot 字节序

1. colonyDay、historyDays  
2. activeWork 数量 + 各 `BeastWorkStatus`  
3. history 数量 + 各 log  
4. autonomousPlanningEnabled  
5. planningMode (byte)  
6. scriptedStepIndex、scriptedStepCount (varint)  
7. planningPhase (byte)  
8. planningLastDecision、planningDetail (utf)  
9. planningRetryCooldown (varint，可读才写——注意版本兼容)  
10. `PlanScriptIO.write(planScript)`  

`BeastWorkStatus.write`：varint citizenId、utf name、byte phase、ResourceLocation itemId、varint count/progress/required、utf detail。
