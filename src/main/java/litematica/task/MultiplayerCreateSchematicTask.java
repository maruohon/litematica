package litematica.task;

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

import malilib.mixin.access.NBTTagLongArrayMixin;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.StringUtils;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.nbt.NbtUtils;
import litematica.network.SchematicSavePacketHandler;
import litematica.render.infohud.InfoHud;
import litematica.scheduler.tasks.TaskBase;
import litematica.schematic.EntityInfo;
import litematica.schematic.ISchematic;
import litematica.schematic.LitematicaSchematic;
import litematica.schematic.SchematicBase;
import litematica.schematic.SchematicMetadata;
import litematica.schematic.SchematicType;
import litematica.schematic.container.LitematicaBlockStateContainerFull;
import litematica.schematic.util.SchematicCreationUtils;
import litematica.selection.AreaSelection;
import litematica.selection.SelectionBox;
import litematica.util.PositionUtils;

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
        if (NbtWrap.containsCompound(tag, "Regions") == false ||
            NbtWrap.containsString(tag, "SaveMethod") == false)
        {
            MessageDispatcher.error("litematica.message.error.schematic_save.server_side.invalid_data");
            return;
        }

        NBTTagCompound regionsTag = NbtWrap.getCompound(tag, "Regions");
        String saveMethod = NbtWrap.getString(tag, "SaveMethod");

        if (saveMethod.equals("AllAtOnce"))
        {
            this.readRegions(regionsTag, this::readEntireRegion);
            this.onSchematicComplete();
        }
        else if (saveMethod.equals("PerChunk"))
        {
            this.readRegions(regionsTag, this::readPerChunkPieceOfRegion);

            if (NbtWrap.getBoolean(tag, "Finished"))
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
        for (String regionName : NbtWrap.getKeys(regionsTag))
        {
            if (NbtWrap.containsCompound(regionsTag, regionName))
            {
                NBTTagCompound regionTag = NbtWrap.getCompound(regionsTag, regionName);
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
        if (NbtWrap.containsCompound(regionTag, "Blocks"))
        {
            NBTTagCompound blocksTag = NbtWrap.getCompound(regionTag, "Blocks");

            if (NbtWrap.containsList(blocksTag, "BlockStatePalette") &&
                NbtWrap.containsLongArray(blocksTag, "BlockStates") &&
                NbtWrap.containsInt(blocksTag, "sizeX") &&
                NbtWrap.containsInt(blocksTag, "sizeY") &&
                NbtWrap.containsInt(blocksTag, "sizeZ"))
            {
                NBTBase nbtBase = NbtWrap.getTag(blocksTag, "BlockStates");
                Vec3i size = new Vec3i(NbtWrap.getInt(blocksTag, "sizeX"),
                                       NbtWrap.getInt(blocksTag, "sizeY"),
                                       NbtWrap.getInt(blocksTag, "sizeZ"));

                if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0)
                {
                    MessageDispatcher.error("litematica.message.error.schematic_save.server_side.invalid_region_size", size);
                    return;
                }

                NBTTagList paletteTag = NbtWrap.getListOfCompounds(blocksTag, "BlockStatePalette");
                long[] blockStateArr = ((NBTTagLongArrayMixin) nbtBase).getArray();
                int paletteSize = NbtWrap.getListSize(paletteTag);
                LitematicaBlockStateContainerFull container = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockStateArr, size);

                if (container != null)
                {
                    SchematicBase.readPaletteFromLitematicaFormatTag(paletteTag, container.getPalette());
                    consumer.accept(container);
                    long totalBlockCount;

                    if (NbtWrap.containsLong(blocksTag, "TotalBlockCount"))
                    {
                        totalBlockCount = NbtWrap.getLong(blocksTag, "TotalBlockCount");
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
        if (NbtWrap.containsList(regionTag, "BlockEntities"))
        {
            NBTTagList listTag = NbtWrap.getListOfCompounds(regionTag, "BlockEntities");
            HashMap<BlockPos, NBTTagCompound> map = new HashMap<>();
            final int size = NbtWrap.getListSize(listTag);

            for (int i = 0; i < size; ++i)
            {
                NBTTagCompound tag = NbtWrap.getCompoundAt(listTag, i);
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
        if (NbtWrap.containsList(regionTag, "Entities"))
        {
            NBTTagList listTag = NbtWrap.getListOfCompounds(regionTag, "Entities");
            ArrayList<EntityInfo> list = new ArrayList<>();
            final int size = NbtWrap.getListSize(listTag);

            for (int i = 0; i < size; ++i)
            {
                NBTTagCompound tag = NbtWrap.getCompoundAt(listTag, i);
                Vec3d pos = NbtUtils.readVec3dFromListTag(tag, "Pos");

                if (pos != null)
                {
                    NbtWrap.remove(tag, "Pos");
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
        if (NbtWrap.containsList(regionTag, tagName))
        {
            NBTTagList listTag = NbtWrap.getListOfCompounds(regionTag, tagName);
            HashMap<BlockPos, NextTickListEntry> map = new HashMap<>();
            final int size = NbtWrap.getListSize(listTag);

            for (int i = 0; i < size; ++i)
            {
                NBTTagCompound tag = NbtWrap.getCompoundAt(listTag, i);
                BlockPos pos = NbtUtils.readBlockPos(tag);
                Block block = RegistryUtils.getBlockByIdStr(NbtWrap.getString(tag, "Block"));

                if (pos != null && block != null)
                {
                    NbtUtils.removeBlockPosFromTag(tag);
                    NextTickListEntry entry = new NextTickListEntry(pos, block);
                    entry.setPriority(NbtWrap.getInt(tag, "Priority"));
                    entry.setScheduledTime(NbtWrap.getInt(tag, "Time"));
                    map.put(pos, entry);
                }
            }

            consumer.accept(map);
        }
    }
}
