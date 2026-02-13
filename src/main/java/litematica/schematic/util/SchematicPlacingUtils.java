package litematica.schematic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.EnabledCondition;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.util.DataTypeUtils;
import malilib.util.game.wrap.BlockWrap;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.BlockMirror;
import malilib.util.position.BlockPos;
import malilib.util.position.BlockRotation;
import malilib.util.position.ChunkPos;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.LayerRange;
import malilib.util.position.Vec3d;
import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;
import malilib.util.world.ScheduledBlockTickData;
import litematica.Litematica;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.scheduler.TaskScheduler;
import litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand;
import litematica.scheduler.tasks.TaskPasteSchematicPerChunkDirect;
import litematica.schematic.Schematic;
import litematica.schematic.SchematicRegion;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.data.EntityData;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.placement.SubRegionPlacement;
import litematica.selection.CornerDefinedBox;
import litematica.selection.SelectionBox;
import litematica.util.PositionUtils;
import litematica.util.WorldUtils;
import litematica.util.value.ReplaceBehavior;
import litematica.util.value.Rotation;

public class SchematicPlacingUtils
{
    public static void pasteCurrentPlacementToWorld()
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        pastePlacementToWorld(manager.getSelectedSchematicPlacement(), true);
    }

    public static void gridPasteCurrentPlacementToWorld()
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        SchematicPlacement placement = manager.getSelectedSchematicPlacement();

        if (placement != null)
        {
            // Only do the Grid paste when the base placement is selected
            if (placement.isRepeatedPlacement() == false && placement.getGridSettings().isEnabled())
            {
                ArrayList<SchematicPlacement> placements = new ArrayList<>();
                placements.add(placement);
                placements.addAll(manager.getGridPlacementsForBasePlacement(placement));

                pastePlacementsToWorld(placements, true, true);
            }
            else
            {
                pastePlacementToWorld(placement, true);
                MessageDispatcher.warning(8000).translate("litematica.message.grid_paste.warning.select_base_placement_for_grid_paste");
            }
        }
        else
        {
            MessageDispatcher.error().translate("litematica.message.error.no_placement_selected");
        }
    }

    public static void pastePlacementToWorld(@Nullable SchematicPlacement placement, boolean changedBlocksOnly)
    {
        if (placement != null)
        {
            pastePlacementsToWorld(Lists.newArrayList(placement), changedBlocksOnly, true);
        }
        else
        {
            MessageDispatcher.error().translate("litematica.message.error.no_placement_selected");
        }
    }

    private static void pastePlacementsToWorld(List<SchematicPlacement> placements,
                                               boolean changedBlocksOnly,
                                               boolean printMessage)
    {
        if (GameWrap.isCreativeMode())
        {
            if (placements.isEmpty() == false)
            {
                LayerRange range = DataManager.getRenderLayerRange().copy();

                if (GameWrap.isSinglePlayer())
                {
                    TaskPasteSchematicPerChunkDirect task = new TaskPasteSchematicPerChunkDirect(placements, range, changedBlocksOnly);
                    TaskScheduler.getInstanceServer().scheduleTask(task, 20);
                }
                else
                {
                    TaskPasteSchematicPerChunkCommand task = new TaskPasteSchematicPerChunkCommand(placements, range, changedBlocksOnly);
                    TaskScheduler.getInstanceClient().scheduleTask(task, Configs.Generic.PASTE_COMMAND_INTERVAL.getIntegerValue());
                }

                if (printMessage)
                {
                    MessageDispatcher.generic().customHotbar().translate("litematica.message.scheduled_task_added");
                }
            }
            else
            {
                MessageDispatcher.error().translate("litematica.message.error.no_placement_selected");
            }
        }
        else
        {
            MessageDispatcher.error().translate("litematica.error.generic.creative_mode_only");
        }
    }

    public static boolean placeToWorld(SchematicPlacement schematicPlacement, World world, LayerRange range, boolean notifyNeighbors)
    {
        if (schematicPlacement.isSchematicLoaded() == false)
        {
            return false;
        }

        boolean success = true;

        try
        {
            WorldUtils.setShouldPreventBlockUpdates(world, true);

            Schematic schematic = schematicPlacement.getSchematic();
            BlockPos origin = schematicPlacement.getPosition();

            for (SubRegionPlacement regionPlacement : schematicPlacement.getEnabledSubRegions())
            {
                String regionName = regionPlacement.getName();
                SchematicRegion schematicRegion = schematic.getRegions().get(regionName);

                if (regionPlacement.isEnabled() && schematicRegion != null)
                {
                    BlockPos regionPos = regionPlacement.getPosition();
                    Vec3i regionSize = schematicRegion.getSize();
                    BlockContainer container = schematicRegion.getBlockContainer();
                    Map<BlockPos, CompoundData> blockEntityMap = schematicRegion.getBlockEntityMap();
                    List<EntityData> entityList = schematicRegion.getEntityList();
                    Map<BlockPos, ScheduledBlockTickData> scheduledBlockTicks = schematicRegion.getBlockTickMap();

                    if (regionPos != null && regionSize != null && container != null && blockEntityMap != null)
                    {
                        if (placeBlocksToWorld(world, origin, regionPos, regionSize, schematicPlacement, regionPlacement,
                                container, blockEntityMap, scheduledBlockTicks, range, notifyNeighbors) == false)
                        {
                            success = false;
                            MessageDispatcher.error().translate("litematica.message.error.schematic_paste_failed_region", regionName);
                        }
                    }
                    else
                    {
                        Litematica.LOGGER.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", schematic.getMetadata().getSchematicName(), regionName);
                    }

                    if (schematicPlacement.ignoreEntities() == false && regionPlacement.ignoreEntities() == false && entityList != null)
                    {
                        placeEntitiesToWorld(world, origin, regionPos, regionSize, schematicPlacement, regionPlacement, entityList, range);
                    }
                }
            }
        }
        finally
        {
            WorldUtils.setShouldPreventBlockUpdates(world, false);
        }

        return success;
    }

    public static boolean placeBlocksToWorld(World world, BlockPos origin, BlockPos regionPos, Vec3i regionSize,
                                             SchematicPlacement schematicPlacement, SubRegionPlacement placement,
                                             BlockContainer container, Map<BlockPos, CompoundData> blockEntityMap,
                                             @Nullable Map<BlockPos, ScheduledBlockTickData> scheduledTicks,
                                             LayerRange range, boolean notifyNeighbors)
    {
        // These are the untransformed relative positions
        BlockPos posEndRelSub = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize));
        BlockPos posEndRel = posEndRelSub.add(regionPos);
        BlockPos posMinRel = malilib.util.position.PositionUtils.getMinCorner(regionPos, posEndRel);

        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        BlockPos posEndAbs = PositionUtils.getTransformedBlockPos(posEndRelSub, placement.getMirror(), placement.getRotation()).add(regionPosTransformed).add(origin);
        BlockPos regionPosAbs = regionPosTransformed.add(origin);

        if (PositionUtils.arePositionsWithinWorld(world, regionPosAbs, posEndAbs) == false || range.intersectsBox(regionPosAbs, posEndAbs) == false)
        {
            return false;
        }

        Pair<Vec3i, Vec3i> pair = SchematicUtils.getLayerRangeClampedSubRegion(range, schematicPlacement, placement, regionSize);

        if (pair == null)
        {
            return false;
        }

        final IBlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockPos.MutBlockPos posMutable = new BlockPos.MutBlockPos();
        ReplaceBehavior replace = Configs.Generic.PASTE_REPLACE_BEHAVIOR.getValue();

        final BlockRotation rotationCombined = schematicPlacement.getRotation().add(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        BlockMirror mirrorSub = placement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CW_90 ||
             schematicPlacement.getRotation() == BlockRotation.CCW_90))
        {
            mirrorSub = mirrorSub == BlockMirror.Z ? BlockMirror.X : BlockMirror.Z;
        }

        Vec3i containerStart = pair.getLeft();
        Vec3i containerEnd = pair.getRight();
        final int startX = containerStart.getX();
        final int startY = containerStart.getY();
        final int startZ = containerStart.getZ();
        final int endX = containerEnd.getX();
        final int endY = containerEnd.getY();
        final int endZ = containerEnd.getZ();

        for (int y = startY; y <= endY; ++y)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                for (int x = startX; x <= endX; ++x)
                {
                    BlockState state = container.getBlockState(x, y, z);

                    if (state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    posMutable.set(x, y, z);
                    CompoundData beData = blockEntityMap.get(posMutable);

                    posMutable.set(posMinRel.getX() + x - regionPos.getX(),
                                   posMinRel.getY() + y - regionPos.getY(),
                                   posMinRel.getZ() + z - regionPos.getZ());

                    BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                    pos = pos.add(regionPosTransformed).add(origin);

                    BlockState stateOld = BlockState.of(world.getBlockState(pos).getActualState(world, pos));

                    if ((replace == ReplaceBehavior.NONE && stateOld.vanillaState().getMaterial() != Material.AIR) ||
                        (replace == ReplaceBehavior.WITH_NON_AIR && state.vanillaState().getMaterial() == Material.AIR))
                    {
                        continue;
                    }

                    if (mirrorMain != BlockMirror.NONE) { state = state.withMirror(mirrorMain); }
                    if (mirrorSub != BlockMirror.NONE)  { state = state.withMirror(mirrorSub); }
                    if (rotationCombined != BlockRotation.NONE) { state = state.withRotation(rotationCombined); }

                    if (stateOld == state)
                    {
                        continue;
                    }

                    TileEntity beOld = world.getTileEntity(pos);

                    if (beOld != null)
                    {
                        if (beOld instanceof IInventory)
                        {
                            ((IInventory) beOld).clear();
                        }

                        world.setBlockState(pos, barrier, 0x14);
                    }

                    if (world.setBlockState(pos, state.vanillaState(), 0x12) && beData != null)
                    {
                        TileEntity be = world.getTileEntity(pos);

                        if (be != null)
                        {
                            beData = beData.copy();
                            DataTypeUtils.putVec3i(beData, pos);

                            try
                            {
                                BlockWrap.readBlockEntityFrom(be, beData);

                                if (mirrorMain != BlockMirror.NONE) { be.mirror(mirrorMain.getVanillaMirror()); }
                                if (mirrorSub != BlockMirror.NONE)  { be.mirror(mirrorSub.getVanillaMirror()); }
                                if (rotationCombined != BlockRotation.NONE) { be.rotate(rotationCombined.getVanillaRotation()); }
                            }
                            catch (Exception e)
                            {
                                Litematica.LOGGER.warn("Failed to load TileEntity data for {} @ {}", state, pos);
                            }
                        }
                    }
                }
            }
        }

        if (notifyNeighbors)
        {
            for (int y = containerStart.getY(); y < containerEnd.getY(); ++y)
            {
                for (int z = containerStart.getZ(); z < containerEnd.getZ(); ++z)
                {
                    for (int x = containerStart.getX(); x < containerEnd.getX(); ++x)
                    {
                        posMutable.set(posMinRel.getX() + x - regionPos.getX(),
                                       posMinRel.getY() + y - regionPos.getY(),
                                       posMinRel.getZ() + z - regionPos.getZ());
                        BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement).add(origin);
                        world.notifyNeighborsRespectDebug(pos, world.getBlockState(pos).getBlock(), false);
                    }
                }
            }
        }

        if (scheduledTicks != null && scheduledTicks.isEmpty() == false)
        {
            // TODO order by the sub-tick priority/ID
            for (Map.Entry<BlockPos, ScheduledBlockTickData> entry : scheduledTicks.entrySet())
            {
                BlockPos pos = entry.getKey().add(regionPosAbs);
                ScheduledBlockTickData tick = entry.getValue();
                world.scheduleBlockUpdate(pos, world.getBlockState(pos).getBlock(), (int) tick.delay, tick.priority);
            }
        }

        return true;
    }

    public static void placeEntitiesToWorld(World world, BlockPos origin, BlockPos regionPos, Vec3i regionSize,
                                            SchematicPlacement schematicPlacement, SubRegionPlacement placement, List<EntityData> entityList, LayerRange range)
    {
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();

        final BlockRotation rotationCombined = schematicPlacement.getRotation().add(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        BlockMirror mirrorSub = placement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CW_90 ||
             schematicPlacement.getRotation() == BlockRotation.CCW_90))
        {
            mirrorSub = mirrorSub == BlockMirror.Z ? BlockMirror.X : BlockMirror.Z;
        }

        for (EntityData entityData : entityList)
        {
            Vec3d pos = entityData.pos;
            pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
            pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());

            if (range.isPositionWithinRange((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z)))
            {
                Entity entity = EntityWrap.createEntityAndPassengersFromNbt(entityData.data, world);

                if (entity != null)
                {
                    double x = pos.x + offX;
                    double y = pos.y + offY;
                    double z = pos.z + offZ;

                    rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
                    EntityWrap.spawnEntityAndPassengersInWorld(entity, world);
                }
            }
        }
    }

    public static boolean placeToWorldWithinChunk(SchematicPlacement schematicPlacement, ChunkPos chunkPos, World world, ReplaceBehavior replace, boolean notifyNeighbors)
    {
        Schematic schematic = schematicPlacement.getSchematic();
        ImmutableMap<String, SelectionBox> enabledRegions = schematicPlacement.getSubRegionBoxes(EnabledCondition.ENABLED);
        Set<String> regionsTouchingChunk = PositionUtils.getSubRegionNamesTouchingChunk(chunkPos.x, chunkPos.z, enabledRegions);
        BlockPos origin = schematicPlacement.getPosition();
        boolean allSuccess = true;

        try
        {
            if (notifyNeighbors == false)
            {
                WorldUtils.setShouldPreventBlockUpdates(world, true);
            }

            for (String regionName : regionsTouchingChunk)
            {
                SubRegionPlacement regionPlacement = schematicPlacement.getSubRegion(regionName);
                SchematicRegion region = schematic.getRegions().get(regionName);

                if (region == null)
                {
                    allSuccess = false;
                    continue;
                }

                if (regionPlacement.isEnabled())
                {
                    CornerDefinedBox box = schematicPlacement.getSubRegionBox(regionName, EnabledCondition.ENABLED);

                    if (box == null)
                    {
                        continue;
                    }

                    IntBoundingBox bounds = PositionUtils.getBoundsWithinChunkForBox(box, chunkPos.x, chunkPos.z);

                    if (bounds == null)
                    {
                        continue;
                    }

                    if (placeBlocksWithinChunk(world, bounds, region, origin, schematicPlacement, regionPlacement, replace, notifyNeighbors) == false)
                    {
                        allSuccess = false;
                        Litematica.LOGGER.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", schematic.getMetadata().getSchematicName(), regionName);
                    }

                    if (schematicPlacement.ignoreEntities() == false && regionPlacement.ignoreEntities() == false)
                    {
                        placeEntitiesToWorldWithinChunk(world, chunkPos, region, origin, schematicPlacement, regionPlacement);
                    }
                }
            }
        }
        finally
        {
            WorldUtils.setShouldPreventBlockUpdates(world, false);
        }

        return allSuccess;
    }

    public static void getRelativeEndPositionFromAreaSize(Vec3i size, BlockPos.MutBlockPos posMutable)
    {
        int x = size.getX();
        int y = size.getY();
        int z = size.getZ();

        x = x >= 0 ? x - 1 : x + 1;
        y = y >= 0 ? y - 1 : y + 1;
        z = z >= 0 ? z - 1 : z + 1;

        posMutable.set(x, y, z);
    }

    public static boolean placeBlocksWithinChunk(World world,
                                                 IntBoundingBox bounds,
                                                 SchematicRegion region,
                                                 BlockPos origin,
                                                 SchematicPlacement schematicPlacement,
                                                 SubRegionPlacement subRegionPlacement,
                                                 ReplaceBehavior replace,
                                                 boolean notifyNeighbors)
    {
        BlockContainer container = region.getBlockContainer();
        Map<BlockPos, CompoundData> blockEntityMap = region.getBlockEntityMap();

        if (bounds == null || container == null || blockEntityMap == null)
        {
            return false;
        }

        BlockPos.MutBlockPos posMutable = new BlockPos.MutBlockPos();

        BlockPos regionOriginRelative = subRegionPlacement.getPosition();
        Vec3i regionSize = region.getSize();
        int rx = regionOriginRelative.getX();
        int ry = regionOriginRelative.getY();
        int rz = regionOriginRelative.getZ();

        // Minimum corner of the region, which is the 0,0,0 of the block container
        getRelativeEndPositionFromAreaSize(regionSize, posMutable);
        BlockPos endRel = posMutable.toImmutable();
        int minX = Math.min(rx, rx + posMutable.getX());
        int minY = Math.min(ry, ry + posMutable.getY());
        int minZ = Math.min(rz, rz + posMutable.getZ());

        // Get the relative offset of the minimum corner to the region origin,
        // and transform that offset by that sub-region's transform
        posMutable.set(minX - rx, minY - ry, minZ - rz);
        BlockPos minOffset = posMutable.toImmutable();
        Rotation subRotation = subRegionPlacement.getFullRotation();
        subRotation.rotate(posMutable);
        BlockPos minOffsetTr = posMutable.toImmutable();

        // Now add the relative origin position to the transformed min corner offset
        posMutable.addMut(regionOriginRelative);
        BlockPos minCornerSub = posMutable.toImmutable();
        // Then transform that minimum corner position by the main placement transform
        Rotation mainRotation = schematicPlacement.getFullRotation();
        mainRotation.rotate(posMutable);
        BlockPos minCornerRel = posMutable.toImmutable();

        // Then add the absolute origin to get the final absolute position of the region/block container min corner
        BlockPos regionMinCorner = posMutable.add(origin);
        int ax = regionMinCorner.getX();
        int ay = regionMinCorner.getY();
        int az = regionMinCorner.getZ();

        // Find the relative start and end offsets of the box within the region block container
        int startX = Math.min(bounds.minX - ax, bounds.maxX - ax);
        int startY = Math.min(bounds.minY - ay, bounds.maxY - ay);
        int startZ = Math.min(bounds.minZ - az, bounds.maxZ - az);

        int endX = Math.max(bounds.minX - ax, bounds.maxX - ax);
        int endY = Math.max(bounds.minY - ay, bounds.maxY - ay);
        int endZ = Math.max(bounds.minZ - az, bounds.maxZ - az);

        Rotation fullTransform = subRotation.add(mainRotation);

        // Reverse transform the container-relative offsets
        posMutable.set(startX, startY, startZ);
        fullTransform.reverse(posMutable);
        int x1 = posMutable.getX();
        int y1 = posMutable.getY();
        int z1 = posMutable.getZ();

        posMutable.set(endX, endY, endZ);
        fullTransform.reverse(posMutable);
        int x2 = posMutable.getX();
        int y2 = posMutable.getY();
        int z2 = posMutable.getZ();

        startX = Math.min(Math.abs(x1), Math.abs(x2));
        startY = Math.min(Math.abs(y1), Math.abs(y2));
        startZ = Math.min(Math.abs(z1), Math.abs(z2));

        endX = Math.max(Math.abs(x1), Math.abs(x2));
        endY = Math.max(Math.abs(y1), Math.abs(y2));
        endZ = Math.max(Math.abs(z1), Math.abs(z2));

        // FIXME These should come from the transform
        /*
        final BlockRotation rotationCombined = schematicPlacement.getRotation().add(subRegionPlacement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        BlockMirror mirrorSub = subRegionPlacement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CW_90 ||
             schematicPlacement.getRotation() == BlockRotation.CCW_90))
        {
            mirrorSub = mirrorSub == BlockMirror.X ? BlockMirror.Z : BlockMirror.X;
        }
        */

        final IBlockState barrier = Blocks.BARRIER.getDefaultState();

        /*
        System.out.printf("=== START ===\n");
        System.out.printf("  bounds: %s\n", bounds);
        System.out.printf("  origin (abs): %s\n", origin);
        System.out.printf("  container size: %s,\n", container.getSize());
        System.out.printf("  regionSize: %s\n", regionSize);
        System.out.printf("  regionOriginRelative: %s\n", regionOriginRelative);
        System.out.printf("  endRel: %s\n", endRel);
        System.out.printf("  minOffset: %s\n", minOffset);
        System.out.printf("  minOffsetTr: %s\n", minOffsetTr);
        System.out.printf("  minCornerSub: %s\n", minCornerSub);
        System.out.printf("  minCornerRel: %s\n", minCornerRel);
        System.out.printf("  regionMinCorner (abs): %s\n", regionMinCorner);
        System.out.printf("  start X/Y/Z: %d, %d, %d\n", startX, startY, startZ);
        System.out.printf("  end X/Y/Z: %d, %d, %d\n", endX, endY, endZ);
        System.out.printf("=== END ===\n");
        */

        for (int y = startY; y <= endY; ++y)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                for (int x = startX; x <= endX; ++x)
                {
                    BlockState state;
                    try
                    {
                        state = container.getBlockState(x, y, z);
                    }
                    catch (Exception e)
                    {
                        System.out.printf("OOPS @ %d, %d, %d\n", x, y, z);
                        return true;
                    }

                    if (state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    posMutable.set(x, y, z);
                    CompoundData beData = blockEntityMap.get(posMutable);
                    fullTransform.rotate(posMutable);
                    posMutable.addMut(regionMinCorner);

                    IBlockState stateOld = world.getBlockState(posMutable);

                    if ((replace == ReplaceBehavior.NONE && stateOld.getMaterial() != Material.AIR) ||
                        (replace == ReplaceBehavior.WITH_NON_AIR && state.vanillaState().getMaterial() == Material.AIR))
                    {
                        continue;
                    }

                    /*
                    if (mirrorMain != BlockMirror.NONE) { state = state.withMirror(mirrorMain); }
                    if (mirrorSub != BlockMirror.NONE)  { state = state.withMirror(mirrorSub); }
                    if (rotationCombined != BlockRotation.NONE) { state = state.withRotation(rotationCombined); }
                    */
                    state = PositionUtils.rotateState(state, fullTransform);

                    TileEntity beOld = world.getTileEntity(posMutable);

                    if (beOld != null)
                    {
                        if (beOld instanceof IInventory)
                        {
                            ((IInventory) beOld).clear();
                        }

                        world.setBlockState(posMutable, barrier, 0x14);
                    }

                    if (world.setBlockState(posMutable.toVanillaPos(), state.vanillaState(), 0x12) && beData != null)
                    {
                        TileEntity be = world.getTileEntity(posMutable);

                        if (be != null)
                        {
                            beData = beData.copy();
                            DataTypeUtils.putVec3i(beData, posMutable);

                            try
                            {
                                BlockWrap.readBlockEntityFrom(be, beData);

                                /*
                                if (mirrorMain != BlockMirror.NONE) { be.mirror(mirrorMain.getVanillaMirror()); }
                                if (mirrorSub != BlockMirror.NONE)  { be.mirror(mirrorSub.getVanillaMirror()); }
                                if (rotationCombined != BlockRotation.NONE) { be.rotate(rotationCombined.getVanillaRotation()); }
                                */
                            }
                            catch (Exception e)
                            {
                                Litematica.LOGGER.warn("Failed to load BlockEntity data for {} @ {}", state, posMutable);
                            }
                        }
                    }
                }
            }
        }

        /*
        if (notifyNeighbors)
        {
            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(posMinRel.getX() + x - regionPos.getX(),
                                       posMinRel.getY() + y - regionPos.getY(),
                                       posMinRel.getZ() + z - regionPos.getZ());
                        BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement).add(origin);
                        world.notifyNeighborsRespectDebug(pos, world.getBlockState(pos).getBlock(), false);
                    }
                }
            }
        }
        */

        return true;
    }

    public static void placeEntitiesToWorldWithinChunk(World world, ChunkPos chunkPos, SchematicRegion region,
                                                       BlockPos origin, SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        BlockPos regionPos = placement.getPosition();
        List<EntityData> entityList = region.getEntityList();

        if (entityList == null)
        {
            return;
        }

        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();
        final double minX = (chunkPos.x << 4);
        final double minZ = (chunkPos.z << 4);
        final double maxX = (chunkPos.x << 4) + 16;
        final double maxZ = (chunkPos.z << 4) + 16;

        final BlockRotation rotationCombined = schematicPlacement.getRotation().add(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        BlockMirror mirrorSub = placement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CW_90 ||
             schematicPlacement.getRotation() == BlockRotation.CCW_90))
        {
            mirrorSub = mirrorSub == BlockMirror.X ? BlockMirror.Z : BlockMirror.X;
        }

        for (EntityData entityData : entityList)
        {
            Vec3d pos = entityData.pos;
            pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
            pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());
            double x = pos.x + offX;
            double y = pos.y + offY;
            double z = pos.z + offZ;

            if (x < minX && x >= maxX && z < minZ && z >= maxZ)
            {
                continue;
            }

            CompoundData entityTag = entityData.data.copy();
            DataTypeUtils.writeVec3dToListTag(entityTag, x, y, z);
            Entity entity = EntityWrap.createEntityAndPassengersFromNbt(entityTag, world);

            if (entity == null)
            {
                continue;
            }

            rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
            //System.out.printf("post: %.1f - rot: %s, mm: %s, ms: %s\n", rotationYaw, rotationCombined, mirrorMain, mirrorSub);
            EntityWrap.spawnEntityAndPassengersInWorld(entity, world);
        }
    }

    public static void rotateEntity(Entity entity, double x, double y, double z, BlockRotation rotationCombined, BlockMirror mirrorMain, BlockMirror mirrorSub)
    {
        float rotationYaw = EntityWrap.getYaw(entity);
        float rotationPitch = EntityWrap.getPitch(entity);

        if (mirrorMain != BlockMirror.NONE)          { rotationYaw = entity.getMirroredYaw(mirrorMain.getVanillaMirror()); }
        if (mirrorSub != BlockMirror.NONE)           { rotationYaw = entity.getMirroredYaw(mirrorSub.getVanillaMirror()); }
        if (rotationCombined != BlockRotation.NONE)  { rotationYaw += rotationYaw - entity.getRotatedYaw(rotationCombined.getVanillaRotation()); }

        entity.setLocationAndAngles(x, y, z, rotationYaw, rotationPitch);
        entity.prevRotationYaw = rotationYaw;
        entity.prevRotationPitch = rotationPitch;

        if (entity instanceof EntityLivingBase)
        {
            EntityLivingBase livingBase = (EntityLivingBase) entity;
            livingBase.rotationYawHead = rotationYaw;
            livingBase.prevRotationYawHead = rotationYaw;
            livingBase.renderYawOffset = rotationYaw;
            livingBase.prevRenderYawOffset = rotationYaw;
        }
    }
}
