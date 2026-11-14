package com.example.cobblemonaddon

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import java.util.UUID

data class LearnMovePacket(val pokemonUuid: UUID, val moveName: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<LearnMovePacket> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<LearnMovePacket>(
            ResourceLocation.fromNamespaceAndPath(CobblemonAddon.MOD_ID, "learn_move")
        )

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, LearnMovePacket> = StreamCodec.of(
            { buf: RegistryFriendlyByteBuf, packet: LearnMovePacket ->
                buf.writeUUID(packet.pokemonUuid)
                buf.writeUtf(packet.moveName)
            },
            { buf: RegistryFriendlyByteBuf ->
                LearnMovePacket(buf.readUUID(), buf.readUtf())
            }
        )
    }
}

object MoveStorageNetworking {

    fun register() {
        PayloadTypeRegistry.playC2S().register(LearnMovePacket.TYPE, LearnMovePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(SyncStoredMovesPacket.TYPE, SyncStoredMovesPacket.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(LearnMovePacket.TYPE) { packet, context ->
            context.server().execute {
                val player = context.player()
                val menu = player.containerMenu as? MoveStorageMenu
                if (menu != null && !menu.materialsSatisfied()) {
                    return@execute
                }
                if (MoveStorageLearning.learn(player, packet.pokemonUuid, packet.moveName)) {
                    menu?.consumeMaterials()
                }
            }
        }
    }
}
