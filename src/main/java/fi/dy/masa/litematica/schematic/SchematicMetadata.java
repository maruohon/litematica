package fi.dy.masa.litematica.schematic;

import net.minecraft.nbt.NBTTagCompound;

public class SchematicMetadata
{
    private String author = "Unknown";
    private String thumbnailLocation = "";
    private String thumbnailData = "";
    private String description = "";
    private long timeCreated;
    private long timeModified;

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

    public long getTimeCreated()
    {
        return timeCreated;
    }

    public long getTimeModified()
    {
        return timeModified;
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

        nbt.setString("Author", this.author);
        nbt.setString("Description", this.description);
        nbt.setString("ThumbnailLocation", this.thumbnailLocation);
        nbt.setString("ThumbnailData", this.thumbnailData);
        nbt.setLong("TimeCreated", this.timeCreated);
        nbt.setLong("TimeModified", this.timeModified);

        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt)
    {
        this.author = nbt.getString("Author");
        this.description = nbt.getString("Description");
        this.thumbnailLocation = nbt.getString("ThumbnailLocation");
        this.thumbnailData = nbt.getString("ThumbnailData");
        this.timeCreated = nbt.getLong("TimeCreated");
        this.timeModified = nbt.getLong("TimeModified");
    }
}
