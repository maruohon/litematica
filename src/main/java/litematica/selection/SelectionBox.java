package litematica.selection;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import malilib.util.data.json.JsonUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.Direction;

public class SelectionBox extends CornerDefinedBox
{
    protected String name;
    protected BoxCorner selectedCorner = BoxCorner.NONE;

    public SelectionBox()
    {
        this(BlockPos.ORIGIN, BlockPos.ORIGIN, "Unnamed");
    }

    public SelectionBox(BlockPos pos1, BlockPos pos2, String name)
    {
        super(pos1, pos2);

        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isCornerSelected(BoxCorner corner)
    {
        return this.selectedCorner == corner;
    }

    public BoxCorner getSelectedCorner()
    {
        return this.selectedCorner;
    }

    public void setSelectedCorner(BoxCorner corner)
    {
        this.selectedCorner = corner;
    }

    public void offsetSelectedCorner(Direction direction, int amount)
    {
        BoxCorner corner = this.selectedCorner;

        if (corner == BoxCorner.NONE || corner == BoxCorner.CORNER_1)
        {
            this.setCorner1(this.corner1.offset(direction, amount));
        }

        if (corner == BoxCorner.NONE || corner == BoxCorner.CORNER_2)
        {
            this.setCorner2(this.corner2.offset(direction, amount));
        }
    }

    @Override
    public SelectionBox copy()
    {
        SelectionBox box = new SelectionBox(this.corner1, this.corner2, this.name);
        box.setSelectedCorner(this.selectedCorner);
        return box;
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
            BlockPos pos1 = JsonUtils.getBlockPos(obj, "pos1");
            BlockPos pos2 = JsonUtils.getBlockPos(obj, "pos2");

            if (pos1 != null && pos2 != null)
            {
                return new SelectionBox(pos1, pos2, obj.get("name").getAsString());
            }
        }

        return null;
    }

    public static void fromJson(JsonObject obj, Consumer<SelectionBox> boxConsumer)
    {
        SelectionBox box = fromJson(obj);

        if (box != null)
        {
            boxConsumer.accept(box);
        }
    }
}
