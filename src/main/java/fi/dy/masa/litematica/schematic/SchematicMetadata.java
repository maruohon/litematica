package fi.dy.masa.litematica.schematic;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.nbt.NbtUtils;

public class SchematicMetadata
{
    private String name = "?";
    private String author = "?";
    private String description = "";
    private Vec3i enclosingSize = Vec3i.NULL_VECTOR;
    private long timeCreated;
    private long timeModified;
    private int regionCount;
    private long totalVolume = -1;
    private long totalBlocks = -1;
    private boolean modifiedSinceSaved;
    @Nullable private int[] thumbnailPixelData;

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

        NbtUtils.putString(nbt, "Name", this.name);
        NbtUtils.putString(nbt, "Author", this.author);
        NbtUtils.putString(nbt, "Description", this.description);

        if (this.regionCount > 0)
        {
            NbtUtils.putInt(nbt, "RegionCount", this.regionCount);
        }

        if (this.totalVolume > 0)
        {
            NbtUtils.putLong(nbt, "TotalVolume", this.totalVolume);
        }

        if (this.totalBlocks >= 0)
        {
            NbtUtils.putLong(nbt, "TotalBlocks", this.totalBlocks);
        }

        if (this.timeCreated > 0)
        {
            NbtUtils.putLong(nbt, "TimeCreated", this.timeCreated);
        }

        if (this.timeModified > 0)
        {
            NbtUtils.putLong(nbt, "TimeModified", this.timeModified);
        }

        NbtUtils.putTag(nbt, "EnclosingSize", NbtUtils.createBlockPosTag(this.enclosingSize));

        if (this.thumbnailPixelData != null)
        {
            NbtUtils.putIntArray(nbt, "PreviewImageData", this.thumbnailPixelData);
        }

        return nbt;
    }

    public void fromTag(NBTTagCompound tag)
    {
        this.name = NbtUtils.getStringOrDefault(tag, "Name", this.name);
        this.author = NbtUtils.getStringOrDefault(tag, "Author", this.author);
        this.description = NbtUtils.getStringOrDefault(tag, "Description", this.description);
        this.regionCount = NbtUtils.getIntOrDefault(tag, "RegionCount", this.regionCount);
        this.timeCreated = NbtUtils.getLongOrDefault(tag, "TimeCreated", this.timeCreated);
        this.timeModified = NbtUtils.getLongOrDefault(tag, "TimeModified", this.timeModified);

        if (NbtUtils.contains(tag, "TotalVolume", Constants.NBT.TAG_ANY_NUMERIC))
        {
            this.totalVolume = NbtUtils.getLong(tag, "TotalVolume");
        }

        if (NbtUtils.contains(tag, "TotalBlocks", Constants.NBT.TAG_ANY_NUMERIC))
        {
            this.totalBlocks = NbtUtils.getLong(tag, "TotalBlocks");
        }

        if (NbtUtils.containsCompound(tag, "EnclosingSize"))
        {
            Vec3i size = NbtUtils.readBlockPos(NbtUtils.getCompound(tag, "EnclosingSize"));

            if (size != null)
            {
                this.enclosingSize = size;
            }
        }

        if (NbtUtils.containsIntArray(tag, "PreviewImageData"))
        {
            this.thumbnailPixelData = NbtUtils.getIntArray(tag, "PreviewImageData");
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }
}
