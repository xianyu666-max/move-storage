# 招式存储器（Move Storage）

Cobblemon 附属模组：把「招式学习器」存入机器，再让宝可梦学习机器里已存储的招式。

- 版本：1.0.0
- 适用：Minecraft 1.21.1 / Fabric / Cobblemon 1.8.1
- 许可证：MPL-2.0（界面代码基于 Cobblemon 的招式学习器机，同样为 MPL-2.0）

## 安装

需要以下前置模组：

| 模组 | 版本 |
|---|---|
| Fabric Loader | ≥ 0.17.2 |
| Fabric API | ≥ 0.116.6+1.21.1 |
| fabric-language-kotlin | ≥ 1.13.6（Cobblemon 内置，通常无需单独安装） |
| Cobblemon | 1.8.1（1.21.1） |

把 jar 放进 `mods` 文件夹即可。

## 合成

3×3 有序合成：

```
D T D      D = 光盘架（cobblemon:disc_shelf）
D P D      T = 招式学习器机（cobblemon:tm_machine）
D D D      P = PC 终端（cobblemon:pc）
```

创造模式可在「功能性方块」标签里找到。

## 使用

### 1. 存入招式

把招式学习器（TM）放进导入槽（界面第一个槽位），或由漏斗 / RS 导入器自动输入：

- 新招式 → 存入机器，TM 被消耗
- 已存过的招式 → 该 TM 被回收，计入该招式的「存量」

```
已存储招式: 水之波动
该招式已存储，招式学习器已回收
```

调试命令：

```
/movestorage store <招式名>
/movestorage list
/movestorage clear
```

### 2. 让宝可梦学习

1. 空手右键打开界面
2. 选属性 → 选招式（已存储的招式置顶、高亮；未存储的灰色）
3. 左侧点选宝可梦（会变暗表示选中）
4. 点右下角「确认」按钮学习

鼠标滑过招式时，左侧宝可梦会实时显示该招式「能学习 / 已学会 / 无法学习」。

### 3. 材料与存量

由 Cobblemon 配置 `infinite_tm_uses` 控制：

| 配置 | 行为 |
|---|---|
| `true`（免费模式） | 材料槽锁定，选中宝可梦后直接确认学习；多余 TM 只作为回收 |
| `false`（消耗模式） | 需要该招式 TM 配方的材料（或该招式的多余 TM 存量），光盘显示属性颜色后才能确认 |

- 学习时优先消耗「该招式的存量」，存量不足再扣材料槽
- 关闭界面时材料槽里的物品会还给玩家

## 特性

- 招式列表显示全部 TM，已存储的排在最前
- 学习界面显示「该招式存量: N」
- 打开界面时方块屏幕亮起（与原版 PC 一致的亮屏）
- 方块碰撞箱与 PC 终端一致
- 破坏方块时，已存储的招式与存量随方块物品保留（创造模式也掉落）
- 与原版招式学习器机完全独立，互不影响

## 命令

```
/movestorage store <招式名>   存入招式（需靠近存储器）
/movestorage list             查看已存储招式
/movestorage clear            清空存储器
```

## 已知限制

- 仅支持 Minecraft 1.21.1 / Cobblemon 1.8.x
- 界面基于 Cobblemon 招式学习器机的源码改造，Cobblemon 大版本更新后可能需要适配
- 与 Inventory Profiles Next 共存时，需要在 `config/inventoryprofilesnext/integrationHints/player-defined.json` 中把 `com.example.cobblemonaddon.gui.MoveStorageScreen` 设为 `ignore`（否则会出现整理按钮）

## 开发

```powershell
.\gradlew.bat build        # 构建 -> build\libs\cobblemonaddon-1.0.0.jar
.\gradlew.bat runClient    # 开发客户端
```

源码结构：

```
src/main/kotlin/com/example/cobblemonaddon/
  CobblemonAddon.kt          入口（注册方块/命令/网络包/创造标签）
  TmStoreBlocks.kt           方块、方块实体、菜单注册
  MoveStorageBlock.kt        方块逻辑（两格、朝向、碰撞箱、NBT 掉落、亮屏）
  MoveStorageBlockEntity.kt  存储数据（招式集合 + 每招式存量 + 容器）
  MoveStorageMenu.kt         菜单（槽位、材料校验/消耗、导入槽）
  MoveStorageLearning.kt     服务端学习逻辑
  LearnMovePacket.kt         学习请求包
  SelectTmPacket.kt          选中招式同步包
  SyncStoredMovesPacket.kt   存储列表同步包
  MoveStorageSorting.kt      已存储置顶排序
  gui/MoveStorageScreen.kt   界面（基于 Cobblemon 招式学习器机改造）
src/main/resources/
  assets/cobblemonaddon/     方块模型、贴图、语言
  data/cobblemonaddon/       合成配方
```

## 许可证

本项目以 **MPL-2.0** 发布。

`gui/MoveStorageScreen.kt` 改编自 Cobblemon 的 `TMMachineScreen.kt`（Copyright (C) 2023 Cobblemon Contributors，MPL-2.0），保留原始版权声明。
