package fi.dy.masa.litematica.schematic;

import java.io.File;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.util.InfoUtils;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

public class SchematicPlacement
{
    private final LitematicaSchematic schematic;
    private BlockPos pos;
    private Rotation rotation = Rotation.NONE;
    private Mirror mirror = Mirror.NONE;
    private File schematicFile;
    private boolean loadToWorld;

    public SchematicPlacement(LitematicaSchematic schematic, BlockPos pos)
    {
        this.schematic = schematic;
        this.pos = pos;
    }

    public boolean getLoadToWorld()
    {
        return this.loadToWorld;
    }

    public LitematicaSchematic getSchematic()
    {
        return schematic;
    }

    public BlockPos getPos1()
    {
        return pos;
    }

    public BlockPos getPos2()
    {
        BlockPos size = this.schematic.getTotalSize();
        size = PositionUtils.getTransformedBlockPos(size, this.mirror, this.rotation);
        return pos.add(size);
    }

    public Rotation getRotation()
    {
        return rotation;
    }

    public Mirror getMirror()
    {
        return mirror;
    }

    public void setLoadToWorld(boolean load)
    {
        this.loadToWorld = load;
    }

    public SchematicPlacement setPos(BlockPos pos)
    {
        this.pos = pos;
        return this;
    }

    public SchematicPlacement setRotation(Rotation rotation)
    {
        this.rotation = rotation;
        return this;
    }

    public SchematicPlacement setMirror(Mirror mirror)
    {
        this.mirror = mirror;
        return this;
    }

    @Nullable
    public JsonObject toJson()
    {
        if (this.schematicFile != null)
        {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();

            arr.add(this.pos.getX());
            arr.add(this.pos.getY());
            arr.add(this.pos.getZ());

            obj.add("schematic", new JsonPrimitive(this.schematicFile.getAbsolutePath()));
            obj.add("pos", arr);
            obj.add("rotation", new JsonPrimitive(this.rotation.name()));
            obj.add("mirror", new JsonPrimitive(this.mirror.name()));
            obj.add("load", new JsonPrimitive(this.loadToWorld));

            return obj;
        }

        // If this placement is for an an in-memory-only Schematic, then there is no point in saving
        // this placement, as the schematic can't be automatically loaded anyway.
        return null;
    }

    @Nullable
    public static SchematicPlacement fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "schematic") &&
            JsonUtils.hasArray(obj, "pos") &&
            JsonUtils.hasString(obj, "rotation") &&
            JsonUtils.hasString(obj, "mirror") &&
            JsonUtils.hasBoolean(obj, "load"))
        {
            File file = new File(obj.get("schematic").getAsString());
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(file.getParentFile(), file.getName(), InfoUtils.INFO_MESSAGE_CONSUMER);

            if (schematic == null)
            {
                LiteModLitematica.logger.warn("Failed to load schematic '{}'", file.getAbsolutePath());
                return null;
            }

            Rotation rotation = Rotation.valueOf(obj.get("rotation").getAsString());
            Mirror mirror = Mirror.valueOf(obj.get("mirror").getAsString());
            JsonArray posArr = obj.get("pos").getAsJsonArray();

            if (posArr.size() != 3)
            {
                LiteModLitematica.logger.warn("Failed to load schematic placement for '{}', invalid position", file.getAbsolutePath());
                return null;
            }

            BlockPos pos = new BlockPos(posArr.get(0).getAsInt(), posArr.get(1).getAsInt(), posArr.get(2).getAsInt());
            SchematicPlacement placement = new SchematicPlacement(schematic, pos);
            placement.setRotation(rotation);
            placement.setMirror(mirror);
            placement.setLoadToWorld(obj.get("load").getAsBoolean());

            return placement;
        }

        return null;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mirror == null) ? 0 : mirror.hashCode());
        result = prime * result + ((pos == null) ? 0 : pos.hashCode());
        result = prime * result + ((rotation == null) ? 0 : rotation.hashCode());
        result = prime * result + ((schematicFile == null) ? 0 : schematicFile.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SchematicPlacement other = (SchematicPlacement) obj;
        if (mirror != other.mirror)
            return false;
        if (pos == null)
        {
            if (other.pos != null)
                return false;
        }
        else if (!pos.equals(other.pos))
            return false;
        if (rotation != other.rotation)
            return false;
        if (schematicFile == null)
        {
            if (other.schematicFile != null)
                return false;
        }
        else if (!schematicFile.equals(other.schematicFile))
            return false;
        return true;
    }
}
