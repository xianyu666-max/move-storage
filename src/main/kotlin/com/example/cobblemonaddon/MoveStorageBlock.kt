package com.example.cobblemonaddon

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.StringRepresentable
import com.mojang.serialization.MapCodec
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.level.Level
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty

/**
 * 招式存储器方块：PC 终端造型，占两格（part=bottom/top），青绿配色。
 */
class MoveStorageBlock(properties: Properties) : HorizontalDirectionalBlock(properties), EntityBlock {

    enum class Part(private val serialized: String) : StringRepresentable {
        BOTTOM("bottom"),
        TOP("top");

        override fun getSerializedName(): String = serialized
    }

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.BOTTOM)
                .setValue(ON, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, PART, ON)
    }

    override fun codec(): MapCodec<out HorizontalDirectionalBlock> = simpleCodec(::MoveStorageBlock)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.setValue(FACING, mirror.mirror(state.getValue(FACING)))

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val above = context.clickedPos.above()
        val below = context.level.getBlockState(context.clickedPos.below())
        if (below.block is MoveStorageBlock && below.getValue(PART) == Part.BOTTOM) {
            return null
        }
        if (!context.level.getBlockState(above).canBeReplaced(context)) {
            return null
        }
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection)
            .setValue(PART, Part.BOTTOM)
            .setValue(ON, false)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        val part = state.getValue(PART)
        if (neighborState.block !is MoveStorageBlock) {
            if (part == Part.TOP && neighborPos == pos.below()) return Blocks.AIR.defaultBlockState()
            if (part == Part.BOTTOM && neighborPos == pos.above()) return Blocks.AIR.defaultBlockState()
        }
        return state
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        val above = pos.above()
        if (level.getBlockState(above).canBeReplaced()) {
            level.setBlock(above, state.setValue(PART, Part.TOP), 3)
        }
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        if (!level.isClientSide) {
            val base = if (state.getValue(PART) == Part.BOTTOM) pos else pos.below()

            // 掉落带 NBT 的存储器（保留已存储招式）
            val be = level.getBlockEntity(base) as? MoveStorageBlockEntity
            if (be != null) {
                val stack = ItemStack(TmStoreBlocks.MOVE_STORAGE_ITEM)
                val tag = be.saveWithFullMetadata(level.registryAccess())
                stack.set(
                    net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag)
                )
                // 创造模式也掉落（保留已存储招式）
                Block.popResource(level, base, stack)
            }

            val other = if (state.getValue(PART) == Part.BOTTOM) pos.above() else pos.below()
            val otherState = level.getBlockState(other)
            if (otherState.block is MoveStorageBlock && otherState.getValue(PART) != state.getValue(PART)) {
                level.removeBlock(other, false)
            }
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    /** 掉落物由 playerWillDestroy 手动生成（带 NBT），这里阻止默认掉落 */
    override fun getDrops(
        state: BlockState,
        params: net.minecraft.world.level.storage.loot.LootParams.Builder
    ): MutableList<ItemStack> = mutableListOf()

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return if (state.getValue(PART) == Part.TOP) {
            when (state.getValue(FACING)) {
                Direction.SOUTH -> SOUTH_AABB_TOP
                Direction.WEST -> WEST_AABB_TOP
                Direction.EAST -> EAST_AABB_TOP
                else -> NORTH_AABB_TOP
            }
        } else {
            when (state.getValue(FACING)) {
                Direction.SOUTH -> SOUTH_AABB_BOTTOM
                Direction.WEST -> WEST_AABB_BOTTOM
                Direction.EAST -> EAST_AABB_BOTTOM
                else -> NORTH_AABB_BOTTOM
            }
        }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return if (state.getValue(PART) == Part.BOTTOM) MoveStorageBlockEntity(pos, state) else null
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (player is ServerPlayer) {
            val base = if (state.getValue(PART) == Part.BOTTOM) pos else pos.below()
            // 打开界面时点亮屏幕（上半格 on=true）
            val topPos = base.above()
            val topState = level.getBlockState(topPos)
            if (topState.block is MoveStorageBlock && !topState.getValue(ON)) {
                level.setBlock(topPos, topState.setValue(ON, true), 3)
            }
            val storage = level.getBlockEntity(base) as? MoveStorageBlockEntity
            if (storage != null) {
                CobblemonAddon.LOGGER.info("[{}] sync stored moves: {}", CobblemonAddon.MOD_ID, storage.storedMoves.size)
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    player,
                    SyncStoredMovesPacket(storage.storedMoves.toList())
                )
            }
            player.openMenu(object : MenuProvider {
                override fun getDisplayName(): Component =
                    Component.translatable("block.cobblemonaddon.move_storage")

                override fun createMenu(syncId: Int, inv: Inventory, p: Player): AbstractContainerMenu {
                    val menu = MoveStorageMenu(
                        syncId,
                        inv,
                        storage ?: net.minecraft.world.SimpleContainer(6),
                        net.minecraft.world.inventory.SimpleContainerData(6)
                    )
                    menu.moveStorageEntity = storage
                    menu.blockPos = base
                    return menu
                }
            })
        }
        return InteractionResult.SUCCESS
    }

    companion object {
        val PART: EnumProperty<Part> = EnumProperty.create("part", Part::class.java)
        val ON: BooleanProperty = BooleanProperty.create("on")

        private val NORTH_AABB_TOP = Shapes.or(
            Shapes.box(0.1875, 0.0, 0.0, 0.8125, 0.875, 0.125),
            Shapes.box(0.125, 0.8125, 0.125, 0.875, 0.9375, 0.6875),
            Shapes.box(0.125, 0.125, 0.125, 0.875, 0.8125, 0.625),
            Shapes.box(0.0625, 0.0, 0.125, 0.125, 0.9375, 0.6875),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.125, 0.6875),
            Shapes.box(0.875, 0.0, 0.125, 0.9375, 0.9375, 0.6875),
            Shapes.box(0.125, 0.0, 0.6875, 0.875, 0.0625, 0.875)
        )

        private val SOUTH_AABB_TOP = Shapes.or(
            Shapes.box(0.1875, 0.0, 0.875, 0.8125, 0.875, 1.0),
            Shapes.box(0.125, 0.8125, 0.3125, 0.875, 0.9375, 0.875),
            Shapes.box(0.125, 0.125, 0.375, 0.875, 0.8125, 0.875),
            Shapes.box(0.875, 0.0, 0.3125, 0.9375, 0.9375, 0.875),
            Shapes.box(0.125, 0.0, 0.3125, 0.875, 0.125, 0.875),
            Shapes.box(0.0625, 0.0, 0.3125, 0.125, 0.9375, 0.875),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.0625, 0.3125)
        )

        private val WEST_AABB_TOP = Shapes.or(
            Shapes.box(0.0, 0.0, 0.1875, 0.125, 0.875, 0.8125),
            Shapes.box(0.125, 0.8125, 0.125, 0.6875, 0.9375, 0.875),
            Shapes.box(0.125, 0.125, 0.125, 0.625, 0.8125, 0.875),
            Shapes.box(0.125, 0.0, 0.0625, 0.6875, 0.9375, 0.125),
            Shapes.box(0.125, 0.0, 0.125, 0.6875, 0.125, 0.875),
            Shapes.box(0.125, 0.0, 0.875, 0.6875, 0.9375, 0.9375),
            Shapes.box(0.6875, 0.0, 0.125, 0.875, 0.0625, 0.875)
        )

        private val EAST_AABB_TOP = Shapes.or(
            Shapes.box(0.875, 0.0, 0.1875, 1.0, 0.875, 0.8125),
            Shapes.box(0.3125, 0.8125, 0.125, 0.875, 0.9375, 0.875),
            Shapes.box(0.375, 0.125, 0.125, 0.875, 0.8125, 0.875),
            Shapes.box(0.3125, 0.0, 0.0625, 0.875, 0.9375, 0.125),
            Shapes.box(0.3125, 0.0, 0.125, 0.875, 0.125, 0.875),
            Shapes.box(0.3125, 0.0, 0.875, 0.875, 0.9375, 0.9375),
            Shapes.box(0.125, 0.0, 0.125, 0.3125, 0.0625, 0.875)
        )

        private val NORTH_AABB_BOTTOM = Shapes.or(
            Shapes.box(0.0625, 0.0625, 0.125, 0.9375, 1.0, 0.9375),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.0625, 0.875),
            Shapes.box(0.1875, 0.0, 0.0, 0.8125, 1.0, 0.125)
        )

        private val SOUTH_AABB_BOTTOM = Shapes.or(
            Shapes.box(0.0625, 0.0625, 0.0625, 0.9375, 1.0, 0.875),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.0625, 0.875),
            Shapes.box(0.1875, 0.0, 0.875, 0.8125, 1.0, 1.0)
        )

        private val WEST_AABB_BOTTOM = Shapes.or(
            Shapes.box(0.125, 0.0625, 0.0625, 0.9375, 1.0, 0.9375),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.0625, 0.875),
            Shapes.box(0.0, 0.0, 0.1875, 0.125, 1.0, 0.8125)
        )

        private val EAST_AABB_BOTTOM = Shapes.or(
            Shapes.box(0.0625, 0.0625, 0.0625, 0.875, 1.0, 0.9375),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.0625, 0.875),
            Shapes.box(0.875, 0.0, 0.1875, 1.0, 1.0, 0.8125)
        )
    }
}
