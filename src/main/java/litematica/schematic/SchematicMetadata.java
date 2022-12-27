package litematica.schematic;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3i;

import malilib.util.data.Constants;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.nbt.NbtUtils;

public class SchematicMetadata
{
    protected String name = "?";
    protected String author = "?";
    protected String description = "";
    protected Vec3i enclosingSize = Vec3i.NULL_VECTOR;
    protected long timeCreated;
    protected long timeModified;
    protected long minecraftDataVersion;
    protected int schematicVersion;
    protected int regionCount;
    protected int entityCount;
    protected int blockEntityCount;
    protected long totalVolume = -1;
    protected long totalBlocks = -1;
    protected boolean modifiedSinceSaved;
    @Nullable protected int[] thumbnailPixelData;

    public String getName()
    {
        return this.name;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public String getDescription()
    {
        return this.description;
    }

    @Nullable
    public int[] getPreviewImagePixelData()
    {
        return this.thumbnailPixelData;
    }

    public int getRegionCount()
    {
        return this.regionCount;
    }

    public long getTotalVolume()
    {
        return this.totalVolume;
    }

    public long getTotalBlocks()
    {
        return this.totalBlocks;
    }

    public int getEntityCount()
    {
        return this.entityCount;
    }

    public int getBlockEntityCount()
    {
        return this.blockEntityCount;
    }

    public Vec3i getEnclosingSize()
    {
        return this.enclosingSize;
    }

    public long getTimeCreated()
    {
        return this.timeCreated;
    }

    public long getTimeModified()
    {
        return this.timeModified;
    }

    public int getSchematicVersion()
    {
        return this.schematicVersion;
    }

    public long getMinecraftDataVersion()
    {
        return this.minecraftDataVersion;
    }

    // TODO
    public String getMinecraftVersion()
    {
        return "???";
    }

    public boolean hasBeenModified()
    {
        return this.timeCreated != this.timeModified;
    }

    public boolean wasModifiedSinceSaved()
    {
        return this.modifiedSinceSaved;
    }

    public void setModifiedSinceSaved()
    {
        this.modifiedSinceSaved = true;
    }

    public void clearModifiedSinceSaved()
    {
        this.modifiedSinceSaved = false;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setPreviewImagePixelData(@Nullable int[] pixelData)
    {
        this.thumbnailPixelData = pixelData;
    }

    public void setRegionCount(int regionCount)
    {
        this.regionCount = regionCount;
    }

    public void setTotalVolume(long totalVolume)
    {
        this.totalVolume = totalVolume;
    }

    public void setTotalBlocks(long totalBlocks)
    {
        this.totalBlocks = totalBlocks;
    }

    public void setEnclosingSize(Vec3i enclosingSize)
    {
        this.enclosingSize = enclosingSize;
    }

    public void setTimeCreated(long timeCreated)
    {
        this.timeCreated = timeCreated;
    }

    public void setTimeModified(long timeModified)
    {
        this.timeModified = timeModified;
    }

    public void setTimeModifiedToNow()
    {
        this.timeModified = System.currentTimeMillis();
    }

    public void copyFrom(SchematicMetadata other)
    {
        this.name = other.name;
        this.author = other.author;
        this.description = other.description;
        this.enclosingSize = other.enclosingSize;
        this.timeCreated = other.timeCreated;
        this.timeModified = other.timeModified;
        this.regionCount = other.regionCount;
        this.totalVolume = other.totalVolume;
        this.totalBlocks = other.totalBlocks;
        this.modifiedSinceSaved = false;

        if (other.thumbnailPixelData != null)
        {
            this.thumbnailPixelData = new int[other.thumbnailPixelData.length];
            System.arraycopy(other.thumbnailPixelData, 0, this.thumbnailPixelData, 0, this.thumbnailPixelData.length);
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }

    public NBTTagCompound toTag()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        NbtWrap.putString(nbt, "Name", this.name);
        NbtWrap.putString(nbt, "Author", this.author);
        NbtWrap.putString(nbt, "Description", this.description);

        if (this.regionCount > 0)
        {
            NbtWrap.putInt(nbt, "RegionCount", this.regionCount);
        }

        if (this.totalVolume > 0)
        {
            NbtWrap.putLong(nbt, "TotalVolume", this.totalVolume);
        }

        if (this.totalBlocks >= 0)
        {
            NbtWrap.putLong(nbt, "TotalBlocks", this.totalBlocks);
        }

        if (this.timeCreated > 0)
        {
            NbtWrap.putLong(nbt, "TimeCreated", this.timeCreated);
        }

        if (this.timeModified > 0)
        {
            NbtWrap.putLong(nbt, "TimeModified", this.timeModified);
        }

        NbtWrap.putTag(nbt, "EnclosingSize", NbtUtils.createBlockPosTag(this.enclosingSize));

        if (this.thumbnailPixelData != null)
        {
            NbtWrap.putIntArray(nbt, "PreviewImageData", this.thumbnailPixelData);
        }

        return nbt;
    }

    public void fromTag(NBTTagCompound tag)
    {
        this.name = NbtWrap.getStringOrDefault(tag, "Name", this.name);
        this.author = NbtWrap.getStringOrDefault(tag, "Author", this.author);
        this.description = NbtWrap.getStringOrDefault(tag, "Description", this.description);
        this.regionCount = NbtWrap.getIntOrDefault(tag, "RegionCount", this.regionCount);
        this.timeCreated = NbtWrap.getLongOrDefault(tag, "TimeCreated", this.timeCreated);
        this.timeModified = NbtWrap.getLongOrDefault(tag, "TimeModified", this.timeModified);

        if (NbtWrap.contains(tag, "TotalVolume", Constants.NBT.TAG_ANY_NUMERIC))
        {
            this.totalVolume = NbtWrap.getLong(tag, "TotalVolume");
        }

        if (NbtWrap.contains(tag, "TotalBlocks", Constants.NBT.TAG_ANY_NUMERIC))
        {
            this.totalBlocks = NbtWrap.getLong(tag, "TotalBlocks");
        }

        if (NbtWrap.containsCompound(tag, "EnclosingSize"))
        {
            Vec3i size = NbtUtils.readBlockPos(NbtWrap.getCompound(tag, "EnclosingSize"));

            if (size != null)
            {
                this.enclosingSize = size;
            }
        }

        if (NbtWrap.containsIntArray(tag, "PreviewImageData"))
        {
            this.thumbnailPixelData = NbtWrap.getIntArray(tag, "PreviewImageData");
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }
}
