package fi.dy.masa.litematica.util;

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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RayTraceUtils
{
    private static final net.minecraft.util.math.Box FULL_BLOCK_BOUNDS = new net.minecraft.util.math.Box(0, 0, 0, 1, 1, 1);

    private static RayTraceWrapper closestBox;
    private static RayTraceWrapper closestCorner;
    private static RayTraceWrapper closestOrigin;
    private static double closestBoxDistance;
    private static double closestCornerDistance;
    private static double closestOriginDistance;
    private static HitType originType;

    @Nullable
    public static BlockPos getTargetedPosition(World world, Entity player, double maxDistance, boolean sneakToOffset)
    {
        HitResult trace = getRayTraceFromEntity(world, player, false, maxDistance);

        if (trace.getType() != HitResult.Type.BLOCK)
        {
            return null;
        }

        BlockHitResult traceBlock = (BlockHitResult) trace;
        BlockPos pos = traceBlock.getBlockPos();

        // Sneaking puts the position adjacent the targeted block face, not sneaking puts it inside the targeted block
        if (sneakToOffset == player.isSneaking())
        {
            pos = pos.offset(traceBlock.getSide());
        }

        return pos;
    }

    @Nonnull
    public static RayTraceWrapper getWrappedRayTraceFromEntity(World world, Entity entity, double range)
    {
        Vec3d eyesPos = entity.getCameraPosVec(1f);
        Vec3d rangedLookRot = entity.getRotationVec(1f).multiply(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        HitResult result = getRayTraceFromEntity(world, entity, false, range);
        double closestVanilla = result.getType() != HitResult.Type.MISS ? result.getPos().distanceTo(eyesPos) : -1D;

        AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();
        RayTraceWrapper wrapper = null;

        clearTraceVars();

        if (!DataManager.getToolMode().getUsesSchematic() && area != null)
        {
            for (Box box : area.getAllSubRegionBoxes())
            {
                boolean hitCorner = false;
                hitCorner |= traceToSelectionBoxCorner(box, Corner.CORNER_1, eyesPos, lookEndPos);
                hitCorner |= traceToSelectionBoxCorner(box, Corner.CORNER_2, eyesPos, lookEndPos);

                if (!hitCorner)
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

        if (closestBoxDistance >= 0 && (closestVanilla < 0 || closestBoxDistance <= closestVanilla))
        {
            wrapper = closestBox;
        }

        // Corners are preferred over box body hits, thus this being after the box check
        if (closestCornerDistance >= 0 && (closestVanilla < 0 || closestCornerDistance <= closestVanilla))
        {
            wrapper = closestCorner;
        }

        // Origins are preferred over everything else
        if (closestOriginDistance >= 0 && (closestVanilla < 0 || closestOriginDistance <= closestVanilla))
        {
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

        if (wrapper == null)
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
            net.minecraft.util.math.Box bb = PositionUtils.createAABBForPosition(pos);
            Optional<Vec3d> optional = bb.raycast(start, end);

            if (optional.isPresent())
            {
                double dist = optional.get().distanceTo(start);

                if (closestCornerDistance < 0 || dist < closestCornerDistance)
                {
                    closestCornerDistance = dist;
                    closestCorner = new RayTraceWrapper(box, corner, optional.get());
                }

                return true;
            }
        }

        return false;
    }

    private static void traceToSelectionBoxBody(Box box, Vec3d start, Vec3d end)
    {
        if (box.getPos1() != null && box.getPos2() != null)
        {
            net.minecraft.util.math.Box bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
            Optional<Vec3d> optional = bb.raycast(start, end);

            if (optional.isPresent())
            {
                double dist = optional.get().distanceTo(start);

                if (closestBoxDistance < 0 || dist < closestBoxDistance)
                {
                    closestBoxDistance = dist;
                    closestBox = new RayTraceWrapper(box, Corner.NONE, optional.get());
                }

            }
        }

    }

    private static void traceToPlacementBox(SchematicPlacement placement, Vec3d start, Vec3d end)
    {
        ImmutableMap<String, Box> boxes = placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        boolean hitSomething = false;

        for (Map.Entry<String, Box> entry : boxes.entrySet())
        {
            String boxName = entry.getKey();
            Box box = entry.getValue();

            if (box.getPos1() != null && box.getPos2() != null)
            {
                net.minecraft.util.math.Box bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
                Optional<Vec3d> optional = bb.raycast(start, end);

                if (optional.isPresent())
                {
                    double dist = optional.get().distanceTo(start);

                    if (closestBoxDistance < 0 || dist < closestBoxDistance)
                    {
                        closestBoxDistance = dist;
                        closestBox = new RayTraceWrapper(placement, optional.get(), boxName);
                    }
                }
            }
        }

    }

    private static void traceToOrigin(BlockPos pos, Vec3d start, Vec3d end, HitType type, @Nullable SchematicPlacement placement)
    {
        if (pos != null)
        {
            net.minecraft.util.math.Box bb = PositionUtils.createAABBForPosition(pos);
            Optional<Vec3d> optional = bb.raycast(start, end);

            if (optional.isPresent())
            {
                double dist = optional.get().distanceTo(start);

                if (closestOriginDistance < 0 || dist < closestOriginDistance)
                {
                    closestOriginDistance = dist;
                    originType = type;

                    if (type == HitType.PLACEMENT_ORIGIN)
                    {
                        closestOrigin = new RayTraceWrapper(placement, optional.get(), null);
                    }

                }
            }
        }

    }

    /**
     * Ray traces to the closest position on the given list
     * @param posList
     * @param entity
     * @param range
     * @return
     */
    @Nullable
    public static BlockHitResult traceToPositions(List<BlockPos> posList, Entity entity, double range)
    {
        if (posList.isEmpty())
        {
            return null;
        }

        Vec3d eyesPos = entity.getCameraPosVec(1f);
        Vec3d rangedLookRot = entity.getRotationVec(1f).multiply(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        double closest = -1D;
        BlockHitResult trace = null;

        for (BlockPos pos : posList)
        {
            if (pos != null)
            {
                BlockHitResult hit = net.minecraft.util.math.Box.raycast(ImmutableList.of(FULL_BLOCK_BOUNDS), eyesPos, lookEndPos, pos);

                if (hit != null)
                {
                    double dist = hit.getPos().distanceTo(eyesPos);

                    if (closest < 0 || dist < closest)
                    {
                        trace = new BlockHitResult(hit.getPos(), hit.getSide(), pos, false);
                        closest = dist;
                    }
                }
            }
        }

        return trace;
    }

    @Nullable
    public static BlockHitResult traceToSchematicWorld(Entity entity, double range,
                                                       boolean respectRenderRange, boolean targetFluids)
    {
        boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

        if (respectRenderRange &&
            (!Configs.Visuals.ENABLE_RENDERING.getBooleanValue() ||
             Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() == invert))
        {
            return null;
        }

        World world = SchematicWorldHandler.getSchematicWorld();

        if (world == null)
        {
            return null;
        }

        Vec3d eyesPos = entity.getCameraPosVec(1f);
        Vec3d rangedLookRot = entity.getRotationVec(1f).multiply(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);
        RaycastContext.FluidHandling fluidMode = targetFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE;

        return rayTraceBlocks(world, eyesPos, lookEndPos, fluidMode, false, true, respectRenderRange, 200);
    }

    @Nullable
    public static RayTraceWrapper getGenericTrace(World worldClient, Entity entity, double range)
    {
        return getGenericTrace(worldClient, entity, range, true, true, false);
    }

    @Nullable
    public static RayTraceWrapper getGenericTrace(World worldClient, Entity entity,
                                                  double range, boolean respectRenderRange,
                                                  boolean targetFluids, boolean includeVerifier)
    {
        HitResult traceClient = getRayTraceFromEntity(worldClient, entity, targetFluids, range);
        HitResult traceSchematic = traceToSchematicWorld(entity, range, respectRenderRange, targetFluids);
        double distClosest = -1D;
        HitType type = HitType.MISS;
        Vec3d eyesPos = entity.getCameraPosVec(1f);
        HitResult trace = null;

        if (traceSchematic != null && traceSchematic.getType() == HitResult.Type.BLOCK)
        {
            double dist = eyesPos.squaredDistanceTo(traceSchematic.getPos());

            trace = traceSchematic;
            distClosest = eyesPos.squaredDistanceTo(traceSchematic.getPos());
            type = HitType.SCHEMATIC_BLOCK;
        }

        if (traceClient.getType() == HitResult.Type.BLOCK)
        {
            double dist = eyesPos.squaredDistanceTo(traceClient.getPos());

            if (distClosest < 0 || dist < distClosest)
            {
                trace = traceClient;
                type = HitType.VANILLA_BLOCK;
            }
        }

        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (includeVerifier && placement != null && placement.hasVerifier())
        {
            SchematicVerifier verifier = placement.getSchematicVerifier();
            List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
            BlockHitResult traceMismatch = traceToPositions(posList, entity, range);

            // Mismatch overlay has priority over other hits
            if (traceMismatch != null)
            {
                trace = traceMismatch;
                type = HitType.MISMATCH_OVERLAY;
            }
        }

        if (type != HitType.MISS)
        {
            return new RayTraceWrapper(type, (BlockHitResult) trace);
        }

        return null;
    }

    @Nullable
    public static RayTraceWrapper getSchematicWorldTraceWrapperIfClosest(World worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getGenericTrace(worldClient, entity, range);

        if (trace != null && trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return trace;
        }

        return null;
    }

    @Nullable
    public static BlockPos getSchematicWorldTraceIfClosest(World worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getSchematicWorldTraceWrapperIfClosest(worldClient, entity, range);
        return trace != null && trace.getHitType() == HitType.SCHEMATIC_BLOCK ? trace.getBlockHitResult().getBlockPos() : null;
    }

    @Nullable
    public static BlockPos getFurthestSchematicWorldBlockBeforeVanilla(World worldClient,
                                                                       Entity entity,
                                                                       double maxRange,
                                                                       boolean requireVanillaBlockBehind)
    {
        Vec3d eyesPos = entity.getCameraPosVec(1f);
        Vec3d rangedLookRot = entity.getRotationVec(1f).multiply(maxRange);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        @Nullable BlockPos closestVanillaPos = null;
        @Nullable Direction side = null;
        double closestVanilla = -1.0;

        HitResult traceVanilla = getRayTraceFromEntity(worldClient, entity, false, maxRange);

        if (traceVanilla.getType() == HitResult.Type.BLOCK)
        {
            closestVanilla = traceVanilla.getPos().squaredDistanceTo(eyesPos);
            BlockHitResult vanillaHitResult = (BlockHitResult) traceVanilla;
            side = vanillaHitResult.getSide();
            closestVanillaPos = vanillaHitResult.getBlockPos();
        }
        else if (requireVanillaBlockBehind)
        {
            return null;
        }

        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        List<BlockHitResult> list = rayTraceBlocksToList(worldSchematic, eyesPos, lookEndPos, RaycastContext.FluidHandling.NONE, false, false, true, 200);
        BlockHitResult furthestTrace = null;
        double furthestDist = -1.0;

        if (!list.isEmpty())
        {
            for (BlockHitResult trace : list)
            {
                double dist = trace.getPos().squaredDistanceTo(eyesPos);

                if ((furthestDist < 0 || dist > furthestDist) &&
                    (dist < closestVanilla || closestVanilla < 0) &&
                        !trace.getBlockPos().equals(closestVanillaPos))
                {
                    furthestDist = dist;
                    furthestTrace = trace;
                }

                if (closestVanilla >= 0 && dist > closestVanilla)
                {
                    break;
                }
            }
        }

        // Didn't trace to any schematic blocks, but hit a vanilla block.
        // Check if there is a schematic block adjacent to the vanilla block
        // (which means that it has a non-full-cube collision box, since
        // it wasn't hit by the trace), and no block in the client world.
        // Note that this method is only used for the "pickBlockLast" type
        // of pick blocking, not for the "first" variant, where this would
        // probably be annoying if you want to pick block the client world block.
        if (furthestTrace == null && side != null && closestVanillaPos != null)
        {
            BlockPos pos = closestVanillaPos.offset(side);
            LayerRange layerRange = DataManager.getRenderLayerRange();

            if (layerRange.isPositionWithinRange(pos) &&
                    !worldSchematic.getBlockState(pos).isAir() &&
                worldClient.getBlockState(pos).isAir())
            {
                return pos;
            }
        }

        return furthestTrace != null ? furthestTrace.getBlockPos() : null;
    }

    @Nullable
    public static RayTraceWrapper getFurthestSchematicWorldTraceBeforeVanilla(World worldClient,
                                                                              Entity entity,
                                                                              double maxRange)
    {
        Vec3d eyesPos = entity.getCameraPosVec(1f);
        Vec3d rangedLookRot = entity.getRotationVec(1f).multiply(maxRange);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);

        @Nullable BlockPos closestVanillaPos = null;
        double closestVanilla = -1.0;

        HitResult traceVanilla = getRayTraceFromEntity(worldClient, entity, false, maxRange);

        if (traceVanilla.getType() == HitResult.Type.BLOCK)
        {
            closestVanilla = traceVanilla.getPos().squaredDistanceTo(eyesPos);
            BlockHitResult vanillaHitResult = (BlockHitResult) traceVanilla;
            closestVanillaPos = vanillaHitResult.getBlockPos();
        }

        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        List<BlockHitResult> list = rayTraceBlocksToList(worldSchematic, eyesPos, lookEndPos, RaycastContext.FluidHandling.NONE, false, false, true, 200);
        BlockHitResult furthestTrace = null;
        double furthestDist = -1.0;

        if (!list.isEmpty())
        {
            for (BlockHitResult trace : list)
            {
                double dist = trace.getPos().squaredDistanceTo(eyesPos);

                if ((furthestDist < 0 || dist > furthestDist) &&
                    (dist < closestVanilla || closestVanilla < 0) &&
                        !trace.getBlockPos().equals(closestVanillaPos))
                {
                    furthestDist = dist;
                    furthestTrace = trace;
                }

                if (closestVanilla >= 0 && dist > closestVanilla)
                {
                    break;
                }
            }
        }

        return furthestTrace != null ? new RayTraceWrapper(HitType.SCHEMATIC_BLOCK, furthestTrace) : null;
    }

    @Nonnull
    public static HitResult getRayTraceFromEntity(World world, Entity entity, boolean useLiquids, double range)
    {
        Vec3d eyesPos = entity.getCameraPosVec(1f);
        Vec3d rangedLookRot = entity.getRotationVec(1f).multiply(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);
        RaycastContext.FluidHandling fluidMode = useLiquids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE;

        HitResult result = rayTraceBlocks(world, eyesPos, lookEndPos, fluidMode, false, false, false, 1000);

        net.minecraft.util.math.Box bb = entity.getBoundingBox().expand(rangedLookRot.x, rangedLookRot.y, rangedLookRot.z).expand(1d, 1d, 1d);
        List<Entity> list = world.getOtherEntities(entity, bb);

        double closest = result != null && result.getType() == HitResult.Type.BLOCK ? eyesPos.distanceTo(result.getPos()) : Double.MAX_VALUE;
        Entity targetEntity = null;
        Optional<Vec3d> optional = Optional.empty();

        for (Entity entityTmp : list) {
            Optional<Vec3d> optionalTmp = entityTmp.getBoundingBox().raycast(eyesPos, lookEndPos);

            if (optionalTmp.isPresent()) {
                double distance = eyesPos.distanceTo(optionalTmp.get());

                if (distance <= closest) {
                    targetEntity = entityTmp;
                    optional = optionalTmp;
                    closest = distance;
                }
            }
        }

        if (targetEntity != null)
        {
            result = new EntityHitResult(targetEntity, optional.get());
        }

        if (result == null || eyesPos.distanceTo(result.getPos()) > range)
        {
            result = BlockHitResult.createMissed(Vec3d.ZERO, Direction.UP, BlockPos.ORIGIN);
        }

        return result;
    }

    /**
     * Mostly copy pasted from World#rayTraceBlocks() except for the added maxSteps argument and the layer range check
     */
    @Nullable
    public static BlockHitResult rayTraceBlocks(World world, Vec3d start, Vec3d end,
            RaycastContext.FluidHandling fluidMode, boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return null;
        }

        LayerRange range = DataManager.getRenderLayerRange();
        RayTraceCalcsData data = new RayTraceCalcsData(start, end, range, fluidMode);

        BlockState blockState = world.getBlockState(data.blockPos);
        FluidState fluidState = world.getFluidState(data.blockPos);

        BlockHitResult trace = traceFirstStep(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange);

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

    @Nullable
    private static BlockHitResult traceFirstStep(RayTraceCalcsData data,
            World world, BlockState blockState, FluidState fluidState,
            boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange)
    {
        if ((!respectLayerRange || data.range.isPositionWithinRange(data.x, data.y, data.z)) &&
            (!ignoreBlockWithoutBoundingBox || !blockState.getCollisionShape(world, data.blockPos).isEmpty()))
        {
            VoxelShape blockShape = blockState.getOutlineShape(world, data.blockPos);
            boolean blockCollidable = ! blockShape.isEmpty();
            boolean fluidCollidable = data.fluidMode.handled(fluidState);

            if (blockCollidable || fluidCollidable)
            {
                BlockHitResult trace = null;

                if (blockCollidable)
                {
                    trace = blockShape.raycast(data.start, data.end, data.blockPos);
                }

                if (trace == null && fluidCollidable)
                {
                    trace = fluidState.getShape(world, data.blockPos).raycast(data.start, data.end, data.blockPos);
                }

                return trace;
            }
        }

        return null;
    }

    private static boolean traceLoopSteps(RayTraceCalcsData data,
            World world, BlockState blockState, FluidState fluidState,
            boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange)
    {
        if ((!respectLayerRange || data.range.isPositionWithinRange(data.x, data.y, data.z)) &&
            (!ignoreBlockWithoutBoundingBox || blockState.getBlock() == Blocks.NETHER_PORTAL ||
                    !blockState.getCollisionShape(world, data.blockPos).isEmpty()))
        {
            VoxelShape blockShape = blockState.getOutlineShape(world, data.blockPos);
            boolean blockCollidable = ! blockShape.isEmpty();
            boolean fluidCollidable = data.fluidMode.handled(fluidState);

            if (!blockCollidable && !fluidCollidable)
            {
                Vec3d pos = new Vec3d(data.currentX, data.currentY, data.currentZ);
                data.trace = BlockHitResult.createMissed(pos, data.facing, data.blockPos);
            }
            else
            {
                BlockHitResult traceTmp = null;

                if (blockCollidable)
                {
                    traceTmp = blockShape.raycast(data.start, data.end, data.blockPos);
                }

                if (traceTmp == null && fluidCollidable)
                {
                    traceTmp = fluidState.getShape(world, data.blockPos).raycast(data.start, data.end, data.blockPos);
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

    public static List<BlockHitResult> rayTraceBlocksToList(World world, Vec3d start, Vec3d end,
            RaycastContext.FluidHandling fluidMode, boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return ImmutableList.of();
        }

        LayerRange range = DataManager.getRenderLayerRange();
        RayTraceCalcsData data = new RayTraceCalcsData(start, end, range, fluidMode);

        BlockState blockState = world.getBlockState(data.blockPos);
        FluidState fluidState = world.getFluidState(data.blockPos);

        BlockHitResult trace = traceFirstStep(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange);
        List<BlockHitResult> hits = new ArrayList<>();

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

        if (Double.isNaN(data.currentX) || Double.isNaN(data.currentY) || Double.isNaN(data.currentZ))
        {
            data.trace = null;
            return true;
        }

        if (data.x == data.xEnd && data.y == data.yEnd && data.z == data.zEnd)
        {
            if (!returnLastNonCollidableBlock)
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

        double relStepX = 999.0D;
        double relStepY = 999.0D;
        double relStepZ = 999.0D;
        double distToEndX = data.end.x - data.currentX;
        double distToEndY = data.end.y - data.currentY;
        double distToEndZ = data.end.z - data.currentZ;

        if (xDiffers)
        {
            relStepX = (nextX - data.currentX) / distToEndX;
        }

        if (yDiffers)
        {
            relStepY = (nextY - data.currentY) / distToEndY;
        }

        if (zDiffers)
        {
            relStepZ = (nextZ - data.currentZ) / distToEndZ;
        }

        if (relStepX == -0.0D)
        {
            relStepX = -1.0E-4D;
        }

        if (relStepY == -0.0D)
        {
            relStepY = -1.0E-4D;
        }

        if (relStepZ == -0.0D)
        {
            relStepZ = -1.0E-4D;
        }

        if (relStepX < relStepY && relStepX < relStepZ)
        {
            data.facing = data.xEnd > data.x ? Direction.WEST : Direction.EAST;
            data.currentX = nextX;
            data.currentY += distToEndY * relStepX;
            data.currentZ += distToEndZ * relStepX;
        }
        else if (relStepY < relStepZ)
        {
            data.facing = data.yEnd > data.y ? Direction.DOWN : Direction.UP;
            data.currentX += distToEndX * relStepY;
            data.currentY = nextY;
            data.currentZ += distToEndZ * relStepY;
        }
        else
        {
            data.facing = data.zEnd > data.z ? Direction.NORTH : Direction.SOUTH;
            data.currentX += distToEndX * relStepZ;
            data.currentY += distToEndY * relStepZ;
            data.currentZ = nextZ;
        }

        data.x = MathHelper.floor(data.currentX) - (data.facing == Direction.EAST ?  1 : 0);
        data.y = MathHelper.floor(data.currentY) - (data.facing == Direction.UP ?    1 : 0);
        data.z = MathHelper.floor(data.currentZ) - (data.facing == Direction.SOUTH ? 1 : 0);
        data.blockPos = new BlockPos(data.x, data.y, data.z);

        return false;
    }

    public static class RayTraceCalcsData
    {
        public final LayerRange range;
        public final RaycastContext.FluidHandling fluidMode;
        public final Vec3d start;
        public final Vec3d end;
        public final int xEnd;
        public final int yEnd;
        public final int zEnd;
        public int x;
        public int y;
        public int z;
        public double currentX;
        public double currentY;
        public double currentZ;
        public BlockPos blockPos;
        public Direction facing;
        public BlockHitResult trace;

        public RayTraceCalcsData(Vec3d start, Vec3d end, LayerRange range, RaycastContext.FluidHandling fluidMode)
        {
            this.start = start;
            this.end = end;
            this.range = range;
            this.fluidMode = fluidMode;
            this.currentX = start.x;
            this.currentY = start.y;
            this.currentZ = start.z;
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
        private Corner corner = Corner.NONE;
        private Vec3d hitVec = Vec3d.ZERO;
        @Nullable private BlockHitResult traceBlock = null;
        @Nullable private EntityHitResult traceEntity = null;
        @Nullable private Box box = null;
        @Nullable private SchematicPlacement schematicPlacement = null;
        @Nullable private String placementRegionName = null;

        public RayTraceWrapper()
        {
            this.type = HitType.MISS;
        }

        public RayTraceWrapper(HitType type)
        {
            this.type = type;
        }

        public RayTraceWrapper(HitType type, BlockHitResult trace)
        {
            this.type = type;
            this.hitVec = trace.getPos();
            this.traceBlock = trace;
        }

        public RayTraceWrapper(HitType type, EntityHitResult trace)
        {
            this.type = type;
            this.hitVec = trace.getPos();
            this.traceEntity = trace;
        }

        public RayTraceWrapper(@org.jetbrains.annotations.Nullable Box box, Corner corner, Vec3d hitVec)
        {
            this.type = corner == Corner.NONE ? HitType.SELECTION_BOX_BODY : HitType.SELECTION_BOX_CORNER;
            this.corner = corner;
            this.hitVec = hitVec;
            this.box = box;
        }

        public RayTraceWrapper(@org.jetbrains.annotations.Nullable SchematicPlacement placement, Vec3d hitVec, @Nullable String regionName)
        {
            this.type = regionName != null ? HitType.PLACEMENT_SUBREGION : HitType.PLACEMENT_ORIGIN;
            this.hitVec = hitVec;
            this.schematicPlacement = placement;
            this.placementRegionName = regionName;
        }

        public HitType getHitType()
        {
            return this.type;
        }

        @Nullable
        public BlockHitResult getBlockHitResult()
        {
            return this.traceBlock;
        }

        @Nullable
        public EntityHitResult getEntityHitResult()
        {
            return this.traceEntity;
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
            VANILLA_BLOCK,
            VANILLA_ENTITY,
            SELECTION_BOX_BODY,
            SELECTION_BOX_CORNER,
            SELECTION_ORIGIN,
            PLACEMENT_SUBREGION,
            PLACEMENT_ORIGIN,
            SCHEMATIC_BLOCK,
            SCHEMATIC_ENTITY,
            MISMATCH_OVERLAY
        }
    }
}
