package litematica.schematic.projects;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import malilib.util.data.json.JsonUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3i;

public class SchematicVersion
{
    protected final SchematicProject project;
    protected final String name;
    protected final String fileName;
    protected final Vec3i areaOffset;
    protected final int version;
    protected final long timeStamp;

    SchematicVersion(SchematicProject project, String name, String fileName,
                     Vec3i areaOffset, int version, long timeStamp)
    {
        this.project = project;
        this.name = name;
        this.fileName = fileName;
        this.areaOffset = areaOffset;
        this.version = version;
        this.timeStamp = timeStamp;
    }

    public SchematicProject getProject()
    {
        return this.project;
    }

    public String getName()
    {
        return this.name;
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public Vec3i getAreaOffset()
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
    public static SchematicVersion fromJson(JsonObject obj, SchematicProject project)
    {
        BlockPos areaOffset = JsonUtils.getBlockPos(obj, "area_offset");

        if (areaOffset != null &&
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasString(obj, "file_name"))
        {
            String name = JsonUtils.getString(obj, "name");
            String fileName = JsonUtils.getString(obj, "file_name");
            int version = JsonUtils.getInteger(obj, "version");
            long timeStamp = JsonUtils.getLong(obj, "timestamp");

            return new SchematicVersion(project, name, fileName, areaOffset, version, timeStamp);
        }

        return null;
    }
}
