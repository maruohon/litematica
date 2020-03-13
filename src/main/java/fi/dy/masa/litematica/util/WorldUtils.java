package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.values.InfoType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.systems.BlockPlacementPositionHandler;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;

public class WorldUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static boolean preventBlockUpdates;

    public static boolean shouldPreventBlockUpdates()
    {
        return preventBlockUpdates;
    }

    public static void setShouldPreventBlockUpdates(boolean prevent)
    {
        preventBlockUpdates = prevent;
    }

    public static void loadChunksClientWorld(WorldClient world, BlockPos origin, Vec3i areaSize)
    {
        BlockPos posEnd = origin.add(PositionUtils.getRelativeEndPositionFromAreaSize(areaSize));
        BlockPos posMin = PositionUtils.getMinCorner(origin, posEnd);
        BlockPos posMax = PositionUtils.getMaxCorner(origin, posEnd);
        final int cxMin = posMin.getX() >> 4;
        final int czMin = posMin.getZ() >> 4;
        final int cxMax = posMax.getX() >> 4;
        final int czMax = posMax.getZ() >> 4;

        for (int cz = czMin; cz <= czMax; ++cz)
        {
            for (int cx = cxMin; cx <= cxMax; ++cx)
            {
                world.getChunkProvider().loadChunk(cx, cz);
            }
        }
    }

    public static void setToolModeBlockState(ToolMode mode, boolean primary, Minecraft mc)
    {
        IBlockState state = Blocks.AIR.getDefaultState();
        double reach = mc.playerController.getBlockReachDistance();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(mc.world, entity, reach, true);

        if (wrapper != null)
        {
            RayTraceResult trace = wrapper.getRayTraceResult();

            if (trace != null)
            {
                BlockPos pos = trace.getBlockPos();

                if (wrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
                {
                    state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
                }
                else if (wrapper.getHitType() == HitType.VANILLA)
                {
                    state = mc.world.getBlockState(pos).getActualState(mc.world, pos);
                }
            }
        }

        if (primary)
        {
            mode.setPrimaryBlock(state);
        }
        else
        {
            mode.setSecondaryBlock(state);
        }
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or the furthest hit block.
     * @param adjacentOnly whether to only accept traced schematic world position that are adjacent to a client world block, ie. normally placeable
     * @param mc
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean pickBlockFirst(Minecraft mc)
    {
        double reach = mc.playerController.getBlockReachDistance();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        BlockPos pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.world, entity, reach);

        if (pos != null)
        {
            doPickBlockForPosition(pos, mc);
            return true;
        }

        return false;
    }

    public static boolean pickBlockLast(boolean adjacentOnly, Minecraft mc)
    {
        BlockPos pos = BlockPlacementPositionHandler.INSTANCE.getCurrentPlacementPosition();

        // No overrides by other mods
        if (pos == null)
        {
            double reach = mc.playerController.getBlockReachDistance();
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            pos = RayTraceUtils.getPickBlockLastTrace(mc.world, entity, reach, adjacentOnly);
        }

        if (pos != null)
        {
            IBlockState state = mc.world.getBlockState(pos);

            if (state.getBlock().isReplaceable(mc.world, pos) || state.getMaterial().isReplaceable())
            {
                doPickBlockForPosition(pos, mc);
                return true;
            }
        }

        return false;
    }

    private static void doPickBlockForPosition(BlockPos pos, Minecraft mc)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        IBlockState state = world.getBlockState(pos);
        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
        EntityPlayer player = mc.player;
        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();

        if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(player, stack, ignoreNbt) == null)
        {
            if (mc.player.capabilities.isCreativeMode)
            {
                TileEntity te = world.getTileEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                if (GuiBase.isCtrlDown() && te != null && mc.world.isAirBlock(pos))
                {
                    stack = stack.copy();
                    ItemUtils.storeTEInStack(stack, te);
                }
            }

            InventoryUtils.switchItemToHand(stack, ignoreNbt, mc);
        }

        EnumHand hand = EntityUtils.getUsedHandForItem(player, stack, ignoreNbt);

        if (hand != null)
        {
            fi.dy.masa.malilib.util.InventoryUtils.preRestockHand(player, hand, 6, true);
        }
    }

    public static void insertSignTextFromSchematic(TileEntitySign teClient)
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic != null)
        {
            TileEntity te = worldSchematic.getTileEntity(teClient.getPos());

            if (te instanceof TileEntitySign)
            {
                ITextComponent[] textSchematic = ((TileEntitySign) te).signText;
                ITextComponent[] textClient = teClient.signText;

                if (textClient != null && textSchematic != null)
                {
                    int size = Math.min(textSchematic.length, textClient.length);

                    for (int i = 0; i < size; ++i)
                    {
                        if (textSchematic[i] != null)
                        {
                            textClient[i] = textSchematic[i].createCopy();
                        }
                    }
                }
            }
        }
    }

    public static void easyPlaceOnUseTick(Minecraft mc)
    {
        if (mc.player != null &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld() &&
            KeybindMulti.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode()))
        {
            WorldUtils.doEasyPlaceAction(mc);
        }
    }

    public static boolean handleEasyPlace(Minecraft mc)
    {
        EnumActionResult result = doEasyPlaceAction(mc);

        if (result == EnumActionResult.FAIL)
        {
            InfoUtils.showMessage((InfoType) Configs.InfoOverlays.EASY_PLACE_WARNINGS.getOptionListValue(), MessageType.WARNING, 1000, "litematica.message.easy_place_fail");
            return true;
        }

        return result != EnumActionResult.PASS;
    }

    private static EnumActionResult doEasyPlaceAction(Minecraft mc)
    {
        double reach = mc.playerController.getBlockReachDistance();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, reach, true);

        if (traceWrapper == null)
        {
            return EnumActionResult.PASS;
        }

        if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            RayTraceResult trace = traceWrapper.getRayTraceResult();
            RayTraceResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(mc.world, entity, false, reach);
            BlockPos pos = trace.getBlockPos();
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
                if (pickBlockFirst(mc) == false)
                {
                    return EnumActionResult.FAIL;
                }

                EnumHand hand = EntityUtils.getUsedHandForItem(mc.player, stack, true);

                // Abort if a wrong item is in the player's hand
                if (hand == null)
                {
                    return EnumActionResult.FAIL;
                }

                Vec3d hitPos = trace.hitVec;
                EnumFacing sideOrig = trace.sideHit;

                // If there is a block in the world right behind the targeted schematic block, then use
                // that block as the click position
                if (traceVanilla != null && traceVanilla.typeOfHit == RayTraceResult.Type.BLOCK)
                {
                    BlockPos posVanilla = traceVanilla.getBlockPos();
                    IBlockState stateVanilla = mc.world.getBlockState(posVanilla);

                    if (stateVanilla.getBlock().isReplaceable(mc.world, posVanilla) == false)
                    {
                        posVanilla = posVanilla.offset(traceVanilla.sideHit);

                        if (pos.equals(posVanilla))
                        {
                            hitPos = traceVanilla.hitVec;
                            sideOrig = traceVanilla.sideHit;
                        }
                    }
                }

                EnumFacing side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);

                // Carpet Accurate Placement protocol support, plus BlockSlab support
                hitPos = applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);

                // Mark that this position has been handled (use the non-offset position that is checked above)
                cacheEasyPlacePosition(pos);

                // Fluid _blocks_ are not replaceable... >_>
                if (stateClient.getBlock().isReplaceable(mc.world, pos) == false &&
                    stateClient.getMaterial().isLiquid())
                {
                    pos = pos.offset(side, -1);
                }

                //System.out.printf("pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                mc.playerController.processRightClickBlock(mc.player, mc.world, pos, side, hitPos, hand);

                if (stateSchematic.getBlock() instanceof BlockSlab && ((BlockSlab) stateSchematic.getBlock()).isDouble())
                {
                    stateClient = mc.world.getBlockState(pos).getActualState(mc.world, pos);

                    if (stateClient.getBlock() instanceof BlockSlab && ((BlockSlab) stateClient.getBlock()).isDouble() == false)
                    {
                        side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        mc.playerController.processRightClickBlock(mc.player, mc.world, pos, side, hitPos, hand);
                    }
                }
            }

            return EnumActionResult.SUCCESS;
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA)
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
            InfoUtils.showMessage((InfoType) Configs.InfoOverlays.EASY_PLACE_WARNINGS.getOptionListValue(), MessageType.WARNING, "litematica.message.placement_restriction_fail");
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
        RayTraceResult trace = RayTraceUtils.getRayTraceFromEntity(mc.world, entity, false, mc.playerController.getBlockReachDistance());

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
            if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(mc.player, stack, true) == null)
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

    /**
     * Checks if the given one block thick slice has non-air blocks or not.
     * NOTE: The axis is the perpendicular axis (that goes through the plane).
     * @param axis
     * @param pos1
     * @param pos2
     * @return
     */
    public static boolean isSliceEmpty(World world, EnumFacing.Axis axis, BlockPos pos1, BlockPos pos2)
    {
        switch (axis)
        {
            case Z:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int z = pos1.getZ();
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);

                for (int cx = cxMin; cx <= cxMax; ++cx)
                {
                    Chunk chunk = world.getChunk(cx, z >> 4);
                    int xMin = Math.max(x1,  cx << 4      );
                    int xMax = Math.min(x2, (cx << 4) + 15);
                    int yMax = Math.min(y2, chunk.getTopFilledSegment() + 15);

                    for (int x = xMin; x <= xMax; ++x)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(x, y, z).getMaterial() != Material.AIR)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }

            case Y:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y = pos1.getY();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    for (int cx = cxMin; cx <= cxMax; ++cx)
                    {
                        Chunk chunk = world.getChunk(cx, cz);

                        if (y > chunk.getTopFilledSegment() + 15)
                        {
                            continue;
                        }

                        int xMin = Math.max(x1,  cx << 4      );
                        int xMax = Math.min(x2, (cx << 4) + 15);
                        int zMin = Math.max(z1,  cz << 4      );
                        int zMax = Math.min(z2, (cz << 4) + 15);

                        for (int z = zMin; z <= zMax; ++z)
                        {
                            for (int x = xMin; x <= xMax; ++x)
                            {
                                if (chunk.getBlockState(x, y, z).getMaterial() != Material.AIR)
                                {
                                    return false;
                                }
                            }
                        }
                    }
                }

                break;
            }

            case X:
            {
                int x = pos1.getX();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    Chunk chunk = world.getChunk(x >> 4, cz);
                    int zMin = Math.max(z1,  cz << 4      );
                    int zMax = Math.min(z2, (cz << 4) + 15);
                    int yMax = Math.min(y2, chunk.getTopFilledSegment() + 15);

                    for (int z = zMin; z <= zMax; ++z)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(x, y, z).getMaterial() != Material.AIR)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }
        }

        return true;
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
