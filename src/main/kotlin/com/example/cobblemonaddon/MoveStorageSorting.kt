package com.example.cobblemonaddon

import com.cobblemon.mod.common.api.tms.TechnicalMachine

/**
 * 招式列表排序：已存储的排前面。
 * 放在普通包里，避免 Kotlin 内联函数在 mixin 包里生成匿名类导致加载失败。
 */
object MoveStorageSorting {

    fun sortByStored(list: List<TechnicalMachine>): MutableList<TechnicalMachine> {
        val stored = ArrayList<TechnicalMachine>(list.size)
        val rest = ArrayList<TechnicalMachine>(list.size)
        for (tm in list) {
            if (MoveStorageClientData.isStored(tm.moveName.name)) stored.add(tm) else rest.add(tm)
        }
        stored.addAll(rest)
        return stored
    }
}
