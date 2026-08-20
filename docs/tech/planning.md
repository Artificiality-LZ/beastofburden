# 规划管线

> 策划：[../design/01-systems/planning.md](../design/01-systems/planning.md) 及同目录分册

## 对象图

- `ColonyPlanner`：持有两种 `PlanningStrategy`、`PlanningService`、`PlanningReport`、mode、retryCooldown、scripted `stepIndex`
- `PlanningService.tick`：单次收集上下文 → 选任务 → resolveTarget → 分配建造者 → `ColonyBuildingExecutor`
- `PlanningContext`：惰性收集建筑、足迹、占用列、路网、`ColonySnapshot`
- `ColonySnapshotCollector`：人口、饱和、守卫、仓库/配送员/工艺链等标志、每类型已建/在建数、结构包
- `BuildTask`：type、action、targetLevel、priority、optional existingBuilding、reason
- `PlanningWorkload`：牛马是否空闲、在建工单 vs 可运营建造者

`ColonyPlanner.getCurrentPhase()` **恒返回** `P0_FOUNDATION`。NBT 仍读写 `phase` 等遗留字段（值为 0）。

## 执行器 `ColonyBuildingExecutor`（新建）

1. 重叠检查、加载蓝图、`prepareAnchorSite`
2. 放置 hut 方块（`AbstractBlockHut.FACING`）
3. `onBlockPlacedByBuildTool`，设 pack/path，level 0
4. 非瞬间：`ConstructionTapeSupport.place` + `building.requestUpgrade(null, builderPos)`
5. 瞬间：`PlanningInstantBuild.completeBuilding` → `onUpgradeComplete`，无工单

升级：在已有建筑上 `requestUpgrade` 或瞬间 complete。

失败 note 字符串（与翻译 `planning.*` 对应）：`missing_building`、`overlap`、`blocked_anchor`、`occupied`、`work_order_failed`、`missing_blueprint`、`field_*` 等。

## 瞬间建造

`PlanningInstantBuildState` volatile，不写盘。Structurize `CreativeBuildingStructureHandler`，5 阶段粘贴，最多 2_000_000 steps。命令 `/beastofburden planningInstantBuild`。

## 脚本 NBT

`FixedPlanScript`：`version`、`custom`、`steps[]`。  
`FixedPlanStep`：`requirements[]`。  
`FixedPlanRequirement`：`kind`、`type`、`level`、`count`。

`ScriptedPlanningStrategy` 另存 `stepIndex`。网络用 `PlanScriptIO` 整包 CompoundTag。

## 成功冷却

`PlanningService.MIN_SUCCESS_COOLDOWN_PASSES = 2`，再与 Config 冷却取 max。
