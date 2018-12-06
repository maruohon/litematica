package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicVerifier;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceFluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.World;

public class RayTraceUtils
{
    private static RayTraceWrapper closestBox;
    private static RayTraceWrapper closestCorner;
    private static RayTraceWrapper closestOrigin;
    private static double closestBoxDistance;
    private static double closestCornerDistance;
    private static double closestOriginDistance;
    private static HitType originType;

    @Nonnull
    public static RayTraceWrapper getWrappedRayTraceFromEntity(World world, Entity entity, double range)
    {
        Vec3d eyesPos = entity.getEyePosition(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        RayTraceResult result = getRayTraceFromEntity(world, entity, false, range);
        double closestVanilla = result.type != RayTraceResult.Type.MISS ? result.hitVec.distanceTo(eyesPos) : -1D;

        AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();
        RayTraceWrapper wrapper = null;

        clearTraceVars();

        if (DataManager.getOperationMode().getUsesSchematic() == false && area != null)
        {
            for (Box box : area.getAllSubRegionBoxes())
            {
                boolean hitCorner = false;
                hitCorner |= traceToSelectionBoxCorner(box, Corner.CORNER_1, eyesPos, lookEndPos);
                hitCorner |= traceToSelectionBoxCorner(box, Corner.CORNER_2, eyesPos, lookEndPos);

                if (hitCorner == false)
                {
                    traceToSelectionBoxBody(box, eyesPos, lookEndPos);
                }
            }

            traceToOrigin(area.getOrigin(), eyesPos, lookEndPos, HitType.SELECTION_ORIGIN, null);
        }

        if (DataManager.getOperationMode().getUsesSchematic())
        {
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
            {
                traceToPlacementBox(placement, eyesPos, lookEndPos);
                traceToOrigin(placement.getOrigin(), eyesPos, lookEndPos, HitType.PLACEMENT_ORIGIN, placement);
            }
        }

        double closestDistance = closestVanilla;

        if (closestBoxDistance >= 0 && (closestVanilla < 0 || closestBoxDistance <= closestVanilla))
        {
            closestDistance = closestBoxDistance;
            wrapper = closestBox;
        }

        // Corners are preferred over box body hits, thus this being after the box check
        if (closestCornerDistance >= 0 && (closestVanilla < 0 || closestCornerDistance <= closestVanilla))
        {
            closestDistance = closestCornerDistance;
            wrapper = closestCorner;
        }

        // Origins are preferred over everything else
        if (closestOriginDistance >= 0 && (closestVanilla < 0 || closestOriginDistance <= closestVanilla))
        {
            closestDistance = closestOriginDistance;

            if (originType == HitType.PLACEMENT_ORIGIN)
            {
                wrapper = closestOrigin;
            }
            else
            {
                wrapper = new RayTraceWrapper(RayTraceWrapper.HitType.SELECTION_ORIGIN);
            }
        }

        clearTraceVars();

        if (wrapper == null || closestDistance < 0)
        {
            wrapper = new RayTraceWrapper();
        }

        return wrapper;
    }

    private static void clearTraceVars()
    {
        closestBox = null;
        closestCorner = null;
        closestOrigin = null;
        closestBoxDistance = -1D;
        closestCornerDistance = -1D;
        closestOriginDistance = -1D;
    }

    private static boolean traceToSelectionBoxCorner(Box box, Corner corner, Vec3d start, Vec3d end)
    {
        BlockPos pos = (corner == Corner.CORNER_1) ? box.getPos1() : (corner == Corner.CORNER_2) ? box.getPos2() : null;

        if (pos != null)
        {
            AxisAlignedBB bb = PositionUtils.createAABBForPosition(pos);
            RayTraceResult hit = bb.calculateIntercept(start, end);

            if (hit != null)
            {
                double dist = hit.hitVec.distanceTo(start);

                if (closestCornerDistance < 0 || dist < closestCornerDistance)
                {
                    closestCornerDistance = dist;
                    closestCorner = new RayTraceWrapper(box, corner, hit.hitVec);
                }

                return true;
            }
        }

        return false;
    }

    private static boolean traceToSelectionBoxBody(Box box, Vec3d start, Vec3d end)
    {
        if (box.getPos1() != null && box.getPos2() != null)
        {
            AxisAlignedBB bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
            RayTraceResult hit = bb.calculateIntercept(start, end);

            if (hit != null)
            {
                double dist = hit.hitVec.distanceTo(start);

                if (closestBoxDistance < 0 || dist < closestBoxDistance)
                {
                    closestBoxDistance = dist;
                    closestBox = new RayTraceWrapper(box, Corner.NONE, hit.hitVec);
                }

                return true;
            }
        }

        return false;
    }

    private static boolean traceToPlacementBox(SchematicPlacement placement, Vec3d start, Vec3d end)
    {
        ImmutableMap<String, Box> boxes = placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        boolean hitSomething = false;

        for (Map.Entry<String, Box> entry : boxes.entrySet())
        {
            String boxName = entry.getKey();
            Box box = entry.getValue();

            if (box.getPos1() != null && box.getPos2() != null)
            {
                AxisAlignedBB bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
                RayTraceResult trace = bb.calculateIntercept(start, end);

                if (trace != null)
                {
                    double dist = trace.hitVec.distanceTo(start);

                    if (closestBoxDistance < 0 || dist < closestBoxDistance)
                    {
                        closestBoxDistance = dist;
                        closestBox = new RayTraceWrapper(placement, trace.hitVec, boxName);
                        hitSomething = true;
                    }
                }
            }
        }

        return hitSomething;
    }

    private static boolean traceToOrigin(BlockPos pos, Vec3d start, Vec3d end, HitType type, @Nullable SchematicPlacement placement)
    {
        if (pos != null)
        {
            AxisAlignedBB bb = PositionUtils.createAABBForPosition(pos);
            RayTraceResult trace = bb.calculateIntercept(start, end);

            if (trace != null)
            {
                double dist = trace.hitVec.distanceTo(start);

                if (closestOriginDistance < 0 || dist < closestOriginDistance)
                {
                    closestOriginDistance = dist;
                    originType = type;

                    if (type == HitType.PLACEMENT_ORIGIN)
                    {
                        closestOrigin = new RayTraceWrapper(placement, trace.hitVec, null);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Ray traces to the closest position on the given list
     * @param posList
     * @param entity
     * @param range
     * @return
     */
    @Nullable
    public static RayTraceResult traceToPositions(List<BlockPos> posList, Entity entity, double range)
    {
        if (posList.isEmpty())
        {
            return null;
        }

        Vec3d eyesPos = entity.getEyePosition(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        double closest = -1D;
        RayTraceResult trace = null;

        for (BlockPos pos : posList)
        {
            if (pos != null)
            {
                AxisAlignedBB bb = PositionUtils.createAABBForPosition(pos);
                RayTraceResult hit = bb.calculateIntercept(eyesPos, lookEndPos);

                if (hit != null)
                {
                    double dist = hit.hitVec.distanceTo(eyesPos);

                    if (closest < 0 || dist < closest)
                    {
                        trace = new RayTraceResult(Type.BLOCK, hit.hitVec, hit.sideHit, pos);
                        closest = dist;
                    }
                }
            }
        }

        return trace;
    }

    @Nullable
    public static RayTraceResult traceToSchematicWorld(Entity entity, double range, boolean respectRenderRange)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world == null)
        {
            return null;
        }

        Vec3d eyesPos = entity.getEyePosition(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);
        RayTraceFluidMode fluidMode = RayTraceFluidMode.ALWAYS;

        return rayTraceBlocks(world, eyesPos, lookEndPos, fluidMode, false, true, respectRenderRange, 200);
    }

    @Nullable
    public static RayTraceWrapper getGenericTrace(World worldClient, Entity entity, double range, boolean respectRenderRange)
    {
        RayTraceResult traceClient = getRayTraceFromEntity(worldClient, entity, true, range);
        RayTraceResult traceSchematic = traceToSchematicWorld(entity, range, respectRenderRange);
        double distClosest = -1D;
        HitType type = HitType.MISS;
        Vec3d eyesPos = entity.getEyePosition(1f);
        RayTraceResult trace = null;

        if (traceSchematic != null && traceSchematic.type == RayTraceResult.Type.BLOCK)
        {
            double dist = eyesPos.squareDistanceTo(traceSchematic.hitVec);

            if (distClosest < 0 || dist < distClosest)
            {
                trace = traceSchematic;
                distClosest = eyesPos.squareDistanceTo(traceSchematic.hitVec);
                type = HitType.SCHEMATIC_BLOCK;
            }
        }

        if (traceClient != null && traceClient.type == RayTraceResult.Type.BLOCK)
        {
            double dist = eyesPos.squareDistanceTo(traceClient.hitVec);

            if (distClosest < 0 || dist < distClosest)
            {
                trace = traceClient;
                distClosest = dist;
                type = HitType.VANILLA;
            }
        }

        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier())
        {
            SchematicVerifier verifier = placement.getSchematicVerifier();
            List<BlockPos> posList = verifier.getSelectedMismatchPositionsForRender();
            RayTraceResult traceMismatch = traceToPositions(posList, entity, range);

            // Mismatch overlay has priority over other hits
            if (traceMismatch != null)
            {
                trace = traceMismatch;
                type = HitType.MISMATCH_OVERLAY;
            }
        }

        if (trace != null)
        {
            return new RayTraceWrapper(type, trace);
        }

        return null;
    }

    @Nullable
    public static BlockPos getSchematicWorldTraceIfClosest(World worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getGenericTrace(worldClient, entity, range, true);

        if (trace != null && trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return trace.getRayTraceResult().getBlockPos();
        }

        return null;
    }

    @Nullable
    public static BlockPos getFurthestSchematicWorldTrace(World worldClient, Entity entity, double range)
    {
        Vec3d eyesPos = entity.getEyePosition(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        RayTraceResult result = getRayTraceFromEntity(worldClient, entity, false, range);
        final double closestVanilla = result.type != RayTraceResult.Type.MISS ? result.hitVec.distanceTo(eyesPos) : -1D;

        RayTraceFluidMode fluidMode = RayTraceFluidMode.NEVER;
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        List<RayTraceResult> list = rayTraceBlocksToList(worldSchematic, eyesPos, lookEndPos, fluidMode, false, false, true, 200);
        RayTraceResult furthestTrace = null;
        double furthestDist = -1D;

        if (list.isEmpty() == false)
        {
            for (RayTraceResult trace : list)
            {
                double dist = trace.hitVec.distanceTo(eyesPos);

                if ((furthestDist < 0 || dist > furthestDist) && (dist < closestVanilla || closestVanilla < 0))
                {
                    furthestDist = dist;
                    furthestTrace = trace;
                }
            }
        }

        return furthestTrace != null ? furthestTrace.getBlockPos() : null;
    }

    @Nonnull
    public static RayTraceResult getRayTraceFromEntity(World world, Entity entity, boolean useLiquids, double range)
    {
        Vec3d eyesPos = entity.getEyePosition(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);
        RayTraceFluidMode fluidMode = useLiquids ? RayTraceFluidMode.ALWAYS : RayTraceFluidMode.NEVER;

        RayTraceResult result = rayTraceBlocks(world, eyesPos, lookEndPos, fluidMode, false, false, false, 1000);

        if (result == null)
        {
            result = new RayTraceResult(RayTraceResult.Type.MISS, Vec3d.ZERO, EnumFacing.UP, BlockPos.ORIGIN);
        }

        AxisAlignedBB bb = entity.getBoundingBox().expand(rangedLookRot.x, rangedLookRot.y, rangedLookRot.z).expand(1d, 1d, 1d);
        List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(entity, bb);

        double closest = result.type == RayTraceResult.Type.BLOCK ? eyesPos.distanceTo(result.hitVec) : Double.MAX_VALUE;
        RayTraceResult entityTrace = null;
        Entity targetEntity = null;

        for (int i = 0; i < list.size(); i++)
        {
            Entity entityTmp = list.get(i);
            bb = entityTmp.getBoundingBox();
            RayTraceResult traceTmp = bb.calculateIntercept(lookEndPos, eyesPos);

            if (traceTmp != null)
            {
                double distance = eyesPos.distanceTo(traceTmp.hitVec);

                if (distance <= closest)
                {
                    targetEntity = entityTmp;
                    entityTrace = traceTmp;
                    closest = distance;
                }
            }
        }

        if (targetEntity != null)
        {
            result = new RayTraceResult(targetEntity, entityTrace.hitVec);
        }

        if (eyesPos.distanceTo(result.hitVec) > range)
        {
            result = new RayTraceResult(RayTraceResult.Type.MISS, Vec3d.ZERO, EnumFacing.UP, BlockPos.ORIGIN);
        }

        return result;
    }

    /**
     * Mostly copy pasted from World#rayTraceBlocks() except for the added maxSteps argument and the layer range check
     */
    @Nullable
    public static RayTraceResult rayTraceBlocks(World world, Vec3d start, Vec3d end,
            RayTraceFluidMode fluidMode, boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return null;
        }

        LayerRange range = DataManager.getRenderLayerRange();
        RayTraceCalcsData data = new RayTraceCalcsData(start, end, range, fluidMode);

        IBlockState blockState = world.getBlockState(data.blockPos);
        IFluidState fluidState = world.getFluidState(data.blockPos);

        RayTraceResult trace = traceFirstStep(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange);

        if (trace != null)
        {
            return trace;
        }

        while (--maxSteps >= 0)
        {
            if (rayTraceCalcs(data, returnLastUncollidableBlock, respectLayerRange))
            {
                return data.trace;
            }

            blockState = world.getBlockState(data.blockPos);
            fluidState = world.getFluidState(data.blockPos);

            if (traceLoopSteps(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange))
            {
                return data.trace;
            }
        }

        return returnLastUncollidableBlock ? data.trace : null;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private static RayTraceResult traceFirstStep(RayTraceCalcsData data,
            World world, IBlockState blockState, IFluidState fluidState,
            boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange)
    {
        if ((respectLayerRange == false || data.range.isPositionWithinRange(data.x, data.y, data.z)) &&
            (ignoreBlockWithoutBoundingBox == false || blockState.getCollisionShape(world, data.blockPos).isEmpty() == false))
        {
            boolean blockCollidable = blockState.getBlock().isCollidable(blockState);
            boolean fluidCollidable = data.fluidMode.predicate.test(fluidState);

            if (blockCollidable || fluidCollidable)
            {
                RayTraceResult trace = null;

                if (blockCollidable)
                {
                    trace = Block.collisionRayTrace(blockState, world, data.blockPos, data.currentPos, data.end);
                }

                if (trace == null && fluidCollidable)
                {
                    trace = VoxelShapes.create(0.0D, 0.0D, 0.0D, 1.0D, fluidState.getHeight(), 1.0D).func_212433_a(data.currentPos, data.end, data.blockPos);
                }

                if (trace != null)
                {
                    return trace;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private static boolean traceLoopSteps(RayTraceCalcsData data,
            World world, IBlockState blockState, IFluidState fluidState,
            boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange)
    {
        if ((respectLayerRange == false || data.range.isPositionWithinRange(data.x, data.y, data.z)) &&
            (ignoreBlockWithoutBoundingBox == false || blockState.getMaterial() == Material.PORTAL ||
             blockState.getCollisionShape(world, data.blockPos).isEmpty() == false))
        {
            boolean blockCollidable = blockState.getBlock().isCollidable(blockState);
            boolean fluidCollidable = data.fluidMode.predicate.test(fluidState);

            if (blockCollidable == false && fluidCollidable == false)
            {
                data.trace = new RayTraceResult(RayTraceResult.Type.MISS, data.currentPos, data.facing, data.blockPos);
            }
            else
            {
                RayTraceResult traceTmp = null;

                if (blockCollidable)
                {
                    traceTmp = Block.collisionRayTrace(blockState, world, data.blockPos, data.currentPos, data.end);
                }

                if (traceTmp == null && fluidCollidable)
                {
                    traceTmp = VoxelShapes.create(0.0D, 0.0D, 0.0D, 1.0D, fluidState.getHeight(), 1.0D).func_212433_a(data.currentPos, data.end, data.blockPos);
                }

                if (traceTmp != null)
                {
                    data.trace = traceTmp;
                    return true;
                }
            }
        }

        return false;
    }

    public static List<RayTraceResult> rayTraceBlocksToList(World world, Vec3d start, Vec3d end,
            RayTraceFluidMode fluidMode, boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return ImmutableList.of();
        }

        LayerRange range = DataManager.getRenderLayerRange();
        RayTraceCalcsData data = new RayTraceCalcsData(start, end, range, fluidMode);

        IBlockState blockState = world.getBlockState(data.blockPos);
        IFluidState fluidState = world.getFluidState(data.blockPos);

        RayTraceResult trace = traceFirstStep(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange);
        List<RayTraceResult> hits = new ArrayList<>();

        if (trace != null)
        {
            hits.add(trace);
        }

        while (--maxSteps >= 0)
        {
            if (rayTraceCalcs(data, returnLastUncollidableBlock, respectLayerRange))
            {
                if (data.trace != null)
                {
                    hits.add(data.trace);
                }

                return hits;
            }

            blockState = world.getBlockState(data.blockPos);
            fluidState = world.getFluidState(data.blockPos);

            if (traceLoopSteps(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange))
            {
                hits.add(data.trace);
            }
        }

        return hits;
    }

    private static boolean rayTraceCalcs(RayTraceCalcsData data, boolean returnLastNonCollidableBlock, boolean respectLayerRange)
    {
        boolean xDiffers = true;
        boolean yDiffers = true;
        boolean zDiffers = true;
        double nextX = 999.0D;
        double nextY = 999.0D;
        double nextZ = 999.0D;

        if (Double.isNaN(data.currentPos.x) || Double.isNaN(data.currentPos.y) || Double.isNaN(data.currentPos.z))
        {
            data.trace = null;
            return true;
        }

        if (data.x == data.xEnd && data.y == data.yEnd && data.z == data.zEnd)
        {
            if (returnLastNonCollidableBlock == false)
            {
                data.trace = null;
            }

            return true;
        }

        if (data.xEnd > data.x)
        {
            nextX = (double) data.x + 1.0D;
        }
        else if (data.xEnd < data.x)
        {
            nextX = (double) data.x + 0.0D;
        }
        else
        {
            xDiffers = false;
        }

        if (data.yEnd > data.y)
        {
            nextY = (double) data.y + 1.0D;
        }
        else if (data.yEnd < data.y)
        {
            nextY = (double) data.y + 0.0D;
        }
        else
        {
            yDiffers = false;
        }

        if (data.zEnd > data.z)
        {
            nextZ = (double) data.z + 1.0D;
        }
        else if (data.zEnd < data.z)
        {
            nextZ = (double) data.z + 0.0D;
        }
        else
        {
            zDiffers = false;
        }

        double d3 = 999.0D;
        double d4 = 999.0D;
        double d5 = 999.0D;
        double d6 = data.end.x - data.currentPos.x;
        double d7 = data.end.y - data.currentPos.y;
        double d8 = data.end.z - data.currentPos.z;

        if (xDiffers)
        {
            d3 = (nextX - data.currentPos.x) / d6;
        }

        if (yDiffers)
        {
            d4 = (nextY - data.currentPos.y) / d7;
        }

        if (zDiffers)
        {
            d5 = (nextZ - data.currentPos.z) / d8;
        }

        if (d3 == -0.0D)
        {
            d3 = -1.0E-4D;
        }

        if (d4 == -0.0D)
        {
            d4 = -1.0E-4D;
        }

        if (d5 == -0.0D)
        {
            d5 = -1.0E-4D;
        }

        if (d3 < d4 && d3 < d5)
        {
            data.facing = data.xEnd > data.x ? EnumFacing.WEST : EnumFacing.EAST;
            data.currentPos = new Vec3d(nextX, data.currentPos.y + d7 * d3, data.currentPos.z + d8 * d3);
        }
        else if (d4 < d5)
        {
            data.facing = data.yEnd > data.y ? EnumFacing.DOWN : EnumFacing.UP;
            data.currentPos = new Vec3d(data.currentPos.x + d6 * d4, nextY, data.currentPos.z + d8 * d4);
        }
        else
        {
            data.facing = data.zEnd > data.z ? EnumFacing.NORTH : EnumFacing.SOUTH;
            data.currentPos = new Vec3d(data.currentPos.x + d6 * d5, data.currentPos.y + d7 * d5, nextZ);
        }

        data.x = MathHelper.floor(data.currentPos.x) - (data.facing == EnumFacing.EAST ?  1 : 0);
        data.y = MathHelper.floor(data.currentPos.y) - (data.facing == EnumFacing.UP ?    1 : 0);
        data.z = MathHelper.floor(data.currentPos.z) - (data.facing == EnumFacing.SOUTH ? 1 : 0);
        data.blockPos = new BlockPos(data.x, data.y, data.z);

        return false;
    }

    public static class RayTraceCalcsData
    {
        public final LayerRange range;
        public final RayTraceFluidMode fluidMode;
        public final Vec3d end;
        public final int xEnd;
        public final int yEnd;
        public final int zEnd;
        public int x;
        public int y;
        public int z;
        public Vec3d currentPos;
        public BlockPos blockPos;
        public EnumFacing facing;
        public RayTraceResult trace;

        public RayTraceCalcsData(Vec3d start, Vec3d end, LayerRange range, RayTraceFluidMode fluidMode)
        {
            this.range = range;
            this.fluidMode = fluidMode;
            this.currentPos = start;
            this.end = end;
            this.xEnd = MathHelper.floor(end.x);
            this.yEnd = MathHelper.floor(end.y);
            this.zEnd = MathHelper.floor(end.z);
            this.x = MathHelper.floor(start.x);
            this.y = MathHelper.floor(start.y);
            this.z = MathHelper.floor(start.z);
            this.blockPos = new BlockPos(x, y, z);
            this.trace = null;
        }
    }

    public static class RayTraceWrapper
    {
        private final HitType type;
        private final Corner corner;
        private final Vec3d hitVec;
        @Nullable
        private final RayTraceResult trace;
        @Nullable
        private final Box box;
        @Nullable
        private final SchematicPlacement schematicPlacement;
        @Nullable
        private final String placementRegionName;

        public RayTraceWrapper()
        {
            this.type = HitType.MISS;
            this.corner = Corner.NONE;
            this.hitVec = Vec3d.ZERO;
            this.trace = null;
            this.box = null;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(RayTraceResult trace)
        {
            this.type = HitType.VANILLA;
            this.corner = Corner.NONE;
            this.hitVec = trace.hitVec;
            this.trace = trace;
            this.box = null;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(HitType type)
        {
            this.type = type;
            this.corner = Corner.NONE;
            this.hitVec = Vec3d.ZERO;
            this.trace = null;
            this.box = null;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(HitType type, RayTraceResult trace)
        {
            this.type = type;
            this.corner = Corner.NONE;
            this.hitVec = trace.hitVec;
            this.trace = trace;
            this.box = null;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(Box box, Corner corner, Vec3d hitVec)
        {
            this.type = corner == Corner.NONE ? HitType.SELECTION_BOX_BODY : HitType.SELECTION_BOX_CORNER;
            this.corner = corner;
            this.hitVec = hitVec;
            this.trace = null;
            this.box = box;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(SchematicPlacement placement, Vec3d hitVec, @Nullable String regionName)
        {
            this.type = regionName != null ? HitType.PLACEMENT_SUBREGION : HitType.PLACEMENT_ORIGIN;
            this.corner = Corner.NONE;
            this.hitVec = hitVec;
            this.trace = null;
            this.box = null;
            this.schematicPlacement = placement;
            this.placementRegionName = regionName;
        }

        public HitType getHitType()
        {
            return this.type;
        }

        @Nullable
        public RayTraceResult getRayTraceResult()
        {
            return this.trace;
        }

        @Nullable
        public Box getHitSelectionBox()
        {
            return this.box;
        }

        @Nullable
        public SchematicPlacement getHitSchematicPlacement()
        {
            return this.schematicPlacement;
        }

        @Nullable
        public String getHitSchematicPlacementRegionName()
        {
            return this.placementRegionName;
        }

        public Vec3d getHitVec()
        {
            return this.hitVec;
        }

        public Corner getHitCorner()
        {
            return this.corner;
        }

        public enum HitType
        {
            MISS,
            VANILLA,
            SELECTION_BOX_BODY,
            SELECTION_BOX_CORNER,
            SELECTION_ORIGIN,
            PLACEMENT_SUBREGION,
            PLACEMENT_ORIGIN,
            SCHEMATIC_BLOCK,
            MISMATCH_OVERLAY;
        }
    }
}
