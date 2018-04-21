package fi.dy.masa.litematica.util;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.schematic.AreaSelection;
import fi.dy.masa.litematica.schematic.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RayTraceUtils
{
    private static RayTraceWrapper closestBox;
    private static RayTraceWrapper closestCorner;
    private static double closestBoxDistance;
    private static double closestCornerDistance;

    @Nonnull
    public static RayTraceWrapper getWrappedRayTraceFromEntity(World world, Entity entity, double range)
    {
        Vec3d eyesVec = entity.getPositionEyes(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookVec = eyesVec.add(rangedLookRot);

        RayTraceResult result = getRayTraceFromEntity(world, entity, false, range);
        double closestVanilla = result.typeOfHit != RayTraceResult.Type.MISS ? result.hitVec.distanceTo(eyesVec) : -1D;

        AreaSelection area = DataManager.getInstance(world).getSelectionManager().getSelectedAreaSelection();
        RayTraceWrapper wrapper = null;

        closestBox = null;
        closestCorner = null;
        closestBoxDistance = -1D;
        closestCornerDistance = -1D;

        if (area != null)
        {
            for (SelectionBox box : area.getAllSelectionsBoxes())
            {
                boolean hitCorner = false;
                hitCorner |= traceToBoxCorner(box, Corner.CORNER_1, eyesVec, lookVec);
                hitCorner |= traceToBoxCorner(box, Corner.CORNER_2, eyesVec, lookVec);

                if (hitCorner == false)
                {
                    traceToBoxBody(box, eyesVec, lookVec);
                }
            }
        }

        double closestDistance = closestVanilla;

        if (closestBoxDistance >= 0 && (closestDistance < 0 || closestBoxDistance <= closestDistance))
        {
            closestDistance = closestBoxDistance;
            wrapper = closestBox;
        }

        // Corners are preferred over box body hits, thus the '<=' and this being after the box check
        if (closestCornerDistance >= 0 && (closestDistance < 0 || closestCornerDistance <= closestDistance))
        {
            closestDistance = closestCornerDistance;
            wrapper = closestCorner;
        }

        if (wrapper == null || closestDistance < 0)
        {
            wrapper = new RayTraceWrapper();
        }

        return wrapper;
    }

    private static boolean traceToBoxCorner(SelectionBox box, Corner corner, Vec3d start, Vec3d end)
    {
        BlockPos pos = (corner == Corner.CORNER_1) ? box.getPos1() : (corner == Corner.CORNER_2) ? box.getPos2() : null;
        boolean hitCorner = false;

        if (pos != null)
        {
            AxisAlignedBB bb = PositionUtils.createAABBForPosition(pos);
            RayTraceResult hit = bb.calculateIntercept(start, end);

            if (hit != null)
            {
                hitCorner = true;
                double dist = hit.hitVec.distanceTo(start);

                if (closestCornerDistance < 0 || dist < closestCornerDistance)
                {
                    closestCornerDistance = dist;
                    closestCorner = new RayTraceWrapper(box, corner, hit.hitVec);
                }
            }
        }

        return hitCorner;
    }

    private static boolean traceToBoxBody(SelectionBox box, Vec3d start, Vec3d end)
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

    @Nonnull
    public static RayTraceResult getRayTraceFromEntity(World world, Entity entity, boolean useLiquids, double range)
    {
        Vec3d eyesVec = entity.getPositionEyes(1f);
        Vec3d rangedLookRot = entity.getLook(1f).scale(range);
        Vec3d lookVec = eyesVec.add(rangedLookRot);

        RayTraceResult result = rayTraceBlocks(world, eyesVec, lookVec, useLiquids, false, false, 1000);

        if (result == null)
        {
            result = new RayTraceResult(RayTraceResult.Type.MISS, Vec3d.ZERO, EnumFacing.UP, BlockPos.ORIGIN);
        }

        AxisAlignedBB bb = entity.getEntityBoundingBox().expand(rangedLookRot.x, rangedLookRot.y, rangedLookRot.z).expand(1d, 1d, 1d);
        List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(entity, bb);

        double closest = result.typeOfHit == RayTraceResult.Type.BLOCK ? eyesVec.distanceTo(result.hitVec) : Double.MAX_VALUE;
        RayTraceResult entityTrace = null;
        Entity targetEntity = null;

        for (int i = 0; i < list.size(); i++)
        {
            Entity entityTmp = list.get(i);
            bb = entityTmp.getEntityBoundingBox();
            RayTraceResult traceTmp = bb.calculateIntercept(lookVec, eyesVec);

            if (traceTmp != null)
            {
                double distance = eyesVec.distanceTo(traceTmp.hitVec);

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

        if (eyesVec.distanceTo(result.hitVec) > range)
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

    public static class RayTraceWrapper
    {
        private final HitType type;
        private final RayTraceResult trace;
        private final SelectionBox box;
        private final Corner corner;
        private final Vec3d hitVec;

        public RayTraceWrapper()
        {
            this.type = HitType.MISS;
            this.trace = null;
            this.box = null;
            this.corner = Corner.NONE;
            this.hitVec = Vec3d.ZERO;
        }

        public RayTraceWrapper(RayTraceResult trace)
        {
            this.type = HitType.VANILLA;
            this.trace = trace;
            this.box = null;
            this.corner = Corner.NONE;
            this.hitVec = trace.hitVec;
        }

        public RayTraceWrapper(SelectionBox box, Corner corner, Vec3d hitVec)
        {
            this.type = corner == Corner.NONE ? HitType.BOX : HitType.CORNER;
            this.trace = null;
            this.box = box;
            this.corner = corner;
            this.hitVec = hitVec;
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
        public SelectionBox getHitSelectionBox()
        {
            return this.box;
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
            BOX,
            CORNER;
        }
    }
}
