package fi.dy.masa.litematica.selection;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.util.math.BlockPos;

import malilib.util.data.json.JsonUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;

public class SelectionBox extends Box
{
    private String name = "Unnamed";
    private Corner selectedCorner = Corner.NONE;

    public SelectionBox()
    {
        super();
    }

    public SelectionBox(@Nullable BlockPos pos1, @Nullable BlockPos pos2, String name)
    {
        super(pos1, pos2);

        this.name = name;
    }

    @Override
    public SelectionBox copy()
    {
        SelectionBox box = new SelectionBox(this.pos1, this.pos2, this.name);
        box.setSelectedCorner(this.selectedCorner);
        return box;
    }

    public String getName()
    {
        return this.name;
    }

    public Corner getSelectedCorner()
    {
        return this.selectedCorner;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setSelectedCorner(Corner corner)
    {
        this.selectedCorner = corner;
    }

    @Override
    @Nullable
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

        if (obj != null)
        {
            obj.add("name", new JsonPrimitive(this.name));
        }

        return obj;
    }

    @Nullable
    public static SelectionBox fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "name"))
        {
            @Nullable BlockPos pos1 = JsonUtils.blockPosFromJson(obj, "pos1");
            @Nullable BlockPos pos2 = JsonUtils.blockPosFromJson(obj, "pos2");

            if (pos1 != null || pos2 != null)
            {
                return new SelectionBox(pos1, pos2, obj.get("name").getAsString());
            }
        }

        return null;
    }
}
