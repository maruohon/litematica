package litematica.schematic;

import java.nio.file.Path;
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

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.Constants;
import malilib.util.game.BlockUtils;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.nbt.NbtUtils;
import litematica.schematic.container.ILitematicaBlockStatePalette;
import litematica.schematic.container.LitematicaBlockStateContainerFull;

public class SpongeSchematic extends SingleRegionSchematic
{
    public static final String FILE_NAME_EXTENSION = ".schem";

    protected int version = 1;

    SpongeSchematic(Path file)
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
        if (NbtWrap.contains(tag, "Width", Constants.NBT.TAG_ANY_NUMERIC) &&
            NbtWrap.contains(tag, "Height", Constants.NBT.TAG_ANY_NUMERIC) &&
            NbtWrap.contains(tag, "Length", Constants.NBT.TAG_ANY_NUMERIC) &&
            NbtWrap.containsInt(tag, "Version") &&
            NbtWrap.containsCompound(tag, "Palette") &&
            NbtWrap.containsByteArray(tag, "BlockData"))
        {
            return isSizeValid(readSizeFromTagImpl(tag));
        }

        return false;
    }

    @Override
    protected void initFromTag(NBTTagCompound tag)
    {
        this.version = NbtWrap.getInt(tag, "Version");
    }

    @Override
    protected Vec3i readSizeFromTag(NBTTagCompound tag)
    {
        return readSizeFromTagImpl(tag);
    }

    private static Vec3i readSizeFromTagImpl(NBTTagCompound tag)
    {
        return new Vec3i(NbtWrap.getInt(tag, "Width"), NbtWrap.getInt(tag, "Height"), NbtWrap.getInt(tag, "Length"));
    }

    @Override
    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        super.readMetadataFromTag(tag);

        if (NbtWrap.containsCompound(tag, "Metadata"))
        {
            NBTTagCompound metaTag = NbtWrap.getCompound(tag, "Metadata");

            if (NbtWrap.containsLong(metaTag, "Date") &&
                this.getMetadata().getTimeCreated() <= 0)
            {
                long time = NbtWrap.getLong(metaTag, "Date");
                this.getMetadata().setTimeCreated(time);
                this.getMetadata().setTimeModified(time);
            }
        }
    }

    protected boolean readPaletteFromTag(NBTTagCompound tag, ILitematicaBlockStatePalette palette)
    {
        Set<String> keys = NbtWrap.getKeys(tag);
        final int size = keys.size();
        List<IBlockState> list = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) { list.add(null); }

        for (String key : keys)
        {
            Optional<IBlockState> stateOptional = BlockUtils.getBlockStateFromString(key);
            int id = NbtWrap.getInt(tag, key);
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
        if (NbtWrap.containsCompound(tag, "Palette") &&
            NbtWrap.containsByteArray(tag, "BlockData") &&
            isSizeValid(this.getSize()))
        {
            NBTTagCompound paletteTag = NbtWrap.getCompound(tag, "Palette");
            byte[] blockData = NbtWrap.getByteArray(tag, "BlockData");
            int paletteSize = NbtWrap.getKeys(paletteTag).size();

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
        NBTTagList tagList = NbtWrap.getListOfCompounds(tag, tagName);

        final int size = NbtWrap.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound beTag = NbtWrap.getCompoundAt(tagList, i);
            BlockPos pos = NbtUtils.readBlockPosFromArrayTag(beTag, "Pos");

            if (pos != null && beTag.isEmpty() == false)
            {
                NbtWrap.putString(beTag, "id", NbtWrap.getString(beTag, "Id"));

                // Remove the Sponge tags from the data that is kept in memory
                NbtWrap.remove(beTag, "Id");
                NbtWrap.remove(beTag, "Pos");

                if (this.version == 1)
                {
                    NbtWrap.remove(beTag, "ContentVersion");
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
        NBTTagList tagList = NbtWrap.getListOfCompounds(tag, "Entities");
        final int size = NbtWrap.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = NbtWrap.getCompoundAt(tagList, i);
            Vec3d pos = NbtUtils.readVec3dFromListTag(entityData);

            if (pos != null && entityData.isEmpty() == false)
            {
                NbtWrap.putString(entityData, "id", NbtWrap.getString(entityData, "Id"));

                // Remove the Sponge tags from the data that is kept in memory
                NbtWrap.remove(entityData, "Id");

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
            NbtWrap.putLong(metaTag, "Date", this.getMetadata().getTimeCreated());
        }

        NbtWrap.putTag(tag, "Metadata", metaTag);
    }

    protected void writeBlocksToTag(NBTTagCompound tag)
    {
        NBTTagCompound paletteTag = this.writePaletteToTag(this.blockContainer.getPalette().getMapping());
        byte[] blockData = ((LitematicaBlockStateContainerFull) this.blockContainer).getBackingArrayAsByteArray();

        NbtWrap.putTag(tag, "Palette", paletteTag);
        NbtWrap.putByteArray(tag, "BlockData", blockData);
    }

    protected NBTTagCompound writePaletteToTag(List<IBlockState> list)
    {
        final int size = list.size();
        NBTTagCompound tag = new NBTTagCompound();

        for (int id = 0; id < size; ++id)
        {
            IBlockState state = list.get(id);
            NbtWrap.putInt(tag, state.toString(), id);
        }

        return tag;
    }

    protected void writeBlockEntitiesToTag(NBTTagCompound tag)
    {
        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<BlockPos, NBTTagCompound> entry : this.blockEntities.entrySet())
        {
            NBTTagCompound beTag = NbtWrap.copy(entry.getValue());
            NbtUtils.writeBlockPosToArrayTag(entry.getKey(), beTag, "Pos");

            // Add the Sponge tag and remove the vanilla/Litematica tag
            NbtWrap.putString(beTag, "Id", NbtWrap.getString(beTag, "id"));
            NbtWrap.remove(beTag, "id");

            if (this.version == 1)
            {
                NbtWrap.putInt(beTag, "ContentVersion", 1);
            }

            NbtWrap.addTag(tagList, beTag);
        }

        NbtWrap.putTag(tag, tagName, tagList);
    }

    protected void writeEntitiesToTag(NBTTagCompound tag)
    {
        NBTTagList tagList = new NBTTagList();

        for (EntityInfo info : this.entities)
        {
            NBTTagCompound entityData = NbtWrap.copy(info.nbt);
            NbtUtils.writeVec3dToListTag(info.pos, entityData);

            // Add the Sponge tag and remove the vanilla/Litematica tag
            NbtWrap.putString(entityData, "Id", NbtWrap.getString(entityData, "id"));
            NbtWrap.remove(entityData, "id");

            if (this.version == 1)
            {
                NbtWrap.putInt(entityData, "ContentVersion", 1);
            }

            NbtWrap.addTag(tagList, entityData);
        }

        NbtWrap.putTag(tag, "Entities", tagList);
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();

        this.writeBlocksToTag(tag);
        this.writeBlockEntitiesToTag(tag);
        this.writeEntitiesToTag(tag);
        this.writeMetadataToTag(tag);

        NbtWrap.putInt(tag, "DataVersion", LitematicaSchematic.MINECRAFT_DATA_VERSION);
        NbtWrap.putInt(tag, "Version", this.version);
        NbtWrap.putInt(tag, "PaletteMax", this.blockContainer.getPalette().getPaletteSize() - 1);
        NbtWrap.putShort(tag, "Width", (short) this.getSize().getX());
        NbtWrap.putShort(tag, "Height", (short) this.getSize().getY());
        NbtWrap.putShort(tag, "Length", (short) this.getSize().getZ());

        return tag;
    }
}
