package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

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
        if (tag.hasKey("Width", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Height", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Length", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Version", Constants.NBT.TAG_INT) &&
            tag.hasKey("Palette", Constants.NBT.TAG_COMPOUND) &&
            tag.hasKey("BlockData", Constants.NBT.TAG_BYTE_ARRAY))
        {
            return isSizeValid(readSizeFromTagImpl(tag));
        }

        return false;
    }

    @Override
    protected void initFromTag(NBTTagCompound tag)
    {
        this.version = tag.getInteger("Version");
    }

    @Override
    protected Vec3i readSizeFromTag(NBTTagCompound tag)
    {
        return readSizeFromTagImpl(tag);
    }

    private static Vec3i readSizeFromTagImpl(NBTTagCompound tag)
    {
        return new Vec3i(tag.getInteger("Width"), tag.getInteger("Height"), tag.getInteger("Length"));
    }

    @Override
    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        NBTTagCompound metaTag = tag.getCompoundTag("Metadata");

        this.getMetadata().fromTag(metaTag);

        if (metaTag.hasKey("Date", Constants.NBT.TAG_LONG))
        {
            long time = metaTag.getLong("Date");
            this.getMetadata().setTimeCreated(time);
            this.getMetadata().setTimeModified(time);
        }
    }

    @Override
    protected boolean readBlocksFromTag(NBTTagCompound tag)
    {
        if (tag.hasKey("Palette", Constants.NBT.TAG_COMPOUND) &&
            tag.hasKey("BlockData", Constants.NBT.TAG_BYTE_ARRAY) &&
            isSizeValid(this.getSize()))
        {
            NBTTagCompound paletteTag = tag.getCompoundTag("Palette");
            byte[] blockData = tag.getByteArray("BlockData");

            this.blockContainer = LitematicaBlockStateContainer.createFromSpongeFormat(paletteTag, blockData, this.getSize());

            if (this.blockContainer == null)
            {
                InfoUtils.printErrorMessage("litematica.message.error.schematic_read.sponge.failed_to_read_blocks");
                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    protected Map<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag)
    {
        Map<BlockPos, NBTTagCompound> blockEntities = new HashMap<>();

        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        NBTTagList tagList = tag.getTagList(tagName, Constants.NBT.TAG_COMPOUND);

        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound beTag = tagList.getCompoundTagAt(i);
            BlockPos pos = NBTUtils.readBlockPosFromArrayTag(beTag, "Pos");

            if (pos != null && beTag.isEmpty() == false)
            {
                beTag.setString("id", beTag.getString("Id"));

                // Remove the Sponge tags from the data that is kept in memory
                beTag.removeTag("Id");
                beTag.removeTag("Pos");

                if (this.version == 1)
                {
                    beTag.removeTag("ContentVersion");
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
        NBTTagList tagList = tag.getTagList("Entities", Constants.NBT.TAG_COMPOUND);
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = tagList.getCompoundTagAt(i);
            Vec3d pos = NBTUtils.readEntityPositionFromTag(entityData);

            if (pos != null && entityData.isEmpty() == false)
            {
                entityData.setString("id", entityData.getString("Id"));

                // Remove the Sponge tags from the data that is kept in memory
                entityData.removeTag("Id");

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
            metaTag.setLong("Date", this.getMetadata().getTimeCreated());
        }

        tag.setTag("Metadata", metaTag);
    }

    protected void writeBlocksToTag(NBTTagCompound tag)
    {
        NBTTagCompound paletteTag = this.blockContainer.getPalette().writeToSpongeTag();
        byte[] blockData = this.blockContainer.getBackingArrayAsByteArray();

        tag.setTag("Palette", paletteTag);
        tag.setByteArray("BlockData", blockData);
    }

    protected void writeBlockEntitiesToTag(NBTTagCompound tag)
    {
        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<BlockPos, NBTTagCompound> entry : this.blockEntities.entrySet())
        {
            NBTTagCompound beTag = entry.getValue().copy();
            NBTUtils.writeBlockPosToArrayTag(entry.getKey(), beTag, "Pos");

            // Add the Sponge tag and remove the vanilla/Litematica tag
            beTag.setString("Id", beTag.getString("id"));
            beTag.removeTag("id");

            if (this.version == 1)
            {
                beTag.setInteger("ContentVersion", 1);
            }

            tagList.appendTag(beTag);
        }

        tag.setTag(tagName, tagList);
    }

    protected void writeEntitiesToTag(NBTTagCompound tag)
    {
        NBTTagList tagList = new NBTTagList();

        for (EntityInfo info : this.entities)
        {
            NBTTagCompound entityData = info.nbt.copy();
            NBTUtils.writeEntityPositionToTag(info.pos, entityData);

            // Add the Sponge tag and remove the vanilla/Litematica tag
            entityData.setString("Id", entityData.getString("id"));
            entityData.removeTag("id");

            if (this.version == 1)
            {
                entityData.setInteger("ContentVersion", 1);
            }

            tagList.appendTag(entityData);
        }

        tag.setTag("Entities", tagList);
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();

        this.writeBlocksToTag(tag);
        this.writeBlockEntitiesToTag(tag);
        this.writeEntitiesToTag(tag);
        this.writeMetadataToTag(tag);

        tag.setInteger("DataVersion", LitematicaSchematic.MINECRAFT_DATA_VERSION);
        tag.setInteger("Version", this.version);
        tag.setInteger("PaletteMax", this.blockContainer.getPalette().getPaletteSize() - 1);
        tag.setShort("Width", (short) this.getSize().getX());
        tag.setShort("Height", (short) this.getSize().getY());
        tag.setShort("Length", (short) this.getSize().getZ());

        return tag;
    }
}
