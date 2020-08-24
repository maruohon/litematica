package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ScheduledTick;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionFixers;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.schematic.conversion.SchematicConverter;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.NBTUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class LitematicaSchematic
{
    public static final String FILE_EXTENSION = ".litematic";
    public static final int SCHEMATIC_VERSION_1_13_2 = 5;
    public static final int MINECRAFT_DATA_VERSION_1_13_2 = 1631; // MC 1.13.2

    public static final int SCHEMATIC_VERSION = 5;
    public static final int MINECRAFT_DATA_VERSION = SharedConstants.getGameVersion().getWorldVersion();

    private final Map<String, LitematicaBlockStateContainer> blockContainers = new HashMap<>();
    private final Map<String, Map<BlockPos, CompoundTag>> tileEntities = new HashMap<>();
    private final Map<String, Map<BlockPos, ScheduledTick<Block>>> pendingBlockTicks = new HashMap<>();
    private final Map<String, Map<BlockPos, ScheduledTick<Fluid>>> pendingFluidTicks = new HashMap<>();
    private final Map<String, List<EntityInfo>> entities = new HashMap<>();
    private final Map<String, BlockPos> subRegionPositions = new HashMap<>();
    private final Map<String, BlockPos> subRegionSizes = new HashMap<>();
    private final SchematicMetadata metadata = new SchematicMetadata();
    private final SchematicConverter converter;
    private int totalBlocks;
    @Nullable
    private final File schematicFile;

    private LitematicaSchematic(@Nullable File file)
    {
        this.schematicFile = file;
        this.converter = SchematicConverter.createForLitematica();
    }

    @Nullable
    public File getFile()
    {
        return this.schematicFile;
    }

    public Vec3i getTotalSize()
    {
        return this.metadata.getEnclosingSize();
    }

    public int getTotalBlocks()
    {
        return this.totalBlocks;
    }

    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    public int getSubRegionCount()
    {
        return this.blockContainers.size();
    }

    @Nullable
    public BlockPos getSubRegionPosition(String areaName)
    {
        return this.subRegionPositions.get(areaName);
    }

    public Map<String, BlockPos> getAreaPositions()
    {
        ImmutableMap.Builder<String, BlockPos> builder = ImmutableMap.builder();

        for (String name : this.subRegionPositions.keySet())
        {
            BlockPos pos = this.subRegionPositions.get(name);
            builder.put(name, pos);
        }

        return builder.build();
    }

    public Map<String, BlockPos> getAreaSizes()
    {
        ImmutableMap.Builder<String, BlockPos> builder = ImmutableMap.builder();

        for (String name : this.subRegionSizes.keySet())
        {
            BlockPos pos = this.subRegionSizes.get(name);
            builder.put(name, pos);
        }

        return builder.build();
    }

    @Nullable
    public BlockPos getAreaSize(String regionName)
    {
        return this.subRegionSizes.get(regionName);
    }

    public Map<String, Box> getAreas()
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();

        for (String name : this.subRegionPositions.keySet())
        {
            BlockPos pos = this.subRegionPositions.get(name);
            BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(this.subRegionSizes.get(name));
            Box box = new Box(pos, pos.add(posEndRel), name);
            builder.put(name, box);
        }

        return builder.build();
    }

    @Nullable
    public static LitematicaSchematic createFromWorld(World world, AreaSelection area, boolean ignoreEntities, String author, IStringConsumer feedback)
    {
        List<Box> boxes = PositionUtils.getValidBoxes(area);

        if (boxes.isEmpty())
        {
            feedback.setString(StringUtils.translate("litematica.error.schematic.create.no_selections"));
            return null;
        }

        LitematicaSchematic schematic = new LitematicaSchematic(null);
        long time = (new Date()).getTime();

        BlockPos origin = area.getEffectiveOrigin();
        schematic.setSubRegionPositions(boxes, origin);
        schematic.setSubRegionSizes(boxes);

        schematic.takeBlocksFromWorld(world, boxes);

        if (ignoreEntities == false)
        {
            schematic.takeEntitiesFromWorld(world, boxes, origin);
        }

        schematic.metadata.setAuthor(author);
        schematic.metadata.setName(area.getName());
        schematic.metadata.setTimeCreated(time);
        schematic.metadata.setTimeModified(time);
        schematic.metadata.setRegionCount(boxes.size());
        schematic.metadata.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        schematic.metadata.setEnclosingSize(PositionUtils.getEnclosingAreaSize(boxes));
        schematic.metadata.setTotalBlocks(schematic.totalBlocks);

        return schematic;
    }

    /**
     * Creates an empty schematic with all the maps and lists and containers already created.
     * This is intended to be used for the chunk-wise schematic creation.
     * @param area
     * @param author
     * @return
     */
    public static LitematicaSchematic createEmptySchematic(AreaSelection area, String author)
    {
        List<Box> boxes = PositionUtils.getValidBoxes(area);

        if (boxes.isEmpty())
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, StringUtils.translate("litematica.error.schematic.create.no_selections"));
            return null;
        }

        LitematicaSchematic schematic = new LitematicaSchematic(null);
        schematic.setSubRegionPositions(boxes, area.getEffectiveOrigin());
        schematic.setSubRegionSizes(boxes);
        schematic.metadata.setAuthor(author);
        schematic.metadata.setName(area.getName());
        schematic.metadata.setRegionCount(boxes.size());
        schematic.metadata.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        schematic.metadata.setEnclosingSize(PositionUtils.getEnclosingAreaSize(boxes));

        for (Box box : boxes)
        {
            String regionName = box.getName();
            BlockPos size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());
            LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);
            schematic.blockContainers.put(regionName, container);
            schematic.tileEntities.put(regionName, new HashMap<>());
            schematic.entities.put(regionName, new ArrayList<>());
            schematic.pendingBlockTicks.put(regionName, new HashMap<>());
            schematic.pendingFluidTicks.put(regionName, new HashMap<>());
        }

        return schematic;
    }

    public void takeEntityDataFromSchematicaSchematic(SchematicaSchematic schematic, String subRegionName)
    {
        this.tileEntities.put(subRegionName, schematic.getTiles());
        this.entities.put(subRegionName, schematic.getEntities());
    }

    public boolean placeToWorld(World world, SchematicPlacement schematicPlacement, boolean notifyNeighbors)
    {
        WorldUtils.setShouldPreventOnBlockAdded(true);

        ImmutableMap<String, SubRegionPlacement> relativePlacements = schematicPlacement.getEnabledRelativeSubRegionPlacements();
        BlockPos origin = schematicPlacement.getOrigin();

        for (String regionName : relativePlacements.keySet())
        {
            SubRegionPlacement placement = relativePlacements.get(regionName);

            if (placement.isEnabled())
            {
                BlockPos regionPos = placement.getPos();
                BlockPos regionSize = this.subRegionSizes.get(regionName);
                LitematicaBlockStateContainer container = this.blockContainers.get(regionName);
                Map<BlockPos, CompoundTag> tileMap = this.tileEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);
                Map<BlockPos, ScheduledTick<Block>> scheduledBlockTicks = this.pendingBlockTicks.get(regionName);
                Map<BlockPos, ScheduledTick<Fluid>> scheduledFluidTicks = this.pendingFluidTicks.get(regionName);

                if (regionPos != null && regionSize != null && container != null && tileMap != null)
                {
                    this.placeBlocksToWorld(world, origin, regionPos, regionSize, schematicPlacement, placement, container, tileMap, scheduledBlockTicks, scheduledFluidTicks, notifyNeighbors);
                }
                else
                {
                    Litematica.logger.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", this.metadata.getName(), regionName);
                }

                if (schematicPlacement.ignoreEntities() == false && placement.ignoreEntities() == false && entityList != null)
                {
                    this.placeEntitiesToWorld(world, origin, regionPos, regionSize, schematicPlacement, placement, entityList);
                }
            }
        }

        WorldUtils.setShouldPreventOnBlockAdded(false);

        return true;
    }

    private boolean placeBlocksToWorld(World world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement,
            LitematicaBlockStateContainer container, Map<BlockPos, CompoundTag> tileMap,
            @Nullable Map<BlockPos, ScheduledTick<Block>> scheduledBlockTicks,
            @Nullable Map<BlockPos, ScheduledTick<Fluid>> scheduledFluidTicks, boolean notifyNeighbors)
    {
        // These are the untransformed relative positions
        BlockPos posEndRelSub = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize);
        BlockPos posEndRel = posEndRelSub.add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        //BlockPos posEndAbs = PositionUtils.getTransformedBlockPos(posEndRelSub, placement.getMirror(), placement.getRotation()).add(regionPosTransformed).add(origin);
        BlockPos regionPosAbs = regionPosTransformed.add(origin);

        /*
        if (PositionUtils.arePositionsWithinWorld(world, regionPosAbs, posEndAbs) == false)
        {
            return false;
        }
        */

        final int sizeX = Math.abs(regionSize.getX());
        final int sizeY = Math.abs(regionSize.getY());
        final int sizeZ = Math.abs(regionSize.getZ());
        final BlockState barrier = Blocks.BARRIER.getDefaultState();
        final boolean ignoreInventories = Configs.Generic.PASTE_IGNORE_INVENTORY.getBooleanValue();
        BlockPos.Mutable posMutable = new BlockPos.Mutable();
        ReplaceBehavior replace = (ReplaceBehavior) Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue();

        final BlockRotation rotationCombined = schematicPlacement.getRotation().rotate(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        BlockMirror mirrorSub = placement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
        }

        int tmp = posMinRel.getY() - regionPos.getY() + regionPosTransformed.getY() + origin.getY();
        int startY = 0;
        int endY = sizeY;

        if (tmp < 0)
        {
            startY += (0 - tmp);
        }

        tmp = posMinRel.getY() - regionPos.getY() + regionPosTransformed.getY() + origin.getY() + (endY - 1);

        if (tmp > 255)
        {
            endY -= (tmp - 255);
        }

        for (int y = startY; y < endY; ++y)
        {
            for (int z = 0; z < sizeZ; ++z)
            {
                for (int x = 0; x < sizeX; ++x)
                {
                    BlockState state = container.get(x, y, z);

                    if (state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    posMutable.set(x, y, z);
                    CompoundTag teNBT = tileMap.get(posMutable);

                    posMutable.set( posMinRel.getX() + x - regionPos.getX(),
                                    posMinRel.getY() + y - regionPos.getY(),
                                    posMinRel.getZ() + z - regionPos.getZ());

                    BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                    pos = pos.add(regionPosTransformed).add(origin);

                    BlockState stateOld = world.getBlockState(pos);

                    if ((replace == ReplaceBehavior.NONE && stateOld.isAir() == false) ||
                        (replace == ReplaceBehavior.WITH_NON_AIR && state.isAir()))
                    {
                        continue;
                    }

                    if (mirrorMain != BlockMirror.NONE) { state = state.mirror(mirrorMain); }
                    if (mirrorSub != BlockMirror.NONE)  { state = state.mirror(mirrorSub); }
                    if (rotationCombined != BlockRotation.NONE) { state = state.rotate(rotationCombined); }

                    if (stateOld == state && state.getBlock().hasBlockEntity() == false)
                    {
                        continue;
                    }

                    BlockEntity teOld = world.getBlockEntity(pos);

                    if (teOld != null)
                    {
                        if (teOld instanceof Inventory)
                        {
                            ((Inventory) teOld).clear();
                        }

                        world.setBlockState(pos, barrier, 0x14);
                    }

                    if (world.setBlockState(pos, state, 0x12) && teNBT != null)
                    {
                        BlockEntity te = world.getBlockEntity(pos);

                        if (te != null)
                        {
                            teNBT = teNBT.copy();
                            teNBT.putInt("x", pos.getX());
                            teNBT.putInt("y", pos.getY());
                            teNBT.putInt("z", pos.getZ());

                            if (ignoreInventories)
                            {
                                teNBT.remove("Items");
                            }

                            try
                            {
                                te.fromTag(state, teNBT);

                                if (ignoreInventories && te instanceof Inventory)
                                {
                                    ((Inventory) te).clear();
                                }

                                if (mirrorMain != BlockMirror.NONE) { te.applyMirror(mirrorMain); }
                                if (mirrorSub != BlockMirror.NONE)  { te.applyMirror(mirrorSub); }
                                if (rotationCombined != BlockRotation.NONE) { te.applyRotation(rotationCombined); }
                            }
                            catch (Exception e)
                            {
                                Litematica.logger.warn("Failed to load TileEntity data for {} @ {}", state, pos);
                            }
                        }
                    }
                }
            }
        }

        if (notifyNeighbors)
        {
            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        posMutable.set( posMinRel.getX() + x - regionPos.getX(),
                                        posMinRel.getY() + y - regionPos.getY(),
                                        posMinRel.getZ() + z - regionPos.getZ());
                        BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement).add(origin);
                        world.updateNeighbors(pos, world.getBlockState(pos).getBlock());
                    }
                }
            }
        }

        if (world instanceof ServerWorld)
        {
            ServerWorld serverWorld = (ServerWorld) world;

            if (scheduledBlockTicks != null && scheduledBlockTicks.isEmpty() == false)
            {
                for (Map.Entry<BlockPos, ScheduledTick<Block>> entry : scheduledBlockTicks.entrySet())
                {
                    BlockPos pos = entry.getKey().add(regionPosAbs);
                    ScheduledTick<Block> tick = entry.getValue();
                    serverWorld.getBlockTickScheduler().schedule(pos, tick.getObject(), (int) tick.time, tick.priority);
                }
            }

            if (scheduledFluidTicks != null && scheduledFluidTicks.isEmpty() == false)
            {
                for (Map.Entry<BlockPos, ScheduledTick<Fluid>> entry : scheduledFluidTicks.entrySet())
                {
                    BlockPos pos = entry.getKey().add(regionPosAbs);
                    BlockState state = world.getBlockState(pos);

                    if (state.getFluidState().isEmpty() == false)
                    {
                        ScheduledTick<Fluid> tick = entry.getValue();
                        serverWorld.getFluidTickScheduler().schedule(pos, tick.getObject(), (int) tick.time, tick.priority);
                    }
                }
            }
        }

        return true;
    }

    private void placeEntitiesToWorld(World world, BlockPos origin, BlockPos regionPos, BlockPos regionSize, SchematicPlacement schematicPlacement, SubRegionPlacement placement, List<EntityInfo> entityList)
    {
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();

        final BlockRotation rotationCombined = schematicPlacement.getRotation().rotate(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        BlockMirror mirrorSub = placement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
        }

        for (EntityInfo info : entityList)
        {
            Entity entity = EntityUtils.createEntityAndPassengersFromNBT(info.nbt, world);

            if (entity != null)
            {
                Vec3d pos = info.posVec;
                pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());
                double x = pos.x + offX;
                double y = pos.y + offY;
                double z = pos.z + offZ;

                this.rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
                EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
            }
        }
    }

    public boolean placeToWorldWithinChunk(World world, ChunkPos chunkPos, SchematicPlacement schematicPlacement, boolean notifyNeighbors)
    {
        Set<String> regionsTouchingChunk = schematicPlacement.getRegionsTouchingChunk(chunkPos.x, chunkPos.z);
        BlockPos origin = schematicPlacement.getOrigin();

        for (String regionName : regionsTouchingChunk)
        {
            SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);

            if (placement.isEnabled())
            {
                BlockPos regionPos = placement.getPos();
                BlockPos regionSize = this.subRegionSizes.get(regionName);
                LitematicaBlockStateContainer container = this.blockContainers.get(regionName);
                Map<BlockPos, CompoundTag> tileMap = this.tileEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);

                if (regionPos != null && regionSize != null && container != null && tileMap != null)
                {
                    this.placeBlocksWithinChunk(world, chunkPos, regionName, origin, regionPos, regionSize, schematicPlacement, placement, container, tileMap, notifyNeighbors);
                }
                else
                {
                    Litematica.logger.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", this.metadata.getName(), regionName);
                }

                if (schematicPlacement.ignoreEntities() == false && placement.ignoreEntities() == false && entityList != null)
                {
                    this.placeEntitiesToWorldWithinChunk(world, chunkPos, origin, regionPos, regionSize, schematicPlacement, placement, entityList);
                }
            }
        }

        return true;
    }

    private void placeBlocksWithinChunk(World world, ChunkPos chunkPos, String regionName,
            BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement,
            LitematicaBlockStateContainer container, Map<BlockPos, CompoundTag> tileMap, boolean notifyNeighbors)
    {
        IntBoundingBox bounds = schematicPlacement.getBoxWithinChunkForRegion(regionName, chunkPos.x, chunkPos.z);

        if (bounds == null)
        {
            return;
        }

        // These are the untransformed relative positions
        BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).add(regionPos);
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

        int startY = 0;
        int endY = Math.abs(regionSize.getY()) - 1;
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        //System.out.printf("sx: %d, sy: %d, sz: %d => ex: %d, ey: %d, ez: %d\n", startX, startY, startZ, endX, endY, endZ);

        if (startX < 0 || startZ < 0 || endX >= container.getSize().getX() || endZ >= container.getSize().getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, sx: %d, sz: %d, ex: %d, ez: %d - size x: %d z: %d =============\n",
                    regionName, startX, startZ, endX, endZ, container.getSize().getX(), container.getSize().getZ());
            return;
        }

        final BlockRotation rotationCombined = schematicPlacement.getRotation().rotate(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        final BlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockMirror mirrorSub = placement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
        }

        int tmp = posMinRel.getY() - regionPos.getY() + regionPosTransformed.getY() + origin.getY();

        if (tmp < 0)
        {
            startY += (0 - tmp);
        }

        tmp = posMinRel.getY() - regionPos.getY() + regionPosTransformed.getY() + origin.getY() + endY;

        if (tmp > 255)
        {
            endY -= (tmp - 255);
        }

        for (int y = startY; y <= endY; ++y)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                for (int x = startX; x <= endX; ++x)
                {
                    BlockState state = container.get(x, y, z);

                    if (state.isAir())
                    {
                        continue;
                    }

                    posMutable.set(x, y, z);
                    CompoundTag teNBT = tileMap.get(posMutable);

                    posMutable.set( posMinRel.getX() + x - regionPos.getX(),
                                    posMinRel.getY() + y - regionPos.getY(),
                                    posMinRel.getZ() + z - regionPos.getZ());

                    BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                    pos = pos.add(regionPosTransformed).add(origin);

                    if (mirrorMain != BlockMirror.NONE) { state = state.mirror(mirrorMain); }
                    if (mirrorSub != BlockMirror.NONE)  { state = state.mirror(mirrorSub); }
                    if (rotationCombined != BlockRotation.NONE) { state = state.rotate(rotationCombined); }

                    if (teNBT != null)
                    {
                        BlockEntity te = world.getBlockEntity(pos);

                        if (te != null)
                        {
                            if (te instanceof Inventory)
                            {
                                ((Inventory) te).clear();
                            }

                            world.setBlockState(pos, barrier, 0x14);
                        }
                    }

                    if (world.setBlockState(pos, state, 0x12) && teNBT != null)
                    {
                        BlockEntity te = world.getBlockEntity(pos);

                        if (te != null)
                        {
                            teNBT = teNBT.copy();
                            teNBT.putInt("x", pos.getX());
                            teNBT.putInt("y", pos.getY());
                            teNBT.putInt("z", pos.getZ());

                            try
                            {
                                te.fromTag(state, teNBT);

                                if (mirrorMain != BlockMirror.NONE) { te.applyMirror(mirrorMain); }
                                if (mirrorSub != BlockMirror.NONE)  { te.applyMirror(mirrorSub); }
                                if (rotationCombined != BlockRotation.NONE) { te.applyRotation(rotationCombined); }
                            }
                            catch (Exception e)
                            {
                                Litematica.logger.warn("Failed to load TileEntity data for {} @ {}", state, pos);
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
                        posMutable.set( posMinRel.getX() + x - regionPos.getX(),
                                        posMinRel.getY() + y - regionPos.getY(),
                                        posMinRel.getZ() + z - regionPos.getZ());
                        BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement).add(origin);
                        world.updateNeighbors(pos, world.getBlockState(pos).getBlock());
                    }
                }
            }
        }
    }

    private void placeEntitiesToWorldWithinChunk(World world, ChunkPos chunkPos, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement, List<EntityInfo> entityList)
    {
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();
        final double minX = (chunkPos.x << 4);
        final double minZ = (chunkPos.z << 4);
        final double maxX = (chunkPos.x << 4) + 16;
        final double maxZ = (chunkPos.z << 4) + 16;

        final BlockRotation rotationCombined = schematicPlacement.getRotation().rotate(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        BlockMirror mirrorSub = placement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
        }

        for (EntityInfo info : entityList)
        {
            Entity entity = EntityUtils.createEntityAndPassengersFromNBT(info.nbt, world);

            if (entity != null)
            {
                Vec3d pos = info.posVec;
                pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());
                double x = pos.x + offX;
                double y = pos.y + offY;
                double z = pos.z + offZ;

                if (x >= minX && x < maxX && z >= minZ && z < maxZ)
                {
                    this.rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
                    //System.out.printf("post: %.1f - rot: %s, mm: %s, ms: %s\n", entity.yaw, rotationCombined, mirrorMain, mirrorSub);
                    EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
                }
            }
        }
    }

    private void rotateEntity(Entity entity, double x, double y, double z, BlockRotation rotationCombined, BlockMirror mirrorMain, BlockMirror mirrorSub)
    {
        float rotationYaw = entity.yaw;

        if (mirrorMain != BlockMirror.NONE)         { rotationYaw = entity.applyMirror(mirrorMain); }
        if (mirrorSub != BlockMirror.NONE)          { rotationYaw = entity.applyMirror(mirrorSub); }
        if (rotationCombined != BlockRotation.NONE) { rotationYaw += entity.yaw - entity.applyRotation(rotationCombined); }

        entity.refreshPositionAndAngles(x, y, z, rotationYaw, entity.pitch);
        EntityUtils.setEntityRotations(entity, rotationYaw, entity.pitch);
    }

    private void takeEntitiesFromWorld(World world, List<Box> boxes, BlockPos origin)
    {
        for (Box box : boxes)
        {
            net.minecraft.util.math.Box bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
            BlockPos regionPosAbs = box.getPos1();
            List<EntityInfo> list = new ArrayList<>();
            List<Entity> entities = world.getOtherEntities(null, bb, null);

            for (Entity entity : entities)
            {
                CompoundTag tag = new CompoundTag();

                if (entity.saveToTag(tag))
                {
                    Vec3d posVec = new Vec3d(entity.getX() - regionPosAbs.getX(), entity.getY() - regionPosAbs.getY(), entity.getZ() - regionPosAbs.getZ());
                    NBTUtils.writeEntityPositionToTag(posVec, tag);
                    list.add(new EntityInfo(posVec, tag));
                }
            }

            this.entities.put(box.getName(), list);
        }
    }

    public void takeEntitiesFromWorldWithinChunk(World world, int chunkX, int chunkZ,
            ImmutableMap<String, IntBoundingBox> volumes, ImmutableMap<String, Box> boxes,
            Set<UUID> existingEntities, BlockPos origin)
    {
        for (Map.Entry<String, IntBoundingBox> entry : volumes.entrySet())
        {
            String regionName = entry.getKey();
            List<EntityInfo> list = this.entities.get(regionName);
            Box box = boxes.get(regionName);

            if (box == null || list == null)
            {
                continue;
            }

            net.minecraft.util.math.Box bb = PositionUtils.createAABBFrom(entry.getValue());
            List<Entity> entities = world.getOtherEntities(null, bb, null);
            BlockPos regionPosAbs = box.getPos1();

            for (Entity entity : entities)
            {
                UUID uuid = entity.getUuid();
                /*
                if (entity.posX >= bb.minX && entity.posX < bb.maxX &&
                    entity.posY >= bb.minY && entity.posY < bb.maxY &&
                    entity.posZ >= bb.minZ && entity.posZ < bb.maxZ)
                */
                if (existingEntities.contains(uuid) == false)
                {
                    CompoundTag tag = new CompoundTag();

                    if (entity.saveToTag(tag))
                    {
                        Vec3d posVec = new Vec3d(entity.getX() - regionPosAbs.getX(), entity.getY() - regionPosAbs.getY(), entity.getZ() - regionPosAbs.getZ());
                        NBTUtils.writeEntityPositionToTag(posVec, tag);
                        list.add(new EntityInfo(posVec, tag));
                        existingEntities.add(uuid);
                    }
                }
            }
        }
    }

    private void takeBlocksFromWorld(World world, List<Box> boxes)
    {
        BlockPos.Mutable posMutable = new BlockPos.Mutable(0, 0, 0);

        for (Box box : boxes)
        {
            BlockPos size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());
            LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);
            Map<BlockPos, CompoundTag> tileEntityMap = new HashMap<>();
            Map<BlockPos, ScheduledTick<Block>> blockTickMap = new HashMap<>();
            Map<BlockPos, ScheduledTick<Fluid>> fluidTickMap = new HashMap<>();

            // We want to loop nice & easy from 0 to n here, but the per-sub-region pos1 can be at
            // any corner of the area. Thus we need to offset from the total area origin
            // to the minimum/negative corner (ie. 0,0 in the loop) corner here.
            final BlockPos minCorner = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            final int startX = minCorner.getX();
            final int startY = minCorner.getY();
            final int startZ = minCorner.getZ();

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        posMutable.set(x + startX, y + startY, z + startZ);
                        BlockState state = world.getBlockState(posMutable);
                        container.set(x, y, z, state);

                        if (state.isAir() == false)
                        {
                            this.totalBlocks++;
                        }

                        if (state.getBlock().hasBlockEntity())
                        {
                            BlockEntity te = world.getBlockEntity(posMutable);

                            if (te != null)
                            {
                                // TODO Add a TileEntity NBT cache from the Chunk packets, to get the original synced data (too)
                                BlockPos pos = new BlockPos(x, y, z);
                                CompoundTag tag = te.toTag(new CompoundTag());
                                NBTUtils.writeBlockPosToTag(pos, tag);
                                tileEntityMap.put(pos, tag);
                            }
                        }
                    }
                }
            }

            if (world instanceof ServerWorld)
            {
                IntBoundingBox tickBox = IntBoundingBox.createProper(
                        startX,         startY,         startZ,
                        startX + sizeX, startY + sizeY, startZ + sizeZ);
                List<ScheduledTick<Block>> blockTicks = ((ServerWorld) world).getBlockTickScheduler().getScheduledTicks(tickBox.toVanillaBox(), false, false);

                if (blockTicks != null)
                {
                    this.getPendingTicksFromWorld(blockTickMap, blockTicks, minCorner, startY, tickBox.maxY, world.getTime());
                }

                List<ScheduledTick<Fluid>> fluidTicks = ((ServerWorld) world).getFluidTickScheduler().getScheduledTicks(tickBox.toVanillaBox(), false, false);

                if (fluidTicks != null)
                {
                    this.getPendingTicksFromWorld(fluidTickMap, fluidTicks, minCorner, startY, tickBox.maxY, world.getTime());
                }
            }

            this.blockContainers.put(box.getName(), container);
            this.tileEntities.put(box.getName(), tileEntityMap);
            this.pendingBlockTicks.put(box.getName(), blockTickMap);
            this.pendingFluidTicks.put(box.getName(), fluidTickMap);
        }
    }

    private <T> void getPendingTicksFromWorld(Map<BlockPos, ScheduledTick<T>> map, List<ScheduledTick<T>> list,
            BlockPos minCorner, int startY, int maxY, final long currentTime)
    {
        final int listSize = list.size();

        for (int i = 0; i < listSize; ++i)
        {
            ScheduledTick<T> entry = list.get(i);

            // The getPendingBlockUpdates() method doesn't check the y-coordinate... :-<
            if (entry.pos.getY() >= startY && entry.pos.getY() < maxY)
            {
                // Store the delay, ie. relative time
                BlockPos posRelative = new BlockPos(
                        entry.pos.getX() - minCorner.getX(),
                        entry.pos.getY() - minCorner.getY(),
                        entry.pos.getZ() - minCorner.getZ());
                ScheduledTick<T> newEntry = new ScheduledTick<>(posRelative, entry.getObject(), entry.time - currentTime, entry.priority);

                map.put(posRelative, newEntry);
            }
        }
    }

    public void takeBlocksFromWorldWithinChunk(World world, int chunkX, int chunkZ,
            ImmutableMap<String, IntBoundingBox> volumes, ImmutableMap<String, Box> boxes)
    {
        BlockPos.Mutable posMutable = new BlockPos.Mutable(0, 0, 0);

        for (Map.Entry<String, IntBoundingBox> volumeEntry : volumes.entrySet())
        {
            String regionName = volumeEntry.getKey();
            IntBoundingBox bb = volumeEntry.getValue();
            Box box = boxes.get(regionName);

            if (box == null)
            {
                Litematica.logger.error("null Box for sub-region '{}' while trying to save chunk-wise schematic", regionName);
                continue;
            }

            LitematicaBlockStateContainer container = this.blockContainers.get(regionName);
            Map<BlockPos, CompoundTag> tileEntityMap = this.tileEntities.get(regionName);
            Map<BlockPos, ScheduledTick<Block>> blockTickMap = this.pendingBlockTicks.get(regionName);
            Map<BlockPos, ScheduledTick<Fluid>> fluidTickMap = this.pendingFluidTicks.get(regionName);

            if (container == null || tileEntityMap == null || blockTickMap == null || fluidTickMap == null)
            {
                Litematica.logger.error("null map(s) for sub-region '{}' while trying to save chunk-wise schematic", regionName);
                continue;
            }

            // We want to loop nice & easy from 0 to n here, but the per-sub-region pos1 can be at
            // any corner of the area. Thus we need to offset from the total area origin
            // to the minimum/negative corner (ie. 0,0 in the loop) corner here.
            final BlockPos minCorner = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            final int offsetX = minCorner.getX();
            final int offsetY = minCorner.getY();
            final int offsetZ = minCorner.getZ();
            // Relative coordinates within the sub-region container:
            final int startX = bb.minX - minCorner.getX();
            final int startY = bb.minY - minCorner.getY();
            final int startZ = bb.minZ - minCorner.getZ();
            final int endX = startX + (bb.maxX - bb.minX);
            final int endY = startY + (bb.maxY - bb.minY);
            final int endZ = startZ + (bb.maxZ - bb.minZ);

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(x + offsetX, y + offsetY, z + offsetZ);
                        BlockState state = world.getBlockState(posMutable);
                        container.set(x, y, z, state);

                        if (state.isAir() == false)
                        {
                            this.totalBlocks++;
                        }

                        if (state.getBlock().hasBlockEntity())
                        {
                            BlockEntity te = world.getBlockEntity(posMutable);

                            if (te != null)
                            {
                                // TODO Add a TileEntity NBT cache from the Chunk packets, to get the original synced data (too)
                                BlockPos pos = new BlockPos(x, y, z);
                                CompoundTag tag = te.toTag(new CompoundTag());
                                NBTUtils.writeBlockPosToTag(pos, tag);
                                tileEntityMap.put(pos, tag);
                            }
                        }
                    }
                }
            }

            if (world instanceof ServerWorld)
            {
                IntBoundingBox tickBox = IntBoundingBox.createProper(
                        offsetX + startX  , offsetY + startY  , offsetZ + startZ  ,
                        offsetX + endX + 1, offsetY + endY + 1, offsetZ + endZ + 1);
                List<ScheduledTick<Block>> blockTicks = ((ServerWorld) world).getBlockTickScheduler().getScheduledTicks(tickBox.toVanillaBox(), false, false);

                if (blockTicks != null)
                {
                    this.getPendingTicksFromWorld(blockTickMap, blockTicks, minCorner, startY, tickBox.maxY, world.getTime());
                }

                List<ScheduledTick<Fluid>> fluidTicks = ((ServerWorld) world).getFluidTickScheduler().getScheduledTicks(tickBox.toVanillaBox(), false, false);

                if (fluidTicks != null)
                {
                    this.getPendingTicksFromWorld(fluidTickMap, fluidTicks, minCorner, startY, tickBox.maxY, world.getTime());
                }
            }
        }
    }

    private void setSubRegionPositions(List<Box> boxes, BlockPos areaOrigin)
    {
        for (Box box : boxes)
        {
            this.subRegionPositions.put(box.getName(), box.getPos1().subtract(areaOrigin));
        }
    }

    private void setSubRegionSizes(List<Box> boxes)
    {
        for (Box box : boxes)
        {
            this.subRegionSizes.put(box.getName(), box.getSize());
        }
    }

    @Nullable
    public LitematicaBlockStateContainer getSubRegionContainer(String regionName)
    {
        return this.blockContainers.get(regionName);
    }

    private CompoundTag writeToNBT()
    {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("Version", SCHEMATIC_VERSION);
        nbt.putInt("MinecraftDataVersion", MINECRAFT_DATA_VERSION);
        nbt.put("Metadata", this.metadata.writeToNBT());
        nbt.put("Regions", this.writeSubRegionsToNBT());

        return nbt;
    }

    private CompoundTag writeSubRegionsToNBT()
    {
        CompoundTag wrapper = new CompoundTag();

        if (this.blockContainers.isEmpty() == false)
        {
            for (String regionName : this.blockContainers.keySet())
            {
                LitematicaBlockStateContainer blockContainer = this.blockContainers.get(regionName);
                Map<BlockPos, CompoundTag> tileMap = this.tileEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);
                Map<BlockPos, ScheduledTick<Block>> pendingBlockTicks = this.pendingBlockTicks.get(regionName);
                Map<BlockPos, ScheduledTick<Fluid>> pendingFluidTicks = this.pendingFluidTicks.get(regionName);

                CompoundTag tag = new CompoundTag();

                tag.put("BlockStatePalette", blockContainer.getPalette().writeToNBT());
                tag.put("BlockStates", new LongArrayTag(blockContainer.getBackingLongArray()));
                tag.put("TileEntities", this.writeTileEntitiesToNBT(tileMap));

                if (pendingBlockTicks != null)
                {
                    tag.put("PendingBlockTicks", this.writePendingTicksToNBT(pendingBlockTicks));
                }

                if (pendingFluidTicks != null)
                {
                    tag.put("PendingFluidTicks", this.writePendingTicksToNBT(pendingFluidTicks));
                }

                // The entity list will not exist, if takeEntities is false when creating the schematic
                if (entityList != null)
                {
                    tag.put("Entities", this.writeEntitiesToNBT(entityList));
                }

                BlockPos pos = this.subRegionPositions.get(regionName);
                tag.put("Position", NBTUtils.createBlockPosTag(pos));

                pos = this.subRegionSizes.get(regionName);
                tag.put("Size", NBTUtils.createBlockPosTag(pos));

                wrapper.put(regionName, tag);
            }
        }

        return wrapper;
    }

    private ListTag writeEntitiesToNBT(List<EntityInfo> entityList)
    {
        ListTag tagList = new ListTag();

        if (entityList.isEmpty() == false)
        {
            for (EntityInfo info : entityList)
            {
                tagList.add(info.nbt);
            }
        }

        return tagList;
    }

    private <T> ListTag writePendingTicksToNBT(Map<BlockPos, ScheduledTick<T>> tickMap)
    {
        ListTag tagList = new ListTag();

        if (tickMap.isEmpty() == false)
        {
            for (ScheduledTick<T> entry : tickMap.values())
            {
                T target = entry.getObject();
                String tagName;
                Identifier rl;

                if (target instanceof Block)
                {
                    rl = Registry.BLOCK.getId((Block) target);
                    tagName = "Block";
                }
                else
                {
                    rl = Registry.FLUID.getId((Fluid) target);
                    tagName = "Fluid";
                }

                if (rl != null)
                {
                    CompoundTag tag = new CompoundTag();

                    tag.putString(tagName, rl.toString());
                    tag.putInt("Priority", entry.priority.getIndex());
                    tag.putInt("Time", (int) entry.time);
                    tag.putInt("x", entry.pos.getX());
                    tag.putInt("y", entry.pos.getY());
                    tag.putInt("z", entry.pos.getZ());

                    tagList.add(tag);
                }
            }
        }

        return tagList;
    }

    private ListTag writeTileEntitiesToNBT(Map<BlockPos, CompoundTag> tileMap)
    {
        ListTag tagList = new ListTag();

        if (tileMap.isEmpty() == false)
        {
            for (CompoundTag tag : tileMap.values())
            {
                tagList.add(tag);
            }
        }

        return tagList;
    }

    private boolean readFromNBT(CompoundTag nbt)
    {
        this.blockContainers.clear();
        this.tileEntities.clear();
        this.entities.clear();
        this.subRegionPositions.clear();
        this.subRegionSizes.clear();

        if (nbt.contains("Version", Constants.NBT.TAG_INT))
        {
            final int version = nbt.getInt("Version");
            final int minecraftDataVersion = nbt.getInt("MinecraftDataVersion");

            if (version >= 1 && version <= SCHEMATIC_VERSION)
            {
                this.metadata.readFromNBT(nbt.getCompound("Metadata"));
                this.readSubRegionsFromNBT(nbt.getCompound("Regions"), version, minecraftDataVersion);

                return true;
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_schematic_version", version);
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_version_information");
        }

        return false;
    }

    private void readSubRegionsFromNBT(CompoundTag tag, int version, int minecraftDataVersion)
    {
        for (String regionName : tag.getKeys())
        {
            if (tag.get(regionName).getType() == Constants.NBT.TAG_COMPOUND)
            {
                CompoundTag regionTag = tag.getCompound(regionName);
                BlockPos regionPos = NBTUtils.readBlockPos(regionTag.getCompound("Position"));
                BlockPos regionSize = NBTUtils.readBlockPos(regionTag.getCompound("Size"));
                Map<BlockPos, CompoundTag> tiles = null;

                if (regionPos != null && regionSize != null)
                {
                    this.subRegionPositions.put(regionName, regionPos);
                    this.subRegionSizes.put(regionName, regionSize);

                    if (version >= 2)
                    {
                        tiles = this.readTileEntitiesFromNBT(regionTag.getList("TileEntities", Constants.NBT.TAG_COMPOUND));
                        this.tileEntities.put(regionName, tiles);
                        this.entities.put(regionName, this.readEntitiesFromNBT(regionTag.getList("Entities", Constants.NBT.TAG_COMPOUND)));
                    }
                    else if (version == 1)
                    {
                        tiles = this.readTileEntitiesFromNBT_v1(regionTag.getList("TileEntities", Constants.NBT.TAG_COMPOUND));
                        this.tileEntities.put(regionName, tiles);
                        this.entities.put(regionName, this.readEntitiesFromNBT_v1(regionTag.getList("Entities", Constants.NBT.TAG_COMPOUND)));
                    }

                    if (version >= 3)
                    {
                        ListTag list = regionTag.getList("PendingBlockTicks", Constants.NBT.TAG_COMPOUND);
                        this.pendingBlockTicks.put(regionName, this.readPendingTicksFromNBT(list, Blocks.AIR));
                    }

                    if (version >= 5)
                    {
                        ListTag list = regionTag.getList("PendingFluidTicks", Constants.NBT.TAG_COMPOUND);
                        this.pendingFluidTicks.put(regionName, this.readPendingTicksFromNBT(list, Fluids.EMPTY));
                    }

                    Tag nbtBase = regionTag.get("BlockStates");

                    // There are no convenience methods in NBTTagCompound yet in 1.12, so we'll have to do it the ugly way...
                    if (nbtBase != null && nbtBase.getType() == Constants.NBT.TAG_LONG_ARRAY)
                    {
                        ListTag palette = regionTag.getList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
                        long[] blockStateArr = ((LongArrayTag) nbtBase).getLongArray();

                        BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).add(regionPos);
                        BlockPos posMin = PositionUtils.getMinCorner(regionPos, posEndRel);
                        BlockPos posMax = PositionUtils.getMaxCorner(regionPos, posEndRel);
                        BlockPos size = posMax.subtract(posMin).add(1, 1, 1);

                        palette = this.convertBlockStatePalette_1_12_to_1_13_2(palette, version, minecraftDataVersion);

                        LitematicaBlockStateContainer container = LitematicaBlockStateContainer.createFrom(palette, blockStateArr, size);

                        if (minecraftDataVersion < MINECRAFT_DATA_VERSION)
                        {
                            this.postProcessContainerIfNeeded(palette, container, tiles);
                        }

                        this.blockContainers.put(regionName, container);
                    }
                }
            }
        }
    }

    public static boolean isSizeValid(@Nullable Vec3i size)
    {
        return size != null && size.getX() > 0 && size.getY() > 0 && size.getZ() > 0;
    }

    @Nullable
    private static Vec3i readSizeFromTagImpl(CompoundTag tag)
    {
        if (tag.contains("size", Constants.NBT.TAG_LIST))
        {
            ListTag tagList = tag.getList("size", Constants.NBT.TAG_INT);

            if (tagList.size() == 3)
            {
                return new Vec3i(tagList.getInt(0), tagList.getInt(1), tagList.getInt(2));
            }
        }

        return null;
    }

    @Nullable
    public static BlockPos readBlockPosFromListTag(CompoundTag tag, String tagName)
    {
        if (tag.contains(tagName, Constants.NBT.TAG_LIST))
        {
            ListTag tagList = tag.getList(tagName, Constants.NBT.TAG_INT);

            if (tagList.size() == 3)
            {
                return new BlockPos(tagList.getInt(0), tagList.getInt(1), tagList.getInt(2));
            }
        }

        return null;
    }

    protected boolean readPaletteFromLitematicaFormatTag(ListTag tagList, ILitematicaBlockStatePalette palette)
    {
        final int size = tagList.size();
        List<BlockState> list = new ArrayList<>(size);

        for (int id = 0; id < size; ++id)
        {
            CompoundTag tag = tagList.getCompound(id);
            BlockState state = NbtHelper.toBlockState(tag);
            list.add(state);
        }

        return palette.setMapping(list);
    }

    public boolean readBlocksFromVanillaStructure(String name, CompoundTag tag)
    {
        Vec3i size = readSizeFromTagImpl(tag);

        if (tag.contains("palette", Constants.NBT.TAG_LIST) &&
            tag.contains("blocks", Constants.NBT.TAG_LIST) &&
            isSizeValid(size))
        {
            ListTag paletteTag = tag.getList("palette", Constants.NBT.TAG_COMPOUND);

            Map<BlockPos, CompoundTag> tileMap = new HashMap<>();
            this.tileEntities.put(name, tileMap);

            BlockState air = Blocks.AIR.getDefaultState();
            int paletteSize = paletteTag.size();
            List<BlockState> list = new ArrayList<>(paletteSize);

            for (int id = 0; id < paletteSize; ++id)
            {
                CompoundTag t = paletteTag.getCompound(id);
                BlockState state = NbtHelper.toBlockState(t);
                list.add(state);
            }

            BlockState zeroState = list.get(0);
            int airId = -1;

            // If air is not ID 0, then we need to re-map the palette such that air is ID 0,
            // due to how it's currently handled in the Litematica container.
            for (int i = 0; i < paletteSize; ++i)
            {
                if (list.get(i) == air)
                {
                    airId = i;
                    break;
                }
            }

            if (airId != 0)
            {
                // No air in the palette, insert it
                if (airId == -1)
                {
                    list.add(0, air);
                    ++paletteSize;
                }
                // Air as some other ID, swap the entries
                else
                {
                    list.set(0, air);
                    list.set(airId, zeroState);
                }
            }

            int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
            LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(size.getX(), size.getY(), size.getZ(), bits, null);
            ILitematicaBlockStatePalette palette = container.getPalette();
            palette.setMapping(list);
            this.blockContainers.put(name, container);

            if (tag.contains("author", Constants.NBT.TAG_STRING))
            {
                this.getMetadata().setAuthor(tag.getString("author"));
            }

            this.subRegionPositions.put(name, BlockPos.ORIGIN);
            this.subRegionSizes.put(name, new BlockPos(size));
            this.metadata.setName(name);
            this.metadata.setRegionCount(1);
            this.metadata.setTotalVolume(size.getX() * size.getY() * size.getZ());
            this.metadata.setEnclosingSize(size);
            this.metadata.setTimeCreated(System.currentTimeMillis());
            this.metadata.setTimeModified(this.metadata.getTimeCreated());

            ListTag blockList = tag.getList("blocks", Constants.NBT.TAG_COMPOUND);
            final int count = blockList.size();
            int totalBlocks = 0;

            for (int i = 0; i < count; ++i)
            {
                CompoundTag blockTag = blockList.getCompound(i);
                BlockPos pos = readBlockPosFromListTag(blockTag, "pos");

                if (pos == null)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read block position for vanilla structure");
                    return false;
                }

                int id = blockTag.getInt("state");
                BlockState state;

                // Air was inserted as ID 0, so the other IDs need to shift
                if (airId == -1)
                {
                    state = palette.getBlockState(id + 1);
                }
                else if (airId != 0)
                {
                    // re-mapping air and ID 0 state
                    if (id == 0)
                    {
                        state = zeroState;
                    }
                    else if (id == airId)
                    {
                        state = air;
                    }
                    else
                    {
                        state = palette.getBlockState(id);
                    }
                }
                else
                {
                    state = palette.getBlockState(id);
                }

                if (state == null)
                {
                    state = air;
                }
                else if (state != air)
                {
                    ++totalBlocks;
                }

                container.set(pos.getX(), pos.getY(), pos.getZ(), state);

                if (blockTag.contains("nbt", Constants.NBT.TAG_COMPOUND))
                {
                    tileMap.put(pos, blockTag.getCompound("nbt"));
                }
            }

            this.metadata.setTotalBlocks(totalBlocks);
            this.entities.put(name, this.readEntitiesFromVanillaStructure(tag));

            return true;
        }

        return false;
    }

    protected List<EntityInfo> readEntitiesFromVanillaStructure(CompoundTag tag)
    {
        List<EntityInfo> entities = new ArrayList<>();
        ListTag tagList = tag.getList("entities", Constants.NBT.TAG_COMPOUND);
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag entityData = tagList.getCompound(i);
            Vec3d pos = readVec3dFromListTag(entityData, "pos");

            if (pos != null && entityData.contains("nbt", Constants.NBT.TAG_COMPOUND))
            {
                entities.add(new EntityInfo(pos, entityData.getCompound("nbt")));
            }
        }

        return entities;
    }

    @Nullable
    public static Vec3d readVec3dFromListTag(@Nullable CompoundTag tag, String tagName)
    {
        if (tag != null && tag.contains(tagName, Constants.NBT.TAG_LIST))
        {
            ListTag tagList = tag.getList(tagName, Constants.NBT.TAG_DOUBLE);

            if (tagList.getElementType() == Constants.NBT.TAG_DOUBLE && tagList.size() == 3)
            {
                return new Vec3d(tagList.getDouble(0), tagList.getDouble(1), tagList.getDouble(2));
            }
        }

        return null;
    }

    private void postProcessContainerIfNeeded(ListTag palette, LitematicaBlockStateContainer container, @Nullable Map<BlockPos, CompoundTag> tiles)
    {
        List<BlockState> states = getStatesFromPaletteTag(palette);

        if (this.converter.createPostProcessStateFilter(states))
        {
            IdentityHashMap<BlockState, SchematicConversionFixers.IStateFixer> postProcessingFilter = this.converter.getPostProcessStateFilter();
            SchematicConverter.postProcessBlocks(container, tiles, postProcessingFilter);
        }
    }

    public static List<BlockState> getStatesFromPaletteTag(ListTag palette)
    {
        List<BlockState> states = new ArrayList<>();
        final int size = palette.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = palette.getCompound(i);
            BlockState state = NbtHelper.toBlockState(tag);

            if (i > 0 || state != LitematicaBlockStateContainer.AIR_BLOCK_STATE)
            {
                states.add(state);
            }
        }

        return states;
    }

    private ListTag convertBlockStatePalette_1_12_to_1_13_2(ListTag oldPalette, int version, int minecraftDataVersion)
    {
        // The Minecraft data version didn't yet exist in the first 1.13.2 builds, so only
        // consider it if it actually exists in the file, ie. is larger than the default value of 0.
        if (version < SCHEMATIC_VERSION_1_13_2 || (minecraftDataVersion < MINECRAFT_DATA_VERSION_1_13_2 && minecraftDataVersion > 0))
        {
            ListTag newPalette = new ListTag();
            final int count = oldPalette.size();

            for (int i = 0; i < count; ++i)
            {
                newPalette.add(SchematicConversionMaps.get_1_13_2_StateTagFor_1_12_Tag(oldPalette.getCompound(i)));
            }

            return newPalette;
        }

        return oldPalette;
    }

    private List<EntityInfo> readEntitiesFromNBT(ListTag tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag entityData = tagList.getCompound(i);
            Vec3d posVec = NBTUtils.readEntityPositionFromTag(entityData);

            if (posVec != null && entityData.isEmpty() == false)
            {
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    private Map<BlockPos, CompoundTag> readTileEntitiesFromNBT(ListTag tagList)
    {
        Map<BlockPos, CompoundTag> tileMap = new HashMap<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompound(i);
            BlockPos pos = NBTUtils.readBlockPos(tag);

            if (pos != null && tag.isEmpty() == false)
            {
                tileMap.put(pos, tag);
            }
        }

        return tileMap;
    }

    @SuppressWarnings("unchecked")
    private <T> Map<BlockPos, ScheduledTick<T>> readPendingTicksFromNBT(ListTag tagList, T clazz)
    {
        Map<BlockPos, ScheduledTick<T>> tickMap = new HashMap<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompound(i);

            if (tag.contains("Time", Constants.NBT.TAG_ANY_NUMERIC)) // XXX these were accidentally saved as longs in version 3
            {
                T target = null;

                // Don't crash on invalid ResourceLocation in 1.13+
                try
                {
                    if (clazz instanceof Block && tag.contains("Block", Constants.NBT.TAG_STRING))
                    {
                        target = (T) Registry.BLOCK.get(new Identifier(tag.getString("Block")));

                        if (target == null || target == Blocks.AIR)
                        {
                            continue;
                        }
                    }
                    else if (clazz instanceof Fluid && tag.contains("Fluid", Constants.NBT.TAG_STRING))
                    {
                        target = (T) Registry.FLUID.get(new Identifier(tag.getString("Fluid")));

                        if (target == null || target == Fluids.EMPTY)
                        {
                            continue;
                        }
                    }
                }
                catch (Exception e)
                {
                }

                if (target != null)
                {
                    BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                    // Note: the time is a relative delay at this point
                    int scheduledTime = tag.getInt("Time");
                    TickPriority priority = TickPriority.byIndex(tag.getInt("Priority"));

                    ScheduledTick<T> entry = new ScheduledTick<>(pos, target, scheduledTime, priority);

                    tickMap.put(pos, entry);
                }
            }
        }

        return tickMap;
    }

    private List<EntityInfo> readEntitiesFromNBT_v1(ListTag tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompound(i);
            Vec3d posVec = NBTUtils.readVec3d(tag);
            CompoundTag entityData = tag.getCompound("EntityData");

            if (posVec != null && entityData.isEmpty() == false)
            {
                // Update the correct position to the TileEntity NBT, where it is stored in version 2
                NBTUtils.writeEntityPositionToTag(posVec, entityData);
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    private Map<BlockPos, CompoundTag> readTileEntitiesFromNBT_v1(ListTag tagList)
    {
        Map<BlockPos, CompoundTag> tileMap = new HashMap<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompound(i);
            CompoundTag tileNbt = tag.getCompound("TileNBT");

            // Note: This within-schematic relative position is not inside the tile tag!
            BlockPos pos = NBTUtils.readBlockPos(tag);

            if (pos != null && tileNbt.isEmpty() == false)
            {
                // Update the correct position to the entity NBT, where it is stored in version 2
                NBTUtils.writeBlockPosToTag(pos, tileNbt);
                tileMap.put(pos, tileNbt);
            }
        }

        return tileMap;
    }

    public boolean writeToFile(File dir, String fileNameIn, boolean override)
    {
        String fileName = fileNameIn;

        if (fileName.endsWith(FILE_EXTENSION) == false)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        File fileSchematic = new File(dir, fileName);

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath());
                return false;
            }

            if (override == false && fileSchematic.exists())
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exists", fileSchematic.getAbsolutePath());
                return false;
            }

            FileOutputStream os = new FileOutputStream(fileSchematic);
            NbtIo.writeCompressed(this.writeToNBT(), os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exception", fileSchematic.getAbsolutePath());
            Litematica.logger.error(StringUtils.translate("litematica.error.schematic_write_to_file_failed.exception", fileSchematic.getAbsolutePath()), e);
            Litematica.logger.error(e.getMessage());
        }

        return false;
    }

    @Nullable
    public static LitematicaSchematic createFromFile(File dir, String fileName)
    {
        return createFromFile(dir, fileName, false);
    }

    public static LitematicaSchematic createFromFile(File dir, String fileName, boolean vanillaStructure)
    {
        if (fileName.endsWith(FILE_EXTENSION) == false && vanillaStructure == false)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        File fileSchematic = new File(dir, fileName);

        if (fileSchematic.exists() == false || fileSchematic.canRead() == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.cant_read", fileSchematic.getAbsolutePath());
            return null;
        }

        try
        {
            FileInputStream is = new FileInputStream(fileSchematic);
            CompoundTag nbt = NbtIo.readCompressed(is);
            is.close();

            if (nbt != null)
            {
                LitematicaSchematic schematic = new LitematicaSchematic(fileSchematic);

                if (vanillaStructure)
                {
                    String name = FileUtils.getNameWithoutExtension(fileName) + " (Converted Structure)";

                    if (schematic.readBlocksFromVanillaStructure(name, nbt))
                    {
                        return schematic;
                    }
                }
                else if (schematic.readFromNBT(nbt))
                {
                    return schematic;
                }
            }
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.exception", fileSchematic.getAbsolutePath());
        }

        return null;
    }

    public static class EntityInfo
    {
        public final Vec3d posVec;
        public final CompoundTag nbt;

        public EntityInfo(Vec3d posVec, CompoundTag nbt)
        {
            this.posVec = posVec;
            this.nbt = nbt;
        }
    }
}
