package com.example.cobblemonaddon

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.tms.TechnicalMachine
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import com.cobblemon.mod.common.block.entity.TMMachineBlockEntity
import com.cobblemon.mod.common.item.components.TMMoveComponent
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerListener
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * 招式存储器的菜单（复制自原版 TMMachineMenu，独立一份）。
 *
 * 槽位布局和原版一致：
 *  0 结果槽（存储器里改成“确认”位）
 *  1 空白 TM 槽（存储器里作为招式学习器导入槽）
 *  2..4 材料槽
 */
class MoveStorageMenu(menuType: MenuType<*>, syncId: Int) : AbstractContainerMenu(menuType, syncId), ContainerListener {

    companion object {
        const val RESULT_SLOT = 0
        const val BLANK_TM_SLOT = 1
        val INGREDIENT_SLOTS = 2..4
    }

    var playerInventory: Inventory? = null
    var tmMachineEntity: TMMachineBlockEntity? = null
    var moveStorageEntity: MoveStorageBlockEntity? = null
    var blockPos: net.minecraft.core.BlockPos? = null
    var inventory: Container? = null
    var containerData: ContainerData? = null

    var currentlySelectedTM: TechnicalMachine? = null

    var onSelectedTMUpdate: (TechnicalMachine?) -> Unit = {}

    /** 同步数据：0 = 多余招式学习器库存 */
    val syncData = SimpleContainerData(2)

    constructor(syncId: Int, playerInventory: Inventory) : this(TmStoreBlocks.MOVE_STORAGE_MENU, syncId) {
        this.playerInventory = playerInventory
        this.inventory = SimpleContainer(6)
        this.containerData = SimpleContainerData(6)
        this.addDataSlots(containerData!!)
        this.addDataSlots(syncData)
        inventory?.startOpen(playerInventory.player)
        initiateSlots()
    }

    constructor(syncId: Int, playerInventory: Inventory, inventory: Container, containerData: ContainerData) : this(TmStoreBlocks.MOVE_STORAGE_MENU, syncId) {
        this.playerInventory = playerInventory
        this.inventory = inventory
        this.containerData = containerData
        this.addDataSlots(containerData)
        this.addDataSlots(syncData)
        inventory.startOpen(playerInventory.player)
        initiateSlots()
        this.addSlotListener(this)
    }

    override fun broadcastChanges() {
        val move = currentlySelectedTM?.moveName?.name
        syncData.set(0, if (move != null) (moveStorageEntity?.spareFor(move) ?: 0) else 0)
        super.broadcastChanges()
    }

    private fun initiateSlots() {
        val startX = 15
        val startY = 144
        val slotWidth = 18
        val slotHeight = 18

        for (row in 0..2) {
            for (col in 0..8) {
                this.addSlot(Slot(playerInventory, 9 + (row * 9) + col, startX + (slotWidth * col), startY + (slotHeight * row)))
            }
        }
        for (col in 0..8) {
            this.addSlot(Slot(playerInventory, col, startX + (slotWidth * col), startY + 58))
        }

        val container = inventory!!
        this.addSlot(TMResultSlot(container, RESULT_SLOT, startX + 37, startY - 28))
        this.addSlot(BlankTMSlot(container, BLANK_TM_SLOT, startX + 90, startY - 28))
        for (index in INGREDIENT_SLOTS) {
            this.addSlot(MaterialSlot(this, container, index, startX + 114 + (18 * (index - INGREDIENT_SLOTS.first)), startY - 28))
        }
    }

    /** 材料槽：只接受当前选中招式配方里对应的材料 */
    class MaterialSlot(
        private val menu: MoveStorageMenu,
        container: Container,
        private val containerIndex: Int,
        x: Int,
        y: Int
    ) : Slot(container, containerIndex, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean {
            if (!menu.needsMaterials()) return false
            val recipe = menu.recipeForCurrent()
            if (recipe.isEmpty()) return false
            val i = containerIndex - INGREDIENT_SLOTS.first
            if (i < 0 || i >= recipe.size) return false
            return recipe[i].ingredient.test(stack)
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var itemStack = ItemStack.EMPTY
        val slot = this.slots[index]
        val maxPlayerInventoryIndex = Inventory.INVENTORY_SIZE - 1

        if (slot.hasItem()) {
            val movedStack = slot.item
            itemStack = movedStack.copy()

            if (index in 36..40) {
                if (!this.moveItemStackTo(movedStack, 0, maxPlayerInventoryIndex, false)) return ItemStack.EMPTY
            } else if (index <= maxPlayerInventoryIndex) {
                if (!this.moveItemStackTo(movedStack, 37, 40 + 1, false)) return ItemStack.EMPTY
            }

            if (movedStack.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }
        return itemStack
    }

    override fun stillValid(player: Player): Boolean {
        return player.let { inventory?.stillValid(it) } ?: false
    }

    fun getBurnProgressValue(): Int = this.containerData?.get(TMMachineBlockEntity.BURN_PROGRESS_INDEX) ?: 0

    fun getBurnProgressRatio(): Float {
        val amount = getBurnProgressValue()
        val max = TMMachineBlockEntity.BURN_TOTAL_TIME
        return if (amount != 0) {
            Mth.clamp((amount.toFloat() / max.toFloat()), 0.0F, 1.0F)
        } else {
            0.0F
        }
    }

    fun getPostCraftTicks(): Int {
        return if (getBurnProgressValue() >= TMMachineBlockEntity.BURN_TOTAL_TIME) {
            getBurnProgressValue() - TMMachineBlockEntity.BURN_TOTAL_TIME
        } else 0
    }

    class TMResultSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false

        // 输出槽由“确认学习”按钮取代，隐藏槽位本身（不渲染、不响应悬停）
        override fun isActive(): Boolean = false
    }

    class BlankTMSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean =
            super.mayPlace(stack) && (stack.item == CobblemonItems.BLANK_TM || stack.item == CobblemonItems.TECHNICAL_MACHINE)
    }

    override fun slotChanged(containerToSend: AbstractContainerMenu, dataSlotIndex: Int, stack: ItemStack) {
        val level = playerInventory?.player?.level()

        // 导入槽放入招式学习器：存进存储器并消耗
        // slotChanged 传的是菜单槽位索引：玩家背包占 0..35，容器槽从 36 开始
        if (dataSlotIndex == Inventory.INVENTORY_SIZE + BLANK_TM_SLOT) {
            val storage0 = moveStorageEntity
            val player0 = playerInventory?.player
            if (storage0 != null && player0 is net.minecraft.server.level.ServerPlayer && !player0.level().isClientSide) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    player0,
                    SyncStoredMovesPacket(storage0.storedMoves.toList())
                )
            }
            CobblemonAddon.LOGGER.info(
                "[{}] import slot changed: client={} item={} storage={}",
                CobblemonAddon.MOD_ID, level?.isClientSide, stack.item, moveStorageEntity != null
            )
            if (level?.isClientSide == false) {
                val storage = moveStorageEntity
                if (storage != null && stack.item == CobblemonItems.TECHNICAL_MACHINE) {
                    val move = TMMoveComponent.getTMMove(stack)
                    CobblemonAddon.LOGGER.info("[{}] import tm move={}", CobblemonAddon.MOD_ID, move?.name)
                    if (move != null) {
                        storage.storeMove(move.name)
                        stack.shrink(stack.count)
                        inventory?.setItem(BLANK_TM_SLOT, ItemStack.EMPTY)
                        val player = playerInventory?.player
                        player?.displayClientMessage(
                            Component.literal("已存储招式: ${move.displayName.string}"),
                            true
                        )
                        if (player is net.minecraft.server.level.ServerPlayer) {
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                player,
                                SyncStoredMovesPacket(storage.storedMoves.toList())
                            )
                        }
                        containerToSend.broadcastChanges()
                    }
                }
            }
        }

        tmMachineEntity?.setChanged()
        inventory?.setChanged()
        playerInventory?.setChanged()
        broadcastChanges()
    }

    override fun dataChanged(containerMenu: AbstractContainerMenu, dataSlotIndex: Int, value: Int) {
        broadcastChanges()
    }

    fun clientSetSelectedTM(tm: TechnicalMachine?) {
        currentlySelectedTM = tm
        onSelectedTMUpdate(tm)
    }

    /** 是否需要消耗材料（由服务端配置 infinite_tm_uses 决定） */
    fun needsMaterials(): Boolean = !com.cobblemon.mod.common.Cobblemon.config.infiniteTmUses

    /** 当前选中招式的 TM 配方（最多 3 种） */
    fun recipeForCurrent(): List<com.cobblemon.mod.common.api.tms.TechnicalMachineRecipe> {
        val tm = currentlySelectedTM ?: return emptyList()
        return TechnicalMachines.moveToTM[tm.moveName]?.getClampedRecipe() ?: emptyList()
    }

    /** 当前缺少的材料（用于按钮悬停提示），null 表示不缺 */
    fun missingMaterialsText(): String? {
        if (!needsMaterials()) return null
        val recipe = recipeForCurrent()
        if (recipe.isEmpty()) return null
        val container = inventory ?: return null
        val missing = mutableListOf<String>()
        for ((index, ingredient) in recipe.withIndex()) {
            val slot = INGREDIENT_SLOTS.first + index
            val provided = container.getItem(slot)
            if (!ingredient.ingredient.test(provided)) {
                val name = ingredient.ingredient.getItems().firstOrNull()?.hoverName?.string ?: "材料"
                missing.add("$name ×${ingredient.count}")
            } else if (provided.count < ingredient.count) {
                val name = ingredient.ingredient.getItems().firstOrNull()?.hoverName?.string ?: "材料"
                missing.add("$name ×${ingredient.count - provided.count}")
            }
        }
        return if (missing.isEmpty()) null else "缺少: " + missing.joinToString(", ")
    }

    /** 当前招式多存入的招式学习器数量（消耗模式下可替代该招式的一次材料） */
    fun spareForCurrent(): Int {
        val move = currentlySelectedTM?.moveName?.name ?: return syncData.get(0)
        return moveStorageEntity?.spareFor(move) ?: syncData.get(0)
    }

    /** 材料槽是否满足当前招式的配方 */
    fun materialsSatisfied(): Boolean {
        if (!needsMaterials()) return true
        if (spareForCurrent() > 0) return true
        val recipe = recipeForCurrent()
        if (recipe.isEmpty()) return true
        val container = inventory ?: return false
        for ((index, ingredient) in recipe.withIndex()) {
            val slot = INGREDIENT_SLOTS.first + index
            val provided = container.getItem(slot)
            if (!ingredient.ingredient.test(provided) || provided.count < ingredient.count) {
                return false
            }
        }
        return true
    }

    /** 扣除材料：优先消耗多余招式学习器库存，否则扣材料槽 */
    fun consumeMaterials(): Boolean {
        if (!needsMaterials()) return true
        val storage = moveStorageEntity
        val moveName = currentlySelectedTM?.moveName?.name
        if (storage != null && moveName != null && storage.consumeSpare(moveName)) {
            return true
        }
        if (!materialsSatisfied()) return false
        val recipe = recipeForCurrent()
        val container = inventory ?: return false
        for ((index, ingredient) in recipe.withIndex()) {
            val slot = INGREDIENT_SLOTS.first + index
            container.getItem(slot).shrink(ingredient.count)
        }
        container.setChanged()
        broadcastChanges()
        return true
    }

    override fun removed(player: Player) {
        // 关闭界面：熄灭屏幕
        val base = blockPos
        if (base != null && !player.level().isClientSide) {
            val topPos = base.above()
            val topState = player.level().getBlockState(topPos)
            if (topState.block is MoveStorageBlock) {
                player.level().setBlock(topPos, topState.setValue(MoveStorageBlock.ON, false), 3)
            }
        }
        // 关闭界面时把材料槽里的物品还给玩家，避免吞物品
        val container = inventory
        if (container != null && player.level().isClientSide.not()) {
            for (index in INGREDIENT_SLOTS) {
                val stack = container.getItem(index)
                if (!stack.isEmpty) {
                    if (!player.inventory.add(stack)) {
                        player.drop(stack, false)
                    }
                    container.setItem(index, ItemStack.EMPTY)
                }
            }
        }
        super.removed(player)
        tmMachineEntity?.setChanged()
    }
}
