package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileInputStream;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.util.Constants;
import fi.dy.masa.litematica.util.NBTUtils;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class SchematicMetadata
{
    private String name = "?";
    private String author = "Unknown";
    private String thumbnailLocation = "";
    private String thumbnailData = "";
    private String description = "";
    private BlockPos enclosingSize = BlockPos.ORIGIN;
    private long timeCreated;
    private long timeModified;
    private int regionCount;
    private int totalVolume;
    private int totalBlocks;

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

    public String getThumbnailLocation()
    {
        return thumbnailLocation;
    }

    public String getThumbnailData()
    {
        return thumbnailData;
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

    public BlockPos getEnclosingSize()
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

    public void setThumbnailLocation(String thumbnailLocation)
    {
        this.thumbnailLocation = thumbnailLocation;
    }

    public void setThumbnailData(String thumbnailData)
    {
        this.thumbnailData = thumbnailData;
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

    public void setEnclosingSize(BlockPos enclosingSize)
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
        nbt.setString("ThumbnailLocation", this.thumbnailLocation);
        nbt.setString("ThumbnailData", this.thumbnailData);
        nbt.setInteger("RegionCount", this.regionCount);
        nbt.setInteger("TotalVolume", this.totalVolume);
        nbt.setInteger("TotalBlocks", this.totalBlocks);
        nbt.setLong("TimeCreated", this.timeCreated);
        nbt.setLong("TimeModified", this.timeModified);
        nbt.setTag("EnclosingSize", NBTUtils.createBlockPosTag(this.enclosingSize));

        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt)
    {
        this.name = nbt.getString("Name");
        this.author = nbt.getString("Author");
        this.description = nbt.getString("Description");
        this.thumbnailLocation = nbt.getString("ThumbnailLocation");
        this.thumbnailData = nbt.getString("ThumbnailData");
        this.regionCount = nbt.getInteger("RegionCount");
        this.totalVolume = nbt.getInteger("TotalVolume");
        this.totalBlocks = nbt.getInteger("TotalBlocks");
        this.timeCreated = nbt.getLong("TimeCreated");
        this.timeModified = nbt.getLong("TimeModified");

        BlockPos size = NBTUtils.readBlockPos(nbt.getCompoundTag("EnclosingSize"));

        if (size != null)
        {
            this.enclosingSize = size;
        }
    }

    @Nullable
    public static SchematicMetadata fromFile(File file)
    {
        if (file.exists() && file.canRead())
        {
            try
            {
                FileInputStream is = new FileInputStream(file);
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
                is.close();

                if (nbt != null && nbt.hasKey("Author", Constants.NBT.TAG_STRING))
                {
                    SchematicMetadata metadata = new SchematicMetadata();
                    metadata.readFromNBT(nbt);
                    return metadata;
                }
            }
            catch (Exception e)
            {
            }
        }

        return null;
    }
}
