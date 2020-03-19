package fi.dy.masa.litematica.schematic.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBlockSpecial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.IMixinItemBlockSpecial;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.ISchematicRegion;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.malilib.util.SubChunkPos;

public class SchematicEditUtils
{
    public static boolean rebuildHandleBlockBreak(Minecraft mc)
    {
        if (mc.player != null &&
            DataManager.getToolMode() == ToolMode.REBUILD &&
            RenderUtils.areSchematicBlocksCurrentlyRendered())
        {
            if (Hotkeys.SCHEMATIC_REBUILD_BREAK_DIRECTION.getKeybind().isKeybindHeld())
            {
                return breakSchematicBlocks(mc);
            }
            else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_ALL.getKeybind().isKeybindHeld())
            {
                return breakAllIdenticalSchematicBlocks(mc);
            }
            else
            {
                return breakSchematicBlock(mc);
            }
        }

        return false;
    }

    public static boolean rebuildHandleBlockPlace(Minecraft mc)
    {
        if (mc.player != null &&
            DataManager.getToolMode() == ToolMode.REBUILD &&
            RenderUtils.areSchematicBlocksCurrentlyRendered())
        {
            if (Hotkeys.SCHEMATIC_REBUILD_REPLACE_DIRECTION.getKeybind().isKeybindHeld())
            {
                return replaceSchematicBlocksInDirection(mc);
            }
            else if (Hotkeys.SCHEMATIC_REBUILD_REPLACE_ALL.getKeybind().isKeybindHeld())
            {
                return replaceAllIdenticalSchematicBlocks(mc);
            }
            else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_DIRECTION.getKeybind().isKeybindHeld())
            {
                return placeSchematicBlocksInDirection(mc);
            }
            else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_ALL.getKeybind().isKeybindHeld())
            {
                return fillAirWithBlocks(mc);
            }
            else
            {
                return placeSchematicBlock(mc);
            }
        }

        return false;
    }

    private static boolean breakSchematicBlock(Minecraft mc)
    {
        return setTargetedSchematicBlockState(mc, Blocks.AIR.getDefaultState());
    }

    private static boolean placeSchematicBlock(Minecraft mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            BlockPos pos = info.pos.offset(info.side);
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

            if (DataManager.getRenderLayerRange().isPositionWithinRange(pos) &&
                world != null && world.isAirBlock(pos))
            {
                return setTargetedSchematicBlockState(pos, info.stateNew);
            }
        }

        return true;
    }

    private static boolean replaceSchematicBlocksInDirection(Minecraft mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            EnumFacing playerFacingH = mc.player.getHorizontalFacing();
            EnumFacing direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);

            // Center region
            if (direction == info.side)
            {
                direction = direction.getOpposite();
            }

            BlockPos posEnd = getReplacementBoxEndPos(info.pos, direction);
            return setSchematicBlockStates(info.pos, posEnd, info.stateNew);
        }

        return false;
    }

    private static boolean replaceAllIdenticalSchematicBlocks(Minecraft mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            return setAllIdenticalSchematicBlockStates(info.pos, info.stateOriginal, info.stateNew);
        }

        return false;
    }

    private static boolean breakSchematicBlocks(Minecraft mc)
    {
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, entity, 20);

        if (wrapper != null)
        {
            RayTraceResult trace = wrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();
            EnumFacing playerFacingH = mc.player.getHorizontalFacing();
            EnumFacing direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(trace.sideHit, playerFacingH, pos, trace.hitVec);

            // Center region
            if (direction == trace.sideHit)
            {
                direction = direction.getOpposite();
            }

            BlockPos posEnd = getReplacementBoxEndPos(pos, direction);

            return setSchematicBlockStates(pos, posEnd, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    private static boolean breakAllIdenticalSchematicBlocks(Minecraft mc)
    {
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, entity, 20);

        // The state can be null in 1.13+
        if (wrapper != null)
        {
            RayTraceResult trace = wrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();
            IBlockState stateOriginal = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);

            return setAllIdenticalSchematicBlockStates(pos, stateOriginal, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    private static boolean placeSchematicBlocksInDirection(Minecraft mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            EnumFacing playerFacingH = mc.player.getHorizontalFacing();
            EnumFacing direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);
            BlockPos posStart = info.pos.offset(info.side); // offset to the adjacent air block

            if (SchematicWorldHandler.getSchematicWorld().getBlockState(posStart).getMaterial() == Material.AIR)
            {
                BlockPos posEnd = getReplacementBoxEndPos(posStart, direction);
                return setSchematicBlockStates(posStart, posEnd, info.stateNew);
            }
        }

        return false;
    }

    private static boolean fillAirWithBlocks(Minecraft mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            BlockPos posStart = info.pos.offset(info.side); // offset to the adjacent air block

            if (SchematicWorldHandler.getSchematicWorld().getBlockState(posStart).getMaterial() == Material.AIR)
            {
                return setAllIdenticalSchematicBlockStates(posStart, Blocks.AIR.getDefaultState(), info.stateNew);
            }
        }

        return false;
    }

    @Nullable
    private static ReplacementInfo getTargetInfo(Minecraft mc)
    {
        ItemStack stack = mc.player.getHeldItemMainhand();

        if ((stack.isEmpty() == false && (stack.getItem() instanceof ItemBlock || stack.getItem() instanceof ItemBlockSpecial)) ||
            (stack.isEmpty() && ToolMode.REBUILD.getPrimaryBlock() != null))
        {
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 20, true);

            if (world != null && traceWrapper != null &&
                traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
            {
                RayTraceResult trace = traceWrapper.getRayTraceResult();
                EnumFacing side = trace.sideHit;
                Vec3d hitVec = trace.hitVec;
                int meta = stack.getItem().getMetadata(stack.getMetadata());
                BlockPos pos = trace.getBlockPos();
                IBlockState stateOriginal = world.getBlockState(pos);
                IBlockState stateNew = Blocks.AIR.getDefaultState();

                if (stack.getItem() instanceof ItemBlock)
                {
                    stateNew = ((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(world, pos.offset(side),
                                    side, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, meta, mc.player);
                }
                else if (stack.getItem() instanceof ItemBlockSpecial)
                {
                    stateNew = ((IMixinItemBlockSpecial) stack.getItem()).getBlock().getStateForPlacement(world, pos.offset(side),
                                    side, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, 0, mc.player);
                }
                else if (ToolMode.REBUILD.getPrimaryBlock() != null)
                {
                    stateNew = ToolMode.REBUILD.getPrimaryBlock();
                }

                return new ReplacementInfo(pos, side, hitVec, stateOriginal, stateNew);
            }
        }

        return null;
    }

    private static BlockPos getReplacementBoxEndPos(BlockPos startPos, EnumFacing direction)
    {
        return getReplacementBoxEndPos(startPos, direction, 10000);
    }

    private static BlockPos getReplacementBoxEndPos(BlockPos startPos, EnumFacing direction, int maxBlocks)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        LayerRange range = DataManager.getRenderLayerRange();
        IBlockState stateStart = world.getBlockState(startPos);
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(startPos);

        while (maxBlocks-- > 0)
        {
            posMutable.move(direction);

            if (range.isPositionWithinRange(posMutable) == false ||
                world.getChunkProvider().isChunkGeneratedAt(posMutable.getX() >> 4, posMutable.getZ() >> 4) == false ||
                world.getBlockState(posMutable) != stateStart)
            {
                posMutable.move(direction.getOpposite());
                break;
            }
        }

        return posMutable.toImmutable();
    }

    private static boolean setTargetedSchematicBlockState(Minecraft mc, IBlockState state)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 20, true);

        if (world != null && traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            RayTraceResult trace = traceWrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();
            return setTargetedSchematicBlockState(pos, state);
        }

        return false;
    }

    private static boolean setTargetedSchematicBlockState(BlockPos pos, IBlockState state)
    {
        if (pos != null)
        {
            SubChunkPos cpos = new SubChunkPos(pos);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().containsPos(pos))
                    {
                        SchematicPlacement placement = part.getPlacement();
                        ISchematic schematic = placement.getSchematic();
                        String regionName = part.getSubRegionName();
                        ISchematicRegion region = schematic.getSchematicRegion(regionName);

                        if (region == null)
                        {
                            continue;
                        }

                        ILitematicaBlockStateContainer container = region.getBlockStateContainer();
                        BlockPos posSchematic = SchematicUtils.getSchematicContainerPositionFromWorldPosition(pos, schematic,
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);

                        if (posSchematic != null)
                        {
                            state = SchematicUtils.getUntransformedBlockState(state, placement, regionName);

                            IBlockState stateOriginal = container.getBlockState(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ());

                            SchematicMetadata metadata = schematic.getMetadata();
                            long totalBlocks = metadata.getTotalBlocks();
                            long increment = 0;

                            if (stateOriginal.getBlock() != Blocks.AIR)
                            {
                                increment = state.getBlock() != Blocks.AIR ? 0 : -1;
                            }
                            else
                            {
                                increment = state.getBlock() != Blocks.AIR ? 1 : 0;
                            }

                            totalBlocks += increment;

                            container.setBlockState(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ(), state);

                            metadata.setTotalBlocks(totalBlocks);
                            metadata.setTimeModifiedToNow();
                            metadata.setModifiedSinceSaved();

                            DataManager.getSchematicPlacementManager().markChunkForRebuild(new ChunkPos(cpos.getX(), cpos.getZ()));

                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setSchematicBlockStates(BlockPos posStart, BlockPos posEnd, IBlockState state)
    {
        if (posStart != null && posEnd != null)
        {
            SubChunkPos cpos = new SubChunkPos(posStart);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().containsPos(posStart))
                    {
                        String regionName = part.getSubRegionName();
                        SchematicPlacement schematicPlacement = part.getPlacement();
                        SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);
                        ISchematic schematic = schematicPlacement.getSchematic();
                        ISchematicRegion region = schematic.getSchematicRegion(regionName);

                        if (region == null)
                        {
                            continue;
                        }

                        ILitematicaBlockStateContainer container = region.getBlockStateContainer();
                        BlockPos posStartSchematic = SchematicUtils.getSchematicContainerPositionFromWorldPosition(posStart, schematic,
                                regionName, schematicPlacement, placement, container);
                        BlockPos posEndSchematic = SchematicUtils.getSchematicContainerPositionFromWorldPosition(posEnd, schematic,
                                regionName, schematicPlacement, placement, container);

                        if (posStartSchematic != null && posEndSchematic != null)
                        {
                            BlockPos posMin = PositionUtils.getMinCorner(posStartSchematic, posEndSchematic);
                            BlockPos posMax = PositionUtils.getMaxCorner(posStartSchematic, posEndSchematic);
                            final int minX = Math.max(posMin.getX(), 0);
                            final int minY = Math.max(posMin.getY(), 0);
                            final int minZ = Math.max(posMin.getZ(), 0);
                            final int maxX = Math.min(posMax.getX(), container.getSize().getX() - 1);
                            final int maxY = Math.min(posMax.getY(), container.getSize().getY() - 1);
                            final int maxZ = Math.min(posMax.getZ(), container.getSize().getZ() - 1);
                            long totalBlocks = schematic.getMetadata().getTotalBlocks();
                            long increment = 0;

                            state = SchematicUtils.getUntransformedBlockState(state, schematicPlacement, regionName);

                            for (int y = minY; y <= maxY; ++y)
                            {
                                for (int z = minZ; z <= maxZ; ++z)
                                {
                                    for (int x = minX; x <= maxX; ++x)
                                    {
                                        IBlockState stateOriginal = container.getBlockState(x, y, z);

                                        if (stateOriginal.getBlock() != Blocks.AIR)
                                        {
                                            increment = state.getBlock() != Blocks.AIR ? 0 : -1;
                                        }
                                        else
                                        {
                                            increment = state.getBlock() != Blocks.AIR ? 1 : 0;
                                        }

                                        totalBlocks += increment;

                                        container.setBlockState(x, y, z, state);
                                    }
                                }
                            }

                            SchematicMetadata metadata = schematic.getMetadata();
                            metadata.setTotalBlocks(totalBlocks);
                            metadata.setTimeModifiedToNow();
                            metadata.setModifiedSinceSaved();

                            DataManager.getSchematicPlacementManager().markAllPlacementsOfSchematicForRebuild(schematic);

                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setAllIdenticalSchematicBlockStates(BlockPos posStart, IBlockState stateOriginal, IBlockState stateNew)
    {
        if (posStart != null)
        {
            SubChunkPos cpos = new SubChunkPos(posStart);
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            List<PlacementPart> list = manager.getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().containsPos(posStart))
                    {
                        if (replaceAllIdenticalBlocks(manager, part, stateOriginal, stateNew))
                        {
                            manager.markAllPlacementsOfSchematicForRebuild(part.getPlacement().getSchematic());
                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean replaceAllIdenticalBlocks(SchematicPlacementManager manager, PlacementPart part,
            IBlockState stateOriginalIn, IBlockState stateNewIn)
    {
        SchematicPlacement schematicPlacement = part.getPlacement();
        ISchematic schematic = schematicPlacement.getSchematic();
        String selectedRegionName = schematicPlacement.getSelectedSubRegionName();
        List<String> regions = new ArrayList<>();

        // If there is a sub-region selected, check that it's the currently targeted region
        if (selectedRegionName != null)
        {
            if (selectedRegionName.equals(part.getSubRegionName()))
            {
                regions.add(selectedRegionName);
            }
            else
            {
                InfoUtils.showInGameMessage(MessageType.WARNING, 20000, "litematica.message.warn.schematic_rebuild.subregion_not_selected");
                return false;
            }
        }
        // No sub-regions selected, replace in the entire schematic
        else
        {
            regions.addAll(schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).keySet());
        }

        LayerRange range = DataManager.getRenderLayerRange();

        long totalBlocks = schematic.getMetadata().getTotalBlocks();
        long increment = 0;

        if (stateOriginalIn.getBlock() != Blocks.AIR)
        {
            increment = stateNewIn.getBlock() != Blocks.AIR ? 0 : -1;
        }
        else
        {
            increment = stateNewIn.getBlock() != Blocks.AIR ? 1 : 0;
        }

        for (String regionName : regions)
        {
            ISchematicRegion region = schematic.getSchematicRegion(regionName);

            if (region == null)
            {
                continue;
            }

            SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);
            ILitematicaBlockStateContainer container = region.getBlockStateContainer();
            Vec3i regionSize = region.getSize();

            if (container == null || placement == null || regionSize == null)
            {
                continue;
            }

            Pair<Vec3i, Vec3i> pair = SchematicUtils.getLayerRangeClampedSubRegion(range, schematicPlacement, placement, regionSize);

            if (pair == null)
            {
                return false;
            }

            Vec3i containerStart = pair.getLeft();
            Vec3i containerEnd = pair.getRight();
            Vec3i size = container.getSize();

            final int startX = containerStart.getX();
            final int startY = containerStart.getY();
            final int startZ = containerStart.getZ();
            final int endX = containerEnd.getX();
            final int endY = containerEnd.getY();
            final int endZ = containerEnd.getZ();

            //System.out.printf("DEBUG == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
            //        regionName, startX, startY, startZ, endX, endY, endZ, container.getSize().getX(), container.getSize().getY(), container.getSize().getZ());

            if (startX < 0 || startY < 0 || startZ < 0 ||
                endX >= size.getX() ||
                endY >= size.getY() ||
                endZ >= size.getZ())
            {
                System.out.printf("OUT OF BOUNDS == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
                        regionName, startX, startY, startZ, endX, endY, endZ, size.getX(), size.getY(), size.getZ());
                return false;
            }

            //System.out.printf("DEBUG == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
            //        regionName, startX, startY, startZ, endX, endY, endZ, size.getX(), size.getY(), size.getZ());

            IBlockState stateOriginal = SchematicUtils.getUntransformedBlockState(stateOriginalIn, schematicPlacement, regionName);
            IBlockState stateNew = SchematicUtils.getUntransformedBlockState(stateNewIn, schematicPlacement, regionName);

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        if (container.getBlockState(x, y, z) == stateOriginal)
                        {
                            container.setBlockState(x, y, z, stateNew);
                            totalBlocks += increment;
                        }
                    }
                }
            }
        }

        SchematicMetadata metadata = schematic.getMetadata();
        metadata.setTotalBlocks(totalBlocks);
        metadata.setTimeModifiedToNow();
        metadata.setModifiedSinceSaved();

        return true;
    }

    private static class ReplacementInfo
    {
        public final BlockPos pos;
        public final EnumFacing side;
        public final Vec3d hitVec;
        public final IBlockState stateOriginal;
        public final IBlockState stateNew;

        public ReplacementInfo(BlockPos pos, EnumFacing side, Vec3d hitVec, IBlockState stateOriginal, IBlockState stateNew)
        {
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.stateOriginal = stateOriginal;
            this.stateNew = stateNew;
        }
    }
}
