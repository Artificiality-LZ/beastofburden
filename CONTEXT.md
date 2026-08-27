# BeastOfBurden — AI 会话上下文

每次新对话请附加本文件。项目架构细节见 [AGENTS.md](AGENTS.md)。

## 环境


| 项                 | 值                                       |
| ----------------- | --------------------------------------- |
| OS / Shell        | Windows 10，PowerShell                   |
| JDK               | Java 17（PATH 中有 `java`；`JAVA_HOME` 可为空） |
| Minecraft / Forge | 1.20.1 / 47.4.20                        |
| MineColonies      | 1.1.873（及以上）                            |
| Gradle            | 8.8 wrapper                             |
| 工作区               | `D:\Minecraft Mod\BeastOfBurden`        |


本地路径（本机已就绪，不要重装、不要改这些目录里的内容）：

- 模拟殖民地源码（只读参考，禁止改）：
  - **对照本模组依赖 1.1.873**：`D:\Minecraft Mod\BeastOfBurden\minecolonies-1.20.1-1.1.873`（标签 `v1.20.1-1.1.873`，包名已是 `com.minecolonies.core`）
  - **较新对照 1.1.1214**（文档里常写的 1214；1.20.1 无 1314 标签）：`D:\Minecraft Mod\BeastOfBurden\minecolonies-1.20.1-1.1.1214`
  - 源码地图 / 挂钩：`文档/技术/模拟殖民地挂钩.md`
  - 旧无版本复制 `minecolonies-release-1.20/` 可删；请改用上面带版本号的目录
- Gradle 分发包：`D:\Minecraft Mod\BeastOfBurden\gradle-8.8-bin.zip`
- 持久 Gradle 缓存：`C:\Users\22762\.gradle`

`gradlew.bat` / `gradlew` 会把 Cursor 沙箱注入的临时 `GRADLE_USER_HOME`（`cursor-sandbox-cache`）纠正回上述持久缓存。直接调用 wrapper 即可。

## 文档

- 环境 / 工程约束：本文件 + [AGENTS.md](AGENTS.md)
- 策划 + 技术（给 AI，改功能先读）：[文档/索引.md](文档/索引.md)

```
文档/
  索引.md                   阅读顺序与何时打开哪一页
  策划/
    00-总览/                定位、术语、核心循环
    01-系统/                职业、补给、规划、选址、农田
    02-数值/                数值
    03-界面/                界面
    04-内容/                建筑目录、默认 12 步计划
    05-配置项.md            配置项与接线状态
  技术/
    架构.md                 tick 与管线
    包与类地图.md           包 / 类地图
    市政厅模块.md           市政厅模块
    补给工作循环.md         AI 与补给
    规划管线.md             规划管线
    网络与存档.md           网络 + NBT
    模拟殖民地挂钩.md       MC 源码地图 + 内部 API 挂钩
    已知洞.md               1.0 已处理 / 仍接受的限制
    1.0验收.md              手工验收清单
```



## 怎么调试

在仓库根目录执行：

```powershell
.\gradlew.bat compileJava    # 快速编译
.\gradlew.bat build          # 完整构建，产物 build/libs/beastofburden-1.1.jar
.\gradlew.bat runClient      # 开发客户端，工作目录 run/
.\gradlew.bat runServer      # 开发服务端
```

- 游戏日志：`run/logs/latest.log`
- 禁止从网络下载 Gradle；wrapper 已指向本地 `gradle-8.8-bin.zip`
- 不要无故 `clean` 或 `--refresh-dependencies`（会丢掉本机缓存、极慢）
- 不要修改本地 MineColonies 源码树（`minecolonies-1.20.1-1.1.873/`、`minecolonies-1.20.1-1.1.1214/`；旧 `minecolonies-release-1.20/` 可删）



## Git

任务完成且编译通过后 **直接 commit，不必再问**。不要 push，除非用户明确要求。提交日志使用中文。

## 硬性约束

- 包名保持 `org.Artificial.beastofburden`（`Artificial` 的 A 大写）
- 新增翻译键同步写入 `en_us.json` 与 `zh_cn.json`
- 新增网络消息时在 `ModNetwork.register()` 注册

