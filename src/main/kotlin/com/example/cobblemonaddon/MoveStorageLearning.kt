package com.example.cobblemonaddon

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.util.party
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import java.util.UUID

object MoveStorageLearning {

    /**
     * 让宝可梦学会招式（复用原版 TMMachineTeachMoveHandler 的学习逻辑）：
     *  - 招式必须在该宝可梦的 TM 可学列表里
     *  - 已学会则跳过
     *  - 招式栏有空位就加入，否则进备用招式
     */
    fun learn(player: ServerPlayer, pokemonUuid: UUID, moveName: String): Boolean {
        val pokemon = player.party()[pokemonUuid] ?: return false
        val move = Moves.getByName(moveName) ?: return false

        if (move !in pokemon.form.moves.tmLearnableMoves()) return false
        if (pokemon.moveSet.getMoveTemplates().contains(move)) return false
        if (pokemon.allAccessibleMoves.contains(move)) return false

        if (pokemon.moveSet.hasSpace()) {
            pokemon.moveSet.add(move.create())
        } else {
            pokemon.benchedMoves.add(BenchedMove(move, 0))
        }

        player.playNotifySound(CobblemonSounds.MOVE_LEARN, SoundSource.MASTER, 1.0F, 1.0F)
        CobblemonAddon.LOGGER.info("[{}] {} learned {}", CobblemonAddon.MOD_ID, pokemon.species.name, moveName)
        return true
    }
}
