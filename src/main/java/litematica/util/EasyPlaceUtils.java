package litematica.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import malilib.input.Keys;
import malilib.overlay.message.MessageDispatcher;
import malilib.overlay.message.MessageOutput;
import malilib.registry.Registry;
import malilib.util.game.BlockUtils;
import malilib.util.game.PlacementUtils;
import malilib.util.game.RayTraceUtils.RayTraceFluidHandling;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameUtils;
import malilib.util.game.wrap.ItemWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkSectionPos;
import malilib.util.position.Direction;
import malilib.util.position.HitPosition;
import malilib.util.position.HitResult;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.LayerRange;
import malilib.util.position.PositionUtils;
import malilib.util.position.Vec3d;
import litematica.Litematica;
import litematica.config.Configs;
import litematica.config.Hotkeys;
import litematica.data.DataManager;
import litematica.materials.MaterialCache;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.tool.ToolMode;
import litematica.util.RayTraceUtils.RayTraceWrapper;
import litematica.world.SchematicWorldHandler;

public class EasyPlaceUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static final HashMap<Block, Boolean> HAS_USE_ACTION_CACHE = new HashMap<>();

    private static boolean isHandling;
    private static boolean isFirstClickEasyPlace;
    private static boolean isFirstClickPlacementRestriction;

    public static boolean isHandling()
    {
        return isHandling;
    }

    public static void setHandling(boolean handling)
    {
        isHandling = handling;
    }

    public static void setIsFirstClick()
    {
        if (shouldDoEasyPlaceActions())
        {
            isFirstClickEasyPlace = true;
        }

        if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
        {
            isFirstClickPlacementRestriction = true;
        }
    }

    private static boolean hasUseAction(Block block)
    {
        Boolean val = HAS_USE_ACTION_CACHE.get(block);

        if (val == null)
        {
            try
            {
                // TODO FIXME cross-MC-version fragile
                String name = Block.class.getSimpleName().equals("Block") ? "onBlockActivated": "a";
                Method method = block.getClass().getMethod(name, World.class, BlockPos.class, IBlockState.class, EntityPlayer.class, EnumHand.class, EnumFacing.class, float.class, float.class, float.class);
                Method baseMethod = Block.class.getMethod(name, World.class, BlockPos.class, IBlockState.class, EntityPlayer.class, EnumHand.class, EnumFacing.class, float.class, float.class, float.class);
                val = method.equals(baseMethod) == false;
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("EasyPlaceUtils: Failed to reflect method Block::onBlockActivated", e);
                val = false;
            }

            HAS_USE_ACTION_CACHE.put(block, val);
        }

        return val.booleanValue();
    }

    public static boolean shouldDoEasyPlaceActions()
    {
        return Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
               DataManager.getToolMode() != ToolMode.SCHEMATIC_EDIT &&
               Hotkeys.EASY_PLACE_ACTIVATION.getKeyBind().isKeyBindHeld();
    }

    public static void easyPlaceOnUseTick()
    {
        if (isHandling == false &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            shouldDoEasyPlaceActions() &&
            Keys.isKeyDown(GameUtils.getOptions().keyBindUseItem.getKeyCode()))
        {
            isHandling = true;
            handleEasyPlace();
            isHandling = false;
        }
    }

    public static boolean handleEasyPlaceWithMessage()
    {
        if (isHandling)
        {
            return false;
        }

        isHandling = true;
        EnumActionResult result = handleEasyPlace();
        isHandling = false;

        // Only print the warning message once per right click
        if (isFirstClickEasyPlace && result == EnumActionResult.FAIL)
        {
            MessageOutput output = Configs.InfoOverlays.EASY_PLACE_WARNINGS.getValue();
            MessageDispatcher.warning(1500).type(output).translate("litematica.message.easy_place_fail");
        }

        isFirstClickEasyPlace = false;

        return result != EnumActionResult.PASS;
    }

    public static void onRightClickTail()
    {
        // If the click wasn't handled yet, handle it now.
        // This is only called when right clicking on air with an empty hand,
        // as in that case neither the processRightClickBlock nor the processRightClick method get called.
        if (isFirstClickEasyPlace)
        {
            handleEasyPlaceWithMessage();
        }
    }

    @Nullable
    private static HitPosition getTargetPosition(@Nullable RayTraceWrapper traceWrapper)
    {
        BlockPos overriddenPos = Registry.BLOCK_PLACEMENT_POSITION_HANDLER.getCurrentPlacementPosition();

        if (overriddenPos != null)
        {
            double reach = Math.max(6, GameUtils.getInteractionManager().getBlockReachDistance());
            Entity entity = GameUtils.getCameraEntity();
            HitResult trace = RayTraceUtils.traceToPositions(Collections.singletonList(overriddenPos), entity, reach);
            BlockPos pos = overriddenPos;
            Vec3d hitPos;
            Direction side;

            if (trace != null && trace.type == HitResult.Type.BLOCK)
            {
                hitPos = trace.pos;
                side = trace.side;
            }
            else
            {
                hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                side = Direction.UP;
            }

            return HitPosition.of(pos, hitPos, side);
        }
        else if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            HitResult trace = traceWrapper.getRayTraceResult();
            return HitPosition.of(trace.blockPos, trace.pos, trace.side);
        }

        return null;
    }

    @Nullable
    private static HitPosition getAdjacentClickPosition(final BlockPos targetPos)
    {
        World world = GameUtils.getClientWorld();
        double reach = Math.max(6, GameUtils.getInteractionManager().getBlockReachDistance());
        Entity entity = GameUtils.getCameraEntity();
        HitResult traceVanilla = malilib.util.game.RayTraceUtils.getRayTraceFromEntity(world, entity, RayTraceFluidHandling.NONE, false, reach);

        if (traceVanilla.type == HitResult.Type.BLOCK)
        {
            BlockPos posVanilla = traceVanilla.blockPos;

            // If there is a block in the world right behind the targeted schematic block, then use
            // that block as the click position
            if (PlacementUtils.isReplaceable(world, posVanilla, false) == false &&
                targetPos.equals(posVanilla.offset(traceVanilla.side)))
            {
                return HitPosition.of(posVanilla, traceVanilla.pos, traceVanilla.side);
            }
        }

        for (Direction side : Direction.ALL_DIRECTIONS)
        {
            BlockPos posSide = targetPos.offset(side);

            if (PlacementUtils.isReplaceable(world, posSide, false) == false)
            {
                Vec3d hitPos = getHitPositionForSidePosition(posSide, side);
                return HitPosition.of(posSide, hitPos, side.getOpposite());
            }
        }

        return null;
    }

    private static Vec3d getHitPositionForSidePosition(BlockPos posSide, Direction sideFromTarget)
    {
        Direction.Axis axis = sideFromTarget.getAxis();
        double x = posSide.getX() + 0.5 - sideFromTarget.getXOffset() * 0.5;
        double y = posSide.getY() + (axis == Direction.Axis.Y ? (sideFromTarget == Direction.DOWN ? 1.0 : 0.0) : 0.0);
        double z = posSide.getZ() + 0.5 - sideFromTarget.getZOffset() * 0.5;

        return new Vec3d(x, y, z);
    }

    @Nullable
    private static HitPosition getClickPosition(HitPosition targetPosition,
                                                IBlockState stateSchematic,
                                                IBlockState stateClient)
    {
        boolean isSlab = stateSchematic.getBlock() instanceof BlockSlab;

        if (isSlab)
        {
            return getClickPositionForSlab(targetPosition, stateSchematic, stateClient);
        }

        BlockPos targetBlockPos = targetPosition.getBlockPos();
        boolean requireAdjacent = Configs.Generic.EASY_PLACE_CLICK_ADJACENT.getBooleanValue();

        return requireAdjacent ? getAdjacentClickPosition(targetBlockPos) : targetPosition;
    }

    @Nullable
    private static HitPosition getClickPositionForSlab(HitPosition targetPosition,
                                                       IBlockState stateSchematic,
                                                       IBlockState stateClient)
    {
        BlockSlab slab = (BlockSlab) stateSchematic.getBlock();
        BlockPos targetBlockPos = targetPosition.getBlockPos();
        World worldClient = GameUtils.getClientWorld();
        boolean isDouble = slab.isDouble();

        if (isDouble)
        {
            if (clientBlockIsSameMaterialSingleSlab(stateSchematic, stateClient))
            {
                boolean isTop = stateClient.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP;
                Direction side = isTop ? Direction.DOWN : Direction.UP;
                Vec3d hitPos = targetPosition.getExactPos();
                return HitPosition.of(targetBlockPos, new Vec3d(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), side);
            }
            else if (PlacementUtils.isReplaceable(worldClient, targetBlockPos, true))
            {
                HitPosition pos = getClickPositionForSlabHalf(targetPosition, stateSchematic, false, worldClient);
                return pos != null ? pos : getClickPositionForSlabHalf(targetPosition, stateSchematic, true, worldClient);
            }
        }
        // Single slab required, so the target position must be replaceable
        else if (isDouble == false && PlacementUtils.isReplaceable(worldClient, targetBlockPos, true))
        {
            boolean isTop = stateSchematic.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP;
            return getClickPositionForSlabHalf(targetPosition, stateSchematic, isTop, worldClient);
        }

        return null;
    }

    @Nullable
    private static HitPosition getClickPositionForSlabHalf(HitPosition targetPosition, IBlockState stateSchematic, boolean isTop, World worldClient)
    {
        BlockPos targetBlockPos = targetPosition.getBlockPos();
        boolean requireAdjacent = Configs.Generic.EASY_PLACE_CLICK_ADJACENT.getBooleanValue();

        // Can click on air blocks, check if the slab can be placed by clicking on the target position itself,
        // or if it's a fluid block, then the block above or below, depending on the half
        if (requireAdjacent == false)
        {
            Direction clickSide = isTop ? Direction.DOWN : Direction.UP;
            boolean isReplaceable = PlacementUtils.isReplaceable(worldClient, targetBlockPos, false);

            if (isReplaceable)
            {
                BlockPos posOffset = targetBlockPos.offset(clickSide);
                IBlockState stateSide = worldClient.getBlockState(posOffset);

                // Clicking on the target position itself does not create a double slab above or below, so just click on the position itself
                if (clientBlockIsSameMaterialSingleSlab(stateSchematic, stateSide) == false)
                {
                    Vec3d hitPos = targetPosition.getExactPos();
                    return HitPosition.of(targetBlockPos, new Vec3d(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), clickSide);
                }
            }
            else if (worldClient.getBlockState(targetBlockPos).getMaterial().isLiquid())
            {
                // Can click on the compensated position without creating a double slab there
                if (canClickOnAdjacentBlockToPlaceSingleSlabAt(targetBlockPos, stateSchematic, clickSide.getOpposite(), worldClient))
                {
                    BlockPos pos = targetBlockPos.offset(clickSide.getOpposite());
                    Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    return HitPosition.of(pos, hitPos, clickSide);
                }
            }
        }

        // Required to be clicking on an existing adjacent block,
        // or couldn't click on the target position itself without creating an adjacent double slab
        return getAdjacentClickPositionForSlab(targetBlockPos, stateSchematic, isTop, worldClient);
    }

    @Nullable
    private static HitPosition getAdjacentClickPositionForSlab(BlockPos targetBlockPos, IBlockState stateSchematic, boolean isTop, World worldClient)
    {
        Direction clickSide = isTop ? Direction.DOWN : Direction.UP;
        Direction clickSideOpposite = clickSide.getOpposite();
        BlockPos posSide = targetBlockPos.offset(clickSideOpposite);

        // Can click on the existing block above or below
        if (canClickOnAdjacentBlockToPlaceSingleSlabAt(targetBlockPos, stateSchematic, clickSideOpposite, worldClient))
        {
            return HitPosition.of(posSide, getHitPositionForSidePosition(posSide, clickSideOpposite), clickSide);
        }
        // Try the sides
        else
        {
            for (Direction side : Direction.HORIZONTAL_DIRECTIONS)
            {
                if (canClickOnAdjacentBlockToPlaceSingleSlabAt(targetBlockPos, stateSchematic, side, worldClient))
                {
                    posSide = targetBlockPos.offset(side);
                    Vec3d hitPos = getHitPositionForSidePosition(posSide, side);
                    double y = isTop ? 0.9 : 0.1;
                    return HitPosition.of(posSide, new Vec3d(hitPos.x, posSide.getY() + y, hitPos.z), side.getOpposite());
                }
            }
        }

        return null;
    }

    private static boolean canClickOnAdjacentBlockToPlaceSingleSlabAt(BlockPos targetBlockPos, IBlockState targetState, Direction side, World worldClient)
    {
        BlockPos posSide = targetBlockPos.offset(side);
        IBlockState stateSide = worldClient.getBlockState(posSide);

        return PlacementUtils.isReplaceable(worldClient, posSide, false) == false &&
               (side.getAxis() != Direction.Axis.Y ||
                clientBlockIsSameMaterialSingleSlab(targetState, stateSide) == false
                || stateSide.getValue(BlockSlab.HALF) != targetState.getValue(BlockSlab.HALF));
    }

    private static EnumActionResult handleEasyPlace()
    {
        Entity entity = GameUtils.getCameraEntity();
        WorldClient world = GameUtils.getClientWorld();
        double reach = Math.max(6, GameUtils.getInteractionManager().getBlockReachDistance());
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(world, entity, reach, true, RayTraceFluidHandling.ANY);
        HitPosition targetPosition = getTargetPosition(traceWrapper);

        // No position override, and didn't ray trace to a schematic block
        if (targetPosition == null)
        {
            if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA)
            {
                return placementRestrictionInEffect() ? EnumActionResult.FAIL : EnumActionResult.PASS;
            }

            return EnumActionResult.PASS;
        }

        final BlockPos targetBlockPos = targetPosition.getBlockPos();
        World schematicWorld = SchematicWorldHandler.getSchematicWorld();
        IBlockState stateSchematic = schematicWorld.getBlockState(targetBlockPos);
        IBlockState stateClient = world.getBlockState(targetBlockPos).getActualState(world, targetBlockPos);
        ItemStack requiredStack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

        // The block is correct already, or it was recently placed, or some of the checks failed
        if (stateSchematic == stateClient || ItemWrap.isEmpty(requiredStack) ||
            easyPlaceIsPositionCached(targetBlockPos) ||
            canPlaceBlock(targetBlockPos, world, stateSchematic, stateClient) == false)
        {
            return EnumActionResult.FAIL;
        }

        HitPosition clickPosition = getClickPosition(targetPosition, stateSchematic, stateClient);
        EnumHand hand = PickBlockUtils.doPickBlockForStack(requiredStack);

        // Didn't find a valid or safe click position, or was unable to pick block
        if (clickPosition == null || hand == null)
        {
            return EnumActionResult.FAIL;
        }

        boolean isSlab = stateSchematic.getBlock() instanceof BlockSlab;
        boolean usingAdjacentClickPosition = clickPosition.getBlockPos().equals(targetBlockPos) == false;
        BlockPos clickPos = clickPosition.getBlockPos();
        Vec3d hitPos = clickPosition.getExactPos();
        Direction side = clickPosition.getSide();

        if (usingAdjacentClickPosition == false && isSlab == false)
        {
            side = applyPlacementFacing(stateSchematic, side, stateClient);

            // Fluid _blocks_ are not replaceable... >_>
            if (stateClient.getBlock().isReplaceable(world, targetBlockPos) == false &&
                stateClient.getMaterial().isLiquid())
            {
                clickPos = clickPos.offset(side, -1);
            }
        }

        if (isSlab == false)
        {
            hitPos = applyCarpetProtocolHitVec(clickPos, stateSchematic, hitPos);
        }

        //System.out.printf("targetPos: %s, clickPos: %s side: %s, hit: %s\n", targetBlockPos, clickPos, side, hitPos);
        stateClient = world.getBlockState(clickPos);
        boolean needsSneak = hasUseAction(stateClient.getBlock());
        boolean didFakeSneak = needsSneak && EntityUtils.setFakedSneakingState(true);
        EntityPlayerSP player = GameUtils.getClientPlayer();

        if (GameUtils.getInteractionManager().processRightClickBlock(player, world, clickPos, side.getVanillaDirection(), hitPos.toVanilla(), hand) == EnumActionResult.SUCCESS)
        {
            // Mark that this position has been handled (use the non-offset position that is checked above)
            cacheEasyPlacePosition(targetBlockPos);

            player.swingArm(hand);
            GameUtils.getClient().entityRenderer.itemRenderer.resetEquippedProgress(hand);

            if (isSlab && ((BlockSlab) stateSchematic.getBlock()).isDouble())
            {
                stateClient = world.getBlockState(targetBlockPos).getActualState(world, targetBlockPos);

                if (stateClient.getBlock() instanceof BlockSlab && ((BlockSlab) stateClient.getBlock()).isDouble() == false)
                {
                    side = stateClient.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP ? Direction.DOWN : Direction.UP;
                    hitPos = new Vec3d(targetBlockPos.getX(), targetBlockPos.getY() + 0.5, targetBlockPos.getZ());
                    //System.out.printf("slab - pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                    GameUtils.getInteractionManager().processRightClickBlock(player, world, targetBlockPos, side.getVanillaDirection(), hitPos.toVanilla(), hand);
                }
            }
        }

        if (didFakeSneak)
        {
            EntityUtils.setFakedSneakingState(false);
        }

        return EnumActionResult.SUCCESS;
    }

    private static boolean clientBlockIsSameMaterialSingleSlab(IBlockState stateSchematic, IBlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if ((blockSchematic instanceof BlockSlab) &&
            (blockClient instanceof BlockSlab) &&
            ((BlockSlab) blockClient).isDouble() == false)
        {
            IProperty<?> propSchematic = ((BlockSlab) blockSchematic).getVariantProperty();
            IProperty<?> propClient = ((BlockSlab) blockClient).getVariantProperty();

            return propSchematic == propClient && stateSchematic.getValue(propSchematic) == stateClient.getValue(propClient);
        }

        return false;
    }

    private static boolean canPlaceBlock(BlockPos targetPos, World worldClient, IBlockState stateSchematic, IBlockState stateClient)
    {
        boolean isSlab = stateSchematic.getBlock() instanceof BlockSlab;

        if (isSlab)
        {
            if (PlacementUtils.isReplaceable(worldClient, targetPos, true) == false &&
                (((BlockSlab) stateSchematic.getBlock()).isDouble() == false
                || clientBlockIsSameMaterialSingleSlab(stateSchematic, stateClient) == false))
            {
                return false;
            }

            return true;
        }

        return PlacementUtils.isReplaceable(worldClient, targetPos, true);
    }

    private static Vec3d applyCarpetProtocolHitVec(BlockPos pos, IBlockState state, Vec3d hitVecIn)
    {
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        Optional<Direction> facingOptional = BlockUtils.getFirstPropertyFacingValue(state);

        if (facingOptional.isPresent())
        {
            x = facingOptional.get().ordinal() + 2 + pos.getX();
        }

        if (block instanceof BlockRedstoneRepeater)
        {
            x += ((state.getValue(BlockRedstoneRepeater.DELAY)) - 1) * 10;
        }
        else if (block instanceof BlockTrapDoor && state.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.TOP)
        {
            x += 10;
        }
        else if (block instanceof BlockRedstoneComparator && state.getValue(BlockRedstoneComparator.MODE) == BlockRedstoneComparator.Mode.SUBTRACT)
        {
            x += 10;
        }
        else if (block instanceof BlockStairs && state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP)
        {
            x += 10;
        }

        return new Vec3d(x, y, z);
    }

    private static Direction applyPlacementFacing(IBlockState stateSchematic, Direction side, IBlockState stateClient)
    {
        Optional<PropertyDirection> propOptional = BlockUtils.getFirstDirectionProperty(stateSchematic);

        if (propOptional.isPresent())
        {
            side = Direction.of(stateSchematic.getValue(propOptional.get()).getOpposite());
        }

        return side;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     */
    public static boolean handlePlacementRestriction()
    {
        boolean cancel = placementRestrictionInEffect();

        if (cancel && isFirstClickPlacementRestriction)
        {
            MessageOutput output = Configs.InfoOverlays.EASY_PLACE_WARNINGS.getValue();
            MessageDispatcher.warning(1000).type(output).translate("litematica.message.placement_restriction_fail");
        }

        isFirstClickPlacementRestriction = false;

        return cancel;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @return true if the use action should be cancelled
     */
    private static boolean placementRestrictionInEffect()
    {
        Entity entity = GameUtils.getCameraEntity();
        World world = GameUtils.getClientWorld();
        double reach = GameUtils.getInteractionManager().getBlockReachDistance();
        HitResult trace = malilib.util.game.RayTraceUtils.getRayTraceFromEntity(world, entity, RayTraceFluidHandling.NONE, false, reach);

        if (trace.type == HitResult.Type.BLOCK)
        {
            BlockPos pos = trace.blockPos;
            IBlockState stateClient = world.getBlockState(pos);

            if (stateClient.getBlock().isReplaceable(world, pos) == false)
            {
                pos = pos.offset(trace.side);
                stateClient = world.getBlockState(pos);
            }

            // The targeted position is far enough from any schematic sub-regions to not need handling
            if (isPositionWithinRangeOfSchematicRegions(pos, 2) == false)
            {
                return false;
            }

            // Placement position is already occupied
            if (stateClient.getBlock().isReplaceable(world, pos) == false &&
                stateClient.getMaterial().isLiquid() == false)
            {
                return true;
            }

            World worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();

            // The targeted position should be air or it's outside the current render range
            if (worldSchematic.isAirBlock(pos) || range.isPositionWithinRange(pos) == false)
            {
                return true;
            }

            IBlockState stateSchematic = worldSchematic.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // The player is holding the wrong item for the targeted position
            return ItemWrap.isEmpty(stack) || EntityWrap.getUsedHandForItem(GameUtils.getClientPlayer(), stack, true) == null;
        }

        return false;
    }

    private static boolean isPositionWithinRangeOfSchematicRegions(BlockPos pos, int range)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        final int minCX = (pos.getX() - range) >> 4;
        final int minCY = (pos.getY() - range) >> 4;
        final int minCZ = (pos.getZ() - range) >> 4;
        final int maxCX = (pos.getX() + range) >> 4;
        final int maxCY = (pos.getY() + range) >> 4;
        final int maxCZ = (pos.getZ() + range) >> 4;
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        for (int cy = minCY; cy <= maxCY; ++cy)
        {
            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                for (int cx = minCX; cx <= maxCX; ++cx)
                {
                    List<IntBoundingBox> boxes = manager.getTouchedBoxesInSubChunk(new ChunkSectionPos(cx, cy, cz));

                    for (IntBoundingBox box : boxes)
                    {
                        if (x >= box.minX - range && x <= box.maxX + range &&
                            y >= box.minY - range && y <= box.maxY + range &&
                            z >= box.minZ - range && z <= box.maxZ + range)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean easyPlaceIsPositionCached(BlockPos pos)
    {
        long currentTime = System.nanoTime();
        boolean cached = false;

        for (int i = 0; i < EASY_PLACE_POSITIONS.size(); ++i)
        {
            PositionCache val = EASY_PLACE_POSITIONS.get(i);
            boolean expired = val.hasExpired(currentTime);

            if (expired)
            {
                EASY_PLACE_POSITIONS.remove(i);
                --i;
            }
            else if (val.getPos().equals(pos))
            {
                cached = true;

                // Keep checking and removing old entries if there are a fair amount
                if (EASY_PLACE_POSITIONS.size() < 16)
                {
                    break;
                }
            }
        }

        return cached;
    }

    private static void cacheEasyPlacePosition(BlockPos pos)
    {
        EASY_PLACE_POSITIONS.add(new PositionCache(pos, System.nanoTime(), 2000000000));
    }

    public static class PositionCache
    {
        private final BlockPos pos;
        private final long time;
        private final long timeout;

        private PositionCache(BlockPos pos, long time, long timeout)
        {
            this.pos = pos;
            this.time = time;
            this.timeout = timeout;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public boolean hasExpired(long currentTime)
        {
            return currentTime - this.time > this.timeout;
        }
    }
}
