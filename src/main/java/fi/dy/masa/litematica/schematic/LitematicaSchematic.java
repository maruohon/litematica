package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.mixin.IMixinDataFixer;
import fi.dy.masa.litematica.mixin.IMixinNBTTagLongArray;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public class LitematicaSchematic
{
    public static final String FILE_EXTENSION = ".litematic";
    public static final int SCHEMATIC_VERSION = 4;
    public static final int MINECRAFT_DATA_VERSION = ((IMixinDataFixer) Minecraft.getMinecraft().getDataFixer()).getVersion();

    private final Map<String, LitematicaBlockStateContainer> blockContainers = new HashMap<>();
    private final Map<String, Map<BlockPos, NBTTagCompound>> blockEntities = new HashMap<>();
    private final Map<String, Map<BlockPos, NextTickListEntry>> pendingBlockTicks = new HashMap<>();
    private final Map<String, List<EntityInfo>> entities = new HashMap<>();
    private final Map<String, SubRegion> subRegions = new HashMap<>();
    private final SchematicMetadata metadata = new SchematicMetadata();
    @Nullable private final File schematicFile;
    private long totalBlocksReadFromWorld;

    public LitematicaSchematic(@Nullable File file)
    {
        this.schematicFile = file;
    }

    @Nullable
    public File getFile()
    {
        return this.schematicFile;
    }

    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    public int getSubRegionCount()
    {
        return this.blockContainers.size();
    }

    public long getTotalBlocksReadFromWorld()
    {
        return this.totalBlocksReadFromWorld;
    }

    public void setTotalBlocksReadFromWorld(long count)
    {
        this.totalBlocksReadFromWorld = count;
    }

    /**
     * Clears all the data from this schematic
     */
    public void clear()
    {
        this.blockContainers.clear();
        this.blockEntities.clear();
        this.entities.clear();
        this.pendingBlockTicks.clear();
    }

    @Nullable
    public BlockPos getSubRegionPosition(String regionName)
    {
        SubRegion region = this.subRegions.get(regionName);
        return region != null ? region.pos : null;
    }

    @Nullable
    public Vec3i getSubRegionSize(String regionName)
    {
        SubRegion region = this.subRegions.get(regionName);
        return region != null ? region.size : null;
    }

    public Collection<String> getSubRegionNames()
    {
        return this.subRegions.keySet();
    }

    /**
     * Returns an immutable view of the sub-regions in this schematic
     * @return
     */
    public ImmutableMap<String, SubRegion> getSubRegions()
    {
        ImmutableMap.Builder<String, SubRegion> builder = ImmutableMap.builder();

        for (Map.Entry<String, SubRegion> entry : this.subRegions.entrySet())
        {
            builder.put(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    /**
     * Returns an immutable view of the sub-region boxes in this schematic
     * @return
     */
    public ImmutableMap<String, Box> getSubRegionBoxes()
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();

        for (Map.Entry<String, SubRegion> entry : this.subRegions.entrySet())
        {
            SubRegion region = entry.getValue();
            String name = entry.getKey();
            Vec3i posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(region.size);
            Box box = new Box(region.pos, region.pos.add(posEndRel), name);
            builder.put(name, box);
        }

        return builder.build();
    }

    /**
     * Sets the sub-region boxes for this schematic.
     * <b>Note:</b> This also clears any previous data, and this is meant to be
     * called before reading things from the world, when creating a schematic.
     * @param boxes
     * @param areaOrigin
     */
    public void setSubRegions(List<Box> boxes, BlockPos areaOrigin)
    {
        this.clear();

        for (Box box : boxes)
        {
            String regionName = box.getName();
            BlockPos pos = box.getPos1().subtract(areaOrigin);
            Vec3i size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());

            this.subRegions.put(regionName, new SubRegion(pos, size));

            try
            {
                this.blockContainers.put(regionName, new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ));
            }
            catch (Exception e)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "TODO - Failed to create the block state container for sub-region: " + regionName);
                LiteModLitematica.logger.warn("Failed to create the block state container for sub-region '{}'", regionName, e.getMessage());
            }

            this.blockEntities.put(regionName, new HashMap<>());
            this.entities.put(regionName, new ArrayList<>());
            this.pendingBlockTicks.put(regionName, new HashMap<>());
        }
    }

    @Nullable
    public LitematicaBlockStateContainer getSubRegionContainer(String regionName)
    {
        return this.blockContainers.get(regionName);
    }

    @Nullable
    public Map<BlockPos, NBTTagCompound> getBlockEntityMap(String regionName)
    {
        return this.blockEntities.get(regionName);
    }

    @Nullable
    public List<EntityInfo> getEntityList(String regionName)
    {
        return this.entities.get(regionName);
    }

    @Nullable
    public Map<BlockPos, NextTickListEntry> getBlockTickMap(String regionName)
    {
        return this.pendingBlockTicks.get(regionName);
    }

    private NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setInteger("Version", SCHEMATIC_VERSION);
        nbt.setInteger("MinecraftDataVersion", MINECRAFT_DATA_VERSION);
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
                Map<BlockPos, NBTTagCompound> tileMap = this.blockEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);
                Map<BlockPos, NextTickListEntry> pendingTicks = this.pendingBlockTicks.get(regionName);

                NBTTagCompound tag = new NBTTagCompound();

                tag.setTag("BlockStatePalette", blockContainer.getPalette().writeToNBT());
                tag.setTag("BlockStates", new NBTTagLongArray(blockContainer.getBackingLongArray()));

                if (tileMap != null)
                {
                    tag.setTag("TileEntities", this.writeTileEntitiesToNBT(tileMap));
                }

                if (pendingTicks != null)
                {
                    tag.setTag("PendingBlockTicks", this.writeBlockTicksToNBT(pendingTicks));
                }

                // The entity list will not exist, if takeEntities is false when creating the schematic
                if (entityList != null)
                {
                    tag.setTag("Entities", this.writeEntitiesToNBT(entityList));
                }

                SubRegion region = this.subRegions.get(regionName);
                tag.setTag("Position", NBTUtils.createBlockPosTag(region.pos));
                tag.setTag("Size", NBTUtils.createBlockPosTag(region.size));

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
                tagList.appendTag(info.nbt);
            }
        }

        return tagList;
    }

    private NBTTagList writeBlockTicksToNBT(Map<BlockPos, NextTickListEntry> tickMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tickMap.isEmpty() == false)
        {
            for (NextTickListEntry entry : tickMap.values())
            {
                ResourceLocation rl = Block.REGISTRY.getNameForObject(entry.getBlock());

                if (rl != null)
                {
                    NBTTagCompound tag = new NBTTagCompound();

                    tag.setString("Block", rl.toString());
                    tag.setInteger("Priority", entry.priority);
                    tag.setInteger("Time", (int) entry.scheduledTime);
                    tag.setInteger("x", entry.position.getX());
                    tag.setInteger("y", entry.position.getY());
                    tag.setInteger("z", entry.position.getZ());

                    tagList.appendTag(tag);
                }
            }
        }

        return tagList;
    }

    private NBTTagList writeTileEntitiesToNBT(Map<BlockPos, NBTTagCompound> tileMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tileMap.isEmpty() == false)
        {
            for (NBTTagCompound tag : tileMap.values())
            {
                tagList.appendTag(tag);
            }
        }

        return tagList;
    }

    private boolean readFromNBT(NBTTagCompound nbt)
    {
        this.blockContainers.clear();
        this.blockEntities.clear();
        this.entities.clear();
        this.pendingBlockTicks.clear();
        this.subRegions.clear();
        this.metadata.clearModifiedSinceSaved();

        if (nbt.hasKey("Version", Constants.NBT.TAG_INT))
        {
            final int version = nbt.getInteger("Version");

            if (version >= 1 && version <= SCHEMATIC_VERSION)
            {
                this.metadata.readFromNBT(nbt.getCompoundTag("Metadata"));
                this.readSubRegionsFromNBT(nbt.getCompoundTag("Regions"), version);

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

    private void readSubRegionsFromNBT(NBTTagCompound tag, int version)
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
                    this.subRegions.put(regionName, new SubRegion(regionPos, regionSize));

                    if (version >= 2)
                    {
                        this.blockEntities.put(regionName, this.readTileEntitiesFromNBT(regionTag.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND)));
                        this.entities.put(regionName, this.readEntitiesFromNBT(regionTag.getTagList("Entities", Constants.NBT.TAG_COMPOUND)));
                    }
                    else if (version == 1)
                    {
                        this.blockEntities.put(regionName, this.readTileEntitiesFromNBT_v1(regionTag.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND)));
                        this.entities.put(regionName, this.readEntitiesFromNBT_v1(regionTag.getTagList("Entities", Constants.NBT.TAG_COMPOUND)));
                    }

                    if (version >= 3)
                    {
                        this.pendingBlockTicks.put(regionName, this.readBlockTicksFromNBT(regionTag.getTagList("PendingBlockTicks", Constants.NBT.TAG_COMPOUND)));
                    }

                    NBTBase nbtBase = regionTag.getTag("BlockStates");

                    // There are no convenience methods in NBTTagCompound yet in 1.12, so we'll have to do it the ugly way...
                    if (nbtBase != null && nbtBase.getId() == Constants.NBT.TAG_LONG_ARRAY)
                    {
                        Vec3i size = new Vec3i(Math.abs(regionSize.getX()), Math.abs(regionSize.getY()), Math.abs(regionSize.getZ()));
                        NBTTagList palette = regionTag.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
                        long[] blockStateArr = ((IMixinNBTTagLongArray) nbtBase).getArray();

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
            NBTTagCompound entityData = tagList.getCompoundTagAt(i);
            Vec3d posVec = NBTUtils.readEntityPositionFromTag(entityData);

            if (posVec != null && entityData.isEmpty() == false)
            {
                entityList.add(new EntityInfo(posVec, entityData));
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
            BlockPos pos = NBTUtils.readBlockPos(tag);

            if (pos != null && tag.isEmpty() == false)
            {
                tileMap.put(pos, tag);
            }
        }

        return tileMap;
    }

    private Map<BlockPos, NextTickListEntry> readBlockTicksFromNBT(NBTTagList tagList)
    {
        Map<BlockPos, NextTickListEntry> tickMap = new HashMap<>();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);

            if (tag.hasKey("Block", Constants.NBT.TAG_STRING) &&
                tag.hasKey("Time", Constants.NBT.TAG_ANY_NUMERIC)) // XXX these were accidentally saved as longs in version 3
            {
                Block block = Block.REGISTRY.getObject(new ResourceLocation(tag.getString("Block")));

                if (block != null && block != Blocks.AIR)
                {
                    BlockPos pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
                    NextTickListEntry entry = new NextTickListEntry(pos, block);
                    entry.setPriority(tag.getInteger("Priority"));

                    // Note: the time is a relative delay at this point
                    entry.setScheduledTime(tag.getInteger("Time"));

                    tickMap.put(pos, entry);
                }
            }
        }

        return tickMap;
    }

    private List<EntityInfo> readEntitiesFromNBT_v1(NBTTagList tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            Vec3d posVec = NBTUtils.readVec3d(tag);
            NBTTagCompound entityData = tag.getCompoundTag("EntityData");

            if (posVec != null && entityData.isEmpty() == false)
            {
                // Update the correct position to the TileEntity NBT, where it is stored in version 2
                NBTUtils.writeEntityPositionToTag(posVec, entityData);
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    private Map<BlockPos, NBTTagCompound> readTileEntitiesFromNBT_v1(NBTTagList tagList)
    {
        Map<BlockPos, NBTTagCompound> tileMap = new HashMap<>();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            NBTTagCompound tileNbt = tag.getCompoundTag("TileNBT");

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
            CompressedStreamTools.writeCompressed(this.writeToNBT(), os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exception", fileSchematic.getAbsolutePath());
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, e.getMessage());
        }

        return false;
    }

    public boolean readFromFile()
    {
        if (this.schematicFile == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.no_file");
            return false;
        }

        File file = this.schematicFile;

        if (file.exists() == false || file.canRead() == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.cant_read", file.getAbsolutePath());
            return false;
        }

        try
        {
            FileInputStream is = new FileInputStream(file);
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
            is.close();

            return nbt != null && this.readFromNBT(nbt);
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.exception", file.getAbsolutePath());
        }

        return false;
    }

    @Nullable
    public static LitematicaSchematic createFromFile(File dir, String fileName)
    {
        if (fileName.endsWith(FILE_EXTENSION) == false)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        File file = new File(dir, fileName);
        LitematicaSchematic schematic = new LitematicaSchematic(file);

        return schematic.readFromFile() ? schematic : null;
    }

    public static class SubRegion
    {
        public final BlockPos pos;
        public final Vec3i size;

        public SubRegion(BlockPos pos, Vec3i size)
        {
            this.pos = pos;
            this.size = size;
        }
    }

    public static class EntityInfo
    {
        public final Vec3d posVec;
        public final NBTTagCompound nbt;

        public EntityInfo(Vec3d posVec, NBTTagCompound nbt)
        {
            this.posVec = posVec;
            this.nbt = nbt;
        }
    }
}
