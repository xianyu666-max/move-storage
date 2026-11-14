package com.example.cobblemonaddon

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.item.components.TMMoveComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * 招式存储器：
 *  - storedMoves：已存储的招式集合
 *  - spareTmsByMove：每个招式「多存入的招式学习器」数量（消耗模式下可代替该招式的一次材料消耗）
 *  - 一个 6 格容器，接收漏斗 / RS 等自动化输入（槽 1 是导入槽）
 */
class MoveStorageBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(TmStoreBlocks.MOVE_STORAGE_ENTITY, pos, state), Container {

    val storedMoves: MutableSet<String> = linkedSetOf()

    /** 招式 -> 多存入的招式学习器数量 */
    val spareTmsByMove: MutableMap<String, Int> = linkedMapOf()

    val items: NonNullList<ItemStack> = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY)

    /** 导入招式；返回 true 表示新存入，false 表示本来就有 */
    fun storeMove(moveName: String): Boolean {
        val added = storedMoves.add(moveName)
        if (added) setChanged()
        return added
    }

    /** 处理放入导入槽的招式学习器：新招式则存储，已存储则计入该招式存量 */
    fun handleImport(stack: ItemStack): Boolean {
        if (stack.item != CobblemonItems.TECHNICAL_MACHINE) return false
        val move = TMMoveComponent.getTMMove(stack) ?: return false
        val added = storedMoves.add(move.name)
        if (!added) {
            spareTmsByMove[move.name] = spareFor(move.name) + 1
        }
        setChanged()
        return added
    }

    fun spareFor(moveName: String): Int = spareTmsByMove[moveName] ?: 0

    fun consumeSpare(moveName: String): Boolean {
        val count = spareFor(moveName)
        if (count <= 0) return false
        if (count == 1) spareTmsByMove.remove(moveName) else spareTmsByMove[moveName] = count - 1
        setChanged()
        return true
    }

    fun hasMove(moveName: String): Boolean = storedMoves.contains(moveName)

    fun clearMoves() {
        storedMoves.clear()
        spareTmsByMove.clear()
        setChanged()
    }

    // ---------- Container ----------

    override fun getContainerSize(): Int = CONTAINER_SIZE

    override fun isEmpty(): Boolean = items.all { it.isEmpty }

    override fun getItem(slot: Int): ItemStack = items[slot]

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val stack = ContainerHelper.removeItem(items, slot, amount)
        if (!stack.isEmpty) setChanged()
        return stack
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(items, slot)

    override fun setItem(slot: Int, stack: ItemStack) {
        items[slot] = stack
        if (slot == IMPORT_SLOT && !stack.isEmpty && stack.item == CobblemonItems.TECHNICAL_MACHINE) {
            handleImport(stack)
            items[slot] = ItemStack.EMPTY
        }
        setChanged()
    }

    override fun stillValid(player: Player): Boolean {
        return level?.getBlockEntity(blockPos) === this
    }

    override fun clearContent() {
        for (i in items.indices) {
            items[i] = ItemStack.EMPTY
        }
        setChanged()
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        return slot == IMPORT_SLOT && stack.item == CobblemonItems.TECHNICAL_MACHINE
    }

    // ---------- NBT ----------

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        storedMoves.clear()
        val list = tag.getList(TAG_MOVES, ListTag.TAG_STRING.toInt())
        for (i in 0 until list.size) {
            storedMoves.add(list.getString(i))
        }
        spareTmsByMove.clear()
        val spare = tag.getCompound(TAG_SPARE)
        for (key in spare.allKeys) {
            spareTmsByMove[key] = spare.getInt(key)
        }
        ContainerHelper.loadAllItems(tag, items, registries)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        val list = ListTag()
        storedMoves.forEach { list.add(StringTag.valueOf(it)) }
        tag.put(TAG_MOVES, list)
        val spare = CompoundTag()
        spareTmsByMove.forEach { (move, count) -> spare.putInt(move, count) }
        tag.put(TAG_SPARE, spare)
        ContainerHelper.saveAllItems(tag, items, registries)
    }

    companion object {
        const val CONTAINER_SIZE = 6
        const val IMPORT_SLOT = 1
        private const val TAG_MOVES = "storedMoves"
        private const val TAG_SPARE = "spareTms"
    }
}
