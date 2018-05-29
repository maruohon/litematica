package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.InfoUtils;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.Vec3f;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.init.Blocks;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.PlacementSettings;

public class SchematicPlacement
{
    private static int nextColorIndex;
    private static final Set<Integer> USED_COLORS = new HashSet<>();

    private String name;
    @Nullable
    private LitematicaSchematic schematic;
    private BlockPos pos;
    private Rotation rotation = Rotation.NONE;
    private Mirror mirror = Mirror.NONE;
    private boolean ignoreEntities;
    private boolean enabled;
    private boolean renderSchematic;
    private int boxesBBColor;
    private Vec3f boxesBBColorVec = new Vec3f(0xFF, 0xFF, 0xFF);

    public SchematicPlacement(LitematicaSchematic schematic, BlockPos pos, String name)
    {
        this.schematic = schematic;
        this.pos = pos;
        this.name = name;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean getRenderSchematic()
    {
        return this.isEnabled() && this.renderSchematic;
    }

    public String getName()
    {
        return this.name;
    }

    public LitematicaSchematic getSchematic()
    {
        return schematic;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public Rotation getRotation()
    {
        return rotation;
    }

    public Mirror getMirror()
    {
        return mirror;
    }

    public Vec3f getBoxesBBColor()
    {
        return this.boxesBBColorVec;
    }

    public PlacementSettings getPlacement()
    {
        PlacementSettings placement = new PlacementSettings();

        placement.setMirror(this.mirror);
        placement.setRotation(this.rotation);
        placement.setIgnoreEntities(this.ignoreEntities);
        placement.setReplacedBlock(Blocks.STRUCTURE_VOID);

        return placement;
    }

    public void setEnabled(boolean enabled)
    {
        if (enabled != this.enabled)
        {
            this.enabled = enabled;
            this.updateRenderers(false);
        }
    }

    public void toggleEnabled()
    {
        this.setEnabled(! this.enabled);
    }

    public void setRenderSchematic(boolean render)
    {
        this.renderSchematic = render;
        this.updateRenderers(false);
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public SchematicPlacement setPos(BlockPos pos)
    {
        this.pos = pos;
        this.updateRenderers(false);
        return this;
    }

    public SchematicPlacement setRotation(Rotation rotation)
    {
        this.rotation = rotation;
        this.updateRenderers(false);
        return this;
    }

    public SchematicPlacement setMirror(Mirror mirror)
    {
        this.mirror = mirror;
        this.updateRenderers(false);
        return this;
    }

    public SchematicPlacement setBoxesBBColor(int color)
    {
        this.boxesBBColor = color;
        this.boxesBBColorVec = new Vec3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
        USED_COLORS.add(color);
        return this;
    }

    public SchematicPlacement setBoxesBBColorNext()
    {
        return this.setBoxesBBColor(getNextBoxColor());
    }

    private void updateRenderers(boolean forceUpdate)
    {
        OverlayRenderer.getInstance().updatePlacementCache();

        if (forceUpdate || this.schematic != null)
        {
            SchematicWorldHandler.getInstance().rebuildSchematicWorld(true);
        }
    }

    public void onRemoved()
    {
        USED_COLORS.remove(this.boxesBBColor);

        if (USED_COLORS.isEmpty())
        {
            nextColorIndex = 0;
        }
    }

    @Nullable
    public JsonObject toJson()
    {
        if (this.schematic.getFile() != null)
        {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();

            arr.add(this.pos.getX());
            arr.add(this.pos.getY());
            arr.add(this.pos.getZ());

            obj.add("schematic", new JsonPrimitive(this.schematic.getFile().getAbsolutePath()));
            obj.add("name", new JsonPrimitive(this.name));
            obj.add("pos", arr);
            obj.add("rotation", new JsonPrimitive(this.rotation.name()));
            obj.add("mirror", new JsonPrimitive(this.mirror.name()));
            obj.add("ignore_entities", new JsonPrimitive(this.ignoreEntities));
            obj.add("enabled", new JsonPrimitive(this.enabled));
            obj.add("bb_color", new JsonPrimitive(this.boxesBBColor));
            obj.add("render_schematic", new JsonPrimitive(this.renderSchematic));

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
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasArray(obj, "pos") &&
            JsonUtils.hasString(obj, "rotation") &&
            JsonUtils.hasString(obj, "mirror"))
        {
            File file = new File(obj.get("schematic").getAsString());
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(file.getParentFile(), file.getName(), InfoUtils.INFO_MESSAGE_CONSUMER);

            if (schematic == null)
            {
                LiteModLitematica.logger.warn("Failed to load schematic '{}'", file.getAbsolutePath());
                return null;
            }

            JsonArray posArr = obj.get("pos").getAsJsonArray();

            if (posArr.size() != 3)
            {
                LiteModLitematica.logger.warn("Failed to load schematic placement for '{}', invalid position", file.getAbsolutePath());
                return null;
            }

            String name = obj.get("name").getAsString();
            BlockPos pos = new BlockPos(posArr.get(0).getAsInt(), posArr.get(1).getAsInt(), posArr.get(2).getAsInt());
            Rotation rotation = Rotation.valueOf(obj.get("rotation").getAsString());
            Mirror mirror = Mirror.valueOf(obj.get("mirror").getAsString());
            SchematicPlacement placement = new SchematicPlacement(schematic, pos, name);
            placement.rotation = rotation;
            placement.mirror = mirror;
            placement.ignoreEntities = JsonUtils.getBoolean(obj, "ignore_entities");
            placement.enabled = JsonUtils.getBoolean(obj, "enabled");
            placement.renderSchematic = JsonUtils.getBoolean(obj, "render_schematic");

            if (JsonUtils.hasInteger(obj, "bb_color"))
            {
                placement.setBoxesBBColor(JsonUtils.getInteger(obj, "bb_color"));
            }
            else
            {
                placement.setBoxesBBColorNext();
            }

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
        result = prime * result + ((schematic == null) ? 0 : schematic.hashCode());
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
        if (schematic == null)
        {
            if (other.schematic != null)
                return false;
        }
        else if (!schematic.equals(other.schematic))
            return false;
        return true;
    }

    private static int getNextBoxColor()
    {
        int length = OverlayRenderer.KELLY_COLORS.length;
        int color = OverlayRenderer.KELLY_COLORS[nextColorIndex];
        nextColorIndex = (nextColorIndex + 1) % length;

        for (int i = 0; i < length; ++i)
        {
            if (USED_COLORS.contains(color) == false)
            {
                return color;
            }

            color = OverlayRenderer.KELLY_COLORS[nextColorIndex];
            nextColorIndex = (nextColorIndex + 1) % length;
        }

        return color;
    }
}
