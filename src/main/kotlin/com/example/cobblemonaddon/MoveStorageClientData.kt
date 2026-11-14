package com.example.cobblemonaddon

/**
 * 客户端缓存的「存储器已存储招式」，由 SyncStoredMovesPacket 更新。
 */
object MoveStorageClientData {

    private val stored: MutableSet<String> = mutableSetOf()

    fun update(moves: List<String>) {
        stored.clear()
        stored.addAll(moves)
    }

    fun isStored(moveName: String): Boolean = stored.contains(moveName)

    fun isEmpty(): Boolean = stored.isEmpty()

    fun count(): Int = stored.size
}
