package fi.dy.masa.litematica.schematic;

import java.io.File;
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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.message.MessageType;
import fi.dy.masa.malilib.mixin.IMixinNBTTagLongArray;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.message.MessageUtils;
import fi.dy.masa.malilib.util.nbt.NbtUtils;

public class LitematicaSchematic extends SchematicBase
{
    public static final String FILE_NAME_EXTENSION = ".litematic";
    public static final int SCHEMATIC_VERSION = 4;

    private final Map<String, LitematicaBlockStateContainerFull> blockContainers = new HashMap<>();
    private final Map<String, Map<BlockPos, NBTTagCompound>> blockEntities = new HashMap<>();
    private final Map<String, Map<BlockPos, NextTickListEntry>> pendingBlockTicks = new HashMap<>();
    private final Map<String, List<EntityInfo>> entities = new HashMap<>();
    private final Map<String, SubRegion> subRegions = new HashMap<>();

    LitematicaSchematic(@Nullable File file)
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
        return this.blockContainers.size();
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
    public ISchematicRegion getSchematicRegion(String regionName)
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
        ImmutableMap<String, ISchematicRegion> regions = this.getRegions();

        if (regions.isEmpty() == false)
        {
            if (regions.size() == 1)
            {
                for (ISchematicRegion region : regions.values())
                {
                    return PositionUtils.getAbsoluteAreaSize(region.getSize());
                }
            }
            else
            {
                List<Box> boxes = new ArrayList<>();

                for (ISchematicRegion region : regions.values())
                {
                    BlockPos pos = region.getPosition();
                    Vec3i end = PositionUtils.getRelativeEndPositionFromAreaSize(region.getSize());
                    Box box = new Box(pos, pos.add(end));
                    boxes.add(box);
                }

                return PositionUtils.getEnclosingAreaSize(boxes);
            }
        }

        return null;
    }

    /**
     * Sets the sub-region boxes for this schematic.
     * <b>Note:</b> This also clears any previous data, and this is meant to be
     * called before reading things from the world, when creating a schematic.
     * @param boxes the sub-region boxes, using absolute world coordinates
     * @param areaOrigin the area selection origin point
     */
    public void setSubRegions(List<SelectionBox> boxes, BlockPos areaOrigin)
    {
        this.clear();

        for (SelectionBox box : boxes)
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
                this.blockContainers.put(regionName, new LitematicaBlockStateContainerFull(new Vec3i(sizeX, sizeY, sizeZ)));
            }
            catch (Exception e)
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "TODO - Failed to create the block state container for sub-region: " + regionName);
                Litematica.logger.warn("Failed to create the block state container for sub-region '{}'", regionName, e.getMessage());
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
                Map<BlockPos, NextTickListEntry> blockTickMap = new HashMap<>();
                List<EntityInfo> entities = new ArrayList<>();

                region.getBlockEntityMap().entrySet().forEach((entry) -> blockEntityMap.put(entry.getKey(), entry.getValue().copy()));
                region.getBlockTickMap().entrySet().forEach((entry) -> blockTickMap.put(entry.getKey(), entry.getValue()));
                region.getEntityList().forEach((info) -> entities.add(info.copy()));
                blockTickMap.putAll(region.getBlockTickMap());

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

        nbt.setInteger("Version", SCHEMATIC_VERSION);
        nbt.setInteger("MinecraftDataVersion", MINECRAFT_DATA_VERSION);
        nbt.setTag("Metadata", this.getMetadata().toTag());
        nbt.setTag("Regions", this.writeSubRegionsToNBT());

        return nbt;
    }

    @Override
    public boolean fromTag(NBTTagCompound tag)
    {
        this.clear();

        if (tag.hasKey("Version", Constants.NBT.TAG_INT))
        {
            final int version = tag.getInteger("Version");

            if (version >= 1 && version <= SCHEMATIC_VERSION)
            {
                this.readMetadataFromTag(tag);
                this.readSubRegionsFromTag(tag, version);

                return true;
            }
            else
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_schematic_version", version);
            }
        }
        else
        {
            MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_version_information");
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
                LitematicaBlockStateContainerFull blockContainer = (LitematicaBlockStateContainerFull) this.blockContainers.get(regionName);
                Map<BlockPos, NBTTagCompound> tileMap = this.blockEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);
                Map<BlockPos, NextTickListEntry> pendingTicks = this.pendingBlockTicks.get(regionName);

                NBTTagCompound tag = new NBTTagCompound();

                tag.setTag("BlockStatePalette", this.writePaletteToLitematicaFormatTag(blockContainer.getPalette()));
                tag.setTag("BlockStates", new NBTTagLongArray(blockContainer.getBackingLongArray()));

                if (tileMap != null)
                {
                    tag.setTag("TileEntities", this.writeBlockEntitiesToListTag(tileMap));
                }

                if (pendingTicks != null)
                {
                    tag.setTag("PendingBlockTicks", this.writeBlockTicksToNBT(pendingTicks));
                }

                // The entity list will not exist, if takeEntities is false when creating the schematic
                if (entityList != null)
                {
                    tag.setTag("Entities", this.writeEntitiesToListTag(entityList));
                }

                SubRegion region = this.subRegions.get(regionName);
                tag.setTag("Position", NbtUtils.createBlockPosTag(region.pos));
                tag.setTag("Size", NbtUtils.createBlockPosTag(region.size));

                wrapper.setTag(regionName, tag);
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

    private boolean readSubRegionsFromTag(NBTTagCompound tag, int version)
    {
        tag = tag.getCompoundTag("Regions");

        for (String regionName : tag.getKeySet())
        {
            if (tag.getTag(regionName).getId() == Constants.NBT.TAG_COMPOUND)
            {
                NBTTagCompound regionTag = tag.getCompoundTag(regionName);
                BlockPos regionPos = NbtUtils.readBlockPos(regionTag.getCompoundTag("Position"));
                BlockPos regionSize = NbtUtils.readBlockPos(regionTag.getCompoundTag("Size"));

                if (regionPos != null && regionSize != null)
                {
                    this.subRegions.put(regionName, new SubRegion(regionPos, regionSize));

                    if (version >= 2)
                    {
                        this.blockEntities.put(regionName, this.readBlockEntitiesFromListTag(regionTag.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND)));
                        this.entities.put(regionName, this.readEntitiesFromListTag(regionTag.getTagList("Entities", Constants.NBT.TAG_COMPOUND)));
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
                        NBTTagList paletteTag = regionTag.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
                        long[] blockStateArr = ((IMixinNBTTagLongArray) nbtBase).getArray();
                        int paletteSize = paletteTag.tagCount();

                        LitematicaBlockStateContainerFull container = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockStateArr, size);

                        if (container == null)
                        {
                            MessageUtils.printErrorMessage("litematica.error.schematic_read_from_file_failed.region_container",
                                                           regionName, this.getFile() != null ? this.getFile().getName() : "<null>");
                            return false;
                        }

                        this.readPaletteFromLitematicaFormatTag(paletteTag, container.getPalette());
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
            Vec3d posVec = NbtUtils.readVec3d(tag);
            NBTTagCompound entityData = tag.getCompoundTag("EntityData");

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
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            NBTTagCompound tileNbt = tag.getCompoundTag("TileNBT");

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
        if (tag.hasKey("Version", Constants.NBT.TAG_INT) &&
            tag.hasKey("Regions", Constants.NBT.TAG_COMPOUND) &&
            tag.hasKey("Metadata", Constants.NBT.TAG_COMPOUND))
        {
            return true;
        }

        return false;
    }

    @Nullable
    public static LitematicaSchematic createFromFile(File dir, String fileName)
    {
        if (fileName.endsWith(FILE_NAME_EXTENSION) == false)
        {
            fileName = fileName + FILE_NAME_EXTENSION;
        }

        File file = new File(dir, fileName);
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
            return this.schematic.blockEntities.computeIfAbsent(this.regionName, (name) -> { return new HashMap<>(); });
        }

        @Override
        public List<EntityInfo> getEntityList()
        {
            return this.schematic.entities.computeIfAbsent(this.regionName, (name) -> { return new ArrayList<>(); });
        }

        @Override
        public Map<BlockPos, NextTickListEntry> getBlockTickMap()
        {
            return this.schematic.pendingBlockTicks.computeIfAbsent(this.regionName, (name) -> { return new HashMap<>(); });
        }
    }
}
