package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;

public class SpongeSchematic extends SingleRegionSchematic
{
    public static final String FILE_NAME_EXTENSION = ".schem";

    protected int version = 1;

    SpongeSchematic(File file)
    {
        super(file);
    }

    @Override
    public SchematicType<?> getType()
    {
        return SchematicType.SPONGE;
    }

    public static boolean isValidSchematic(NBTTagCompound tag)
    {
        if (NbtUtils.contains(tag, "Width", Constants.NBT.TAG_ANY_NUMERIC) &&
            NbtUtils.contains(tag, "Height", Constants.NBT.TAG_ANY_NUMERIC) &&
            NbtUtils.contains(tag, "Length", Constants.NBT.TAG_ANY_NUMERIC) &&
            NbtUtils.containsInt(tag, "Version") &&
            NbtUtils.containsCompound(tag, "Palette") &&
            NbtUtils.containsByteArray(tag, "BlockData"))
        {
            return isSizeValid(readSizeFromTagImpl(tag));
        }

        return false;
    }

    @Override
    protected void initFromTag(NBTTagCompound tag)
    {
        this.version = NbtUtils.getInt(tag, "Version");
    }

    @Override
    protected Vec3i readSizeFromTag(NBTTagCompound tag)
    {
        return readSizeFromTagImpl(tag);
    }

    private static Vec3i readSizeFromTagImpl(NBTTagCompound tag)
    {
        return new Vec3i(NbtUtils.getInt(tag, "Width"), NbtUtils.getInt(tag, "Height"), NbtUtils.getInt(tag, "Length"));
    }

    @Override
    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        super.readMetadataFromTag(tag);

        if (NbtUtils.containsCompound(tag, "Metadata"))
        {
            NBTTagCompound metaTag = NbtUtils.getCompound(tag, "Metadata");

            if (NbtUtils.containsLong(metaTag, "Date") &&
                this.getMetadata().getTimeCreated() <= 0)
            {
                long time = NbtUtils.getLong(metaTag, "Date");
                this.getMetadata().setTimeCreated(time);
                this.getMetadata().setTimeModified(time);
            }
        }
    }

    protected boolean readPaletteFromTag(NBTTagCompound tag, ILitematicaBlockStatePalette palette)
    {
        Set<String> keys = NbtUtils.getKeys(tag);
        final int size = keys.size();
        List<IBlockState> list = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) { list.add(null); }

        for (String key : keys)
        {
            Optional<IBlockState> stateOptional = BlockUtils.getBlockStateFromString(key);
            int id = NbtUtils.getInt(tag, key);
            IBlockState state;

            if (stateOptional.isPresent())
            {
                state = stateOptional.get();
            }
            else
            {
                MessageDispatcher.warning().translate("litematica.message.error.schematic_read.sponge.palette.unknown_block", key);
                state = LitematicaBlockStateContainerFull.AIR_BLOCK_STATE;
            }

            if (id < 0 || id >= size)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.sponge.palette.invalid_id", id);
                return false;
            }

            list.set(id, state);
        }

        return palette.setMapping(list);
    }

    @Override
    protected boolean readBlocksFromTag(NBTTagCompound tag)
    {
        if (NbtUtils.containsCompound(tag, "Palette") &&
            NbtUtils.containsByteArray(tag, "BlockData") &&
            isSizeValid(this.getSize()))
        {
            NBTTagCompound paletteTag = NbtUtils.getCompound(tag, "Palette");
            byte[] blockData = NbtUtils.getByteArray(tag, "BlockData");
            int paletteSize = NbtUtils.getKeys(paletteTag).size();

            this.blockContainer = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockData, this.getSize());

            if (this.blockContainer == null)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.sponge.failed_to_read_blocks");
                return false;
            }

            return this.readPaletteFromTag(paletteTag, this.blockContainer.getPalette());
        }

        return false;
    }

    @Override
    protected Map<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag)
    {
        Map<BlockPos, NBTTagCompound> blockEntities = new HashMap<>();

        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        NBTTagList tagList = NbtUtils.getListOfCompounds(tag, tagName);

        final int size = NbtUtils.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound beTag = NbtUtils.getCompoundAt(tagList, i);
            BlockPos pos = NbtUtils.readBlockPosFromArrayTag(beTag, "Pos");

            if (pos != null && beTag.isEmpty() == false)
            {
                NbtUtils.putString(beTag, "id", NbtUtils.getString(beTag, "Id"));

                // Remove the Sponge tags from the data that is kept in memory
                NbtUtils.remove(beTag, "Id");
                NbtUtils.remove(beTag, "Pos");

                if (this.version == 1)
                {
                    NbtUtils.remove(beTag, "ContentVersion");
                }

                blockEntities.put(pos, beTag);
            }
        }

        return blockEntities;
    }

    @Override
    protected List<EntityInfo> readEntitiesFromTag(NBTTagCompound tag)
    {
        List<EntityInfo> entities = new ArrayList<>();
        NBTTagList tagList = NbtUtils.getListOfCompounds(tag, "Entities");
        final int size = NbtUtils.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = NbtUtils.getCompoundAt(tagList, i);
            Vec3d pos = NbtUtils.readVec3dFromListTag(entityData);

            if (pos != null && entityData.isEmpty() == false)
            {
                NbtUtils.putString(entityData, "id", NbtUtils.getString(entityData, "Id"));

                // Remove the Sponge tags from the data that is kept in memory
                NbtUtils.remove(entityData, "Id");

                entities.add(new EntityInfo(pos, entityData));
            }
        }

        return entities;
    }

    protected void writeMetadataToTag(NBTTagCompound tag)
    {
        NBTTagCompound metaTag = this.getMetadata().toTag();

        if (this.getMetadata().getTimeCreated() > 0)
        {
            NbtUtils.putLong(metaTag, "Date", this.getMetadata().getTimeCreated());
        }

        NbtUtils.putTag(tag, "Metadata", metaTag);
    }

    protected void writeBlocksToTag(NBTTagCompound tag)
    {
        NBTTagCompound paletteTag = this.writePaletteToTag(this.blockContainer.getPalette().getMapping());
        byte[] blockData = ((LitematicaBlockStateContainerFull) this.blockContainer).getBackingArrayAsByteArray();

        NbtUtils.putTag(tag, "Palette", paletteTag);
        NbtUtils.putByteArray(tag, "BlockData", blockData);
    }

    protected NBTTagCompound writePaletteToTag(List<IBlockState> list)
    {
        final int size = list.size();
        NBTTagCompound tag = new NBTTagCompound();

        for (int id = 0; id < size; ++id)
        {
            IBlockState state = list.get(id);
            NbtUtils.putInt(tag, state.toString(), id);
        }

        return tag;
    }

    protected void writeBlockEntitiesToTag(NBTTagCompound tag)
    {
        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<BlockPos, NBTTagCompound> entry : this.blockEntities.entrySet())
        {
            NBTTagCompound beTag = entry.getValue().copy();
            NbtUtils.writeBlockPosToArrayTag(entry.getKey(), beTag, "Pos");

            // Add the Sponge tag and remove the vanilla/Litematica tag
            NbtUtils.putString(beTag, "Id", NbtUtils.getString(beTag, "id"));
            NbtUtils.remove(beTag, "id");

            if (this.version == 1)
            {
                NbtUtils.putInt(beTag, "ContentVersion", 1);
            }

            NbtUtils.addTag(tagList, beTag);
        }

        NbtUtils.putTag(tag, tagName, tagList);
    }

    protected void writeEntitiesToTag(NBTTagCompound tag)
    {
        NBTTagList tagList = new NBTTagList();

        for (EntityInfo info : this.entities)
        {
            NBTTagCompound entityData = info.nbt.copy();
            NbtUtils.writeVec3dToListTag(info.pos, entityData);

            // Add the Sponge tag and remove the vanilla/Litematica tag
            NbtUtils.putString(entityData, "Id", NbtUtils.getString(entityData, "id"));
            NbtUtils.remove(entityData, "id");

            if (this.version == 1)
            {
                NbtUtils.putInt(entityData, "ContentVersion", 1);
            }

            NbtUtils.addTag(tagList, entityData);
        }

        NbtUtils.putTag(tag, "Entities", tagList);
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();

        this.writeBlocksToTag(tag);
        this.writeBlockEntitiesToTag(tag);
        this.writeEntitiesToTag(tag);
        this.writeMetadataToTag(tag);

        NbtUtils.putInt(tag, "DataVersion", LitematicaSchematic.MINECRAFT_DATA_VERSION);
        NbtUtils.putInt(tag, "Version", this.version);
        NbtUtils.putInt(tag, "PaletteMax", this.blockContainer.getPalette().getPaletteSize() - 1);
        NbtUtils.putShort(tag, "Width", (short) this.getSize().getX());
        NbtUtils.putShort(tag, "Height", (short) this.getSize().getY());
        NbtUtils.putShort(tag, "Length", (short) this.getSize().getZ());

        return tag;
    }
}
