package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class PositionUtils
{
    public static final BlockPosComparator BLOCK_POS_COMPARATOR = new BlockPosComparator();
    public static final ChunkPosComparator CHUNK_POS_COMPARATOR = new ChunkPosComparator();

    public static final EnumFacing.Axis[] AXES_ALL = new EnumFacing.Axis[] { EnumFacing.Axis.X, EnumFacing.Axis.Y, EnumFacing.Axis.Z };
    public static final EnumFacing[] FACING_ALL = new EnumFacing[] { EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST };
    public static final EnumFacing[] ADJACENT_SIDES_ZY = new EnumFacing[] { EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH };
    public static final EnumFacing[] ADJACENT_SIDES_XY = new EnumFacing[] { EnumFacing.DOWN, EnumFacing.UP, EnumFacing.EAST, EnumFacing.WEST };
    public static final EnumFacing[] ADJACENT_SIDES_XZ = new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST };

    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XN_ZN = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i(-1,  0,  0), new Vec3i( 0,  0, -1), new Vec3i(-1,  0, -1) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XP_ZN = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i( 1,  0,  0), new Vec3i( 0,  0, -1), new Vec3i( 1,  0, -1) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XN_ZP = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i(-1,  0,  0), new Vec3i( 0,  0,  1), new Vec3i(-1,  0,  1) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XP_ZP = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i( 1,  0,  0), new Vec3i( 0,  0,  1), new Vec3i( 1,  0,  1) };
    private static final Vec3i[][] EDGE_NEIGHBOR_OFFSETS_Y = new Vec3i[][] { EDGE_NEIGHBOR_OFFSETS_XN_ZN, EDGE_NEIGHBOR_OFFSETS_XP_ZN, EDGE_NEIGHBOR_OFFSETS_XN_ZP, EDGE_NEIGHBOR_OFFSETS_XP_ZP };

    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XN_YN = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i(-1,  0,  0), new Vec3i( 0, -1,  0), new Vec3i(-1, -1,  0) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XP_YN = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i( 1,  0,  0), new Vec3i( 0, -1,  0), new Vec3i( 1, -1,  0) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XN_YP = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i(-1,  0,  0), new Vec3i( 0,  1,  0), new Vec3i(-1,  1,  0) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XP_YP = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i( 1,  0,  0), new Vec3i( 0,  1,  0), new Vec3i( 1,  1,  0) };
    private static final Vec3i[][] EDGE_NEIGHBOR_OFFSETS_Z = new Vec3i[][] { EDGE_NEIGHBOR_OFFSETS_XN_YN, EDGE_NEIGHBOR_OFFSETS_XP_YN, EDGE_NEIGHBOR_OFFSETS_XN_YP, EDGE_NEIGHBOR_OFFSETS_XP_YP };

    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_YN_ZN = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i( 0, -1,  0), new Vec3i( 0,  0, -1), new Vec3i( 0, -1, -1) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_YP_ZN = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i( 0,  1,  0), new Vec3i( 0,  0, -1), new Vec3i( 0,  1, -1) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_YN_ZP = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i( 0, -1,  0), new Vec3i( 0,  0,  1), new Vec3i( 0, -1,  1) };
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_YP_ZP = new Vec3i[] { new Vec3i( 0,  0,  0), new Vec3i( 0,  1,  0), new Vec3i( 0,  0,  1), new Vec3i( 0,  1,  1) };
    private static final Vec3i[][] EDGE_NEIGHBOR_OFFSETS_X = new Vec3i[][] { EDGE_NEIGHBOR_OFFSETS_YN_ZN, EDGE_NEIGHBOR_OFFSETS_YP_ZN, EDGE_NEIGHBOR_OFFSETS_YN_ZP, EDGE_NEIGHBOR_OFFSETS_YP_ZP };

    public static Vec3i[] getEdgeNeighborOffsets(EnumFacing.Axis axis, int cornerIndex)
    {
        switch (axis)
        {
            case X: return EDGE_NEIGHBOR_OFFSETS_X[cornerIndex];
            case Y: return EDGE_NEIGHBOR_OFFSETS_Y[cornerIndex];
            case Z: return EDGE_NEIGHBOR_OFFSETS_Z[cornerIndex];
        }

        return null;
    }

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

    public static BlockPos getTransformedPlacementPosition(BlockPos posWithinSub, SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        BlockPos pos = posWithinSub;
        pos = getTransformedBlockPos(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        pos = getTransformedBlockPos(pos, placement.getMirror(), placement.getRotation());
        return pos;
    }

    public static boolean arePositionsWithinWorld(World world, BlockPos pos1, BlockPos pos2)
    {
        if (pos1.getY() >= LayerRange.WORLD_VERTICAL_SIZE_MIN && pos1.getY() <= LayerRange.WORLD_VERTICAL_SIZE_MAX &&
            pos2.getY() >= LayerRange.WORLD_VERTICAL_SIZE_MIN && pos2.getY() <= LayerRange.WORLD_VERTICAL_SIZE_MAX)
        {
            WorldBorder border = world.getWorldBorder();
            return border.contains(pos1) && border.contains(pos2);
        }

        return false;
    }

    public static boolean isBoxWithinWorld(World world, Box box)
    {
        if (box.getPos1() != null && box.getPos2() != null)
        {
            return arePositionsWithinWorld(world, box.getPos1(), box.getPos2());
        }

        return false;
    }

    public static boolean isPlacementWithinWorld(World world, SchematicPlacement schematicPlacement, boolean respectRenderRange)
    {
        LayerRange range = DataManager.getRenderLayerRange();
        BlockPos.MutableBlockPos posMutable1 = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos posMutable2 = new BlockPos.MutableBlockPos();

        for (Box box : schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values())
        {
            if (respectRenderRange)
            {
                if (range.intersectsBox(box.getPos1(), box.getPos2()))
                {
                    StructureBoundingBox bb = range.getClampedArea(box.getPos1(), box.getPos2());

                    if (bb != null)
                    {
                        posMutable1.setPos(bb.minX, bb.minY, bb.minZ);
                        posMutable2.setPos(bb.maxX, bb.maxY, bb.maxZ);

                        if (arePositionsWithinWorld(world, posMutable1, posMutable2) == false)
                        {
                            return false;
                        }
                    }
                }
            }
            else if (isBoxWithinWorld(world, box) == false)
            {
                return false;
            }
        }

        return true;
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

    public static BlockPos getAreaSizeFromRelativeEndPositionAbs(BlockPos posEndRelative)
    {
        int x = posEndRelative.getX();
        int y = posEndRelative.getY();
        int z = posEndRelative.getZ();

        x = x >= 0 ? x + 1 : x - 1;
        y = y >= 0 ? y + 1 : y - 1;
        z = z >= 0 ? z + 1 : z - 1;

        return new BlockPos(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    public static BlockPos getRelativeEndPositionFromAreaSize(Vec3i size)
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

    public static ImmutableMap<String, StructureBoundingBox> getBoxesWithinChunk(int chunkX, int chunkZ, ImmutableMap<String, Box> subRegions)
    {
        ImmutableMap.Builder<String, StructureBoundingBox> builder = new ImmutableMap.Builder<>();

        for (Map.Entry<String, Box> entry : subRegions.entrySet())
        {
            Box box = entry.getValue();
            StructureBoundingBox bb = box != null ? PositionUtils.getBoundsWithinChunkForBox(box, chunkX, chunkZ) : null;

            if (bb != null)
            {
                builder.put(entry.getKey(), bb);
            }
        }

        return builder.build();
    }

    public static Set<ChunkPos> getTouchedChunks(ImmutableMap<String, Box> boxes)
    {
        return getTouchedChunksForBoxes(boxes.values());
    }

    public static Set<ChunkPos> getTouchedChunksForBoxes(Collection<Box> boxes)
    {
        Set<ChunkPos> set = new HashSet<>();

        for (Box box : boxes)
        {
            final int boxXMin = Math.min(box.getPos1().getX(), box.getPos2().getX()) >> 4;
            final int boxZMin = Math.min(box.getPos1().getZ(), box.getPos2().getZ()) >> 4;
            final int boxXMax = Math.max(box.getPos1().getX(), box.getPos2().getX()) >> 4;
            final int boxZMax = Math.max(box.getPos1().getZ(), box.getPos2().getZ()) >> 4;

            for (int cz = boxZMin; cz <= boxZMax; ++cz)
            {
                for (int cx = boxXMin; cx <= boxXMax; ++cx)
                {
                    set.add(new ChunkPos(cx, cz));
                }
            }
        }

        return set;
    }

    @Nullable
    public static StructureBoundingBox getBoundsWithinChunkForBox(Box box, int chunkX, int chunkZ)
    {
        final int chunkXMin = chunkX << 4;
        final int chunkZMin = chunkZ << 4;
        final int chunkXMax = chunkXMin + 15;
        final int chunkZMax = chunkZMin + 15;

        final int boxXMin = Math.min(box.getPos1().getX(), box.getPos2().getX());
        final int boxZMin = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
        final int boxXMax = Math.max(box.getPos1().getX(), box.getPos2().getX());
        final int boxZMax = Math.max(box.getPos1().getZ(), box.getPos2().getZ());

        boolean notOverlapping = boxXMin > chunkXMax || boxZMin > chunkZMax || boxXMax < chunkXMin || boxZMax < chunkZMin;

        if (notOverlapping == false)
        {
            final int xMin = Math.max(chunkXMin, boxXMin);
            final int yMin = Math.min(box.getPos1().getY(), box.getPos2().getY());
            final int zMin = Math.max(chunkZMin, boxZMin);
            final int xMax = Math.min(chunkXMax, boxXMax);
            final int yMax = Math.max(box.getPos1().getY(), box.getPos2().getY());
            final int zMax = Math.min(chunkZMax, boxZMax);

            return new StructureBoundingBox(xMin, yMin, zMin, xMax, yMax, zMax);
        }

        return null;
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

    public static AxisAlignedBB createAABBFrom(StructureBoundingBox bb)
    {
        return createAABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
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
     * Returns the given position adjusted such that the coordinate indicated by <b>type</b>
     * is set to the value in <b>value</b>
     * @param pos
     * @param value
     * @param type
     * @return
     */
    public static BlockPos getModifiedPosition(BlockPos pos, int value, CoordinateType type)
    {

        switch (type)
        {
            case X:
                pos = new BlockPos(     value, pos.getY(), pos.getZ());
                break;
            case Y:
                pos = new BlockPos(pos.getX(),      value, pos.getZ());
                break;
            case Z:
                pos = new BlockPos(pos.getX(), pos.getY(),      value);
                break;
        }

        return pos;
    }

    public static int getCoordinate(BlockPos pos, CoordinateType type)
    {
        switch (type)
        {
            case X: return pos.getX();
            case Y: return pos.getY();
            case Z: return pos.getZ();
        }

        return 0;
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

    public static BlockPos getReverseTransformedBlockPos(BlockPos pos, Mirror mirror, Rotation rotation)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean isRotated = true;
        int tmp = x;

        switch (rotation)
        {
            case CLOCKWISE_90:
                x = z;
                z = -tmp;
                break;
            case COUNTERCLOCKWISE_90:
                x = -z;
                z = tmp;
                break;
            case CLOCKWISE_180:
                x = -x;
                z = -z;
                break;
            default:
                isRotated = false;
        }

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
                if (isRotated == false)
                {
                    return pos;
                }
        }

        return new BlockPos(x, y, z);
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

    public static Vec3d getTransformedPosition(Vec3d originalPos, Mirror mirror, Rotation rotation)
    {
        double x = originalPos.x;
        double y = originalPos.y;
        double z = originalPos.z;
        boolean transformed = true;

        switch (mirror)
        {
            case LEFT_RIGHT:
                z = 1.0D - z;
                break;
            case FRONT_BACK:
                x = 1.0D - x;
                break;
            default:
                transformed = false;
        }

        switch (rotation)
        {
            case COUNTERCLOCKWISE_90:
                return new Vec3d(z, y, 1.0D - x);
            case CLOCKWISE_90:
                return new Vec3d(1.0D - z, y, x);
            case CLOCKWISE_180:
                return new Vec3d(1.0D - x, y, 1.0D - z);
            default:
                return transformed ? new Vec3d(x, y, z) : originalPos;
        }
    }

    public static BlockPos getModifiedPartiallyLockedPosition(BlockPos posOriginal, BlockPos posNew, int lockMask)
    {
        if (lockMask != 0)
        {
            int x = posNew.getX();
            int y = posNew.getY();
            int z = posNew.getZ();

            if ((lockMask & (0x1 << CoordinateType.X.ordinal())) != 0)
            {
                x = posOriginal.getX();
            }

            if ((lockMask & (0x1 << CoordinateType.Y.ordinal())) != 0)
            {
                y = posOriginal.getY();
            }

            if ((lockMask & (0x1 << CoordinateType.Z.ordinal())) != 0)
            {
                z = posOriginal.getZ();
            }

            posNew = new BlockPos(x, y, z);
        }

        return posNew;
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

    public static float getRotatedYaw(float yaw, Rotation rotation)
    {
        switch (rotation)
        {
            case CLOCKWISE_180:
                yaw += 180.0F;
                break;
            case COUNTERCLOCKWISE_90:
                yaw += 270.0F;
                break;
            case CLOCKWISE_90:
                yaw += 90.0F;
                break;
            default:
        }

        return MathHelper.wrapDegrees(yaw);
    }

    public static float getMirroredYaw(float yaw, Mirror mirror)
    {
        switch (mirror)
        {
            case LEFT_RIGHT:
                yaw = 180.0F - yaw;
                break;
            case FRONT_BACK:
                yaw = -yaw;
                break;
            default:
        }

        return MathHelper.wrapDegrees(yaw);
    }

    public static class BlockPosComparator implements Comparator<BlockPos>
    {
        private BlockPos posReference = BlockPos.ORIGIN;
        private boolean closestFirst;

        public void setClosestFirst(boolean closestFirst)
        {
            this.closestFirst = closestFirst;
        }

        public void setReferencePosition(BlockPos pos)
        {
            this.posReference = pos;
        }

        @Override
        public int compare(BlockPos pos1, BlockPos pos2)
        {
            double dist1 = pos1.distanceSq(this.posReference);
            double dist2 = pos2.distanceSq(this.posReference);

            if (dist1 == dist2)
            {
                return 0;
            }

            return dist1 < dist2 == this.closestFirst ? -1 : 1;
        }
    }

    public static class ChunkPosComparator implements Comparator<ChunkPos>
    {
        private BlockPos posReference = BlockPos.ORIGIN;
        private boolean closestFirst;

        public ChunkPosComparator setClosestFirst(boolean closestFirst)
        {
            this.closestFirst = closestFirst;
            return this;
        }

        public ChunkPosComparator setReferencePosition(BlockPos pos)
        {
            this.posReference = pos;
            return this;
        }

        @Override
        public int compare(ChunkPos pos1, ChunkPos pos2)
        {
            double dist1 = this.distanceSq(pos1);
            double dist2 = this.distanceSq(pos2);

            if (dist1 == dist2)
            {
                return 0;
            }

            return dist1 < dist2 == this.closestFirst ? -1 : 1;
        }

        private double distanceSq(ChunkPos pos)
        {
            double dx = (double) (pos.x << 4) - this.posReference.getX();
            double dz = (double) (pos.z << 4) - this.posReference.getZ();

            return dx * dx + dz * dz;
        }
    }

    public enum Corner
    {
        NONE,
        CORNER_1,
        CORNER_2;
    }
}
