package com.example.cobblemonaddon

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * 打开存储器界面时，服务端把「已存储招式」发给客户端，
 * 客户端用来置灰未存储的招式、以及把已存储的排到前面。
 */
data class SyncStoredMovesPacket(val moves: List<String>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SyncStoredMovesPacket> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<SyncStoredMovesPacket>(
            ResourceLocation.fromNamespaceAndPath(CobblemonAddon.MOD_ID, "sync_stored_moves")
        )

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncStoredMovesPacket> = StreamCodec.of(
            { buf: RegistryFriendlyByteBuf, packet: SyncStoredMovesPacket ->
                buf.writeVarInt(packet.moves.size)
                packet.moves.forEach { buf.writeUtf(it) }
            },
            { buf: RegistryFriendlyByteBuf ->
                val size = buf.readVarInt()
                val list = ArrayList<String>(size)
                repeat(size) { list.add(buf.readUtf()) }
                SyncStoredMovesPacket(list)
            }
        )
    }
}
