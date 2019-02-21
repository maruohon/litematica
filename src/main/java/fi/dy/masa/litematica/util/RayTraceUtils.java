package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.LayerRange;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
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
        Vec3d eyesPos = entity.getPositionEyes(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        RayTraceResult result = getRayTraceFromEntity(world, entity, false, range);
        double closestVanilla = result.typeOfHit != RayTraceResult.Type.MISS ? result.hitVec.distanceTo(eyesPos) : -1D;

        AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();
        RayTraceWrapper wrapper = null;

        clearTraceVars();

        if (DataManager.getToolMode().getUsesSchematic() == false && area != null)
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

            BlockPos origin = area.getExplicitOrigin();

            if (origin != null)
            {
                traceToOrigin(origin, eyesPos, lookEndPos, HitType.SELECTION_ORIGIN, null);
            }
        }

        if (DataManager.getToolMode().getUsesSchematic())
        {
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
            {
                if (placement.isEnabled())
                {
                    traceToPlacementBox(placement, eyesPos, lookEndPos);
                    traceToOrigin(placement.getOrigin(), eyesPos, lookEndPos, HitType.PLACEMENT_ORIGIN, placement);
                }
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

        Vec3d eyesPos = entity.getPositionEyes(1f);
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
        boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

        if (respectRenderRange &&
            (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() == false ||
             Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() == invert))
        {
            return null;
        }

        World world = SchematicWorldHandler.getSchematicWorld();

        if (world == null)
        {
            return null;
        }

        Vec3d eyesPos = entity.getPositionEyes(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        return rayTraceSchematicWorldBlocks(world, eyesPos, lookEndPos, true, false, true, respectRenderRange, 200);
    }

    @Nullable
    public static RayTraceWrapper getGenericTrace(World worldClient, Entity entity, double range, boolean respectRenderRange)
    {
        RayTraceResult traceClient = getRayTraceFromEntity(worldClient, entity, true, range);
        RayTraceResult traceSchematic = traceToSchematicWorld(entity, range, respectRenderRange);
        double distClosest = -1D;
        HitType type = HitType.MISS;
        Vec3d eyesPos = entity.getPositionEyes(1f);
        RayTraceResult trace = null;

        if (traceSchematic != null && traceSchematic.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            double dist = eyesPos.squareDistanceTo(traceSchematic.hitVec);

            if (distClosest < 0 || dist < distClosest)
            {
                trace = traceSchematic;
                distClosest = eyesPos.squareDistanceTo(traceSchematic.hitVec);
                type = HitType.SCHEMATIC_BLOCK;
            }
        }

        if (traceClient != null && traceClient.typeOfHit == RayTraceResult.Type.BLOCK)
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
            List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
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
        Vec3d eyesPos = entity.getPositionEyes(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        RayTraceResult result = getRayTraceFromEntity(worldClient, entity, false, range);
        final double closestVanilla = result.typeOfHit != RayTraceResult.Type.MISS ? result.hitVec.distanceTo(eyesPos) : -1D;

        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        List<RayTraceResult> list = rayTraceSchematicWorldBlocksToList(worldSchematic, eyesPos, lookEndPos, false, false, false, true, 200);
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
        Vec3d eyesPos = entity.getPositionEyes(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        RayTraceResult result = rayTraceBlocks(world, eyesPos, lookEndPos, useLiquids, false, false, 1000);

        if (result == null)
        {
            result = new RayTraceResult(RayTraceResult.Type.MISS, Vec3d.ZERO, EnumFacing.UP, BlockPos.ORIGIN);
        }

        AxisAlignedBB bb = entity.getEntityBoundingBox().expand(rangedLookRot.x, rangedLookRot.y, rangedLookRot.z).expand(1d, 1d, 1d);
        List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(entity, bb);

        double closest = result.typeOfHit == RayTraceResult.Type.BLOCK ? eyesPos.distanceTo(result.hitVec) : Double.MAX_VALUE;
        RayTraceResult entityTrace = null;
        Entity targetEntity = null;

        for (int i = 0; i < list.size(); i++)
        {
            Entity entityTmp = list.get(i);
            bb = entityTmp.getEntityBoundingBox();
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
     * Copy pasted from World#rayTraceBlocks() except for the added maxSteps argument
     */
    @Nullable
    public static RayTraceResult rayTraceBlocks(World world, Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, int maxSteps)
    {
        if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z) ||
            Double.isNaN(vec32.x) || Double.isNaN(vec32.y) || Double.isNaN(vec32.z))
        {
            return null;
        }

        final int xEnd = MathHelper.floor(vec32.x);
        final int yEnd = MathHelper.floor(vec32.y);
        final int zEnd = MathHelper.floor(vec32.z);
        int x = MathHelper.floor(vec31.x);
        int y = MathHelper.floor(vec31.y);
        int z = MathHelper.floor(vec31.z);
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if ((ignoreBlockWithoutBoundingBox == false || state.getCollisionBoundingBox(world, pos) != Block.NULL_AABB) &&
             block.canCollideCheck(state, stopOnLiquid))
        {
            RayTraceResult raytraceresult = state.collisionRayTrace(world, pos, vec31, vec32);

            if (raytraceresult != null)
            {
                return raytraceresult;
            }
        }

        RayTraceResult trace = null;

        while (--maxSteps >= 0)
        {
            if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z))
            {
                return null;
            }

            if (x == xEnd && y == yEnd && z == zEnd)
            {
                return returnLastUncollidableBlock ? trace : null;
            }

            boolean flag2 = true;
            boolean flag = true;
            boolean flag1 = true;
            double d0 = 999.0D;
            double d1 = 999.0D;
            double d2 = 999.0D;

            if (xEnd > x)
            {
                d0 = (double)x + 1.0D;
            }
            else if (xEnd < x)
            {
                d0 = (double)x + 0.0D;
            }
            else
            {
                flag2 = false;
            }

            if (yEnd > y)
            {
                d1 = (double)y + 1.0D;
            }
            else if (yEnd < y)
            {
                d1 = (double)y + 0.0D;
            }
            else
            {
                flag = false;
            }

            if (zEnd > z)
            {
                d2 = (double)z + 1.0D;
            }
            else if (zEnd < z)
            {
                d2 = (double)z + 0.0D;
            }
            else
            {
                flag1 = false;
            }

            double d3 = 999.0D;
            double d4 = 999.0D;
            double d5 = 999.0D;
            double d6 = vec32.x - vec31.x;
            double d7 = vec32.y - vec31.y;
            double d8 = vec32.z - vec31.z;

            if (flag2)
            {
                d3 = (d0 - vec31.x) / d6;
            }

            if (flag)
            {
                d4 = (d1 - vec31.y) / d7;
            }

            if (flag1)
            {
                d5 = (d2 - vec31.z) / d8;
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

            EnumFacing enumfacing;

            if (d3 < d4 && d3 < d5)
            {
                enumfacing = xEnd > x ? EnumFacing.WEST : EnumFacing.EAST;
                vec31 = new Vec3d(d0, vec31.y + d7 * d3, vec31.z + d8 * d3);
            }
            else if (d4 < d5)
            {
                enumfacing = yEnd > y ? EnumFacing.DOWN : EnumFacing.UP;
                vec31 = new Vec3d(vec31.x + d6 * d4, d1, vec31.z + d8 * d4);
            }
            else
            {
                enumfacing = zEnd > z ? EnumFacing.NORTH : EnumFacing.SOUTH;
                vec31 = new Vec3d(vec31.x + d6 * d5, vec31.y + d7 * d5, d2);
            }

            x = MathHelper.floor(vec31.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
            y = MathHelper.floor(vec31.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
            z = MathHelper.floor(vec31.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
            pos = new BlockPos(x, y, z);
            IBlockState iblockstate1 = world.getBlockState(pos);
            Block block1 = iblockstate1.getBlock();

            if (!ignoreBlockWithoutBoundingBox || iblockstate1.getMaterial() == Material.PORTAL || iblockstate1.getCollisionBoundingBox(world, pos) != Block.NULL_AABB)
            {
                if (block1.canCollideCheck(iblockstate1, stopOnLiquid))
                {
                    RayTraceResult raytraceresult1 = iblockstate1.collisionRayTrace(world, pos, vec31, vec32);

                    if (raytraceresult1 != null)
                    {
                        return raytraceresult1;
                    }
                }
                else
                {
                    trace = new RayTraceResult(RayTraceResult.Type.MISS, vec31, enumfacing, pos);
                }
            }
        }

        return returnLastUncollidableBlock ? trace : null;
    }

    @Nullable
    public static RayTraceResult rayTraceSchematicWorldBlocks(World world, Vec3d posStart, Vec3d posEnd,
            boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, boolean respectRenderRange, int maxSteps)
    {
        if (Double.isNaN(posStart.x) || Double.isNaN(posStart.y) || Double.isNaN(posStart.z) ||
            Double.isNaN(posEnd.x) || Double.isNaN(posEnd.y) || Double.isNaN(posEnd.z))
        {
            return null;
        }

        final int xEnd = MathHelper.floor(posEnd.x);
        final int yEnd = MathHelper.floor(posEnd.y);
        final int zEnd = MathHelper.floor(posEnd.z);
        RayTraceCalcsData data = new RayTraceCalcsData(posStart, posEnd);
        LayerRange range = DataManager.getRenderLayerRange();

        data.x = MathHelper.floor(data.posStart.x);
        data.y = MathHelper.floor(data.posStart.y);
        data.z = MathHelper.floor(data.posStart.z);
        data.pos = new BlockPos(data.x, data.y, data.z);
        IBlockState state = world.getBlockState(data.pos);
        Block block = state.getBlock();

        if ((respectRenderRange == false || range.isPositionWithinRange(data.x, data.y, data.z)) &&
            (ignoreBlockWithoutBoundingBox == false || state.getCollisionBoundingBox(world, data.pos) != Block.NULL_AABB) &&
             block.canCollideCheck(state, stopOnLiquid))
        {
            RayTraceResult trace = state.collisionRayTrace(world, data.pos, data.posStart, posEnd);

            if (trace != null)
            {
                return trace;
            }
        }

        RayTraceResult trace = null;

        while (--maxSteps >= 0)
        {
            if (Double.isNaN(data.posStart.x) || Double.isNaN(data.posStart.y) || Double.isNaN(data.posStart.z))
            {
                return null;
            }

            if (data.x == xEnd && data.y == yEnd && data.z == zEnd)
            {
                return returnLastUncollidableBlock ? trace : null;
            }

            rayTraceCalcs(data);

            state = world.getBlockState(data.pos);
            block = state.getBlock();

            if ((respectRenderRange == false || range.isPositionWithinRange(data.x, data.y, data.z)) &&
                (!ignoreBlockWithoutBoundingBox || state.getMaterial() == Material.PORTAL ||
                 state.getCollisionBoundingBox(world, data.pos) != Block.NULL_AABB))
            {
                if (block.canCollideCheck(state, stopOnLiquid))
                {
                    RayTraceResult traceTmp = state.collisionRayTrace(world, data.pos, data.posStart, posEnd);

                    if (traceTmp != null)
                    {
                        return traceTmp;
                    }
                }
                else
                {
                    trace = new RayTraceResult(RayTraceResult.Type.MISS, data.posStart, data.facing, data.pos);
                }
            }
        }

        return returnLastUncollidableBlock ? trace : null;
    }

    public static List<RayTraceResult> rayTraceSchematicWorldBlocksToList(World world, Vec3d posStart, Vec3d posEnd,
            boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, boolean respectRenderRange, int maxSteps)
    {
        if (Double.isNaN(posStart.x) || Double.isNaN(posStart.y) || Double.isNaN(posStart.z) ||
            Double.isNaN(posEnd.x) || Double.isNaN(posEnd.y) || Double.isNaN(posEnd.z))
        {
            return ImmutableList.of();
        }

        final int xEnd = MathHelper.floor(posEnd.x);
        final int yEnd = MathHelper.floor(posEnd.y);
        final int zEnd = MathHelper.floor(posEnd.z);
        RayTraceCalcsData data = new RayTraceCalcsData(posStart, posEnd);
        LayerRange range = DataManager.getRenderLayerRange();

        data.x = MathHelper.floor(data.posStart.x);
        data.y = MathHelper.floor(data.posStart.y);
        data.z = MathHelper.floor(data.posStart.z);
        data.pos = new BlockPos(data.x, data.y, data.z);
        IBlockState state = world.getBlockState(data.pos);
        Block block = state.getBlock();
        List<RayTraceResult> hits = new ArrayList<>();

        if ((respectRenderRange == false || range.isPositionWithinRange(data.x, data.y, data.z)) &&
            (ignoreBlockWithoutBoundingBox == false || state.getCollisionBoundingBox(world, data.pos) != Block.NULL_AABB) &&
             block.canCollideCheck(state, stopOnLiquid))
        {
            RayTraceResult traceTmp = state.collisionRayTrace(world, data.pos, data.posStart, posEnd);

            if (traceTmp != null)
            {
                //return ImmutableList.of(traceTmp.getBlockPos());
                hits.add(traceTmp);
            }
        }

        while (--maxSteps >= 0)
        {
            if (Double.isNaN(data.posStart.x) || Double.isNaN(data.posStart.y) || Double.isNaN(data.posStart.z))
            {
                return hits;
            }

            if (data.x == xEnd && data.y == yEnd && data.z == zEnd)
            {
                return hits;
            }

            rayTraceCalcs(data);

            state = world.getBlockState(data.pos);
            block = state.getBlock();

            if ((respectRenderRange == false || range.isPositionWithinRange(data.x, data.y, data.z)) &&
                (!ignoreBlockWithoutBoundingBox || state.getMaterial() == Material.PORTAL ||
                 state.getCollisionBoundingBox(world, data.pos) != Block.NULL_AABB))
            {
                if (block.canCollideCheck(state, stopOnLiquid))
                {
                    RayTraceResult traceTmp = state.collisionRayTrace(world, data.pos, data.posStart, posEnd);

                    if (traceTmp != null)
                    {
                        hits.add(traceTmp);
                    }
                }
            }
        }

        return hits;
    }

    private static void rayTraceCalcs(RayTraceCalcsData data)
    {
        boolean xDiffers = true;
        boolean yDiffers = true;
        boolean zDiffers = true;
        double nextX = 999.0D;
        double nextY = 999.0D;
        double nextZ = 999.0D;

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
        double d6 = data.posEnd.x - data.posStart.x;
        double d7 = data.posEnd.y - data.posStart.y;
        double d8 = data.posEnd.z - data.posStart.z;

        if (xDiffers)
        {
            d3 = (nextX - data.posStart.x) / d6;
        }

        if (yDiffers)
        {
            d4 = (nextY - data.posStart.y) / d7;
        }

        if (zDiffers)
        {
            d5 = (nextZ - data.posStart.z) / d8;
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
            data.posStart = new Vec3d(nextX, data.posStart.y + d7 * d3, data.posStart.z + d8 * d3);
        }
        else if (d4 < d5)
        {
            data.facing = data.yEnd > data.y ? EnumFacing.DOWN : EnumFacing.UP;
            data.posStart = new Vec3d(data.posStart.x + d6 * d4, nextY, data.posStart.z + d8 * d4);
        }
        else
        {
            data.facing = data.zEnd > data.z ? EnumFacing.NORTH : EnumFacing.SOUTH;
            data.posStart = new Vec3d(data.posStart.x + d6 * d5, data.posStart.y + d7 * d5, nextZ);
        }

        data.x = MathHelper.floor(data.posStart.x) - (data.facing == EnumFacing.EAST ?  1 : 0);
        data.y = MathHelper.floor(data.posStart.y) - (data.facing == EnumFacing.UP ?    1 : 0);
        data.z = MathHelper.floor(data.posStart.z) - (data.facing == EnumFacing.SOUTH ? 1 : 0);
        data.pos = new BlockPos(data.x, data.y, data.z);
    }

    private static class RayTraceCalcsData
    {
        private Vec3d posStart;
        private final Vec3d posEnd;
        private final int xEnd;
        private final int yEnd;
        private final int zEnd;
        private int x;
        private int y;
        private int z;
        private BlockPos pos;
        private EnumFacing facing;

        private RayTraceCalcsData(Vec3d posStart, Vec3d posEnd)
        {
            this.posStart = posStart;
            this.posEnd = posEnd;
            this.xEnd = MathHelper.floor(posEnd.x);
            this.yEnd = MathHelper.floor(posEnd.y);
            this.zEnd = MathHelper.floor(posEnd.z);
            this.x = MathHelper.floor(posStart.x);
            this.x = MathHelper.floor(posStart.x);
            this.x = MathHelper.floor(posStart.x);
            this.pos = new BlockPos(x, y, z);
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
