package fi.dy.masa.litematica.tool;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.util.JsonUtils;

public class ToolModeData
{
    public static final DeleteModeData DELETE = new DeleteModeData();

    public static class DeleteModeData
    {
        private boolean usePlacement;

        public boolean getUsePlacement()
        {
            return this.usePlacement;
        }

        public void toggleUsePlacement()
        {
            this.usePlacement = ! this.usePlacement;
        }

        public JsonObject toJson()
        {
            JsonObject obj = new JsonObject();
            obj.add("use_placement", new JsonPrimitive(this.usePlacement));
            return obj;
        }

        public void fromJson(JsonObject obj)
        {
            this.usePlacement = JsonUtils.getBoolean(obj, "use_placement");
        }
    }
}
