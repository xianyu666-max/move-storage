package com.example.cobblemonaddon

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour

object TmStoreBlocks {

    lateinit var MOVE_STORAGE: Block
        private set
    lateinit var MOVE_STORAGE_ITEM: Item
        private set
    lateinit var MOVE_STORAGE_ENTITY: BlockEntityType<MoveStorageBlockEntity>
        private set
    lateinit var MOVE_STORAGE_MENU: MenuType<MoveStorageMenu>
        private set

    fun register() {
        val id = ResourceLocation.fromNamespaceAndPath(CobblemonAddon.MOD_ID, "move_storage")

        MOVE_STORAGE = Registry.register(
            BuiltInRegistries.BLOCK,
            id,
            MoveStorageBlock(
                BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            )
        )

        MOVE_STORAGE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            id,
            BlockItem(MOVE_STORAGE, Item.Properties())
        )

        MOVE_STORAGE_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id,
            BlockEntityType.Builder.of(
                { pos, state -> MoveStorageBlockEntity(pos, state) },
                MOVE_STORAGE
            ).build(null)
        )

        MOVE_STORAGE_MENU = Registry.register(
            BuiltInRegistries.MENU,
            id,
            MenuType(
                net.minecraft.world.inventory.MenuType.MenuSupplier { syncId, inv ->
                    MoveStorageMenu(syncId, inv)
                },
                net.minecraft.world.flag.FeatureFlags.VANILLA_SET
            )
        )
    }
}
