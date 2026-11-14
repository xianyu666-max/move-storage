# Changelog

## 1.0.0 — First release

### Move Storage

- New block "Move Storage": two-block PC-style machine, right-click to open
- Insert a TM to store its move; stored TMs are counted as stock and consumed
- Move list shows all TMs: stored ones on top and clickable, unstored ones greyed out
- Select a Pokémon and press Confirm to learn; hovering a move updates learnable / already known / not learnable states live
- Cost mode (`infinite_tm_uses=false`): requires recipe materials or spare stock, confirm greys out with a "what's missing" tooltip, materials returned on close
- Free mode (`infinite_tm_uses=true`): material slots locked, learn directly
- Breaking the block drops stored moves with it, creative mode included
- Commands: `/movestorage store|list|clear`
- Hopper / item pipe input supported

---

## 1.0.0 — 首次发布

### 招式存储器

- 新增方块「招式存储器」：PC 造型两格高，右键打开招式学习界面
- 放入招式学习器即可存入招式；已存过的 TM 自动回收并计入存量
- 招式列表显示全部 TM：已存储的置顶高亮，未存储的置灰不可点
- 选中宝可梦后点「确认」学习；鼠标滑过招式时实时显示能学习 / 已学会 / 无法学习
- 消耗模式（`infinite_tm_uses=false`）：需要 TM 配方材料或该招式存量，缺少材料时确认键置灰并提示缺什么；关闭界面返还材料
- 免费模式（`infinite_tm_uses=true`）：材料槽锁定，直接确认学习
- 破坏方块时已存储的招式随方块掉落（创造模式也掉落）
- 命令：`/movestorage store|list|clear`
- 兼容漏斗 / 物品管道自动输入
