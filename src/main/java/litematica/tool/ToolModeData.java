package litematica.tool;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import malilib.util.data.json.JsonUtils;

public class ToolModeData
{
    // TODO change these into a nicer system with something like INamedJsonSerializable
    public static final ActionTargetData DELETE = new ActionTargetData();
    public static final ActionTargetData UPDATE_BLOCKS = new ActionTargetData();

    public static class ActionTargetData
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
