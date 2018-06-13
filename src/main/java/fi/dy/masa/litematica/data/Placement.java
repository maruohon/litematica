package fi.dy.masa.litematica.data;

import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.util.JsonUtils;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

public class Placement
{
    private final String name;
    private final BlockPos defaultPos;
    private BlockPos pos;
    private Rotation rotation = Rotation.NONE;
    private Mirror mirror = Mirror.NONE;
    private boolean enabled = true;

    public Placement(BlockPos pos, String name)
    {
        this.pos = pos;
        this.defaultPos = pos;
        this.name = name;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public String getName()
    {
        return this.name;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }

    public Rotation getRotation()
    {
        return this.rotation;
    }

    public Mirror getMirror()
    {
        return this.mirror;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void toggleEnabled()
    {
        this.setEnabled(! this.isEnabled());
    }

    public void setPos(BlockPos pos)
    {
        this.pos = pos;
    }

    public void setRotation(Rotation rotation)
    {
        this.rotation = rotation;
    }

    public void setMirror(Mirror mirror)
    {
        this.mirror = mirror;
    }

    public void resetToOriginalValues()
    {
        this.pos = this.defaultPos;
        this.rotation = Rotation.NONE;
        this.mirror = Mirror.NONE;
        this.enabled = true;
    }

    public boolean isRegionPlacementModified(BlockPos originalPosition)
    {
        return this.isEnabled() == false ||
               this.getMirror() != Mirror.NONE ||
               this.getRotation() != Rotation.NONE ||
               this.getPos().equals(originalPosition) == false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        arr.add(this.pos.getX());
        arr.add(this.pos.getY());
        arr.add(this.pos.getZ());

        obj.add("pos", arr);
        obj.add("name", new JsonPrimitive(this.getName()));
        obj.add("rotation", new JsonPrimitive(this.rotation.name()));
        obj.add("mirror", new JsonPrimitive(this.mirror.name()));
        obj.add("enabled", new JsonPrimitive(this.enabled));

        return obj;
    }

    @Nullable
    public static Placement fromJson(JsonObject obj)
    {
        if (JsonUtils.hasArray(obj, "pos") &&
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasString(obj, "rotation") &&
            JsonUtils.hasString(obj, "mirror"))
        {
            JsonArray posArr = obj.get("pos").getAsJsonArray();

            if (posArr.size() != 3)
            {
                LiteModLitematica.logger.warn("Placement.fromJson(): Failed to load a placement from JSON, invalid position data");
                return null;
            }

            BlockPos pos = new BlockPos(posArr.get(0).getAsInt(), posArr.get(1).getAsInt(), posArr.get(2).getAsInt());
            Placement placement = new Placement(pos, obj.get("name").getAsString());
            placement.setEnabled(JsonUtils.getBoolean(obj, "enabled"));

            try
            {
                Rotation rotation = Rotation.valueOf(obj.get("rotation").getAsString());
                Mirror mirror = Mirror.valueOf(obj.get("mirror").getAsString());

                placement.setRotation(rotation);
                placement.setMirror(mirror);
            }
            catch (Exception e)
            {
                LiteModLitematica.logger.warn("Placement.fromJson(): Invalid rotation or mirror value for a placement");
            }

            return placement;
        }

        return null;
    }
}
