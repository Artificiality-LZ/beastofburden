# 选址、占地、道路

> 状态：**已实现**（道路硬过滤未接线）  
> 代码：`PlacementSearch`、`OccupancyMap`、`StructureOverlapGuard`、`RoadPlanner`、`BuilderAssigner`

## 选址（新小屋）

- 搜索半径：`planningSearchRadius` 默认 **96**
- 锚点网格步长：**3**
- 每类型最多评估 `planningMaxCandidates` 默认 **500**
- 地表：`WORLD_SURFACE_WG`，再在 surfaceY+2 到 surfaceY-6 试 Y
- 必须在殖民地内；`OccupancyMap.isLooseAnchorCandidate`；四种水平朝向都检查不重叠，再挑朝向

### 打分偏好（相对）

- 距殖民地中心
- 距最近已有建筑（约 20 格为佳）
- 地面平整
- `RoadPlanner.roadScoreBonus`：可达 +12/(1+d/8)，不可达 **-4**
- 类别：物流偏中心、防御偏远、食物偏低处、矿工低于中心、工艺靠近邻居

朝向：前方 1–4 格较空、靠近中心、少遮挡。

配置 `planningRequireRoadAccess` 默认 false，**代码未当作硬拒绝**（只影响分数）。

## 占地与间隔

- 足迹来自：已有建筑、管理器里聚合遗漏的建筑、BUILD/UPGRADE/REPAIR 工单、农田足迹
- 足迹 AABB 之间最少空格：`planningMinBlueprintSeparation` 默认 **4**（0 = 允许贴边）
- `StructureOverlapGuard`：足迹重叠，或蓝图体积内扫到其他 hut / 已注册 IBuilding
- 落点前 `prepareAnchorSite`：清锚点、净空、必要时垫地柱；可清树叶原木、可替换方块、草泥土沙砾石等

锚点相对地面：`BlueprintAnchorOffsets.TERRAIN_SINK_BLOCKS = 1`，结合 Structurize `groundlevel` 标签。

## 建造者分配

- 水平距离 ≤ `planningBuilderRadius` 默认 **100**
- 每座建造者小屋队列 ≤ `planningMaxBuilderQueue` 默认 **3**（按 `order.getClaimedBy()` 统计）
- 选最近、等级 ≥ 目标等级、队列未满者
- 自我升级：目标等级 = 当前 + 1 时允许
- 冷启动 / 农田：不要求真实建造者坐标（`BlockPos.ZERO`）

超出范围 → `no_builder_range`；没有合适建造者 → `no_builder`。

## 道路

- 路网节点：殖民地中心 + 所有 level>0 的小屋
- 可达：距路网 `(max(40, searchRadius/2))²` 内，或 BFS ≤ 128 步（搜索上限 900）
- 冷启动：锚点在 `searchRadius²` 内的中心附近视为可达
- **铺路**：非农田、非冷启动的成功放置后，从最近节点到入口 BFS，最多 **48** 块圆石（`paveEntrance`）

## 结构包

`StructurePackResolver`：殖民地包 → 市政厅包 → 任意已建小屋包。缺失 → `missing_pack`。
