package fi.dy.masa.litematica.config.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.config.interfaces.ConfigType;
import net.minecraft.util.math.MathHelper;

public class ConfigDouble extends ConfigBase
{
    private final double minValue;
    private final double maxValue;
    private final double defaultValue;
    private double value;

    public ConfigDouble(String name, double defaultValue, double minValue, double maxValue, String comment)
    {
        super(ConfigType.DOUBLE, name, comment);

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public double getValue()
    {
        return this.value;
    }

    public double getDefaultValue()
    {
        return this.defaultValue;
    }

    public void setValue(double value)
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
            this.setValue(Double.parseDouble(value));
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
                this.setValue(primitive.getAsDouble());
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
