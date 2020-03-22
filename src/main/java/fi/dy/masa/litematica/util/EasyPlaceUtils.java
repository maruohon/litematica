package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
import fi.dy.masa.malilib.util.RayTraceUtils.RayTraceFluidHandling;
import fi.dy.masa.malilib.util.SubChunkPos;

public class EasyPlaceUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();

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

    private static EnumActionResult handleEasyPlace(Minecraft mc)
    {
        BlockPos overriddenPos = BlockPlacementPositionHandler.INSTANCE.getCurrentPlacementPosition();

        double reach = Math.max(6, mc.playerController.getBlockReachDistance());
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, RayTraceFluidHandling.ANY, reach, true, false);

        if (traceWrapper == null && overriddenPos == null)
        {
            return EnumActionResult.PASS;
        }

        if (overriddenPos != null || (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK))
        {
            RayTraceResult traceVanilla = fi.dy.masa.malilib.util.RayTraceUtils.getRayTraceFromEntity(mc.world, entity, RayTraceFluidHandling.NONE, false, reach);
            BlockPos pos;
            Vec3d hitPos;
            EnumFacing sideOrig;

            if (traceWrapper != null)
            {
                RayTraceResult trace = traceWrapper.getRayTraceResult();
                pos = trace.getBlockPos();
                hitPos = trace.hitVec;
                sideOrig = trace.sideHit;
            }
            else
            {
                RayTraceResult trace = RayTraceUtils.traceToPositions(Arrays.asList(overriddenPos), entity, reach);
                pos = overriddenPos;

                if (trace != null)
                {
                    hitPos = trace.hitVec;
                    sideOrig = trace.sideHit;
                }
                else
                {
                    hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    sideOrig = EnumFacing.UP;
                }
            }

            BlockPos clickPos = pos;
            World world = SchematicWorldHandler.getSchematicWorld();
            IBlockState stateSchematic = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // Already placed to that position, possible server sync delay
            if (easyPlaceIsPositionCached(pos))
            {
                return EnumActionResult.FAIL;
            }

            if (stack.isEmpty() == false)
            {
                IBlockState stateClient = mc.world.getBlockState(pos).getActualState(mc.world, pos);

                if (stateSchematic == stateClient)
                {
                    return EnumActionResult.FAIL;
                }

                // Abort if there is already a block in the target position
                if (easyPlaceBlockChecksCancel(stateSchematic, stateClient, mc.world, pos))
                {
                    return EnumActionResult.FAIL;
                }

                // Abort if the required item was not able to be pick-block'd
                if (InventoryUtils.doPickBlockForStack(stack, mc) == false)
                {
                    return EnumActionResult.FAIL;
                }

                EnumHand hand = EntityUtils.getUsedHandForItem(mc.player, stack, true);

                // Abort if a wrong item is in the player's hand
                if (hand == null)
                {
                    return EnumActionResult.FAIL;
                }

                boolean foundClickPosition = false;

                if (Configs.Generic.EASY_PLACE_CLICK_ADJACENT.getBooleanValue())
                {
                    // If there is a block in the world right behind the targeted schematic block, then use
                    // that block as the click position
                    if (traceVanilla != null && traceVanilla.typeOfHit == RayTraceResult.Type.BLOCK)
                    {
                        BlockPos posVanilla = traceVanilla.getBlockPos();
                        IBlockState stateVanilla = mc.world.getBlockState(posVanilla);

                        if (stateVanilla.getBlock().isReplaceable(mc.world, posVanilla) == false &&
                            stateVanilla.getMaterial().isReplaceable() == false &&
                            pos.equals(posVanilla.offset(traceVanilla.sideHit)))
                        {
                            hitPos = traceVanilla.hitVec;
                            sideOrig = traceVanilla.sideHit;
                            clickPos = posVanilla;
                            foundClickPosition = true;
                        }
                    }

                    if (foundClickPosition == false)
                    {
                        for (EnumFacing side : fi.dy.masa.malilib.util.PositionUtils.ALL_DIRECTIONS)
                        {
                            BlockPos posSide = pos.offset(side);
                            IBlockState stateSide = mc.world.getBlockState(posSide);

                            if (stateSide.getBlock().isReplaceable(mc.world, posSide) == false &&
                                stateSide.getMaterial().isReplaceable() == false)
                            {
                                hitPos = new Vec3d(posSide.getX() + 0.5, posSide.getY() + 0.5, posSide.getZ() + 0.5);
                                sideOrig = side.getOpposite();
                                clickPos = posSide;
                                foundClickPosition = true;
                                break;
                            }
                        }
                    }

                    if (foundClickPosition == false)
                    {
                        return EnumActionResult.FAIL;
                    }
                }

                // Mark that this position has been handled (use the non-offset position that is checked above)
                cacheEasyPlacePosition(pos);

                EnumFacing side = sideOrig;

                if (foundClickPosition == false || stateSchematic.getBlock() instanceof BlockSlab)
                {
                    side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                }

                // Fluid _blocks_ are not replaceable... >_>
                if (foundClickPosition == false &&
                    stateClient.getBlock().isReplaceable(mc.world, pos) == false &&
                    stateClient.getMaterial().isLiquid())
                {
                    clickPos = clickPos.offset(side, -1);
                }

                // Carpet Accurate Placement protocol support, plus BlockSlab support
                hitPos = applyCarpetProtocolHitVec(clickPos, stateSchematic, hitPos);

                //System.out.printf("pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                if (mc.playerController.processRightClickBlock(mc.player, mc.world, clickPos, side, hitPos, hand) == EnumActionResult.SUCCESS)
                {
                    mc.player.swingArm(hand);
                    mc.entityRenderer.itemRenderer.resetEquippedProgress(hand);
                }

                if (stateSchematic.getBlock() instanceof BlockSlab && ((BlockSlab) stateSchematic.getBlock()).isDouble())
                {
                    stateClient = mc.world.getBlockState(pos).getActualState(mc.world, pos);

                    if (stateClient.getBlock() instanceof BlockSlab && ((BlockSlab) stateClient.getBlock()).isDouble() == false)
                    {
                        side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        //System.out.printf("slab - pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                        mc.playerController.processRightClickBlock(mc.player, mc.world, pos, side, hitPos, hand);
                    }
                }
            }

            return EnumActionResult.SUCCESS;
        }
        else if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA)
        {
            return placementRestrictionInEffect(mc) ? EnumActionResult.FAIL : EnumActionResult.PASS;
        }

        return EnumActionResult.PASS;
    }

    private static boolean easyPlaceBlockChecksCancel(IBlockState stateSchematic, IBlockState stateClient, World worldClient, BlockPos pos)
    {
        Block blockSchematic = stateSchematic.getBlock();

        if (blockSchematic instanceof BlockSlab && ((BlockSlab) blockSchematic).isDouble())
        {
            if (stateClient.getBlock() instanceof BlockSlab && ((BlockSlab) stateClient.getBlock()).isDouble() == false)
            {
                IProperty<?> propSchematic = ((BlockSlab) stateSchematic.getBlock()).getVariantProperty();
                IProperty<?> propClient = ((BlockSlab) stateClient.getBlock()).getVariantProperty();

                return propSchematic != propClient || stateSchematic.getValue(propSchematic) != stateClient.getValue(propClient);
            }
        }

        if (stateClient.getBlock().isReplaceable(worldClient, pos) == false &&
            stateClient.getMaterial().isLiquid() == false)
        {
            return true;
        }

        return false;
    }

    public static Vec3d applyCarpetProtocolHitVec(BlockPos pos, IBlockState state, Vec3d hitVecIn)
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
        else if (block instanceof BlockSlab && ((BlockSlab) block).isDouble() == false)
        {
            //x += 10; // Doesn't actually exist (yet?)

            // Do it via vanilla
            if (state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP)
            {
                y = pos.getY() + 0.9;
            }
            else
            {
                y = pos.getY();
            }
        }

        return new Vec3d(x, y, z);
    }

    private static EnumFacing applyPlacementFacing(IBlockState stateSchematic, EnumFacing side, IBlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if (blockSchematic instanceof BlockSlab)
        {
            if (((BlockSlab) blockSchematic).isDouble() &&
                blockClient instanceof BlockSlab &&
                ((BlockSlab) blockClient).isDouble() == false)
            {
                if (stateClient.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP)
                {
                    return EnumFacing.DOWN;
                }
                else
                {
                    return EnumFacing.UP;
                }
            }
            // Single slab
            else
            {
                return EnumFacing.NORTH;
            }
        }
        else
        {
            PropertyDirection prop = BlockUtils.getFirstDirectionProperty(stateSchematic);

            if (prop != null)
            {
                side = stateSchematic.getValue(prop).getOpposite();
            }
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
