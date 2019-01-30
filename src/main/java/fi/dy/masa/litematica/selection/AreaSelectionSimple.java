package fi.dy.masa.litematica.selection;

import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.util.math.BlockPos;

public class AreaSelectionSimple extends AreaSelection
{
    public AreaSelectionSimple(boolean createDefaultBox)
    {
        if (createDefaultBox)
        {
            this.createDefaultBoxIfNeeded();
        }
    }

    @Override
    public boolean setSelectedSubRegionBox(String name)
    {
        // NO-OP
        return false;
    }

    @Override
    @Nullable
    public String createNewSubRegionBox(BlockPos pos1, String nameIn)
    {
        // NO-OP
        return null;
    }

    @Override
    public boolean addSubRegionBox(Box box, boolean replace)
    {
        // NO-OP
        return false;
    }

    @Override
    public void removeAllSubRegionBoxes()
    {
        // NO-OP
    }

    @Override
    public boolean removeSubRegionBox(String name)
    {
        // NO-OP
        return false;
    }

    @Override
    public boolean removeSelectedSubRegionBox()
    {
        // NO-OP
        return false;
    }

    private void createDefaultBoxIfNeeded()
    {
        if (this.subRegionBoxes.size() != 1)
        {
            this.subRegionBoxes.clear();
            Box box = new Box(BlockPos.ORIGIN, BlockPos.ORIGIN, this.getName());
            this.subRegionBoxes.put(box.getName(), box);
            this.currentBox = box.getName();
        }
        else if (this.currentBox == null || this.subRegionBoxes.get(this.currentBox) == null)
        {
            this.currentBox = this.subRegionBoxes.keySet().iterator().next();
        }
    }

    public AreaSelectionSimple copy()
    {
        return fromJson(this.toJson());
    }

    public static AreaSelectionSimple fromJson(JsonObject obj)
    {
        AreaSelectionSimple area = new AreaSelectionSimple(false);

        if (JsonUtils.hasArray(obj, "boxes"))
        {
            JsonArray arr = obj.get("boxes").getAsJsonArray();

            if (arr.size() > 0)
            {
                // The simple area will only have one box
                JsonElement el = arr.get(0);

                if (el.isJsonObject())
                {
                    Box box = Box.fromJson(el.getAsJsonObject());

                    if (box != null)
                    {
                        area.subRegionBoxes.put(box.getName(), box);
                        area.currentBox = box.getName();
                    }
                }
            }
        }

        if (JsonUtils.hasString(obj, "name"))
        {
            area.setName(obj.get("name").getAsString());
        }

        BlockPos pos = JsonUtils.blockPosFromJson(obj, "origin");

        if (pos != null)
        {
            area.setExplicitOrigin(pos);
        }
        else
        {
            area.updateCalculatedOrigin();
        }

        // Make sure the simple area has exactly one box, and that it's selected
        area.createDefaultBoxIfNeeded();

        return area;
    }
}
