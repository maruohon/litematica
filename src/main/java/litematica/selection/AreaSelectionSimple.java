package litematica.selection;

import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import malilib.util.data.json.JsonUtils;
import malilib.util.position.BlockPos;

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
    public boolean setSelectedSelectionBox(String name)
    {
        // NO-OP
        return false;
    }

    @Override
    @Nullable
    public String createNewSelectionBox(BlockPos pos1, String nameIn)
    {
        // NO-OP
        return null;
    }

    @Override
    public boolean addSelectionBox(SelectionBox box, boolean replace)
    {
        // NO-OP
        return false;
    }

    @Override
    public void removeAllSelectionBoxes()
    {
        // NO-OP
    }

    @Override
    public boolean removeSelectionBox(String name)
    {
        // NO-OP
        return false;
    }

    @Override
    public boolean removeSelectedBox()
    {
        // NO-OP
        return false;
    }

    private void createDefaultBoxIfNeeded()
    {
        if (this.selectionBoxes.size() != 1)
        {
            this.selectionBoxes.clear();
            SelectionBox box = new SelectionBox(BlockPos.ORIGIN, BlockPos.ORIGIN, this.getName());
            this.selectionBoxes.put(box.getName(), box);
            this.selectedBoxName = box.getName();
        }
        else if (this.selectedBoxName == null || this.selectionBoxes.get(this.selectedBoxName) == null)
        {
            this.selectedBoxName = this.selectionBoxes.keySet().iterator().next();
        }
    }

    @Override
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
                    SelectionBox box = SelectionBox.fromJson(el.getAsJsonObject());

                    if (box != null)
                    {
                        area.selectionBoxes.put(box.getName(), box);
                        area.selectedBoxName = box.getName();
                    }
                }
            }
        }

        if (JsonUtils.hasString(obj, "name"))
        {
            area.setName(obj.get("name").getAsString());
        }

        BlockPos pos = JsonUtils.getBlockPos(obj, "origin");

        if (pos != null)
        {
            area.setManualOrigin(pos);
        }
        else
        {
            area.updateAutomaticOrigin();
        }

        // Make sure the simple area has exactly one box, and that it's selected
        area.createDefaultBoxIfNeeded();

        return area;
    }
}
