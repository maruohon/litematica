package fi.dy.masa.litematica.schematic;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.util.Constants;
import fi.dy.masa.litematica.util.NBTUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3i;

public class SchematicMetadata
{
    private String name = "?";
    private String author = "Unknown";
    private String description = "";
    private Vec3i enclosingSize = Vec3i.NULL_VECTOR;
    private long timeCreated;
    private long timeModified;
    private int regionCount;
    private int totalVolume;
    private int totalBlocks;
    private int[] thumbnailPixelData;

    public String getName()
    {
        return this.name;
    }

    public String getAuthor()
    {
        return author;
    }

    public String getDescription()
    {
        return this.description;
    }

    @Nullable
    public int[] getPreviewImagePixelData()
    {
        return thumbnailPixelData;
    }

    public int getRegionCount()
    {
        return this.regionCount;
    }

    public int getTotalVolume()
    {
        return this.totalVolume;
    }

    public int getTotalBlocks()
    {
        return this.totalBlocks;
    }

    public Vec3i getEnclosingSize()
    {
        return this.enclosingSize;
    }

    public long getTimeCreated()
    {
        return timeCreated;
    }

    public long getTimeModified()
    {
        return timeModified;
    }

    public boolean hasBeenModified()
    {
        return this.timeCreated != this.timeModified;
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

    public void setPreviewImagePixelData(int[] pixelData)
    {
        this.thumbnailPixelData = pixelData;
    }

    public void setRegionCount(int regionCount)
    {
        this.regionCount = regionCount;
    }

    public void setTotalVolume(int totalVolume)
    {
        this.totalVolume = totalVolume;
    }

    public void setTotalBlocks(int totalBlocks)
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

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setString("Name", this.name);
        nbt.setString("Author", this.author);
        nbt.setString("Description", this.description);
        nbt.setInteger("RegionCount", this.regionCount);
        nbt.setInteger("TotalVolume", this.totalVolume);
        nbt.setInteger("TotalBlocks", this.totalBlocks);
        nbt.setLong("TimeCreated", this.timeCreated);
        nbt.setLong("TimeModified", this.timeModified);
        nbt.setTag("EnclosingSize", NBTUtils.createBlockPosTag(this.enclosingSize));

        if (this.thumbnailPixelData != null)
        {
            nbt.setIntArray("PreviewImageData", this.thumbnailPixelData);
        }

        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt)
    {
        this.name = nbt.getString("Name");
        this.author = nbt.getString("Author");
        this.description = nbt.getString("Description");
        this.regionCount = nbt.getInteger("RegionCount");
        this.totalVolume = nbt.getInteger("TotalVolume");
        this.totalBlocks = nbt.getInteger("TotalBlocks");
        this.timeCreated = nbt.getLong("TimeCreated");
        this.timeModified = nbt.getLong("TimeModified");

        Vec3i size = NBTUtils.readBlockPos(nbt.getCompoundTag("EnclosingSize"));

        if (size != null)
        {
            this.enclosingSize = size;
        }

        if (nbt.hasKey("PreviewImageData", Constants.NBT.TAG_INT_ARRAY))
        {
            this.thumbnailPixelData = nbt.getIntArray("PreviewImageData");
        }
    }
}
