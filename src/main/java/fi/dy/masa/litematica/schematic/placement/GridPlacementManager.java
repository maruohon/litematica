package fi.dy.masa.litematica.schematic.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.GameUtils;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;

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

    public ArrayList<SchematicPlacement> getGridPlacementsForBasePlacement(SchematicPlacement basePlacement)
    {
        ArrayList<SchematicPlacement> gridPlacements = new ArrayList<>();
        HashMap<Vec3i, SchematicPlacement> map = this.gridPlacementsPerPlacement.get(basePlacement);

        if (map != null)
        {
            gridPlacements.addAll(map.values());
        }

        return gridPlacements;
    }

    void updateGridPlacementsFor(SchematicPlacement basePlacement)
    {
        // Never accidentally repeat the repeated placements
        if (basePlacement.isRepeatedPlacement())
        {
            return;
        }

        boolean existedBefore = this.basePlacements.contains(basePlacement);
        boolean modified = false;

        // Grid repeating was enabled, or setting were changed
        if (basePlacement.isEnabled() && basePlacement.getGridSettings().isEnabled())
        {
            // Grid settings changed for an existing grid placement
            if (existedBefore)
            {
                modified |= this.removeAllGridPlacementsOf(basePlacement);
            }
            else
            {
                this.basePlacements.add(basePlacement);
            }

            modified |= this.addGridPlacementsWithinLoadedAreaFor(basePlacement);
        }
        // Grid repeating disabled
        else if (existedBefore)
        {
            modified |= this.removeAllGridPlacementsOf(basePlacement);
            this.basePlacements.remove(basePlacement);
        }

        if (modified)
        {
            OverlayRenderer.getInstance().updatePlacementCache();
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
            Vec3i repeatNeg = settings.getRepeatNegative();
            Vec3i repeatPos = settings.getRepeatPositive();
            int sizeX = size.getX();
            int sizeY = size.getY();
            int sizeZ = size.getZ();
            Box baseBox = basePlacement.getEnclosingBox();
            BlockPos repeatAreaMinCorner = baseBox.getPos1().add(-repeatNeg.getX() * sizeX, -repeatNeg.getY() * sizeY, -repeatNeg.getZ() * sizeZ);
            BlockPos repeatAreaMaxCorner = baseBox.getPos2().add( repeatPos.getX() * sizeX,  repeatPos.getY() * sizeY,  repeatPos.getZ() * sizeZ);
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
                SchematicPlacement placement = basePlacement.copyAsFullyLoaded(true);
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

    /**
     * Creates and adds all the grid placements within the loaded area
     * for the provided normal placement
     * @return true if some placements were added
     */
    private boolean addGridPlacementsWithinLoadedAreaFor(SchematicPlacement basePlacement)
    {
        HashMap<Vec3i, SchematicPlacement> placements = this.createGridPlacementsWithinLoadedAreaFor(basePlacement);

        if (placements.isEmpty() == false)
        {
            return this.addGridPlacements(basePlacement, placements);
        }

        return false;
    }

    /**
     * Removes all grid placements of the provided placement, and the base placement itself
     * so that the automatic updating doesn't re-create them.
     * @param basePlacement
     */
    void onPlacementRemoved(SchematicPlacement basePlacement)
    {
        this.removeAllGridPlacementsOf(basePlacement);
        this.basePlacements.remove(basePlacement);
    }

    /**
     * Removes all repeated grid placements of the provided normal placement
     * @return true if some placements were removed
     */
    private boolean removeAllGridPlacementsOf(SchematicPlacement basePlacement)
    {
        HashMap<Vec3i, SchematicPlacement> placements = this.gridPlacementsPerPlacement.get(basePlacement);

        if (placements != null)
        {
            // Create a copy of the key set to avoid CME
            HashSet<Vec3i> points = new HashSet<>(placements.keySet());
            return this.removeGridPlacements(basePlacement, points);
        }

        return false;
    }

    /**
     * Updates the grid placements for the provided normal placement,
     * adding or removing placements as needed so that the current
     * loaded area has all the required grid placements.
     * @return true if some placements were added or removed
     */
    boolean createOrRemoveGridPlacementsForLoadedArea()
    {
        Box currentArea = this.getCurrentLoadedArea(1);
        boolean modified = false;

        if (currentArea != null)
        {
            for (SchematicPlacement basePlacement : this.basePlacements)
            {
                HashSet<Vec3i> currentGridPoints = this.getGridPointsWithinAreaFor(basePlacement, currentArea);
                HashSet<Vec3i> outOfRangePoints = this.getExistingOutOfRangeGridPointsFor(basePlacement, currentGridPoints);
                HashSet<Vec3i> newPoints = this.getNewGridPointsFor(basePlacement, currentGridPoints);
                //System.out.printf("c: %d, o: %d, n: %d\n", currentGridPoints.size(), outOfRangePoints.size(), newPoints.size());

                if (outOfRangePoints.isEmpty() == false)
                {
                    modified |= this.removeGridPlacements(basePlacement, outOfRangePoints);
                }

                if (newPoints.isEmpty() == false)
                {
                    HashMap<Vec3i, SchematicPlacement> placements = this.createGridPlacementsForPoints(basePlacement, newPoints);
                    modified |= this.addGridPlacements(basePlacement, placements);
                }
            }
        }

        if (modified)
        {
            OverlayRenderer.getInstance().updatePlacementCache();
        }

        return modified;
    }

    /**
     * Adds the provided grid placements for the provided normal placement,
     * if there are no grid placements yet for those grid points.
     * @param basePlacement
     * @param placements
     * @return true if some placements were added
     */
    private boolean addGridPlacements(SchematicPlacement basePlacement, HashMap<Vec3i, SchematicPlacement> placements)
    {
        boolean modified = false;

        if (placements.isEmpty() == false)
        {
            HashMap<Vec3i, SchematicPlacement> map = this.gridPlacementsPerPlacement.computeIfAbsent(basePlacement, k -> new HashMap<>());

            for (Map.Entry<Vec3i, SchematicPlacement> entry : placements.entrySet())
            {
                Vec3i point = entry.getKey();
                SchematicPlacement placement = entry.getValue();

                if (map.containsKey(point) == false)
                {
                    map.put(point, placement);
                    this.schematicPlacementManager.addVisiblePlacement(placement);
                    this.schematicPlacementManager.addTouchedChunksFor(placement, false);
                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     * Removes the grid placements of the provided normal placement
     * from the requested grid points, if they exist
     * @param basePlacement
     * @param gridPoints
     * @return true if some placements were removed
     */
    private boolean removeGridPlacements(SchematicPlacement basePlacement, Collection<Vec3i> gridPoints)
    {
        HashMap<Vec3i, SchematicPlacement> map = this.gridPlacementsPerPlacement.get(basePlacement);
        boolean modified = false;

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
                    modified = true;
                }
            }
        }

        return modified;
    }

    @Nullable
    private Box getCurrentLoadedArea(int expandChunks)
    {
        EntityPlayer player = GameUtils.getClientPlayer();

        if (player == null)
        {
            return null;
        }

        int centerChunkX = ((int) Math.floor(EntityUtils.getX(player))) >> 4;
        int centerChunkZ = ((int) Math.floor(EntityUtils.getZ(player))) >> 4;
        int chunkRadius = GameUtils.getRenderDistanceChunks() + expandChunks;
        BlockPos corner1 = new BlockPos( (centerChunkX - chunkRadius) << 4      ,   0,  (centerChunkZ - chunkRadius) << 4      );
        BlockPos corner2 = new BlockPos(((centerChunkX + chunkRadius) << 4) + 15, 255, ((centerChunkZ + chunkRadius) << 4) + 15);

        return new Box(corner1, corner2);
    }
}
