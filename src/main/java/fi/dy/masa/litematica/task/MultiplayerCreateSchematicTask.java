package fi.dy.masa.litematica.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import fi.dy.masa.malilib.mixin.IMixinNBTTagLongArray;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.network.SchematicSavePacketHandler;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.scheduler.tasks.TaskBase;
import fi.dy.masa.litematica.schematic.EntityInfo;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicBase;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.SchematicType;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.util.SchematicCreationUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils;

public class MultiplayerCreateSchematicTask extends TaskBase
{
    protected final UUID taskId;
    protected final Consumer<ISchematic> schematicConsumer;
    protected final LitematicaSchematic schematic;
    protected final String selectionName;

    public MultiplayerCreateSchematicTask(AreaSelection selection, UUID taskId, Consumer<ISchematic> schematicConsumer)
    {
        this.taskId = taskId;
        this.schematicConsumer = schematicConsumer;
        this.schematic = SchematicType.LITEMATICA.createSchematic(null);
        this.selectionName = selection.getName();

        List<SelectionBox> boxes = selection.getAllSubRegionBoxes();
        this.schematic.setSubRegions(boxes, selection.getEffectiveOrigin());

        SchematicMetadata metadata = this.schematic.getMetadata();
        metadata.setRegionCount(boxes.size());
        metadata.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        metadata.setEnclosingSize(PositionUtils.getEnclosingAreaSize(boxes));

        this.name = StringUtils.translate("litematica.hud.task_name.save_schematic.server_side");
        this.infoHudLines.add(StringUtils.translate("litematica.hud.save_schematic.server_side.waiting"));

        InfoHud.getInstance().addInfoHudRenderer(this, true);
    }

    @Override
    public boolean execute()
    {
        return this.finished;
    }

    @Override
    public void stop()
    {
        if (this.finished == false)
        {
            MessageDispatcher.warning("litematica.message.error.schematic_save_interrupted");
        }

        SchematicSavePacketHandler.INSTANCE.removeSaveTask(this.taskId);

        super.stop();
    }

    public void onReceiveData(NBTTagCompound tag)
    {
        if (NbtUtils.hasCompound(tag, "Regions") == false ||
            NbtUtils.hasString(tag, "SaveMethod") == false)
        {
            MessageDispatcher.error("litematica.message.error.schematic_save.server_side.invalid_data");
            return;
        }

        NBTTagCompound regionsTag = NbtUtils.getCompound(tag, "Regions");
        String saveMethod = NbtUtils.getString(tag, "SaveMethod");

        if (saveMethod.equals("AllAtOnce"))
        {
            this.readRegions(regionsTag, this::readEntireRegion);
            this.onSchematicComplete();
        }
        else if (saveMethod.equals("PerChunk"))
        {
            this.readRegions(regionsTag, this::readPerChunkPieceOfRegion);

            if (NbtUtils.getBoolean(tag, "Finished"))
            {
                this.onSchematicComplete();
            }
        }
    }

    protected void onSchematicComplete()
    {
        SchematicCreationUtils.setSchematicMetadataOnCreation(this.schematic, this.selectionName);
        this.schematicConsumer.accept(this.schematic);
        this.finished = true;
    }

    protected void readRegions(NBTTagCompound regionsTag, BiConsumer<String, NBTTagCompound> regionReader)
    {
        for (String regionName : NbtUtils.getKeys(regionsTag))
        {
            if (NbtUtils.hasCompound(regionsTag, regionName))
            {
                NBTTagCompound regionTag = NbtUtils.getCompound(regionsTag, regionName);
                regionReader.accept(regionName, regionTag);
            }
        }
    }

    protected void readEntireRegion(String regionName, NBTTagCompound regionTag)
    {
        LitematicaSchematic.LitematicaSubRegion region = this.schematic.getSchematicRegion(regionName);

        if (region != null)
        {
            this.readBlocks(regionTag, region::setBlockStateContainer);
            this.readBlockEntities(regionTag, region::setBlockEntityMap);
            this.readEntities(regionTag, region::setEntityList);
            this.readScheduledTicks(regionTag, "BlockTicks", region::setBlockTickMap);
        }
    }

    protected void readPerChunkPieceOfRegion(String regionName, NBTTagCompound regionTag)
    {
        LitematicaSchematic.LitematicaSubRegion region = this.schematic.getSchematicRegion(regionName);

        if (region != null)
        {
            // TODO blocks
            //this.readBlocks(regionTag, region::setBlockStateContainer);
            this.readBlockEntities(regionTag, map -> region.getBlockEntityMap().putAll(map));
            this.readEntities(regionTag, list -> region.getEntityList().addAll(list));
            this.readScheduledTicks(regionTag, "BlockTicks", map -> region.getBlockTickMap().putAll(map));
        }
    }

    protected void readBlocks(NBTTagCompound regionTag, Consumer<LitematicaBlockStateContainerFull> consumer)
    {
        if (NbtUtils.hasCompound(regionTag, "Blocks"))
        {
            NBTTagCompound blocksTag = NbtUtils.getCompound(regionTag, "Blocks");

            if (NbtUtils.hasList(blocksTag, "BlockStatePalette") &&
                NbtUtils.hasLongArray(blocksTag, "BlockStates") &&
                NbtUtils.hasInt(blocksTag, "sizeX") &&
                NbtUtils.hasInt(blocksTag, "sizeY") &&
                NbtUtils.hasInt(blocksTag, "sizeZ"))
            {
                NBTBase nbtBase = blocksTag.getTag("BlockStates");
                Vec3i size = new Vec3i(NbtUtils.getInt(blocksTag, "sizeX"),
                                       NbtUtils.getInt(blocksTag, "sizeY"),
                                       NbtUtils.getInt(blocksTag, "sizeZ"));

                if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0)
                {
                    MessageDispatcher.error("litematica.message.error.schematic_save.server_side.invalid_region_size", size);
                    return;
                }

                NBTTagList paletteTag = NbtUtils.getListOfCompounds(blocksTag, "BlockStatePalette");
                long[] blockStateArr = ((IMixinNBTTagLongArray) nbtBase).getArray();
                int paletteSize = NbtUtils.getListSize(paletteTag);
                LitematicaBlockStateContainerFull container = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockStateArr, size);

                if (container != null)
                {
                    SchematicBase.readPaletteFromLitematicaFormatTag(paletteTag, container.getPalette());
                    consumer.accept(container);
                    long totalBlockCount;

                    if (NbtUtils.hasLong(blocksTag, "TotalBlockCount"))
                    {
                        totalBlockCount = NbtUtils.getLong(blocksTag, "TotalBlockCount");
                    }
                    else
                    {
                        totalBlockCount = container.getTotalBlockCount();
                    }

                    this.schematic.getMetadata().setTotalBlocks(totalBlockCount);

                    return;
                }
                else
                {
                    MessageDispatcher.error("litematica.message.error.schematic_save.server_side.failed_to_create_block_container");
                }
            }
        }

        MessageDispatcher.error("litematica.message.error.schematic_save.server_side.missing_tags_in_block_data");
    }

    protected void readBlockEntities(NBTTagCompound regionTag, Consumer<HashMap<BlockPos, NBTTagCompound>> consumer)
    {
        if (NbtUtils.hasList(regionTag, "BlockEntities"))
        {
            NBTTagList listTag = NbtUtils.getListOfCompounds(regionTag, "BlockEntities");
            HashMap<BlockPos, NBTTagCompound> map = new HashMap<>();
            final int size = NbtUtils.getListSize(listTag);

            for (int i = 0; i < size; ++i)
            {
                NBTTagCompound tag = listTag.getCompoundTagAt(i);
                BlockPos pos = NbtUtils.readBlockPos(tag);

                if (pos != null)
                {
                    NbtUtils.removeBlockPosFromTag(tag);
                    map.put(pos, tag);
                }
            }

            consumer.accept(map);
        }
    }

    protected void readEntities(NBTTagCompound regionTag, Consumer<List<EntityInfo>> consumer)
    {
        if (NbtUtils.hasList(regionTag, "Entities"))
        {
            NBTTagList listTag = NbtUtils.getListOfCompounds(regionTag, "Entities");
            ArrayList<EntityInfo> list = new ArrayList<>();
            final int size = NbtUtils.getListSize(listTag);

            for (int i = 0; i < size; ++i)
            {
                NBTTagCompound tag = listTag.getCompoundTagAt(i);
                Vec3d pos = NbtUtils.readVec3dFromListTag(tag, "Pos");

                if (pos != null)
                {
                    NbtUtils.remove(tag, "Pos");
                    list.add(new EntityInfo(pos, tag));
                }
            }

            consumer.accept(list);
        }
    }

    protected <T> void readScheduledTicks(NBTTagCompound regionTag,
                                          String tagName,
                                          //Function<String, T> objectFactory, // TODO
                                          Consumer<HashMap<BlockPos, NextTickListEntry>> consumer)
    {
        if (NbtUtils.hasList(regionTag, tagName))
        {
            NBTTagList listTag = NbtUtils.getListOfCompounds(regionTag, tagName);
            HashMap<BlockPos, NextTickListEntry> map = new HashMap<>();
            final int size = NbtUtils.getListSize(listTag);

            for (int i = 0; i < size; ++i)
            {
                NBTTagCompound tag = listTag.getCompoundTagAt(i);
                BlockPos pos = NbtUtils.readBlockPos(tag);
                Block block = BlockUtils.getBlockByRegistryName(NbtUtils.getString(tag, "Block"));

                if (pos != null && block != null)
                {
                    NbtUtils.removeBlockPosFromTag(tag);
                    NextTickListEntry entry = new NextTickListEntry(pos, block);
                    entry.setPriority(NbtUtils.getInt(tag, "Priority"));
                    entry.setScheduledTime(NbtUtils.getInt(tag, "Time"));
                    map.put(pos, entry);
                }
            }

            consumer.accept(map);
        }
    }
}
