package fi.dy.masa.litematica.schematic.projects;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.util.math.BlockPos;

public class SchematicVersion
{
    private final String name;
    private final String fileName;
    private final BlockPos areaOffset;
    private final int version;
    private final long timeStamp;

    SchematicVersion(String name, String fileName, BlockPos areaOffset, int version, long timeStamp)
    {
        this.name = name;
        this.fileName = fileName;
        this.areaOffset = areaOffset;
        this.version = version;
        this.timeStamp = timeStamp;
    }

    public String getName()
    {
        return this.name;
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public BlockPos getAreaOffset()
    {
        return this.areaOffset;
    }

    public int getVersion()
    {
        return this.version;
    }

    public long getTimeStamp()
    {
        return this.timeStamp;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("name", new JsonPrimitive(this.name));
        obj.add("file_name", new JsonPrimitive(this.fileName));
        obj.add("area_offset", JsonUtils.blockPosToJson(this.areaOffset));
        obj.add("version", new JsonPrimitive(this.version));
        obj.add("timestamp", new JsonPrimitive(this.timeStamp));

        return obj;
    }

    @Nullable
    public static SchematicVersion fromJson(JsonObject obj)
    {
        BlockPos areaOffset = JsonUtils.blockPosFromJson(obj, "area_offset");

        if (areaOffset != null &&
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasString(obj, "file_name"))
        {
            String name = JsonUtils.getString(obj, "name");
            String fileName = JsonUtils.getString(obj, "file_name");
            int version = JsonUtils.getInteger(obj, "version");
            long timeStamp = JsonUtils.getLong(obj, "timestamp");

            return new SchematicVersion(name, fileName, areaOffset, version, timeStamp);
        }

        return null;
    }
}
