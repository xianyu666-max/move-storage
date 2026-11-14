package com.example.cobblemonaddon

import com.cobblemon.mod.common.Cobblemon
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.world.item.CreativeModeTabs
import org.slf4j.LoggerFactory

/**
 * 模组入口。Fabric 会实例化这个类并调用 [onInitialize]（服务端与客户端都会执行）。
 */
class CobblemonAddon : ModInitializer {

    override fun onInitialize() {
        LOGGER.info("[{}] init ok, cobblemon {} (modid={})", MOD_ID, Cobblemon.VERSION, Cobblemon.MODID)

        TmStoreBlocks.register()
        MoveStorageCommand.register()
        MoveStorageNetworking.register()
        SelectTmNetworking.register()

        // 加入创造模式「功能性方块」标签
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register { entries ->
            entries.accept(TmStoreBlocks.MOVE_STORAGE_ITEM)
        }

        LOGGER.info("[{}] move storage registered", MOD_ID)
    }

    companion object {
        const val MOD_ID: String = "cobblemonaddon"
        val LOGGER = LoggerFactory.getLogger(MOD_ID)
    }
}
