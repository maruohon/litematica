package litematica.schematic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;

import malilib.mixin.access.NBTTagLongArrayMixin;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.Constants;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.nbt.NbtUtils;
import litematica.Litematica;
import litematica.schematic.container.ILitematicaBlockStateContainer;
import litematica.schematic.container.LitematicaBlockStateContainerFull;
import litematica.selection.SelectionBox;
import litematica.util.PositionUtils;

public class LitematicaSchematic extends SchematicBase
{
    public static final String FILE_NAME_EXTENSION = ".litematic";
    public static final int SCHEMATIC_VERSION = 4;

    private final Map<String, LitematicaBlockStateContainerFull> blockContainers = new HashMap<>();
    private final Map<String, Map<BlockPos, NBTTagCompound>> blockEntities = new HashMap<>();
    // TODO FIXME use a custom class for holding this data
    private final Map<String, Map<BlockPos, NextTickListEntry>> pendingBlockTicks = new HashMap<>();
    private final Map<String, List<EntityInfo>> entities = new HashMap<>();
    private final Map<String, SubRegion> subRegions = new HashMap<>();

    LitematicaSchematic(@Nullable Path file)
    {
        super(file);
    }

    @Override
    public SchematicType<?> getType()
    {
        return SchematicType.LITEMATICA;
    }

    @Override
    public int getSubRegionCount()
    {
        return this.subRegions.size();
    }

    @Override
    public void clear()
    {
        this.subRegions.clear();
        this.blockContainers.clear();
        this.blockEntities.clear();
        this.entities.clear();
        this.pendingBlockTicks.clear();
        this.getMetadata().clearModifiedSinceSaved();
    }

    @Override
    public ImmutableList<String> getRegionNames()
    {
        return ImmutableList.copyOf(this.subRegions.keySet());
    }

    @Override
    @Nullable
    public LitematicaSubRegion getSchematicRegion(String regionName)
    {
        return this.subRegions.containsKey(regionName) ? new LitematicaSubRegion(this, regionName) : null;
    }

    /**
     * Returns an immutable view of the sub-regions in this schematic
     * @return
     */
    @Override
    public ImmutableMap<String, ISchematicRegion> getRegions()
    {
        ImmutableMap.Builder<String, ISchematicRegion> builder = ImmutableMap.builder();

        for (String regionName : this.subRegions.keySet())
        {
            builder.put(regionName, new LitematicaSubRegion(this, regionName));
        }

        return builder.build();
    }

    @Override
    public Vec3i getEnclosingSize()
    {
        return PositionUtils.getEnclosingAreaSize(PositionUtils.getEnclosingBoxAroundRegions(this.getRegions().values()));
    }

    /**
     * Sets the sub-regions and creates the empty HashMaps and Lists,
     * but does not create the block state containers.
     */
    public void setSubRegions(List<SelectionBox> boxes, BlockPos areaOrigin)
    {
        this.clear();

        for (SelectionBox box : boxes)
        {
            String regionName = box.getName();
            BlockPos pos = box.getCorner1().subtract(areaOrigin);
            Vec3i size = box.getSize();

            this.subRegions.put(regionName, new SubRegion(pos, size));
            this.blockEntities.put(regionName, new HashMap<>());
            this.entities.put(regionName, new ArrayList<>());
            this.pendingBlockTicks.put(regionName, new HashMap<>());
        }
    }

    /**
     * Sets the sub-region boxes for this schematic.
     * <b>Note:</b> This also clears any previous data, and this is meant to be
     * called before reading things from the world, when creating a schematic.
     * @param boxes the sub-region boxes, using absolute world coordinates
     * @param areaOrigin the area selection origin point
     */
    public void setAndInitializeSubRegions(List<SelectionBox> boxes, BlockPos areaOrigin)
    {
        this.clear();

        for (SelectionBox box : boxes)
        {
            String regionName = box.getName();
            BlockPos pos = box.getCorner1().subtract(areaOrigin);
            Vec3i size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());

            this.subRegions.put(regionName, new SubRegion(pos, size));

            try
            {
                this.blockContainers.put(regionName, new LitematicaBlockStateContainerFull(new Vec3i(sizeX, sizeY, sizeZ)));
            }
            catch (Exception e)
            {
                MessageDispatcher.error().translate("TODO - Failed to create the block state container for sub-region: " + regionName);
                Litematica.LOGGER.warn("Failed to create the block state container for sub-region '{}'", regionName, e.getMessage());
            }

            this.blockEntities.put(regionName, new HashMap<>());
            this.entities.put(regionName, new ArrayList<>());
            this.pendingBlockTicks.put(regionName, new HashMap<>());
        }
    }

    @Override
    public void readFrom(ISchematic other)
    {
        this.clear();

        ImmutableMap<String, ISchematicRegion> regions = other.getRegions();

        if (regions.isEmpty() == false)
        {
            for (Map.Entry<String, ISchematicRegion> regionEntry : regions.entrySet())
            {
                String regionName = regionEntry.getKey();
                ISchematicRegion region = regionEntry.getValue();
                ILitematicaBlockStateContainer containerOther = region.getBlockStateContainer();

                this.subRegions.put(regionName, new SubRegion(region.getPosition(), region.getSize()));

                if (containerOther instanceof LitematicaBlockStateContainerFull)
                {
                    this.blockContainers.put(regionName, (LitematicaBlockStateContainerFull) containerOther.copy());
                }
                else
                {
                    Vec3i size = containerOther.getSize();
                    LitematicaBlockStateContainerFull container = new LitematicaBlockStateContainerFull(size, false);
                    this.copyContainerContents(containerOther, container);
                    this.blockContainers.put(regionName, container);
                }

                Map<BlockPos, NBTTagCompound> blockEntityMap = new HashMap<>();
                Map<BlockPos, NextTickListEntry> blockTickMap = new HashMap<>(region.getBlockTickMap());
                List<EntityInfo> entities = new ArrayList<>();

                region.getBlockEntityMap().forEach((key, value) -> blockEntityMap.put(key, NbtWrap.copy(value)));
                region.getEntityList().forEach((info) -> entities.add(info.copy()));

                this.blockEntities.put(regionName, blockEntityMap);
                this.pendingBlockTicks.put(regionName, blockTickMap);
                this.entities.put(regionName, entities);
            }

            this.getMetadata().copyFrom(other.getMetadata());
        }
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        NbtWrap.putInt(nbt, "Version", SCHEMATIC_VERSION);
        NbtWrap.putInt(nbt, "MinecraftDataVersion", MINECRAFT_DATA_VERSION);
        NbtWrap.putTag(nbt, "Metadata", this.getMetadata().toTag());
        NbtWrap.putTag(nbt, "Regions", this.writeSubRegionsToNBT());

        return nbt;
    }

    @Override
    public boolean fromTag(NBTTagCompound tag)
    {
        this.clear();

        if (NbtWrap.containsInt(tag, "Version"))
        {
            final int version = NbtWrap.getInt(tag, "Version");

            if (version >= 1 && version <= SCHEMATIC_VERSION)
            {
                this.readMetadataFromTag(tag);
                this.readSubRegionsFromTag(tag, version);

                return true;
            }
            else
            {
                MessageDispatcher.error().translate("litematica.error.schematic_load.unsupported_schematic_version", version);
            }
        }
        else
        {
            MessageDispatcher.error().translate("litematica.error.schematic_load.no_schematic_version_information");
        }

        return false;
    }

    private NBTTagCompound writeSubRegionsToNBT()
    {
        NBTTagCompound wrapper = new NBTTagCompound();

        if (this.blockContainers.isEmpty() == false)
        {
            for (String regionName : this.blockContainers.keySet())
            {
                LitematicaBlockStateContainerFull blockContainer = this.blockContainers.get(regionName);
                Map<BlockPos, NBTTagCompound> tileMap = this.blockEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);
                Map<BlockPos, NextTickListEntry> pendingTicks = this.pendingBlockTicks.get(regionName);

                NBTTagCompound tag = new NBTTagCompound();

                NbtWrap.putTag(tag, "BlockStatePalette", this.writePaletteToLitematicaFormatTag(blockContainer.getPalette()));
                NbtWrap.putTag(tag, "BlockStates", new NBTTagLongArray(blockContainer.getBackingLongArray()));

                if (tileMap != null)
                {
                    NbtWrap.putTag(tag, "TileEntities", this.writeBlockEntitiesToListTag(tileMap));
                }

                if (pendingTicks != null)
                {
                    NbtWrap.putTag(tag, "PendingBlockTicks", this.writeBlockTicksToNBT(pendingTicks));
                }

                // The entity list will not exist, if takeEntities is false when creating the schematic
                if (entityList != null)
                {
                    NbtWrap.putTag(tag, "Entities", this.writeEntitiesToListTag(entityList));
                }

                SubRegion region = this.subRegions.get(regionName);
                NbtWrap.putTag(tag, "Position", NbtUtils.createBlockPosTag(region.pos));
                NbtWrap.putTag(tag, "Size", NbtUtils.createBlockPosTag(region.size));

                NbtWrap.putTag(wrapper, regionName, tag);
            }
        }

        return wrapper;
    }

    private NBTTagList writeBlockTicksToNBT(Map<BlockPos, NextTickListEntry> tickMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tickMap.isEmpty() == false)
        {
            for (NextTickListEntry entry : tickMap.values())
            {
                String id = RegistryUtils.getBlockIdStr(entry.getBlock());

                if (id != null)
                {
                    NBTTagCompound tag = new NBTTagCompound();

                    NbtWrap.putString(tag, "Block", id);
                    NbtWrap.putInt(tag, "Priority", entry.priority);
                    NbtWrap.putInt(tag, "Time", (int) entry.scheduledTime);
                    NbtUtils.putVec3i(tag, entry.position);

                    NbtWrap.addTag(tagList, tag);
                }
            }
        }

        return tagList;
    }

    private boolean readSubRegionsFromTag(NBTTagCompound tag, int version)
    {
        tag = NbtWrap.getCompound(tag, "Regions");

        for (String regionName : NbtWrap.getKeys(tag))
        {
            if (NbtWrap.getTypeId(NbtWrap.getTag(tag, regionName)) == Constants.NBT.TAG_COMPOUND)
            {
                NBTTagCompound regionTag = NbtWrap.getCompound(tag, regionName);
                BlockPos regionPos = NbtUtils.readBlockPos(NbtWrap.getCompound(regionTag, "Position"));
                BlockPos regionSize = NbtUtils.readBlockPos(NbtWrap.getCompound(regionTag, "Size"));

                if (regionPos != null && regionSize != null)
                {
                    this.subRegions.put(regionName, new SubRegion(regionPos, regionSize));

                    NBTTagList beList = NbtWrap.getListOfCompounds(regionTag, "TileEntities");
                    NBTTagList entityList = NbtWrap.getListOfCompounds(regionTag, "Entities");

                    if (version >= 2)
                    {
                        this.blockEntities.put(regionName, this.readBlockEntitiesFromListTag(beList));
                        this.entities.put(regionName, this.readEntitiesFromListTag(entityList));
                    }
                    else if (version == 1)
                    {
                        this.blockEntities.put(regionName, this.readTileEntitiesFromNBT_v1(beList));
                        this.entities.put(regionName, this.readEntitiesFromNBT_v1(entityList));
                    }

                    if (version >= 3)
                    {
                        this.pendingBlockTicks.put(regionName, this.readBlockTicksFromNBT(NbtWrap.getListOfCompounds(regionTag, "PendingBlockTicks")));
                    }

                    NBTBase nbtBase = NbtWrap.getTag(regionTag, "BlockStates");

                    // There are no convenience methods in NBTTagCompound yet in 1.12, so we'll have to do it the ugly way...
                    if (nbtBase != null && NbtWrap.getTypeId(nbtBase) == Constants.NBT.TAG_LONG_ARRAY)
                    {
                        Vec3i size = new Vec3i(Math.abs(regionSize.getX()), Math.abs(regionSize.getY()), Math.abs(regionSize.getZ()));
                        NBTTagList paletteTag = NbtWrap.getListOfCompounds(regionTag, "BlockStatePalette");
                        long[] blockStateArr = ((NBTTagLongArrayMixin) nbtBase).getArray();
                        int paletteSize = NbtWrap.getListSize(paletteTag);

                        LitematicaBlockStateContainerFull container = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockStateArr, size);

                        if (container == null)
                        {
                            String fileName = this.getFile() != null ? this.getFile().getFileName().toString() : "<null>";
                            MessageDispatcher.error().translate("litematica.error.schematic_read_from_file_failed.region_container",
                                                                regionName, fileName);
                            return false;
                        }

                        readPaletteFromLitematicaFormatTag(paletteTag, container.getPalette());
                        this.blockContainers.put(regionName, container);
                    }
                    else
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private Map<BlockPos, NextTickListEntry> readBlockTicksFromNBT(NBTTagList tagList)
    {
        Map<BlockPos, NextTickListEntry> tickMap = new HashMap<>();
        final int size = NbtWrap.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = NbtWrap.getCompoundAt(tagList, i);

            if (NbtWrap.containsString(tag, "Block") &&
                NbtWrap.contains(tag, "Time", Constants.NBT.TAG_ANY_NUMERIC)) // XXX these were accidentally saved as longs in version 3
            {
                Block block = RegistryUtils.getBlockByIdStr(NbtWrap.getString(tag, "Block"));

                if (block != null && block != Blocks.AIR)
                {
                    BlockPos pos = NbtUtils.readBlockPos(tag);
                    NextTickListEntry entry = new NextTickListEntry(pos, block);
                    entry.setPriority(NbtWrap.getInt(tag, "Priority"));

                    // Note: the time is a relative delay at this point
                    entry.setScheduledTime(NbtWrap.getInt(tag, "Time"));

                    tickMap.put(pos, entry);
                }
            }
        }

        return tickMap;
    }

    private List<EntityInfo> readEntitiesFromNBT_v1(NBTTagList tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = NbtWrap.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = NbtWrap.getCompoundAt(tagList, i);
            Vec3d posVec = NbtUtils.readVec3d(tag);
            NBTTagCompound entityData = NbtWrap.getCompound(tag, "EntityData");

            if (posVec != null && entityData.isEmpty() == false)
            {
                // Update the correct position to the Entity NBT, where it is stored in version 2
                NbtUtils.writeVec3dToListTag(posVec, entityData);
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    private Map<BlockPos, NBTTagCompound> readTileEntitiesFromNBT_v1(NBTTagList tagList)
    {
        Map<BlockPos, NBTTagCompound> tileMap = new HashMap<>();
        final int size = NbtWrap.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = NbtWrap.getCompoundAt(tagList, i);
            NBTTagCompound tileNbt = NbtWrap.getCompound(tag, "TileNBT");

            // Note: This within-schematic relative position is not inside the tile tag!
            BlockPos pos = NbtUtils.readBlockPos(tag);

            if (pos != null && tileNbt.isEmpty() == false)
            {
                tileMap.put(pos, tileNbt);
            }
        }

        return tileMap;
    }

    public static Boolean isValidSchematic(NBTTagCompound tag)
    {
        return NbtWrap.containsInt(tag, "Version") &&
               NbtWrap.containsCompound(tag, "Regions") &&
               NbtWrap.containsCompound(tag, "Metadata");
    }

    @Nullable
    public static LitematicaSchematic createFromFile(Path dir, String fileName)
    {
        if (fileName.endsWith(FILE_NAME_EXTENSION) == false)
        {
            fileName = fileName + FILE_NAME_EXTENSION;
        }

        Path file = dir.resolve(fileName);
        LitematicaSchematic schematic = new LitematicaSchematic(file);

        return schematic.readFromFile() ? schematic : null;
    }

    public static class LitematicaSubRegion implements ISchematicRegion
    {
        private final LitematicaSchematic schematic;
        private final String regionName;

        public LitematicaSubRegion(LitematicaSchematic schematic, String regionName)
        {
            this.schematic = schematic;
            this.regionName = regionName;
        }

        @Override
        public BlockPos getPosition()
        {
            return this.schematic.subRegions.get(this.regionName).pos;
        }

        @Override
        public Vec3i getSize()
        {
            return this.schematic.subRegions.get(this.regionName).size;
        }

        @Override
        public ILitematicaBlockStateContainer getBlockStateContainer()
        {
            return this.schematic.blockContainers.get(this.regionName);
        }

        @Override
        public Map<BlockPos, NBTTagCompound> getBlockEntityMap()
        {
            return this.schematic.blockEntities.computeIfAbsent(this.regionName, name -> new HashMap<>());
        }

        @Override
        public List<EntityInfo> getEntityList()
        {
            return this.schematic.entities.computeIfAbsent(this.regionName, name -> new ArrayList<>());
        }

        @Override
        public Map<BlockPos, NextTickListEntry> getBlockTickMap()
        {
            return this.schematic.pendingBlockTicks.computeIfAbsent(this.regionName, name -> new HashMap<>());
        }

        // TODO FIXME clean this up by moving these to a MutableSchematicRegion interface and class

        public void setBlockStateContainer(LitematicaBlockStateContainerFull container)
        {
            Vec3i containerSize = container.getSize();
            Vec3i regionSize = this.getSize();

            if (Math.abs(containerSize.getX()) == Math.abs(regionSize.getX()) &&
                Math.abs(containerSize.getY()) == Math.abs(regionSize.getY()) &&
                Math.abs(containerSize.getZ()) == Math.abs(regionSize.getZ()))
            {
                this.schematic.blockContainers.put(this.regionName, container);
            }
            else
            {
                // FIXME/TODO
                MessageDispatcher.error("Invalid container size %s, expected %s", containerSize, regionSize);
            }
        }

        public void setBlockEntityMap(Map<BlockPos, NBTTagCompound> map)
        {
            this.schematic.blockEntities.put(this.regionName, map);
        }

        public void setEntityList(List<EntityInfo> list)
        {
            this.schematic.entities.put(this.regionName, list);
        }

        public void setBlockTickMap(Map<BlockPos, NextTickListEntry> map)
        {
            this.schematic.pendingBlockTicks.put(this.regionName, map);
        }
    }
}
