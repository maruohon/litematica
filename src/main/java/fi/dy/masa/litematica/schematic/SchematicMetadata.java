package fi.dy.masa.litematica.schematic;

import javax.annotation.Nullable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.NBTUtils;

public class SchematicMetadata
{
    private String name = "?";
    private String author = "Unknown";
    private String description = "";
    private Vec3i enclosingSize = Vec3i.ZERO;
    private long timeCreated;
    private long timeModified;
    private int regionCount;
    private int totalVolume;
    private int totalBlocks;
    private int[] thumbnailPixelData;
    private boolean modifiedSinceSaved;

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

    public void setTimeModifiedToNow()
    {
        this.timeModified = System.currentTimeMillis();
    }

    public NbtCompound writeToNBT()
    {
        NbtCompound nbt = new NbtCompound();

        nbt.putString("Name", this.name);
        nbt.putString("Author", this.author);
        nbt.putString("Description", this.description);
        nbt.putInt("RegionCount", this.regionCount);
        nbt.putInt("TotalVolume", this.totalVolume);
        nbt.putInt("TotalBlocks", this.totalBlocks);
        nbt.putLong("TimeCreated", this.timeCreated);
        nbt.putLong("TimeModified", this.timeModified);
        nbt.put("EnclosingSize", NBTUtils.createBlockPosTag(this.enclosingSize));

        if (this.thumbnailPixelData != null)
        {
            nbt.putIntArray("PreviewImageData", this.thumbnailPixelData);
        }

        return nbt;
    }

    public void readFromNBT(NbtCompound nbt)
    {
        this.name = nbt.getString("Name");
        this.author = nbt.getString("Author");
        this.description = nbt.getString("Description");
        this.regionCount = nbt.getInt("RegionCount");
        this.totalVolume = nbt.getInt("TotalVolume");
        this.totalBlocks = nbt.getInt("TotalBlocks");
        this.timeCreated = nbt.getLong("TimeCreated");
        this.timeModified = nbt.getLong("TimeModified");

        Vec3i size = NBTUtils.readBlockPos(nbt.getCompound("EnclosingSize"));
        this.enclosingSize = size != null ? size : BlockPos.ORIGIN;

        if (nbt.contains("PreviewImageData", Constants.NBT.TAG_INT_ARRAY))
        {
            this.thumbnailPixelData = nbt.getIntArray("PreviewImageData");
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }
}
