package fi.dy.masa.litematica.data;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.config.KeyCallbacks;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase.InfoType;
import fi.dy.masa.litematica.gui.interfaces.IMessageConsumer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class SchematicPlacementManager
{
    private final List<SchematicPlacement> schematicPlacements = new ArrayList<>();
    @Nullable
    private SchematicPlacement selectedPlacement;
    private boolean originSelected;

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
            SchematicPlacement placement = this.schematicPlacements.get(i);

            if (placement.getSchematic() == schematic)
            {
                if (this.selectedPlacement == placement)
                {
                    this.selectedPlacement = null;
                    this.originSelected = false;
                }

                this.schematicPlacements.remove(i);
                placement.onRemoved();
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

    public boolean isOriginSelected()
    {
        return this.originSelected;
    }

    public void setSelectedSchematicPlacement(@Nullable SchematicPlacement placement)
    {
        if (placement == null || this.schematicPlacements.contains(placement))
        {
            this.selectedPlacement = placement;
            OverlayRenderer.getInstance().updatePlacementCache();
        }
    }

    public boolean changeSelection(World world, Entity entity, int maxDistance)
    {
        if (this.schematicPlacements.size() > 0)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, maxDistance);

            SchematicPlacement placement = this.getSelectedSchematicPlacement();

            if (placement != null)
            {
                placement.setSelectedSubRegionName(null);
            }

            if (trace.getHitType() == HitType.PLACEMENT_SUBREGION)
            {
                this.setSelectedSchematicPlacement(trace.getHitSchematicPlacement());
                this.getSelectedSchematicPlacement().setSelectedSubRegionName(trace.getHitSchematicPlacementRegionName());
                this.originSelected = false;
                return true;
            }
            else if (trace.getHitType() == HitType.PLACEMENT_ORIGIN)
            {
                this.setSelectedSchematicPlacement(trace.getHitSchematicPlacement());
                this.getSelectedSchematicPlacement().setSelectedSubRegionName(null);
                this.originSelected = true;
                return true;
            }
            else if (trace.getHitType() == HitType.MISS)
            {
                this.setSelectedSchematicPlacement(null);
                this.originSelected = false;
                return true;
            }
        }

        return false;
    }

    public void setPositionOfCurrentSelectionToRayTrace(Minecraft mc, double maxDistance)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            boolean movingBox = schematicPlacement.getSelectedSubRegionPlacement() != null;
            boolean movingOrigin = this.originSelected;

            if (movingBox || movingOrigin)
            {
                RayTraceResult trace = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, false, maxDistance);

                if (trace.typeOfHit != RayTraceResult.Type.BLOCK)
                {
                    return;
                }

                BlockPos pos = trace.getBlockPos();

                // Sneaking puts the position inside the targeted block, not sneaking puts it against the targeted face
                if (mc.player.isSneaking() == false)
                {
                    pos = pos.offset(trace.sideHit);
                }

                if (movingBox)
                {
                    schematicPlacement.moveSubRegionTo(schematicPlacement.getSelectedSubRegionName(), pos);

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    KeyCallbacks.printMessage(mc, "litematica.message.placement.moved_subregion_to", posStr);
                }
                // Moving the origin point
                else
                {
                    BlockPos old = schematicPlacement.getOrigin();
                    schematicPlacement.setOrigin(pos);
                    String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                    String posStrNew = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    KeyCallbacks.printMessage(mc, "litematica.message.placement.moved_placement_origin", posStrOld, posStrNew);
                }

                schematicPlacement.updateRenderers();
            }
        }
    }

    public void nudgePositionOfCurrentSelection(EnumFacing direction, int amount)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            Placement placement = schematicPlacement.getSelectedSubRegionPlacement();

            if (placement != null)
            {
                // getPos returns a relative position, but moveSubRegionTo takes an absolute position...
                BlockPos old = placement.getPos().add(schematicPlacement.getOrigin());
                schematicPlacement.moveSubRegionTo(placement.getName(), old.offset(direction, amount));
            }
            // Moving the origin point
            else if (this.originSelected)
            {
                BlockPos old = schematicPlacement.getOrigin();
                schematicPlacement.setOrigin(old.offset(direction, amount));
            }
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.schematicPlacements.size() > 0)
        {
            JsonArray arr = new JsonArray();
            int selectedIndex = 0;
            boolean indexValid = false;

            for (int i = 0; i < this.schematicPlacements.size(); ++i)
            {
                SchematicPlacement placement = this.schematicPlacements.get(i);
                JsonObject objPlacement = placement.toJson();

                if (objPlacement != null)
                {
                    arr.add(objPlacement);

                    if (this.selectedPlacement == placement)
                    {
                        indexValid = true;
                    }
                    else if (indexValid == false)
                    {
                        selectedIndex++;
                    }
                }
            }

            obj.add("placements", arr);

            if (indexValid)
            {
                obj.add("selected", new JsonPrimitive(selectedIndex));
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
                        SchematicHolder.getInstance().addSchematic(placement.getSchematic(), placement.getSchematic().getMetadata().getName());
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

        OverlayRenderer.getInstance().updatePlacementCache();
    }
}
