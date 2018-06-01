package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.mixin.IMixinNBTTagLongArray;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.Constants;
import fi.dy.masa.litematica.util.NBTUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LitematicaSchematic
{
    public static final String FILE_EXTENSION = ".litematic";
    public static final int SCHEMATIC_VERSION = 1;
    private final Map<String, LitematicaBlockStateContainer> blockContainers = new HashMap<>();
    private final Map<String, Map<BlockPos, NBTTagCompound>> tileEntities = new HashMap<>();
    private final Map<String, List<EntityInfo>> entities = new HashMap<>();
    private final Map<String, BlockPos> subRegionPositions = new HashMap<>();
    private final Map<String, BlockPos> subRegionSizes = new HashMap<>();
    private final SchematicMetadata metadata = new SchematicMetadata();
    private BlockPos totalSize = BlockPos.ORIGIN;
    private int totalBlocks;
    @Nullable
    private final File schematicFile;

    private LitematicaSchematic(@Nullable File file)
    {
        this.schematicFile = file;
    }

    @Nullable
    public File getFile()
    {
        return this.schematicFile;
    }

    public BlockPos getTotalSize()
    {
        return this.totalSize;
    }

    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    public int getSubRegionCount()
    {
        return this.blockContainers.size();
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
    public static LitematicaSchematic createSchematic(World world, AreaSelection area, boolean takeEntities, String author, IStringConsumer feedback)
    {
        List<Box> boxes = PositionUtils.getValidBoxes(area);

        if (boxes.isEmpty())
        {
            feedback.setString(I18n.format("litematica.error.schematic.create.no_selections"));
            return null;
        }

        LitematicaSchematic schematic = new LitematicaSchematic(null);

        long time = (new Date()).getTime();
        schematic.totalSize = PositionUtils.getEnclosingAreaSize(area);
        schematic.metadata.setAuthor(author);
        schematic.metadata.setTimeCreated(time);
        schematic.metadata.setTimeModified(time);
        schematic.metadata.setRegionCount(boxes.size());
        schematic.metadata.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        schematic.metadata.setEnclosingSize(PositionUtils.getEnclosingAreaSize(boxes));

        schematic.setSubRegionPositions(boxes, area.getOrigin());
        schematic.setSubRegionSizes(boxes);

        schematic.takeBlocksFromWorld(world, boxes, area.getOrigin());
        schematic.metadata.setTotalBlocks(schematic.totalBlocks);

        if (takeEntities)
        {
            schematic.takeEntitiesFromWorld(world, boxes, area.getOrigin());
        }

        return schematic;
    }

    public boolean placeToWorld(World world, SchematicPlacement schematicPlacement, boolean notifyNeighbors)
    {
        /*
        Pair<BlockPos, BlockPos> pair = PositionUtils.getEnclosingAreaCorners(this.getAreas().values());
        BlockPos posMin = pair.getLeft();
        BlockPos posMax = pair.getRight();

        // FIXME testing, move elsewhere
        if (world.getChunkProvider() instanceof ChunkProviderClient)
        {
            BlockPos pos1 = Template.transformedBlockPos(placement, posMin).add(origin);
            BlockPos pos2 = Template.transformedBlockPos(placement, posMax).add(origin);
            int x1 = pos1.getX() >> 4;
            int z1 = pos1.getZ() >> 4;
            int x2 = pos2.getX() >> 4;
            int z2 = pos2.getZ() >> 4;
            int border = 3;

            for (int cz = z1 - border; cz <= z2 + border; ++cz)
            {
                for (int cx = x1 - border; cx <= x2 + border; ++cx)
                {
                    if (((ChunkProviderClient) world.getChunkProvider()).getLoadedChunk(cx, cz) == null)
                    {
                        ((ChunkProviderClient) world.getChunkProvider()).loadChunk(cx, cz);
                    }
                }
            }
        }
        */

        ImmutableMap<String, Placement> relativePlacements = schematicPlacement.getRelativeSubRegionPlacements();
        BlockPos origin = schematicPlacement.getOrigin();

        for (String regionName : relativePlacements.keySet())
        {
            Placement placement = relativePlacements.get(regionName);

            if (placement.isEnabled())
            {
                BlockPos regionPos = placement.getPos();
                BlockPos regionSize = this.subRegionSizes.get(regionName);
                LitematicaBlockStateContainer container = this.blockContainers.get(regionName);
                Map<BlockPos, NBTTagCompound> tileMap = this.tileEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);

                if (regionPos != null && regionSize != null && container != null && tileMap != null && entityList != null)
                {
                    this.placeBlocksToWorld(world, origin, regionPos, regionSize, schematicPlacement, placement, container, tileMap, notifyNeighbors);
                    this.placeEntitiesToWorld(world, origin, regionPos, schematicPlacement, placement, entityList);
                }
                else
                {
                    LiteModLitematica.logger.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", this.metadata.getName(), regionName);
                }
            }
        }

        return true;
    }

    private void placeBlocksToWorld(World world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, Placement placement,
            LitematicaBlockStateContainer container, Map<BlockPos, NBTTagCompound> tileMap, boolean notifyNeighbors)
    {
        // These are the untransformed relative positions
        BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);
        BlockPos posMaxRel = PositionUtils.getMaxCorner(regionPos, posEndRel);
        BlockPos posMinAbs = PositionUtils.getTransformedPlacementPosition(regionPos.subtract(posMinRel), schematicPlacement, placement).add(origin);
        BlockPos posMaxAbs = PositionUtils.getTransformedPlacementPosition(regionPos.subtract(posMaxRel), schematicPlacement, placement).add(origin);
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        if (PositionUtils.arePositionsWithinWorld(world, posMinAbs, posMaxAbs))
        {
            final int sizeX = posMaxRel.getX() - posMinRel.getX() + 1;
            final int sizeY = posMaxRel.getY() - posMinRel.getY() + 1;
            final int sizeZ = posMaxRel.getZ() - posMinRel.getZ() + 1;
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        IBlockState state = container.get(x, y, z);

                        if (state.getBlock() == Blocks.AIR)
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

                        Mirror mirror = schematicPlacement.getMirror();
                        if (mirror != Mirror.NONE) { state = state.withMirror(mirror); }
                        mirror = placement.getMirror();
                        if (mirror != Mirror.NONE) { state = state.withMirror(mirror); }

                        Rotation rotation = schematicPlacement.getRotation().add(placement.getRotation());
                        if (rotation != Rotation.NONE) { state = state.withRotation(rotation); }

                        if (teNBT != null)
                        {
                            TileEntity te = world.getTileEntity(pos);

                            if (te != null)
                            {
                                if (te instanceof IInventory)
                                {
                                    ((IInventory) te).clear();
                                }

                                world.setBlockState(pos, Blocks.BARRIER.getDefaultState(), 4);
                            }
                        }

                        if (world.setBlockState(pos, state, 0x12) && teNBT != null)
                        {
                            TileEntity te = world.getTileEntity(pos);

                            if (te != null)
                            {
                                teNBT.setInteger("x", pos.getX());
                                teNBT.setInteger("y", pos.getY());
                                teNBT.setInteger("z", pos.getZ());
                                te.readFromNBT(teNBT);
                                te.mirror(placement.getMirror());
                                te.rotate(placement.getRotation());
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
                            posMutable.setPos(  posMinRel.getX() + x - regionPos.getX(),
                                                posMinRel.getY() + y - regionPos.getY(),
                                                posMinRel.getZ() + z - regionPos.getZ());
                            BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement).add(origin);
                            world.notifyNeighborsRespectDebug(pos, world.getBlockState(pos).getBlock(), false);
                        }
                    }
                }
            }
        }
    }

    private void placeEntitiesToWorld(World world, BlockPos origin, BlockPos regionPos, SchematicPlacement schematicPlacement, Placement placement, List<EntityInfo> entityList)
    {
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();

        for (EntityInfo info : entityList)
        {
            Entity entity = EntityList.createEntityFromNBT(info.nbt, world);

            if (entity != null)
            {
                Vec3d pos = info.posVec;
                pos = PositionUtils.transformedVec3d(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                pos = PositionUtils.transformedVec3d(pos, placement.getMirror(), placement.getRotation());

                entity.setLocationAndAngles(pos.x + offX, pos.y + offY, pos.z + offZ, entity.rotationYaw, entity.rotationPitch);
                world.spawnEntity(entity);

                entity.prevRotationYaw = entity.rotationYaw;
                entity.prevRotationPitch = entity.rotationPitch;
                entity.ticksExisted = 2;
            }
        }
    }

    private void takeEntitiesFromWorld(World world, List<Box> boxes, BlockPos origin)
    {
        for (Box box : boxes)
        {
            AxisAlignedBB bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
            BlockPos regionPosAbs = box.getPos1();
            List<EntityInfo> list = new ArrayList<>();
            List<Entity> entities = world.getEntitiesInAABBexcluding(null, bb, null);

            for (Entity entity : entities)
            {
                NBTTagCompound tag = new NBTTagCompound();

                if (entity.writeToNBTOptional(tag))
                {
                    Vec3d posVec = new Vec3d(entity.posX - regionPosAbs.getX(), entity.posY - regionPosAbs.getY(), entity.posZ - regionPosAbs.getZ());
                    BlockPos pos;

                    if (entity instanceof EntityPainting)
                    {
                        pos = ((EntityPainting) entity).getHangingPosition().subtract(regionPosAbs);
                    }
                    else
                    {
                        pos = new BlockPos(posVec);
                    }

                    list.add(new EntityInfo(pos, posVec, tag));
                }
            }

            this.entities.put(box.getName(), list);
        }
    }

    private void takeBlocksFromWorld(World world, List<Box> boxes, BlockPos origin)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

        for (Box box : boxes)
        {
            Map<BlockPos, NBTTagCompound> map = new HashMap<>();
            BlockPos size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());
            LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);

            // We want to loop nice & easy from 0 to n here, but the per-sub-region pos1 can be at
            // any corner of the area. Thus we need to offset from the total area origin
            // to the minimum/negative corner (ie. 0,0 in the loop) corner here.
            BlockPos minCorner = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            final int startX = minCorner.getX();
            final int startY = minCorner.getY();
            final int startZ = minCorner.getZ();

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        posMutable.setPos(x + startX, y + startY, z + startZ);
                        IBlockState state = world.getBlockState(posMutable).getActualState(world, posMutable);
                        container.set(x, y, z, state);

                        if (state.getBlock() != Blocks.AIR)
                        {
                            this.totalBlocks++;
                        }

                        if (state.getBlock().hasTileEntity())
                        {
                            TileEntity te = world.getTileEntity(posMutable);

                            if (te != null)
                            {
                                // TODO Add a TileEntity NBT cache from the Chunk packets, to get the original synced data (too)
                                BlockPos pos = new BlockPos(x + startX - origin.getX(), y + startY - origin.getY(), z + startZ - origin.getZ());
                                NBTTagCompound nbt = new NBTTagCompound();
                                nbt = te.writeToNBT(nbt);
                                map.put(pos, nbt);
                            }
                        }
                    }
                }
            }

            this.tileEntities.put(box.getName(), map);
            this.blockContainers.put(box.getName(), container);
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

    private NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setInteger("Version", SCHEMATIC_VERSION);
        nbt.setTag("TotalSize", NBTUtils.createBlockPosTag(this.totalSize));
        nbt.setTag("Metadata", this.metadata.writeToNBT());
        nbt.setTag("Regions", this.writeSubRegionsToNBT());

        return nbt;
    }

    private NBTTagCompound writeSubRegionsToNBT()
    {
        NBTTagCompound wrapper = new NBTTagCompound();

        if (this.blockContainers.isEmpty() == false)
        {
            for (String regionName : this.blockContainers.keySet())
            {
                LitematicaBlockStateContainer blockContainer = this.blockContainers.get(regionName);
                Map<BlockPos, NBTTagCompound> tileMap = this.tileEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);

                NBTTagCompound tag = new NBTTagCompound();

                tag.setTag("BlockStatePalette", blockContainer.getPalette().writeToNBT());
                tag.setTag("BlockStates", new NBTTagLongArray(blockContainer.getBackingLongArray()));
                tag.setTag("TileEntities", this.writeTileEntitiesToNBT(tileMap));

                // The entity list will not exist, if takeEntities is false when creating the schematic
                if (entityList != null)
                {
                    tag.setTag("Entities", this.writeEntitiesToNBT(entityList));
                }

                BlockPos pos = this.subRegionPositions.get(regionName);
                tag.setTag("Position", NBTUtils.createBlockPosTag(pos));

                pos = this.subRegionSizes.get(regionName);
                tag.setTag("Size", NBTUtils.createBlockPosTag(pos));

                wrapper.setTag(regionName, tag);
            }
        }

        return wrapper;
    }

    private NBTTagList writeEntitiesToNBT(List<EntityInfo> entityList)
    {
        NBTTagList tagList = new NBTTagList();

        if (entityList.isEmpty() == false)
        {
            for (EntityInfo info : entityList)
            {
                NBTTagCompound tag = new NBTTagCompound();

                NBTUtils.writeBlockPosToTag(info.pos, tag);
                NBTUtils.writeVec3dToTag(info.posVec, tag);
                tag.setTag("EntityData", info.nbt);

                tagList.appendTag(tag);
            }
        }

        return tagList;
    }

    private NBTTagList writeTileEntitiesToNBT(Map<BlockPos, NBTTagCompound> tileMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tileMap.isEmpty() == false)
        {
            for (Map.Entry<BlockPos, NBTTagCompound> entry : tileMap.entrySet())
            {
                NBTTagCompound tag = new NBTTagCompound();
                NBTTagCompound tileNbt = entry.getValue();
                tileNbt.removeTag("x");
                tileNbt.removeTag("y");
                tileNbt.removeTag("z");
                tag.setTag("TileNBT", entry.getValue());

                // Note: This within-schematic relative position is not inside the tile tag!
                NBTUtils.writeBlockPosToTag(entry.getKey(), tag);

                tagList.appendTag(tag);
            }
        }

        return tagList;
    }

    private boolean readFromNBT(NBTTagCompound nbt)
    {
        if (nbt.getInteger("Version") == SCHEMATIC_VERSION)
        {
            this.blockContainers.clear();
            this.tileEntities.clear();
            this.entities.clear();
            this.subRegionPositions.clear();
            this.subRegionSizes.clear();

            BlockPos size = NBTUtils.readBlockPos(nbt.getCompoundTag("TotalSize"));

            if (size != null)
            {
                this.totalSize = size;
            }

            this.metadata.readFromNBT(nbt.getCompoundTag("Metadata"));
            this.readSubRegionsFromNBT(nbt.getCompoundTag("Regions"));

            return true;
        }

        return false;
    }

    private void readSubRegionsFromNBT(NBTTagCompound tag)
    {
        for (String regionName : tag.getKeySet())
        {
            if (tag.getTag(regionName).getId() == Constants.NBT.TAG_COMPOUND)
            {
                NBTTagCompound regionTag = tag.getCompoundTag(regionName);
                BlockPos regionPos = NBTUtils.readBlockPos(regionTag.getCompoundTag("Position"));
                BlockPos regionSize = NBTUtils.readBlockPos(regionTag.getCompoundTag("Size"));

                if (regionPos != null && regionSize != null)
                {
                    this.subRegionPositions.put(regionName, regionPos);
                    this.subRegionSizes.put(regionName, regionSize);

                    this.tileEntities.put(regionName, this.readTileEntitiesFromNBT(regionTag.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND)));
                    this.entities.put(regionName, this.readEntitiesFromNBT(regionTag.getTagList("Entities", Constants.NBT.TAG_COMPOUND)));

                    NBTBase nbtBase = regionTag.getTag("BlockStates");

                    // There are no convenience methods in NBTTagCompound yet in 1.12, so we'll have to do it the ugly way...
                    if (nbtBase != null && nbtBase.getId() == Constants.NBT.TAG_LONG_ARRAY)
                    {
                        NBTTagList palette = regionTag.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
                        long[] blockStateArr = ((IMixinNBTTagLongArray) nbtBase).getArray();

                        BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).add(regionPos);
                        BlockPos posMin = PositionUtils.getMinCorner(regionPos, posEndRel);
                        BlockPos posMax = PositionUtils.getMaxCorner(regionPos, posEndRel);
                        BlockPos size = posMax.subtract(posMin).add(1, 1, 1);

                        LitematicaBlockStateContainer container = LitematicaBlockStateContainer.createFrom(palette, blockStateArr, size);
                        this.blockContainers.put(regionName, container);
                    }
                }
            }
        }
    }

    private List<EntityInfo> readEntitiesFromNBT(NBTTagList tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            BlockPos pos = NBTUtils.readBlockPos(tag);
            Vec3d posVec = NBTUtils.readVec3d(tag);
            NBTTagCompound entityData = tag.getCompoundTag("EntityData");

            if (pos != null && posVec != null && entityData.hasNoTags() == false)
            {
                entityList.add(new EntityInfo(pos, posVec, entityData));
            }
        }

        return entityList;
    }

    private Map<BlockPos, NBTTagCompound> readTileEntitiesFromNBT(NBTTagList tagList)
    {
        Map<BlockPos, NBTTagCompound> tileMap = new HashMap<>();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            NBTTagCompound tileNbt = tag.getCompoundTag("TileNBT");

            // Note: This within-schematic relative position is not inside the tile tag!
            BlockPos pos = NBTUtils.readBlockPos(tag);

            if (pos != null && tileNbt.hasNoTags() == false)
            {
                tileMap.put(pos, tileNbt);
            }
        }

        return tileMap;
    }

    public boolean writeToFile(File dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;

        if (fileName.endsWith(FILE_EXTENSION) == false)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        File fileSchematic = new File(dir, fileName);
        File fileMeta = new File(dir, fileName + "_meta");

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath()));
                return false;
            }

            if (override == false && fileSchematic.exists())
            {
                feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.exists", fileSchematic.getAbsolutePath()));
                return false;
            }

            if (override == false && fileMeta.exists())
            {
                feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.exists", fileMeta.getAbsolutePath()));
                return false;
            }

            FileOutputStream os = new FileOutputStream(fileSchematic);
            CompressedStreamTools.writeCompressed(this.writeToNBT(), os);
            os.close();

            os = new FileOutputStream(fileMeta);
            CompressedStreamTools.writeCompressed(this.metadata.writeToNBT(), os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.exception", fileSchematic.getAbsolutePath()));
        }

        return false;
    }

    @Nullable
    public static LitematicaSchematic createFromFile(File dir, String fileNameIn, IStringConsumer feedback)
    {
        String fileName = fileNameIn;

        if (fileName.endsWith(FILE_EXTENSION) == false)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        File fileSchematic = new File(dir, fileName);

        if (fileSchematic.exists() == false || fileSchematic.canRead() == false)
        {
            feedback.setString(I18n.format("litematica.error.schematic_read_from_file_failed.cant_read", fileSchematic.getAbsolutePath()));
            return null;
        }

        try
        {
            FileInputStream is = new FileInputStream(fileSchematic);
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
            is.close();

            if (nbt != null)
            {
                LitematicaSchematic schematic = new LitematicaSchematic(fileSchematic);

                if (schematic.readFromNBT(nbt))
                {
                    return schematic;
                }
            }
        }
        catch (Exception e)
        {
            feedback.setString(I18n.format("litematica.error.schematic_read_from_file_failed.exception", fileSchematic.getAbsolutePath()));
        }

        return null;
    }

    public static class EntityInfo
    {
        public final BlockPos pos;
        public final Vec3d posVec;
        public final NBTTagCompound nbt;

        public EntityInfo(BlockPos pos, Vec3d posVec, NBTTagCompound nbt)
        {
            this.pos = pos;
            this.posVec = posVec;
            this.nbt = nbt;
        }
    }
}
