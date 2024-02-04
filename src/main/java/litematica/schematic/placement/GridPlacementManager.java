package litematica.schematic.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;

import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.Vec3i;
import litematica.render.OverlayRenderer;
import litematica.util.PositionUtils;

public class GridPlacementManager
{
    private final SchematicPlacementManager schematicPlacementManager;
    private final Map<SchematicPlacement, Map<Vec3i, SchematicPlacement>> gridPlacementsPerPlacement = new HashMap<>();

    // This base placement set is used instead of the keySet of gridPlacementsPerPlacement
    // mostly because when logging in, the client player is at first in a wrong location,
    // and thus the repeat area might not overlap that initial client player's loaded area.
    // That would cause no grid placements to be created when loading the base placement from file,
    // and then the periodic update would not know to handle that base placement at all.
    private final Set<SchematicPlacement> basePlacements = new HashSet<>();

    public GridPlacementManager(SchematicPlacementManager schematicPlacementManager)
    {
        this.schematicPlacementManager = schematicPlacementManager;
    }

    public void clear()
    {
        this.gridPlacementsPerPlacement.clear();
        this.basePlacements.clear();
    }

    public List<SchematicPlacement> getGridPlacementsForBasePlacement(SchematicPlacement basePlacement)
    {
        List<SchematicPlacement> gridPlacements = new ArrayList<>();
        Map<Vec3i, SchematicPlacement> map = this.gridPlacementsPerPlacement.get(basePlacement);

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

    private Map<Vec3i, SchematicPlacement> createGridPlacementsWithinLoadedAreaFor(SchematicPlacement basePlacement)
    {
        IntBoundingBox currentArea = this.getCurrentLoadedArea(1);

        if (currentArea != null)
        {
            return this.createGridPlacementsWithinAreaFor(basePlacement, currentArea);
        }

        return new HashMap<>();
    }

    private Set<Vec3i> getGridPointsWithinAreaFor(SchematicPlacement basePlacement, IntBoundingBox area)
    {
        Set<Vec3i> points = new HashSet<>();
        GridSettings settings = basePlacement.getGridSettings();
        Vec3i size = settings.getSize();

        if (settings.isEnabled() && PositionUtils.areAllCoordinatesAtLeast(size, 1))
        {
            Vec3i repeatNeg = settings.getRepeatNegative();
            Vec3i repeatPos = settings.getRepeatPositive();
            int sizeX = size.getX();
            int sizeY = size.getY();
            int sizeZ = size.getZ();
            IntBoundingBox baseBox = basePlacement.getEnclosingBox();
            IntBoundingBox repeatEnclosingBox = IntBoundingBox.createProper(baseBox.minX - repeatNeg.getX() * sizeX,
                                                                            baseBox.minY - repeatNeg.getY() * sizeY,
                                                                            baseBox.minZ - repeatNeg.getZ() * sizeZ,
                                                                            baseBox.maxX + repeatPos.getX() * sizeX,
                                                                            baseBox.maxY + repeatPos.getY() * sizeY,
                                                                            baseBox.maxZ + repeatPos.getZ() * sizeZ);

            // Get the box where the repeated placements intersect the target area
            //Box intersectingBox = repeatEnclosingBox.createIntersectingBox(area);
            IntBoundingBox intersectingBox = repeatEnclosingBox.createIntersectingBox(area);
            //System.out.printf("plop 2, size: %s, rep encl: %s .. %s\n", size, repeatEnclosingBox.getPos1(), repeatEnclosingBox.getPos2());

            if (intersectingBox != null)
            {
                // Get the minimum and maximum repeat counts of the edge-most repeated placements that
                // touch the intersection box.
                //System.out.printf("inters min: %s, max: %s\n", p1, p2);
                int minX = (intersectingBox.minX - baseBox.minX) / sizeX;
                int minY = (intersectingBox.minY - baseBox.minY) / sizeY;
                int minZ = (intersectingBox.minZ - baseBox.minZ) / sizeZ;
                int maxX = (intersectingBox.maxX - baseBox.minX) / sizeX;
                int maxY = (intersectingBox.maxY - baseBox.minY) / sizeY;
                int maxZ = (intersectingBox.maxZ - baseBox.minZ) / sizeZ;
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

    private Map<Vec3i, SchematicPlacement> createGridPlacementsWithinAreaFor(SchematicPlacement basePlacement, IntBoundingBox area)
    {
        Set<Vec3i> gridPoints = this.getGridPointsWithinAreaFor(basePlacement, area);
        return this.createGridPlacementsForPoints(basePlacement, gridPoints);
    }

    private Map<Vec3i, SchematicPlacement> createGridPlacementsForPoints(SchematicPlacement basePlacement, Set<Vec3i> gridPoints)
    {
        Map<Vec3i, SchematicPlacement> placements = new HashMap<>();

        if (gridPoints.isEmpty() == false)
        {
            BlockPos baseOrigin = basePlacement.getPosition();
            Vec3i size = basePlacement.getGridSettings().getSize();
            int sizeX = size.getX();
            int sizeY = size.getY();
            int sizeZ = size.getZ();

            for (Vec3i point : gridPoints)
            {
                SchematicPlacement placement = basePlacement.createRepeatedCopy();
                placement.setOrigin(baseOrigin.add(point.getX() * sizeX, point.getY() * sizeY, point.getZ() * sizeZ));
                placement.updateEnclosingBox();
                placements.put(point, placement);
                //System.out.printf("repeat placement @ %s [%d, %d, %d]\n", placement.getOrigin(), point.getX(), point.getY(), point.getZ());
            }
        }

        return placements;
    }

    private Set<Vec3i> getExistingOutOfRangeGridPointsFor(SchematicPlacement basePlacement, Set<Vec3i> currentGridPoints)
    {
        Map<Vec3i, SchematicPlacement> placements = this.gridPlacementsPerPlacement.get(basePlacement);

        if (placements != null)
        {
            Set<Vec3i> outOfRangePoints = new HashSet<>(placements.keySet());
            outOfRangePoints.removeAll(currentGridPoints);
            return outOfRangePoints;
        }

        return new HashSet<>();
    }

    private Set<Vec3i> getNewGridPointsFor(SchematicPlacement basePlacement, Set<Vec3i> currentGridPoints)
    {
        Map<Vec3i, SchematicPlacement> placements = this.gridPlacementsPerPlacement.get(basePlacement);

        if (placements != null)
        {
            Set<Vec3i> newPoints = new HashSet<>(currentGridPoints);
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
        Map<Vec3i, SchematicPlacement> placements = this.createGridPlacementsWithinLoadedAreaFor(basePlacement);

        if (placements.isEmpty() == false)
        {
            return this.addGridPlacements(basePlacement, placements);
        }

        return false;
    }

    /**
     * Removes all grid placements of the provided placement, and the base placement itself
     * so that the automatic updating doesn't re-create them.
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
        Map<Vec3i, SchematicPlacement> placements = this.gridPlacementsPerPlacement.get(basePlacement);

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
        IntBoundingBox currentArea = this.getCurrentLoadedArea(1);
        boolean modified = false;

        if (currentArea != null)
        {
            for (SchematicPlacement basePlacement : this.basePlacements)
            {
                Set<Vec3i> currentGridPoints = this.getGridPointsWithinAreaFor(basePlacement, currentArea);
                Set<Vec3i> outOfRangePoints = this.getExistingOutOfRangeGridPointsFor(basePlacement, currentGridPoints);
                Set<Vec3i> newPoints = this.getNewGridPointsFor(basePlacement, currentGridPoints);
                //System.out.printf("c: %d, o: %d, n: %d\n", currentGridPoints.size(), outOfRangePoints.size(), newPoints.size());

                if (outOfRangePoints.isEmpty() == false)
                {
                    modified |= this.removeGridPlacements(basePlacement, outOfRangePoints);
                }

                if (newPoints.isEmpty() == false)
                {
                    Map<Vec3i, SchematicPlacement> placements = this.createGridPlacementsForPoints(basePlacement, newPoints);
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
    private boolean addGridPlacements(SchematicPlacement basePlacement, Map<Vec3i, SchematicPlacement> placements)
    {
        boolean modified = false;

        if (placements.isEmpty() == false)
        {
            Map<Vec3i, SchematicPlacement> map = this.gridPlacementsPerPlacement.computeIfAbsent(basePlacement, k -> new HashMap<>());

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
     * @return true if some placements were removed
     */
    private boolean removeGridPlacements(SchematicPlacement basePlacement, Collection<Vec3i> gridPoints)
    {
        Map<Vec3i, SchematicPlacement> map = this.gridPlacementsPerPlacement.get(basePlacement);
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
    private IntBoundingBox getCurrentLoadedArea(int expandChunks)
    {
        EntityPlayer player = GameWrap.getClientPlayer();

        if (player == null)
        {
            return null;
        }

        int centerChunkX = EntityWrap.getChunkX(player);
        int centerChunkZ = EntityWrap.getChunkZ(player);
        int chunkRadius = GameWrap.getRenderDistanceChunks() + expandChunks;
        int playerY = (int) EntityWrap.getY(player);

        return IntBoundingBox.createProper( (centerChunkX - chunkRadius) << 4      , playerY - 512,  (centerChunkZ - chunkRadius) << 4,
                                           ((centerChunkX + chunkRadius) << 4) + 15, playerY + 512, ((centerChunkZ + chunkRadius) << 4) + 15);
    }
}
