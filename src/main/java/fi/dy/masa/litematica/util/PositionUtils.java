package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

public class PositionUtils
{
    public static BlockPos getMinCorner(BlockPos pos1, BlockPos pos2)
    {
        return new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
    }

    public static BlockPos getMaxCorner(BlockPos pos1, BlockPos pos2)
    {
        return new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
    }

    public static boolean isPositionInsideArea(BlockPos pos, BlockPos posMin, BlockPos posMax)
    {
        return pos.getX() >= posMin.getX() && pos.getX() <= posMax.getX() &&
               pos.getY() >= posMin.getY() && pos.getY() <= posMax.getY() &&
               pos.getZ() >= posMin.getZ() && pos.getZ() <= posMax.getZ();
    }

    public static BlockPos getTransformedRelativePlacementPosition(BlockPos posRel, SchematicPlacement schematicPlacement, Placement placement)
    {
        BlockPos pos;

        pos = getTransformedBlockPos(posRel, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        pos = getTransformedBlockPos(posRel, placement.getMirror(), placement.getRotation());

        return pos;
    }

    public static boolean arePositionsWithinWorld(World world, BlockPos posRel1, BlockPos posRel2, BlockPos offset,
            SchematicPlacement schematicPlacement, Placement placement)
    {
        BlockPos pos1 = getTransformedRelativePlacementPosition(posRel1, schematicPlacement, placement).add(offset);
        BlockPos pos2 = getTransformedRelativePlacementPosition(posRel2, schematicPlacement, placement).add(offset);

        return arePositionsWithinWorld(world, pos1, pos2);
    }

    public static boolean arePositionsWithinWorld(World world, BlockPos pos1, BlockPos pos2)
    {
        if (pos1.getY() >= 0 && pos1.getY() < 256 &&
            pos2.getY() >= 0 && pos2.getY() < 256)
        {
            WorldBorder border = world.getWorldBorder();
            return border.contains(pos1) && border.contains(pos2);
        }

        return false;
    }

    public static BlockPos getAreaSizeFromRelativeEndPosition(BlockPos posEndRelative)
    {
        int x = posEndRelative.getX();
        int y = posEndRelative.getY();
        int z = posEndRelative.getZ();

        x = x >= 0 ? x + 1 : x - 1;
        y = y >= 0 ? y + 1 : y - 1;
        z = z >= 0 ? z + 1 : z - 1;

        return new BlockPos(x, y, z);
    }

    public static BlockPos getRelativeEndPositionFromAreaSize(BlockPos size)
    {
        int x = size.getX();
        int y = size.getY();
        int z = size.getZ();

        x = x >= 0 ? x - 1 : x + 1;
        y = y >= 0 ? y - 1 : y + 1;
        z = z >= 0 ? z - 1 : z + 1;

        return new BlockPos(x, y, z);
    }

    public static List<Box> getValidBoxes(AreaSelection area)
    {
        List<Box> boxes = new ArrayList<>();
        Collection<Box> originalBoxes = area.getAllSubRegionBoxes();

        for (Box box : originalBoxes)
        {
            if (isBoxValid(box))
            {
                boxes.add(box);
            }
        }

        return boxes;
    }

    public static boolean isBoxValid(Box box)
    {
        return box.getPos1() != null && box.getPos2() != null;
    }

    public static BlockPos getEnclosingAreaSize(AreaSelection area)
    {
        return getEnclosingAreaSize(area.getAllSubRegionBoxes());
    }

    public static BlockPos getEnclosingAreaSize(Collection<Box> boxes)
    {
        Pair<BlockPos, BlockPos> pair = getEnclosingAreaCorners(boxes);
        return pair.getRight().subtract(pair.getLeft()).add(1, 1, 1);
    }

    /**
     * Returns the min and max corners of the enclosing box around the given collection of boxes.
     * The minimum corner is the left entry and the maximum corner is the right entry of the pair.
     * @param boxes
     * @return
     */
    @Nullable
    public static Pair<BlockPos, BlockPos> getEnclosingAreaCorners(Collection<Box> boxes)
    {
        if (boxes.isEmpty())
        {
            return null;
        }

        BlockPos.MutableBlockPos posMin = new BlockPos.MutableBlockPos( 60000000,  60000000,  60000000);
        BlockPos.MutableBlockPos posMax = new BlockPos.MutableBlockPos(-60000000, -60000000, -60000000);

        for (Box box : boxes)
        {
            getMinMaxCoords(posMin, posMax, box.getPos1());
            getMinMaxCoords(posMin, posMax, box.getPos2());
        }

        return Pair.of(posMin.toImmutable(), posMax.toImmutable());
    }

    private static void getMinMaxCoords(BlockPos.MutableBlockPos posMin, BlockPos.MutableBlockPos posMax, @Nullable BlockPos posToCheck)
    {
        if (posToCheck != null)
        {
            posMin.setPos(  Math.min(posMin.getX(), posToCheck.getX()),
                            Math.min(posMin.getY(), posToCheck.getY()),
                            Math.min(posMin.getZ(), posToCheck.getZ()));

            posMax.setPos(  Math.max(posMax.getX(), posToCheck.getX()),
                            Math.max(posMax.getY(), posToCheck.getY()),
                            Math.max(posMax.getZ(), posToCheck.getZ()));
        }
    }

    public static int getTotalVolume(Collection<Box> boxes)
    {
        if (boxes.isEmpty())
        {
            return 0;
        }

        int volume = 0;

        for (Box box : boxes)
        {
            if (isBoxValid(box))
            {
                BlockPos min = getMinCorner(box.getPos1(), box.getPos2());
                BlockPos max = getMaxCorner(box.getPos1(), box.getPos2());
                volume += (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
            }
        }

        return volume;
    }

    /**
     * Creates an enclosing AABB around the given positions. They will both be inside the box.
     */
    public static AxisAlignedBB createEnclosingAABB(BlockPos pos1, BlockPos pos2)
    {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        return createAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Creates an AABB for the given position
     */
    public static AxisAlignedBB createAABBForPosition(BlockPos pos)
    {
        return createAABBForPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Creates an AABB for the given position
     */
    public static AxisAlignedBB createAABBForPosition(int x, int y, int z)
    {
        return createAABB(x, y, z, x + 1, y + 1, z + 1);
    }

    /**
     * Creates an AABB with the given bounds
     */
    public static AxisAlignedBB createAABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Mirrors and then rotates the given position around the origin
     */
    public static BlockPos getTransformedBlockPos(BlockPos pos, Mirror mirror, Rotation rotation)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean isMirrored = true;

        switch (mirror)
        {
            // LEFT_RIGHT is essentially NORTH_SOUTH
            case LEFT_RIGHT:
                z = -z;
                break;
            // FRONT_BACK is essentially EAST_WEST
            case FRONT_BACK:
                x = -x;
                break;
            default:
                isMirrored = false;
        }

        switch (rotation)
        {
            case CLOCKWISE_90:
                return new BlockPos(-z, y,  x);
            case COUNTERCLOCKWISE_90:
                return new BlockPos( z, y, -x);
            case CLOCKWISE_180:
                return new BlockPos(-x, y, -z);
            default:
                return isMirrored ? new BlockPos(x, y, z) : pos;
        }
    }

    /**
     * Does the opposite transform from getTransformedBlockPos(), to return the original,
     * non-transformed position from the transformed position.
     */
    public static BlockPos getOriginalPositionFromTransformed(BlockPos pos, Mirror mirror, Rotation rotation)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int tmp;
        boolean noRotation = false;

        switch (rotation)
        {
            case CLOCKWISE_90:
                tmp = x;
                x = -z;
                z = tmp;
            case COUNTERCLOCKWISE_90:
                tmp = x;
                x = z;
                z = -tmp;
            case CLOCKWISE_180:
                x = -x;
                z = -z;
            default:
                noRotation = true;
        }

        switch (mirror)
        {
            case LEFT_RIGHT:
                z = -z;
                break;
            case FRONT_BACK:
                x = -x;
                break;
            default:
                if (noRotation)
                {
                    return pos;
                }
        }

        return new BlockPos(x, y, z);
    }

    public static Vec3d transformedVec3d(Vec3d vec, Mirror mirrorIn, Rotation rotationIn)
    {
        double x = vec.x;
        double y = vec.y;
        double z = vec.z;
        boolean isMirrored = true;

        switch (mirrorIn)
        {
            case LEFT_RIGHT:
                z = 1.0D - z;
                break;
            case FRONT_BACK:
                x = 1.0D - x;
                break;
            default:
                isMirrored = false;
        }

        switch (rotationIn)
        {
            case COUNTERCLOCKWISE_90:
                return new Vec3d(z, y, 1.0D - x);
            case CLOCKWISE_90:
                return new Vec3d(1.0D - z, y, x);
            case CLOCKWISE_180:
                return new Vec3d(1.0D - x, y, 1.0D - z);
            default:
                return isMirrored ? new Vec3d(x, y, z) : vec;
        }
    }

    /**
     * Gets the "front" facing from the given positions,
     * so that pos1 is in the "front left" corner and pos2 is in the "back right" corner
     * of the area, when looking at the "front" face of the area.
     */
    public static EnumFacing getFacingFromPositions(BlockPos pos1, BlockPos pos2)
    {
        if (pos1 == null || pos2 == null)
        {
            return null;
        }

        return getFacingFromPositions(pos1.getX(), pos1.getZ(), pos2.getX(), pos2.getZ());
    }

    private static EnumFacing getFacingFromPositions(int x1, int z1, int x2, int z2)
    {
        if (x2 == x1)
        {
            return z2 > z1 ? EnumFacing.SOUTH : EnumFacing.NORTH;
        }

        if (z2 == z1)
        {
            return x2 > x1 ? EnumFacing.EAST : EnumFacing.WEST;
        }

        if (x2 > x1)
        {
            return z2 > z1 ? EnumFacing.EAST : EnumFacing.NORTH;
        }

        return z2 > z1 ? EnumFacing.SOUTH : EnumFacing.WEST;
    }

    public static Rotation cycleRotation(Rotation rotation, boolean reverse)
    {
        int ordinal = rotation.ordinal();

        if (reverse)
        {
            ordinal = ordinal == 0 ? Rotation.values().length - 1 : ordinal - 1;
        }
        else
        {
            ordinal = ordinal >= Rotation.values().length - 1 ? 0 : ordinal + 1;
        }

        return Rotation.values()[ordinal];
    }

    public static Mirror cycleMirror(Mirror mirror, boolean reverse)
    {
        int ordinal = mirror.ordinal();

        if (reverse)
        {
            ordinal = ordinal == 0 ? Mirror.values().length - 1 : ordinal - 1;
        }
        else
        {
            ordinal = ordinal >= Mirror.values().length - 1 ? 0 : ordinal + 1;
        }

        return Mirror.values()[ordinal];
    }

    public static String getRotationNameShort(Rotation rotation)
    {
        switch (rotation)
        {
            case CLOCKWISE_90:          return "CW_90";
            case CLOCKWISE_180:         return "CW_180";
            case COUNTERCLOCKWISE_90:   return "CCW_90";
            case NONE:
            default:                    return "NONE";
        }
    }

    public static String getMirrorName(Mirror mirror)
    {
        switch (mirror)
        {
            case FRONT_BACK:    return "FRONT_BACK";
            case LEFT_RIGHT:    return "LEFT_RIGHT";
            case NONE:
            default:            return "NONE";
        }
    }

    public enum Corner
    {
        NONE,
        CORNER_1,
        CORNER_2;
    }
}
