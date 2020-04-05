package fi.dy.masa.litematica.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.systems.BlockPlacementPositionHandler;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.PlacementUtils;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.malilib.util.RayTraceUtils.RayTraceFluidHandling;
import fi.dy.masa.malilib.util.SubChunkPos;
import fi.dy.masa.malilib.util.data.HitPosition;

public class EasyPlaceUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static final HashMap<Block, Boolean> HAS_USE_ACTION_CACHE = new HashMap<>();

    private static boolean isHandling;
    private static boolean isFirstClick;

    public static boolean isHandling()
    {
        return isHandling;
    }

    public static void setHandling(boolean handling)
    {
        isHandling = handling;
    }

    public static void setIsFirstClick(boolean isFirst)
    {
        isFirstClick = isFirst;
    }

    private static boolean hasUseAction(Block block)
    {
        Boolean val = HAS_USE_ACTION_CACHE.get(block);

        if (val == null)
        {
            try
            {
                String name = Block.class.getSimpleName().equals("Block") ? "onBlockActivated": "a";
                Method method = block.getClass().getMethod(name, World.class, BlockPos.class, IBlockState.class, EntityPlayer.class, EnumHand.class, EnumFacing.class, float.class, float.class, float.class);
                Method baseMethod = Block.class.getMethod(name, World.class, BlockPos.class, IBlockState.class, EntityPlayer.class, EnumHand.class, EnumFacing.class, float.class, float.class, float.class);
                val = method.equals(baseMethod) == false;
            }
            catch (Exception e)
            {
                LiteModLitematica.logger.warn("EasyPlaceUtils: Failed to reflect method Block::onBlockActivated", e);
                val = false;
            }

            HAS_USE_ACTION_CACHE.put(block, val);
        }

        return val.booleanValue();
    }

    public static void easyPlaceOnUseTick(Minecraft mc)
    {
        if (mc.player != null && isHandling == false &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld() &&
            KeybindMulti.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode()))
        {
            isHandling = true;
            handleEasyPlace(mc);
            isHandling = false;
        }
    }

    public static boolean handleEasyPlaceWithMessage(Minecraft mc, boolean printFailMessage)
    {
        if (isHandling)
        {
            return false;
        }

        isHandling = true;
        EnumActionResult result = handleEasyPlace(mc);
        isHandling = false;

        // Only print the warning message once per right click
        if (printFailMessage && isFirstClick && result == EnumActionResult.FAIL)
        {
            InfoUtils.showMessage(Configs.InfoOverlays.EASY_PLACE_WARNINGS.getOptionListValue(), MessageType.WARNING, 1000, "litematica.message.easy_place_fail");
        }

        isFirstClick = false;

        return result != EnumActionResult.PASS;
    }

    public static void onRightClickTail(Minecraft mc)
    {
        // If the click wasn't handled yet, handle it now.
        // This is only called when right clicking on air with an empty hand,
        // as in that case neither the processRightClickBlock nor the processRightClick method get called.
        if (isFirstClick)
        {
            handleEasyPlaceWithMessage(mc, true);
        }
    }

    @Nullable
    private static HitPosition getTargetPosition(@Nullable RayTraceWrapper traceWrapper, Minecraft mc)
    {
        BlockPos overriddenPos = BlockPlacementPositionHandler.INSTANCE.getCurrentPlacementPosition();

        if (overriddenPos != null)
        {
            double reach = Math.max(6, mc.playerController.getBlockReachDistance());
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            RayTraceResult trace = RayTraceUtils.traceToPositions(Arrays.asList(overriddenPos), entity, reach);
            BlockPos pos = overriddenPos;
            Vec3d hitPos;
            EnumFacing side;

            if (trace != null)
            {
                hitPos = trace.hitVec;
                side = trace.sideHit;
            }
            else
            {
                hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                side = EnumFacing.UP;
            }

            return HitPosition.of(pos, hitPos, side);
        }
        else if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            RayTraceResult trace = traceWrapper.getRayTraceResult();
            return HitPosition.of(trace.getBlockPos(), trace.hitVec, trace.sideHit);
        }

        return null;
    }

    @Nullable
    private static HitPosition getAdjacentClickPosition(final BlockPos targetPos, Minecraft mc)
    {
        World world = mc.world;
        double reach = Math.max(6, mc.playerController.getBlockReachDistance());
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceResult traceVanilla = fi.dy.masa.malilib.util.RayTraceUtils.getRayTraceFromEntity(world, entity, RayTraceFluidHandling.NONE, false, reach);

        if (traceVanilla != null && traceVanilla.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            BlockPos posVanilla = traceVanilla.getBlockPos();

            // If there is a block in the world right behind the targeted schematic block, then use
            // that block as the click position
            if (PlacementUtils.isReplaceable(world, posVanilla, false) == false &&
                targetPos.equals(posVanilla.offset(traceVanilla.sideHit)))
            {
                return HitPosition.of(posVanilla, traceVanilla.hitVec, traceVanilla.sideHit);
            }
        }

        for (EnumFacing side : fi.dy.masa.malilib.util.PositionUtils.ALL_DIRECTIONS)
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

    private static Vec3d getHitPositionForSidePosition(BlockPos posSide, EnumFacing sideFromTarget)
    {
        EnumFacing.Axis axis = sideFromTarget.getAxis();
        double x = posSide.getX() + 0.5 - sideFromTarget.getXOffset() * 0.5;
        double y = posSide.getY() + (axis == EnumFacing.Axis.Y ? (sideFromTarget == EnumFacing.DOWN ? 1.0 : 0.0) : 0.0);
        double z = posSide.getZ() + 0.5 - sideFromTarget.getZOffset() * 0.5;

        return new Vec3d(x, y, z);
    }

    @Nullable
    private static HitPosition getClickPosition(HitPosition targetPosition, IBlockState stateSchematic, IBlockState stateClient, Minecraft mc)
    {
        boolean isSlab = stateSchematic.getBlock() instanceof BlockSlab;

        if (isSlab)
        {
            return getClickPositionForSlab(targetPosition, stateSchematic, stateClient, mc);
        }

        BlockPos targetBlockPos = targetPosition.getBlockPos();
        boolean requireAdjacent = Configs.Generic.EASY_PLACE_CLICK_ADJACENT.getBooleanValue();

        return requireAdjacent ? getAdjacentClickPosition(targetBlockPos, mc) : targetPosition;
    }

    @Nullable
    private static HitPosition getClickPositionForSlab(HitPosition targetPosition, IBlockState stateSchematic, IBlockState stateClient, Minecraft mc)
    {
        BlockSlab slab = (BlockSlab) stateSchematic.getBlock();
        BlockPos targetBlockPos = targetPosition.getBlockPos();
        World worldClient = mc.world;
        boolean isDouble = slab.isDouble();

        if (isDouble)
        {
            if (clientBlockIsSameMaterialSingleSlab(stateSchematic, stateClient))
            {
                boolean isTop = stateClient.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP;
                EnumFacing side = isTop ? EnumFacing.DOWN : EnumFacing.UP;
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
            EnumFacing clickSide = isTop ? EnumFacing.DOWN : EnumFacing.UP;
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
        EnumFacing clickSide = isTop ? EnumFacing.DOWN : EnumFacing.UP;
        EnumFacing clickSideOpposite = clickSide.getOpposite();
        BlockPos posSide = targetBlockPos.offset(clickSideOpposite);

        // Can click on the existing block above or below
        if (canClickOnAdjacentBlockToPlaceSingleSlabAt(targetBlockPos, stateSchematic, clickSideOpposite, worldClient))
        {
            return HitPosition.of(posSide, getHitPositionForSidePosition(posSide, clickSideOpposite), clickSide);
        }
        // Try the sides
        else
        {
            for (EnumFacing side : PositionUtils.HORIZONTAL_DIRECTIONS)
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

    private static boolean canClickOnAdjacentBlockToPlaceSingleSlabAt(BlockPos targetBlockPos, IBlockState targetState, EnumFacing side, World worldClient)
    {
        BlockPos posSide = targetBlockPos.offset(side);
        IBlockState stateSide = worldClient.getBlockState(posSide);

        return PlacementUtils.isReplaceable(worldClient, posSide, false) == false &&
               (side.getAxis() != EnumFacing.Axis.Y ||
                clientBlockIsSameMaterialSingleSlab(targetState, stateSide) == false
                || stateSide.getValue(BlockSlab.HALF) != targetState.getValue(BlockSlab.HALF));
    }

    private static EnumActionResult handleEasyPlace(Minecraft mc)
    {
        double reach = Math.max(6, mc.playerController.getBlockReachDistance());
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, RayTraceFluidHandling.ANY, reach, true, false);
        HitPosition targetPosition = getTargetPosition(traceWrapper, mc);

        // No position override, and didn't ray trace to a schematic block
        if (targetPosition == null)
        {
            if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA)
            {
                return placementRestrictionInEffect(mc) ? EnumActionResult.FAIL : EnumActionResult.PASS;
            }

            return EnumActionResult.PASS;
        }

        final BlockPos targetBlockPos = targetPosition.getBlockPos();
        World schematicWorld = SchematicWorldHandler.getSchematicWorld();
        IBlockState stateSchematic = schematicWorld.getBlockState(targetBlockPos);
        IBlockState stateClient = mc.world.getBlockState(targetBlockPos).getActualState(mc.world, targetBlockPos);
        ItemStack requiredStack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

        // The block is correct already, or it was recently placed, or some of the checks failed
        if (stateSchematic == stateClient || requiredStack.isEmpty() ||
            easyPlaceIsPositionCached(targetBlockPos) ||
            canPlaceBlock(targetBlockPos, mc.world, stateSchematic, stateClient) == false)
        {
            return EnumActionResult.FAIL;
        }

        HitPosition clickPosition = getClickPosition(targetPosition, stateSchematic, stateClient, mc);
        EnumHand hand = InventoryUtils.doPickBlockForStack(requiredStack, mc);

        // Didn't find a valid or safe click position, or was unable to pick block
        if (clickPosition == null || hand == null)
        {
            return EnumActionResult.FAIL;
        }

        boolean isSlab = stateSchematic.getBlock() instanceof BlockSlab;
        boolean usingAdjacentClickPosition = clickPosition.getBlockPos().equals(targetBlockPos) == false;
        BlockPos clickPos = clickPosition.getBlockPos();
        Vec3d hitPos = clickPosition.getExactPos();
        EnumFacing side = clickPosition.getSide();

        if (usingAdjacentClickPosition == false && isSlab == false)
        {
            side = applyPlacementFacing(stateSchematic, side, stateClient);

            // Fluid _blocks_ are not replaceable... >_>
            if (stateClient.getBlock().isReplaceable(mc.world, targetBlockPos) == false &&
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
        stateClient = mc.world.getBlockState(clickPos);
        boolean needsSneak = hasUseAction(stateClient.getBlock());
        boolean didFakeSneak = needsSneak && EntityUtils.setFakedSneakingState(mc, true);

        if (mc.playerController.processRightClickBlock(mc.player, mc.world, clickPos, side, hitPos, hand) == EnumActionResult.SUCCESS)
        {
            // Mark that this position has been handled (use the non-offset position that is checked above)
            cacheEasyPlacePosition(targetBlockPos);

            mc.player.swingArm(hand);
            mc.entityRenderer.itemRenderer.resetEquippedProgress(hand);

            if (isSlab && ((BlockSlab) stateSchematic.getBlock()).isDouble())
            {
                stateClient = mc.world.getBlockState(targetBlockPos).getActualState(mc.world, targetBlockPos);

                if (stateClient.getBlock() instanceof BlockSlab && ((BlockSlab) stateClient.getBlock()).isDouble() == false)
                {
                    side = stateClient.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP ? EnumFacing.DOWN : EnumFacing.UP;
                    hitPos = new Vec3d(targetBlockPos.getX(), targetBlockPos.getY() + 0.5, targetBlockPos.getZ());
                    //System.out.printf("slab - pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                    mc.playerController.processRightClickBlock(mc.player, mc.world, targetBlockPos, side, hitPos, hand);
                }
            }
        }

        if (didFakeSneak)
        {
            EntityUtils.setFakedSneakingState(mc, false);
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
        EnumFacing facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);

        if (facing != null)
        {
            x = facing.ordinal() + 2 + pos.getX();
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

    private static EnumFacing applyPlacementFacing(IBlockState stateSchematic, EnumFacing side, IBlockState stateClient)
    {
        PropertyDirection prop = BlockUtils.getFirstDirectionProperty(stateSchematic);

        if (prop != null)
        {
            side = stateSchematic.getValue(prop).getOpposite();
        }

        return side;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @param doEasyPlace
     * @param restrictPlacement
     * @return
     */
    public static boolean handlePlacementRestriction(Minecraft mc)
    {
        boolean cancel = placementRestrictionInEffect(mc);

        if (cancel)
        {
            InfoUtils.showMessage(Configs.InfoOverlays.EASY_PLACE_WARNINGS.getOptionListValue(), MessageType.WARNING, "litematica.message.placement_restriction_fail");
        }

        return cancel;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @param doEasyPlace
     * @param restrictPlacement
     * @return true if the use action should be cancelled
     */
    private static boolean placementRestrictionInEffect(Minecraft mc)
    {
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        double reach = mc.playerController.getBlockReachDistance();
        RayTraceResult trace = fi.dy.masa.malilib.util.RayTraceUtils.getRayTraceFromEntity(mc.world, entity, RayTraceFluidHandling.NONE, false, reach);

        if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            BlockPos pos = trace.getBlockPos();
            IBlockState stateClient = mc.world.getBlockState(pos);

            if (stateClient.getBlock().isReplaceable(mc.world, pos) == false)
            {
                pos = pos.offset(trace.sideHit);
                stateClient = mc.world.getBlockState(pos);
            }

            // The targeted position is far enough from any schematic sub-regions to not need handling
            if (isPositionWithinRangeOfSchematicRegions(pos, 2) == false)
            {
                return false;
            }

            // Placement position is already occupied
            if (stateClient.getBlock().isReplaceable(mc.world, pos) == false &&
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
            if (stack.isEmpty() || EntityUtils.getUsedHandForItem(mc.player, stack, true) == null)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isPositionWithinRangeOfSchematicRegions(BlockPos pos, int range)
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
                    List<IntBoundingBox> boxes = manager.getTouchedBoxesInSubChunk(new SubChunkPos(cx, cy, cz));

                    for (int i = 0; i < boxes.size(); ++i)
                    {
                        IntBoundingBox box = boxes.get(i);

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

    public static boolean easyPlaceIsPositionCached(BlockPos pos)
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
