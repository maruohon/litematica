package fi.dy.masa.litematica.schematic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkDirect;
import fi.dy.masa.litematica.schematic.EntityInfo;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.ISchematicRegion;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;

public class SchematicPlacingUtils
{
    public static void pasteCurrentPlacementToWorld(Minecraft mc)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        pastePlacementToWorld(manager.getSelectedSchematicPlacement(), true, mc);
    }

    public static void gridPasteCurrentPlacementToWorld(Minecraft mc)
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

                pastePlacementsToWorld(placements, true, true, mc);
            }
            else
            {
                pastePlacementToWorld(placement, true, mc);
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 8000, "litematica.message.grid_paste.warning.select_base_placement_for_grid_paste");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
        }
    }

    public static void pastePlacementToWorld(@Nullable SchematicPlacement placement, boolean changedBlocksOnly, Minecraft mc)
    {
        if (placement != null)
        {
            pastePlacementsToWorld(Lists.newArrayList(placement), changedBlocksOnly, true, mc);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
        }
    }

    private static void pastePlacementsToWorld(List<SchematicPlacement> placements, boolean changedBlocksOnly, boolean printMessage, Minecraft mc)
    {
        if (mc.player != null && mc.player.capabilities.isCreativeMode)
        {
            if (placements.isEmpty() == false)
            {
                LayerRange range = DataManager.getRenderLayerRange().copy();

                if (mc.isSingleplayer())
                {
                    if (placements.size() == 1)
                    {
                        directPaste(placements.get(0), range, printMessage, mc);
                    }
                    else
                    {
                        TaskPasteSchematicPerChunkDirect task = new TaskPasteSchematicPerChunkDirect(placements, range, changedBlocksOnly);
                        TaskScheduler.getInstanceServer().scheduleTask(task, 20);

                        if (printMessage)
                        {
                            InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                        }
                    }
                }
                else
                {
                    TaskPasteSchematicPerChunkCommand task = new TaskPasteSchematicPerChunkCommand(placements, range, changedBlocksOnly);
                    TaskScheduler.getInstanceClient().scheduleTask(task, Configs.Generic.PASTE_COMMAND_INTERVAL.getIntegerValue());

                    if (printMessage)
                    {
                        InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                    }
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    private static void directPaste(SchematicPlacement placement, LayerRange range, boolean printMessage, Minecraft mc)
    {
        final WorldServer world = mc.getIntegratedServer().getWorld(fi.dy.masa.malilib.util.WorldUtils.getDimensionId(mc.player.getEntityWorld()));

        world.addScheduledTask(() ->
        {
            if (placeToWorld(placement, world, range, false))
            {
                if (printMessage)
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.schematic_pasted");
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.schematic_paste_failed");
            }
        });

        if (printMessage)
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
        }
    }

    public static boolean placeToWorld(SchematicPlacement schematicPlacement, World world, LayerRange range, boolean notifyNeighbors)
    {
        WorldUtils.setShouldPreventBlockUpdates(true);

        ISchematic schematic = schematicPlacement.getSchematic();
        ImmutableMap<String, SubRegionPlacement> relativePlacements = schematicPlacement.getEnabledRelativeSubRegionPlacements();
        BlockPos origin = schematicPlacement.getOrigin();

        for (String regionName : relativePlacements.keySet())
        {
            SubRegionPlacement placement = relativePlacements.get(regionName);
            ISchematicRegion region = schematic.getSchematicRegion(regionName);

            if (placement.isEnabled() && region != null)
            {
                BlockPos regionPos = placement.getPos();
                Vec3i regionSize = region.getSize();
                ILitematicaBlockStateContainer container = region.getBlockStateContainer();
                Map<BlockPos, NBTTagCompound> blockEntityMap = region.getBlockEntityMap();
                List<EntityInfo> entityList = region.getEntityList();
                Map<BlockPos, NextTickListEntry> scheduledBlockTicks = region.getBlockTickMap();

                if (regionPos != null && regionSize != null && container != null && blockEntityMap != null)
                {
                    placeBlocksToWorld(world, origin, regionPos, regionSize, schematicPlacement, placement, container, blockEntityMap, scheduledBlockTicks, range, notifyNeighbors);
                }
                else
                {
                    LiteModLitematica.logger.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", schematic.getMetadata().getName(), regionName);
                }

                if (schematicPlacement.ignoreEntities() == false && placement.ignoreEntities() == false && entityList != null)
                {
                    placeEntitiesToWorld(world, origin, regionPos, regionSize, schematicPlacement, placement, entityList, range);
                }
            }
        }

        WorldUtils.setShouldPreventBlockUpdates(false);

        return true;
    }

    public static boolean placeBlocksToWorld(World world, BlockPos origin, BlockPos regionPos, Vec3i regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement,
            ILitematicaBlockStateContainer container, Map<BlockPos, NBTTagCompound> tileMap,
            @Nullable Map<BlockPos, NextTickListEntry> scheduledTicks, LayerRange range, boolean notifyNeighbors)
    {
        // These are the untransformed relative positions
        BlockPos posEndRelSub = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize));
        BlockPos posEndRel = posEndRelSub.add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

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
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
        ReplaceBehavior replace = (ReplaceBehavior) Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue();

        final Rotation rotationCombined = schematicPlacement.getRotation().add(placement.getRotation());
        final Mirror mirrorMain = schematicPlacement.getMirror();
        Mirror mirrorSub = placement.getMirror();

        if (mirrorSub != Mirror.NONE &&
            (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
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
                    IBlockState state = container.getBlockState(x, y, z);

                    if (state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    posMutable.setPos(x, y, z);
                    NBTTagCompound teNBT = tileMap.get(posMutable);

                    posMutable.setPos(  posMinRel.getX() + x - regionPos.getX(),
                                        posMinRel.getY() + y - regionPos.getY(),
                                        posMinRel.getZ() + z - regionPos.getZ());

                    BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                    pos = pos.add(regionPosTransformed).add(origin);

                    IBlockState stateOld = world.getBlockState(pos).getActualState(world, pos);

                    if ((replace == ReplaceBehavior.NONE && stateOld.getMaterial() != Material.AIR) ||
                        (replace == ReplaceBehavior.WITH_NON_AIR && state.getMaterial() == Material.AIR))
                    {
                        continue;
                    }

                    if (mirrorMain != Mirror.NONE) { state = state.withMirror(mirrorMain); }
                    if (mirrorSub != Mirror.NONE)  { state = state.withMirror(mirrorSub); }
                    if (rotationCombined != Rotation.NONE) { state = state.withRotation(rotationCombined); }

                    if (stateOld == state)
                    {
                        continue;
                    }

                    TileEntity teOld = world.getTileEntity(pos);

                    if (teOld != null)
                    {
                        if (teOld instanceof IInventory)
                        {
                            ((IInventory) teOld).clear();
                        }

                        world.setBlockState(pos, barrier, 0x14);
                    }

                    if (world.setBlockState(pos, state, 0x12) && teNBT != null)
                    {
                        TileEntity te = world.getTileEntity(pos);

                        if (te != null)
                        {
                            teNBT = teNBT.copy();
                            teNBT.setInteger("x", pos.getX());
                            teNBT.setInteger("y", pos.getY());
                            teNBT.setInteger("z", pos.getZ());

                            try
                            {
                                te.readFromNBT(teNBT);

                                if (mirrorMain != Mirror.NONE) { te.mirror(mirrorMain); }
                                if (mirrorSub != Mirror.NONE)  { te.mirror(mirrorSub); }
                                if (rotationCombined != Rotation.NONE) { te.rotate(rotationCombined); }
                            }
                            catch (Exception e)
                            {
                                LiteModLitematica.logger.warn("Failed to load TileEntity data for {} @ {}", state, pos);
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
                        posMutable.setPos(  posMinRel.getX() + x - regionPos.getX(),
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
            for (Map.Entry<BlockPos, NextTickListEntry> entry : scheduledTicks.entrySet())
            {
                BlockPos pos = entry.getKey().add(regionPosAbs);
                NextTickListEntry tick = entry.getValue();
                world.scheduleBlockUpdate(pos, world.getBlockState(pos).getBlock(), (int) tick.scheduledTime, tick.priority);
            }
        }

        return true;
    }

    public static void placeEntitiesToWorld(World world, BlockPos origin, BlockPos regionPos, Vec3i regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement, List<EntityInfo> entityList, LayerRange range)
    {
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();

        final Rotation rotationCombined = schematicPlacement.getRotation().add(placement.getRotation());
        final Mirror mirrorMain = schematicPlacement.getMirror();
        Mirror mirrorSub = placement.getMirror();

        if (mirrorSub != Mirror.NONE &&
            (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        }

        for (EntityInfo info : entityList)
        {
            Vec3d pos = info.pos;
            pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
            pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());

            if (range.isPositionWithinRange((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z)))
            {
                Entity entity = EntityUtils.createEntityAndPassengersFromNBT(info.nbt, world);

                if (entity != null)
                {
                    double x = pos.x + offX;
                    double y = pos.y + offY;
                    double z = pos.z + offZ;

                    rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
                    EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
                }
            }
        }
    }

    public static boolean placeToWorldWithinChunk(SchematicPlacement schematicPlacement, ChunkPos chunkPos, World world, boolean notifyNeighbors)
    {
        ISchematic schematic = schematicPlacement.getSchematic();
        Set<String> regionsTouchingChunk = schematicPlacement.getRegionsTouchingChunk(chunkPos.x, chunkPos.z);
        BlockPos origin = schematicPlacement.getOrigin();
        boolean allSuccess = true;

        if (notifyNeighbors == false)
        {
            WorldUtils.setShouldPreventBlockUpdates(true);
        }

        for (String regionName : regionsTouchingChunk)
        {
            SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);
            ISchematicRegion region = schematic.getSchematicRegion(regionName);

            if (region == null)
            {
                allSuccess = false;
                continue;
            }

            if (placement.isEnabled())
            {
                if (placeBlocksWithinChunk(world, chunkPos, regionName, region, origin, schematicPlacement, placement, notifyNeighbors) == false)
                {
                    allSuccess = false;
                    LiteModLitematica.logger.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", schematic.getMetadata().getName(), regionName);
                }

                if (schematicPlacement.ignoreEntities() == false && placement.ignoreEntities() == false)
                {
                    placeEntitiesToWorldWithinChunk(world, chunkPos, region, origin, schematicPlacement, placement);
                }
            }
        }

        WorldUtils.setShouldPreventBlockUpdates(false);

        return allSuccess;
    }

    public static boolean placeBlocksWithinChunk(World world, ChunkPos chunkPos, String regionName, ISchematicRegion region,
            BlockPos origin, SchematicPlacement schematicPlacement, SubRegionPlacement placement, boolean notifyNeighbors)
    {
        IntBoundingBox bounds = schematicPlacement.getBoxWithinChunkForRegion(regionName, chunkPos.x, chunkPos.z);
        ILitematicaBlockStateContainer container = region.getBlockStateContainer();
        Map<BlockPos, NBTTagCompound> blockEntityMap = region.getBlockEntityMap();

        if (bounds == null || container == null || blockEntityMap == null)
        {
            return false;
        }

        BlockPos regionPos = placement.getPos();
        Vec3i regionSize = region.getSize();

        // These are the untransformed relative positions
        BlockPos posEndRel = (new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize))).add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        // The transformed sub-region origin position
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // The relative offset of the affected region's corners, to the sub-region's origin corner
        BlockPos boxMinRel = new BlockPos(bounds.minX - origin.getX() - regionPosTransformed.getX(), 0, bounds.minZ - origin.getZ() - regionPosTransformed.getZ());
        BlockPos boxMaxRel = new BlockPos(bounds.maxX - origin.getX() - regionPosTransformed.getX(), 0, bounds.maxZ - origin.getZ() - regionPosTransformed.getZ());

        // Reverse transform that relative offset, to get the untransformed orientation's offsets
        boxMinRel = PositionUtils.getReverseTransformedBlockPos(boxMinRel, placement.getMirror(), placement.getRotation());
        boxMaxRel = PositionUtils.getReverseTransformedBlockPos(boxMaxRel, placement.getMirror(), placement.getRotation());

        boxMinRel = PositionUtils.getReverseTransformedBlockPos(boxMinRel, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        boxMaxRel = PositionUtils.getReverseTransformedBlockPos(boxMaxRel, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // Get the offset relative to the sub-region's minimum corner, instead of the origin corner (which can be at any corner)
        boxMinRel = boxMinRel.subtract(posMinRel.subtract(regionPos));
        boxMaxRel = boxMaxRel.subtract(posMinRel.subtract(regionPos));

        BlockPos posMin = PositionUtils.getMinCorner(boxMinRel, boxMaxRel);
        BlockPos posMax = PositionUtils.getMaxCorner(boxMinRel, boxMaxRel);

        final int startX = posMin.getX();
        final int startZ = posMin.getZ();
        final int endX = posMax.getX();
        final int endZ = posMax.getZ();

        final int startY = 0;
        final int endY = Math.abs(regionSize.getY()) - 1;
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        //System.out.printf("sx: %d, sy: %d, sz: %d => ex: %d, ey: %d, ez: %d\n", startX, startY, startZ, endX, endY, endZ);

        if (startX < 0 || startZ < 0 || endX >= container.getSize().getX() || endZ >= container.getSize().getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, sx: %d, sz: %d, ex: %d, ez: %d - size x: %d z: %d =============\n",
                    regionName, startX, startZ, endX, endZ, container.getSize().getX(), container.getSize().getZ());
            return false;
        }

        final Rotation rotationCombined = schematicPlacement.getRotation().add(placement.getRotation());
        final Mirror mirrorMain = schematicPlacement.getMirror();
        final IBlockState barrier = Blocks.BARRIER.getDefaultState();
        Mirror mirrorSub = placement.getMirror();

        if (mirrorSub != Mirror.NONE &&
            (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        }

        for (int y = startY; y <= endY; ++y)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                for (int x = startX; x <= endX; ++x)
                {
                    IBlockState state = container.getBlockState(x, y, z);

                    if (state.getBlock() == Blocks.AIR)
                    {
                        continue;
                    }

                    posMutable.setPos(x, y, z);
                    NBTTagCompound teNBT = blockEntityMap.get(posMutable);

                    posMutable.setPos(  posMinRel.getX() + x - regionPos.getX(),
                                        posMinRel.getY() + y - regionPos.getY(),
                                        posMinRel.getZ() + z - regionPos.getZ());

                    BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                    pos = pos.add(regionPosTransformed).add(origin);

                    if (mirrorMain != Mirror.NONE) { state = state.withMirror(mirrorMain); }
                    if (mirrorSub != Mirror.NONE)  { state = state.withMirror(mirrorSub); }
                    if (rotationCombined != Rotation.NONE) { state = state.withRotation(rotationCombined); }

                    if (teNBT != null)
                    {
                        TileEntity te = world.getTileEntity(pos);

                        if (te != null)
                        {
                            if (te instanceof IInventory)
                            {
                                ((IInventory) te).clear();
                            }

                            world.setBlockState(pos, barrier, 0x14);
                        }
                    }

                    if (world.setBlockState(pos, state, 0x12) && teNBT != null)
                    {
                        TileEntity te = world.getTileEntity(pos);

                        if (te != null)
                        {
                            teNBT = teNBT.copy();
                            teNBT.setInteger("x", pos.getX());
                            teNBT.setInteger("y", pos.getY());
                            teNBT.setInteger("z", pos.getZ());

                            try
                            {
                                te.readFromNBT(teNBT);

                                if (mirrorMain != Mirror.NONE) { te.mirror(mirrorMain); }
                                if (mirrorSub != Mirror.NONE)  { te.mirror(mirrorSub); }
                                if (rotationCombined != Rotation.NONE) { te.rotate(rotationCombined); }
                            }
                            catch (Exception e)
                            {
                                LiteModLitematica.logger.warn("Failed to load TileEntity data for {} @ {}", state, pos);
                            }
                        }
                    }
                }
            }
        }

        if (notifyNeighbors)
        {
            for (int y = startX; y <= endY; ++y)
            {
                for (int z = startY; z <= endZ; ++z)
                {
                    for (int x = startZ; x <= endX; ++x)
                    {
                        posMutable.setPos(  posMinRel.getX() + x - regionPos.getX(),
                                            posMinRel.getY() + y - regionPos.getY(),
                                            posMinRel.getZ() + z - regionPos.getZ());
                        BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement).add(origin);
                        world.notifyNeighborsRespectDebug(pos, world.getBlockState(pos).getBlock(), false);
                    }
                }
            }
        }

        return true;
    }

    public static void placeEntitiesToWorldWithinChunk(World world, ChunkPos chunkPos, ISchematicRegion region,
            BlockPos origin, SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        BlockPos regionPos = placement.getPos();
        List<EntityInfo> entityList = region.getEntityList();

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

        final Rotation rotationCombined = schematicPlacement.getRotation().add(placement.getRotation());
        final Mirror mirrorMain = schematicPlacement.getMirror();
        Mirror mirrorSub = placement.getMirror();

        if (mirrorSub != Mirror.NONE &&
            (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        }

        for (EntityInfo info : entityList)
        {
            Entity entity = EntityUtils.createEntityAndPassengersFromNBT(info.nbt, world);

            if (entity != null)
            {
                Vec3d pos = info.pos;
                pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());
                double x = pos.x + offX;
                double y = pos.y + offY;
                double z = pos.z + offZ;

                if (x >= minX && x < maxX && z >= minZ && z < maxZ)
                {
                    rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
                    //System.out.printf("post: %.1f - rot: %s, mm: %s, ms: %s\n", rotationYaw, rotationCombined, mirrorMain, mirrorSub);
                    EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
                }
            }
        }
    }

    public static void rotateEntity(Entity entity, double x, double y, double z, Rotation rotationCombined, Mirror mirrorMain, Mirror mirrorSub)
    {
        float rotationYaw = entity.rotationYaw;

        if (mirrorMain != Mirror.NONE)          { rotationYaw = entity.getMirroredYaw(mirrorMain); }
        if (mirrorSub != Mirror.NONE)           { rotationYaw = entity.getMirroredYaw(mirrorSub); }
        if (rotationCombined != Rotation.NONE)  { rotationYaw += entity.rotationYaw - entity.getRotatedYaw(rotationCombined); }

        entity.setLocationAndAngles(x, y, z, rotationYaw, entity.rotationPitch);

        entity.prevRotationYaw = rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;

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
