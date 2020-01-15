package fi.dy.masa.litematica.schematic.placement;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;

public class GridPlacementManager
{
    private final SchematicPlacementManager schematicPlacementManager;
    private final HashMap<SchematicPlacement, HashMap<Vec3i, SchematicPlacement>> gridPlacementsPerPlacement = new HashMap<>();

    // This base placement set is used instead of the keySet of gridPlacementsPerPlacement
    // mostly because when logging in, the client player is at first in a wrong location,
    // and thus the repeat area might not overlap that initial client player's loaded area.
    // That would cause no grid placements to be created when loading the base placement from file,
    // and then the periodic update would not know to handle that base placement at all.
    private final HashSet<SchematicPlacement> basePlacements = new HashSet<>();

    public GridPlacementManager(SchematicPlacementManager schematicPlacementManager)
    {
        this.schematicPlacementManager = schematicPlacementManager;
    }

    public void clear()
    {
        this.gridPlacementsPerPlacement.clear();
        this.basePlacements.clear();
    }

    void updateGridPlacementsFor(SchematicPlacement basePlacement)
    {
        // Never accidentally repeat the repeated placements
        if (basePlacement.isRepeatedPlacement())
        {
            return;
        }

        boolean existedBefore = this.basePlacements.contains(basePlacement);

        // Grid repeating was enabled, or setting were changed
        if (basePlacement.isEnabled() && basePlacement.getGridSettings().isEnabled())
        {
            // Grid settings changed for an existing grid placement
            if (existedBefore)
            {
                this.removeAllGridPlacementsOf(basePlacement, false);
            }
            else
            {
                this.basePlacements.add(basePlacement);
            }

            this.addGridPlacementsWithinLoadedAreaFor(basePlacement, true);
        }
        // Grid repeating disabled
        else if (existedBefore)
        {
            this.removeAllGridPlacementsOf(basePlacement, true);
            this.basePlacements.remove(basePlacement);
        }
    }

    private HashMap<Vec3i, SchematicPlacement> createGridPlacementsWithinLoadedAreaFor(SchematicPlacement basePlacement)
    {
        Box currentArea = this.getCurrentLoadedArea(1);

        if (currentArea != null)
        {
            return this.createGridPlacementsWithinAreaFor(basePlacement, currentArea);
        }

        return new HashMap<>();
    }

    private HashSet<Vec3i> getGridPointsWithinAreaFor(SchematicPlacement basePlacement, Box area)
    {
        HashSet<Vec3i> points = new HashSet<>();
        GridSettings settings = basePlacement.getGridSettings();
        Vec3i size = settings.getSize();

        if (settings.isEnabled() && PositionUtils.areAllCoordinatesAtLeast(size, 1))
        {
            IntBoundingBox repeat = settings.getRepeatCounts();
            int sizeX = size.getX();
            int sizeY = size.getY();
            int sizeZ = size.getZ();
            Box baseBox = basePlacement.getEclosingBox();
            BlockPos repeatAreaMinCorner = baseBox.getPos1().add(-repeat.minX * sizeX, -repeat.minY * sizeY, -repeat.minZ * sizeZ);
            BlockPos repeatAreaMaxCorner = baseBox.getPos2().add( repeat.maxX * sizeX,  repeat.maxY * sizeY,  repeat.maxZ * sizeZ);
            Box repeatEnclosingBox = new Box(repeatAreaMinCorner, repeatAreaMaxCorner);

            // Get the box where the repeated placements intersect the target area
            Box intersectingBox = repeatEnclosingBox.createIntersectingBox(area);
            //System.out.printf("plop 2, size: %s, rep encl: %s .. %s\n", size, repeatEnclosingBox.getPos1(), repeatEnclosingBox.getPos2());

            if (intersectingBox != null)
            {
                // Get the minimum and maximum repeat counts of the edge-most repeated placements that
                // touch the intersection box.
                BlockPos p1 = intersectingBox.getPos1();
                BlockPos p2 = intersectingBox.getPos2();
                BlockPos baseMinCorner = baseBox.getPos1();

                //System.out.printf("inters min: %s, max: %s\n", p1, p2);
                int minX = (p1.getX() - baseMinCorner.getX()) / sizeX;
                int minY = (p1.getY() - baseMinCorner.getY()) / sizeY;
                int minZ = (p1.getZ() - baseMinCorner.getZ()) / sizeZ;
                int maxX = (p2.getX() - baseMinCorner.getX()) / sizeX;
                int maxY = (p2.getY() - baseMinCorner.getY()) / sizeY;
                int maxZ = (p2.getZ() - baseMinCorner.getZ()) / sizeZ;
                //System.out.printf("rep: x: %d .. %d, y: %d .. %d, z: %d .. %s\n", minX, maxX, minY, maxY, minZ, maxZ);

                for (int y = minY; y <= maxY; ++y)
                {
                    for (int z = minZ; z <= maxZ; ++z)
                    {
                        for (int x = minX; x <= maxX; ++x)
                        {
                            if (x != 0 || y != 0 || z != 0)
                            {
                                points.add(new Vec3i(x, y, z));
                                //System.out.printf("repeat placement @ %s [%d, %d, %d]\n", placement.getOrigin(), repX, repY, repZ);
                            }
                        }
                    }
                }
            }
        }

        return points;
    }

    private HashMap<Vec3i, SchematicPlacement> createGridPlacementsWithinAreaFor(SchematicPlacement basePlacement, Box area)
    {
        HashSet<Vec3i> gridPoints = this.getGridPointsWithinAreaFor(basePlacement, area);
        return this.createGridPlacementsForPoints(basePlacement, gridPoints);
    }

    private HashMap<Vec3i, SchematicPlacement> createGridPlacementsForPoints(SchematicPlacement basePlacement, HashSet<Vec3i> gridPoints)
    {
        HashMap<Vec3i, SchematicPlacement> placements = new HashMap<>();

        if (gridPoints.isEmpty() == false)
        {
            BlockPos baseOrigin = basePlacement.getOrigin();
            Vec3i size = basePlacement.getGridSettings().getSize();
            int sizeX = size.getX();
            int sizeY = size.getY();
            int sizeZ = size.getZ();

            for (Vec3i point : gridPoints)
            {
                SchematicPlacement placement = basePlacement.copy(true);
                placement.setOrigin(baseOrigin.add(point.getX() * sizeX, point.getY() * sizeY, point.getZ() * sizeZ));
                placement.updateEnclosingBox();
                placements.put(point, placement);
                //System.out.printf("repeat placement @ %s [%d, %d, %d]\n", placement.getOrigin(), point.getX(), point.getY(), point.getZ());
            }
        }

        return placements;
    }

    private HashSet<Vec3i> getExistingOutOfRangeGridPointsFor(SchematicPlacement basePlacement, HashSet<Vec3i> currentGridPoints)
    {
        HashMap<Vec3i, SchematicPlacement> placements = this.gridPlacementsPerPlacement.get(basePlacement);

        if (placements != null)
        {
            HashSet<Vec3i> outOfRangePoints = new HashSet<>(placements.keySet());
            outOfRangePoints.removeAll(currentGridPoints);
            return outOfRangePoints;
        }

        return new HashSet<>();
    }

    private HashSet<Vec3i> getNewGridPointsFor(SchematicPlacement basePlacement, HashSet<Vec3i> currentGridPoints)
    {
        HashMap<Vec3i, SchematicPlacement> placements = this.gridPlacementsPerPlacement.get(basePlacement);

        if (placements != null)
        {
            HashSet<Vec3i> newPoints = new HashSet<>(currentGridPoints);
            newPoints.removeAll(placements.keySet());
            return newPoints;
        }
        else
        {
            return currentGridPoints;
        }
    }

    private void addGridPlacementsWithinLoadedAreaFor(SchematicPlacement basePlacement, boolean updateOverlay)
    {
        HashMap<Vec3i, SchematicPlacement> placements = this.createGridPlacementsWithinLoadedAreaFor(basePlacement);

        if (placements.isEmpty() == false)
        {
            this.addGridPlacements(basePlacement, placements, updateOverlay);
        }
    }

    void removeAllGridPlacementsOf(SchematicPlacement basePlacement, boolean updateOverlay)
    {
        HashMap<Vec3i, SchematicPlacement> placements = this.gridPlacementsPerPlacement.get(basePlacement);

        if (placements != null)
        {
            // Create a copy of the key set to avoid CME
            HashSet<Vec3i> points = new HashSet<>(placements.keySet());
            this.removeGridPlacements(basePlacement, points, updateOverlay);
        }
    }

    void createOrRemoveGridPlacementsForLoadedArea()
    {
        Box currentArea = this.getCurrentLoadedArea(1);

        if (currentArea != null)
        {
            boolean modified = false;

            for (SchematicPlacement basePlacement : this.basePlacements)
            {
                HashSet<Vec3i> currentGridPoints = this.getGridPointsWithinAreaFor(basePlacement, currentArea);
                HashSet<Vec3i> outOfRangePoints = this.getExistingOutOfRangeGridPointsFor(basePlacement, currentGridPoints);
                HashSet<Vec3i> newPoints = this.getNewGridPointsFor(basePlacement, currentGridPoints);
                //System.out.printf("c: %d, o: %d, n: %d\n", currentGridPoints.size(), outOfRangePoints.size(), newPoints.size());

                if (outOfRangePoints.isEmpty() == false)
                {
                    this.removeGridPlacements(basePlacement, outOfRangePoints, false);
                    modified = true;
                }

                if (newPoints.isEmpty() == false)
                {
                    HashMap<Vec3i, SchematicPlacement> placements = this.createGridPlacementsForPoints(basePlacement, newPoints);
                    this.addGridPlacements(basePlacement, placements, false);
                    modified = true;
                }
            }

            if (modified)
            {
                OverlayRenderer.getInstance().updatePlacementCache();
            }
        }
    }

    private void addGridPlacements(SchematicPlacement basePlacement, HashMap<Vec3i, SchematicPlacement> placements, boolean updateOverlay)
    {
        if (placements.isEmpty() == false)
        {
            HashMap<Vec3i, SchematicPlacement> map = this.gridPlacementsPerPlacement.get(basePlacement);

            if (map == null)
            {
                map = new HashMap<>();
                this.gridPlacementsPerPlacement.put(basePlacement, map);
            }

            for (Map.Entry<Vec3i, SchematicPlacement> entry : placements.entrySet())
            {
                Vec3i point = entry.getKey();
                SchematicPlacement placement = entry.getValue();

                if (map.containsKey(point) == false)
                {
                    map.put(point, placement);
                    this.schematicPlacementManager.addVisiblePlacement(placement);
                    this.schematicPlacementManager.addTouchedChunksFor(placement, false);
                }
            }

            if (updateOverlay)
            {
                OverlayRenderer.getInstance().updatePlacementCache();
            }
        }
    }

    private void removeGridPlacements(SchematicPlacement basePlacement, Collection<Vec3i> gridPoints, boolean updateOverlay)
    {
        HashMap<Vec3i, SchematicPlacement> map = this.gridPlacementsPerPlacement.get(basePlacement);

        if (gridPoints.isEmpty() == false && map != null)
        {
            for (Vec3i point : gridPoints)
            {
                SchematicPlacement placement = map.get(point);

                if (placement != null)
                {
                    this.schematicPlacementManager.removeTouchedChunksFor(placement);
                    this.schematicPlacementManager.removeVisiblePlacement(placement);
                    map.remove(point);
                }
            }

            if (updateOverlay)
            {
                OverlayRenderer.getInstance().updatePlacementCache();
            }
        }
    }

    @Nullable
    private Box getCurrentLoadedArea(int expandChunks)
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player == null)
        {
            return null;
        }

        int centerChunkX = ((int) Math.floor(player.posX)) >> 4;
        int centerChunkZ = ((int) Math.floor(player.posZ)) >> 4;
        int chunkRadius = mc.gameSettings.renderDistanceChunks + expandChunks;
        BlockPos corner1 = new BlockPos( (centerChunkX - chunkRadius) << 4      ,   0,  (centerChunkZ - chunkRadius) << 4      );
        BlockPos corner2 = new BlockPos(((centerChunkX + chunkRadius) << 4) + 15, 255, ((centerChunkZ + chunkRadius) << 4) + 15);

        return new Box(corner1, corner2);
    }
}
