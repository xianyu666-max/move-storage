package com.example.cobblemonaddon

import com.cobblemon.mod.common.api.moves.Moves
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult

/**
 * 调试命令：
 *   /movestorage store <招式名>  —— 把招式存入附近的招式存储器
 *   /movestorage list            —— 列出已存储招式
 *   /movestorage clear           —— 清空存储器
 */
object MoveStorageCommand {

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("movestorage")
                    .then(
                        Commands.literal("store")
                            .then(
                                Commands.argument("move", StringArgumentType.word())
                                    .executes { ctx ->
                                        val player = ctx.source.playerOrException
                                        val moveName = StringArgumentType.getString(ctx, "move")
                                        val move = Moves.getByName(moveName)
                                        if (move == null) {
                                            ctx.source.sendFailure(Component.literal("未知招式: $moveName"))
                                            return@executes 0
                                        }
                                        val storage = findStorage(player)
                                        if (storage == null) {
                                            ctx.source.sendFailure(Component.literal("附近没有招式存储器（看向它或站到它旁边）"))
                                            return@executes 0
                                        }
                                        val added = storage.storeMove(move.name)
                                        val text = if (added) {
                                            "已存储招式: ${move.displayName.string}"
                                        } else {
                                            "该招式已经存储过了: ${move.displayName.string}"
                                        }
                                        ctx.source.sendSuccess({ Component.literal(text) }, false)
                                        1
                                    }
                            )
                    )
                    .then(
                        Commands.literal("list").executes { ctx ->
                            val player = ctx.source.playerOrException
                            val storage = findStorage(player)
                            if (storage == null) {
                                ctx.source.sendFailure(Component.literal("附近没有招式存储器"))
                                return@executes 0
                            }
                            if (storage.storedMoves.isEmpty()) {
                                ctx.source.sendSuccess({ Component.literal("存储器是空的") }, false)
                                return@executes 1
                            }
                            val names = storage.storedMoves
                                .map { Moves.getByName(it)?.displayName?.string ?: it }
                                .joinToString(", ")
                            ctx.source.sendSuccess({ Component.literal("已存储 (${storage.storedMoves.size}): $names") }, false)
                            1
                        }
                    )
                    .then(
                        Commands.literal("clear").executes { ctx ->
                            val player = ctx.source.playerOrException
                            val storage = findStorage(player)
                            if (storage == null) {
                                ctx.source.sendFailure(Component.literal("附近没有招式存储器"))
                                return@executes 0
                            }
                            storage.clearMoves()
                            ctx.source.sendSuccess({ Component.literal("已清空招式存储器") }, false)
                            1
                        }
                    )
            )
        }
    }

    /** 先看玩家视线目标方块，再在周围 3 格范围内找 */
    private fun findStorage(player: Player): MoveStorageBlockEntity? {
        val hit = player.pick(6.0, 0.0f, false)
        if (hit is BlockHitResult) {
            val be = player.level().getBlockEntity(hit.blockPos)
            if (be is MoveStorageBlockEntity) {
                return be
            }
        }
        val origin = player.blockPosition()
        for (dx in -3..3) {
            for (dy in -2..2) {
                for (dz in -3..3) {
                    val be = player.level().getBlockEntity(origin.offset(dx, dy, dz))
                    if (be is MoveStorageBlockEntity) {
                        return be
                    }
                }
            }
        }
        return null
    }

    @Suppress("unused")
    private val airStack: ItemStack = ItemStack.EMPTY
}
