package fi.dy.masa.litematica.config.interfaces;

public interface IConfig extends INamed
{
    ConfigType getType();

    String getStringValue();
}
