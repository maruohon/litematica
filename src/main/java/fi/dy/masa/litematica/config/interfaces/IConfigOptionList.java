package fi.dy.masa.litematica.config.interfaces;

public interface IConfigOptionList
{
    IConfigOptionListEntry getOptionListValue();

    void setOptionListValue(IConfigOptionListEntry value);
}