package fi.dy.masa.litematica.data;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase.InfoType;
import fi.dy.masa.litematica.gui.interfaces.IMessageConsumer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.resources.I18n;

public class SchematicPlacementManager
{
    private final List<SchematicPlacement> schematicPlacements = new ArrayList<>();
    @Nullable
    private SchematicPlacement selectedPlacement;

    public List<SchematicPlacement> getAllSchematicsPlacements()
    {
        return this.schematicPlacements;
    }

    public void addSchematicPlacement(SchematicPlacement placement, @Nullable IMessageConsumer messageConsumer)
    {
        if (this.schematicPlacements.contains(placement) == false)
        {
            this.schematicPlacements.add(placement);

            if (messageConsumer != null)
            {
                messageConsumer.addMessage(InfoType.SUCCESS, I18n.format("litematica.message.schematic_placement_created", placement.getName()));
            }

            if (placement.isEnabled())
            {
                OverlayRenderer.getInstance().updatePlacementCache();
                SchematicWorldHandler.getInstance().rebuildSchematicWorld(true);
            }
        }
        else if (messageConsumer != null)
        {
            messageConsumer.addMessage(InfoType.ERROR, I18n.format("litematica.error.duplicate_schematic_load"));
        }
    }

    public boolean removeSchematicPlacement(SchematicPlacement placement)
    {
        if (this.selectedPlacement == placement)
        {
            this.selectedPlacement = null;
        }

        boolean ret = this.schematicPlacements.remove(placement);

        if (ret)
        {
            placement.onRemoved();
            OverlayRenderer.getInstance().updatePlacementCache();
            SchematicWorldHandler.getInstance().rebuildSchematicWorld(true);
        }

        return ret;
    }

    public void removeAllPlacementsOfSchematic(LitematicaSchematic schematic)
    {
        for (int i = 0; i < this.schematicPlacements.size(); ++i)
        {
            if (this.schematicPlacements.get(i).getSchematic() == schematic)
            {
                this.schematicPlacements.remove(i).onRemoved();
                --i;
            }
        }

        OverlayRenderer.getInstance().updatePlacementCache();
        SchematicWorldHandler.getInstance().rebuildSchematicWorld(true);
    }

    @Nullable
    public SchematicPlacement getSelectedSchematicPlacement()
    {
        return this.selectedPlacement;
    }

    public void setSelectedSchematicPlacement(@Nullable SchematicPlacement placement)
    {
        if (placement == null || this.schematicPlacements.contains(placement))
        {
            this.selectedPlacement = placement;
            OverlayRenderer.getInstance().updatePlacementCache();
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.schematicPlacements.size() > 0)
        {
            JsonArray arr = new JsonArray();
            int index = -1;

            for (int i = 0; i < this.schematicPlacements.size(); ++i)
            {
                SchematicPlacement placement = this.schematicPlacements.get(i);
                JsonObject objPlacement = placement.toJson();

                if (objPlacement != null)
                {
                    arr.add(objPlacement);

                    if (this.selectedPlacement == placement)
                    {
                        index = i;
                    }
                }
            }

            obj.add("placements", arr);

            if (index >= 0)
            {
                obj.add("selected", new JsonPrimitive(index));
            }
        }

        return obj;
    }

    public void loadFromJson(JsonObject obj)
    {
        this.schematicPlacements.clear();
        this.selectedPlacement = null;

        if (JsonUtils.hasArray(obj, "placements"))
        {
            JsonArray arr = obj.get("placements").getAsJsonArray();
            int index = JsonUtils.hasInteger(obj, "selected") ? obj.get("selected").getAsInt() : -1;
            final int size = arr.size();

            for (int i = 0; i < size; ++i)
            {
                JsonElement el = arr.get(i);

                if (el.isJsonObject())
                {
                    SchematicPlacement placement = SchematicPlacement.fromJson(el.getAsJsonObject());

                    if (placement != null)
                    {
                        this.schematicPlacements.add(placement);
                    }
                }
                else
                {
                    // Invalid data in the array, don't select an entry
                    index = -1;
                }
            }

            if (index >= 0 && index < this.schematicPlacements.size())
            {
                this.selectedPlacement = this.schematicPlacements.get(index);
            }
        }
    }
}
