package fi.dy.masa.litematica.config.options;

import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import fi.dy.masa.litematica.config.interfaces.ConfigType;

public abstract class ConfigBase
{
    private final ConfigType type;
    private final String name;
    private String comment;

    public ConfigBase(ConfigType type, String name, String comment)
    {
        this.type = type;
        this.name = name;
        this.comment = comment;
    }

    public ConfigType getType()
    {
        return this.type;
    }

    public String getName()
    {
        return this.name;
    }

    @Nullable
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public abstract String getStringValue();

    public abstract void setValueFromString(String value);

    public abstract void setValueFromJsonElement(JsonElement element);

    public abstract JsonElement getAsJsonElement();
}
