# 农田

> 状态：**已实现**  
> 代码：`FieldPlanner`、`FieldBlueprintPaths`、`ColonyFieldSupport`

## 何时出现

- 固定式：步骤里有 `FIELD` 需求且当前田数不足。
- 启发式：存在「缺田的农夫」。
- 规划门控：缺田时 **允许在建造者忙碌时仍尝试 PLACE_FIELD**。

## 每名农夫要多少田

```
desired = min(3, 1 + farmerLevel / 2)
```

| 农夫等级 | 目标田数 |
|----------|----------|
| 1 | 1 |
| 2–3 | 2 |
| 4–5 | 3 |

距农夫小屋至少 **8** 格；候选步长 **4**；朝向固定 NORTH。

## 蓝图路径（按序尝试）

1. `infrastructure/fields/basicfield.blueprint`
2. `agriculture/fields/basicfield.blueprint`
3. `decorations/fields/basicfield.blueprint`
4. `decorations/basicfield.blueprint`
5. `fields/basicfield.blueprint`

失败 → `missing_blueprint` / `field_*` 失败注记。

## 放置方式

农田 **不走建造者工单**：`PlanningInstantBuild.pasteBlueprint` 瞬间粘贴，再反射注册 `FarmField` 并 `FarmerFieldsModule.assignExtension`。

返回锚点为评分点的 `below()`（低一格）。默认田足迹半径 **5**（占用图）。

无可用农夫 → `missing_farmer`。
