package fi.dy.masa.litematica.config.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.config.interfaces.ConfigType;

public class ConfigString extends ConfigBase
{
    private final String defaultValue;
    private String value;

    public ConfigString(String name, String defaultValue, String comment)
    {
        super(ConfigType.STRING, name, comment);

        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getValue()
    {
        return this.value;
    }

    public String getDefaultValue()
    {
        return this.defaultValue;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public String getStringValue()
    {
        return this.value;
    }

    @Override
    public void setValueFromString(String value)
    {
        this.value = value;
    }

    @Override
    public void setValueFromJsonElement(JsonElement element)
    {
        try
        {
            if (element.isJsonPrimitive())
            {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                this.value = primitive.getAsString();
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
