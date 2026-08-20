# 物资补给：卡住请求 → 生成 → 配送

> 状态：**已实现**  
> 代码：`UnfulfillableRequestDetector`、`ItemGenerationTask`、`ColonyLogistics`、`BeastofBurdenRequestQueue`  
> 技术对照：[../../tech/ai-work.md](../../tech/ai-work.md)  
> 数值：[../02-economy/numbers.md](../02-economy/numbers.md)

## 设计意图

牛马不是第二套配送员。只处理 **正常物流短期内搞不定** 的可配送请求，避免早期（没仓库/没配送员）或配方链断裂时殖民地停转。

## 何谓「卡住」

必须同时满足：

1. 请求仍处于活跃可配送状态（未完成 / 未取消 / 未失败等）。
2. 属于下列之一：
   - 已指派给玩家解析器或重试解析器（或其子请求）
   - 被 `IPlayerRequestResolver` / `IRetryingRequestResolver` 解析
   - **早期物流**（无仓库或无在职工配送员）下，建筑上未关闭的请求
3. `ColonySupplyChecker.canColonySupply` 为 false（请求者建筑库存和仓库都拿不出来）。

## 工作流（玩家视角）

1. 市政厅页「当前工作」变为「生成」，带物品图标与进度。
2. 生成结束写入日志「生成了」。
3. 牛马走向请求目标，「配送中」。
4. 送达写入「送达了」；若中途请求已不再需要则「取消了」。

生成期间市民显示工作状态，可被打断为 false（进行中不换活）。

## 生成规则

- 物品从请求抽出（`RequestItemUtils.extractItemStack`），按 **整叠价值** 计时。
- 时间公式见数值文档。有下限 `minGenerationTicks`（默认 40）。
- 每 20 tick 在头顶刷附魔粒子。
- 产出进市民背包；装不下则掉落。

## 配送规则

- 走到 `getDeliveryPosition`（市民位置或建筑位置）。
- 履约优先级：市民打开的请求 → 建筑打开的请求 → `overruleRequest` 兜底。
- 配送前再检查是否仍卡住；已能被正常供应则取消本次任务。

## 扫描节奏

全殖民地请求扫描约每 **40** 服务端 tick 一次（`ColonyRequestEventHandler`）。AI 取活前也可 `scanColonyIfDue`。

队列优先级：配送员请求用其 `getPriority()`；普通 `IDeliverable` 为 0；其他为 `Integer.MIN_VALUE`。取出时再次验证仍卡住。

## 非目标

- 不主动生产「殖民地还没请求的」物资。
- 不维护仓库库存水位。
- 不绕过 MineColonies 请求系统直接塞箱子（必须履约请求）。
