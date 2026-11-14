package com.example.cobblemonaddon

import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * 客户端把当前选中的招式同步到服务端（材料槽校验/消耗需要）。
 */
data class SelectTmPacket(val moveName: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SelectTmPacket> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<SelectTmPacket>(
            ResourceLocation.fromNamespaceAndPath(CobblemonAddon.MOD_ID, "select_tm")
        )

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SelectTmPacket> = StreamCodec.of(
            { buf: RegistryFriendlyByteBuf, packet: SelectTmPacket -> buf.writeUtf(packet.moveName) },
            { buf: RegistryFriendlyByteBuf -> SelectTmPacket(buf.readUtf()) }
        )
    }
}

object SelectTmNetworking {

    fun register() {
        PayloadTypeRegistry.playC2S().register(SelectTmPacket.TYPE, SelectTmPacket.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(SelectTmPacket.TYPE) { packet, context ->
            context.server().execute {
                val menu = context.player().containerMenu as? MoveStorageMenu ?: return@execute
                val move = Moves.getByName(packet.moveName) ?: return@execute
                menu.currentlySelectedTM = TechnicalMachines.moveToTM[move]
            }
        }
    }
}
