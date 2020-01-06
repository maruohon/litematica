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

    protected int version;

    SpongeSchematic(File file)
    {
        super(file);
    }

    @Override
    public String getFileNameExtension()
    {
        return FILE_NAME_EXTENSION;
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

        if (metaTag.hasKey("Author", Constants.NBT.TAG_STRING))
        {
            this.getMetadata().setAuthor(metaTag.getString("Author"));
        }

        if (metaTag.hasKey("Name", Constants.NBT.TAG_STRING))
        {
            this.getMetadata().setName(metaTag.getString("Name"));
        }

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
            Vec3d posVec = NBTUtils.readEntityPositionFromTag(entityData);

            if (posVec != null && entityData.isEmpty() == false)
            {
                entityData.setString("id", entityData.getString("Id"));

                // Remove the Sponge tags from the data that is kept in memory
                entityData.removeTag("Id");

                entities.add(new EntityInfo(posVec, entityData));
            }
        }

        return entities;
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        return tag;
    }
}
