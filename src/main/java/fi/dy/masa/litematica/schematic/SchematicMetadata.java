package fi.dy.masa.litematica.schematic;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.NBTUtils;

public class SchematicMetadata
{
    private String name = "?";
    private String author = "Unknown";
    private String description = "";
    private Vec3i enclosingSize = Vec3i.NULL_VECTOR;
    private long timeCreated;
    private long timeModified;
    private int regionCount;
    private long totalVolume;
    private long totalBlocks;
    private int[] thumbnailPixelData;
    private boolean modifiedSinceSaved;

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

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setString("Name", this.name);
        nbt.setString("Author", this.author);
        nbt.setString("Description", this.description);
        nbt.setInteger("RegionCount", this.regionCount);
        nbt.setLong("TotalVolume", this.totalVolume);
        nbt.setLong("TotalBlocks", this.totalBlocks);
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
        this.totalVolume = nbt.getLong("TotalVolume");
        this.totalBlocks = nbt.getLong("TotalBlocks");
        this.timeCreated = nbt.getLong("TimeCreated");
        this.timeModified = nbt.getLong("TimeModified");

        Vec3i size = NBTUtils.readBlockPos(nbt.getCompoundTag("EnclosingSize"));
        this.enclosingSize = size != null ? size : BlockPos.ORIGIN;

        if (nbt.hasKey("PreviewImageData", Constants.NBT.TAG_INT_ARRAY))
        {
            this.thumbnailPixelData = nbt.getIntArray("PreviewImageData");
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }
}
