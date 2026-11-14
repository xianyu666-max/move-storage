package com.example.cobblemonaddon

import com.example.cobblemonaddon.gui.MoveStorageScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.screens.MenuScreens

/**
 * 客户端入口：必须是 public 无参类（Kotlin object 的构造器是 private，Fabric 无法实例化）。
 */
class TmStoreClient : ClientModInitializer {

    override fun onInitializeClient() {
        MenuScreens.register(TmStoreBlocks.MOVE_STORAGE_MENU) { menu, inv, title ->
            MoveStorageScreen(menu, inv, title)
        }

        ClientPlayNetworking.registerGlobalReceiver(SyncStoredMovesPacket.TYPE) { packet, context ->
            context.client().execute {
                CobblemonAddon.LOGGER.info("[{}] client stored moves: {}", CobblemonAddon.MOD_ID, packet.moves.size)
                MoveStorageClientData.update(packet.moves)
                val screen = net.minecraft.client.Minecraft.getInstance().screen
                if (screen is MoveStorageScreen) {
                    screen.refreshMoveList()
                }
            }
        }

        CobblemonAddon.LOGGER.info("[{}] client screens registered", CobblemonAddon.MOD_ID)
    }
}
