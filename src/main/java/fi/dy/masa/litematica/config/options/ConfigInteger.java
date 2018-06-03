package fi.dy.masa.litematica.config.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.config.interfaces.ConfigType;
import net.minecraft.util.math.MathHelper;

public class ConfigInteger extends ConfigBase
{
    private final int minValue;
    private final int maxValue;
    private final int defaultValue;
    private int value;

    public ConfigInteger(String name, int defaultValue, int minValue, int maxValue, String comment)
    {
        super(ConfigType.INTEGER, name, comment);

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public int getValue()
    {
        return this.value;
    }

    public int getDefaultValue()
    {
        return this.defaultValue;
    }

    public void setValue(int value)
    {
        this.value = MathHelper.clamp(value, this.minValue, this.maxValue);
    }

    @Override
    public String getStringValue()
    {
        return String.valueOf(this.value);
    }

    @Override
    public void setValueFromString(String value)
    {
        try
        {
            this.setValue(Integer.parseInt(value));
        }
        catch (Exception e)
        {
            LiteModLitematica.logger.warn("Failed to set config value for {} from the string '{}'", this.getName(), value, e);
        }
    }

    @Override
    public void setValueFromJsonElement(JsonElement element)
    {
        try
        {
            if (element.isJsonPrimitive())
            {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                this.setValue(primitive.getAsInt());
            }
            else
            {
                LiteModLitematica.logger.warn("Failed to set config value for '{}' from the JSON element '{}'", this.getName(), element);
            }
        }
        catch (Exception e)
        {
            LiteModLitematica.logger.warn("Failed to set config value for '{}' from the JSON element '{}'", this.getName(), element, e);
        }
    }

    @Override
    public JsonElement getAsJsonElement()
    {
        return new JsonPrimitive(this.value);
    }
}
