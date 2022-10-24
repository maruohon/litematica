package fi.dy.masa.litematica;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.dy.masa.litematica.config.Configs;

public class Litematica
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    public static void printDebug(String key, Object... args)
    {
        if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue())
        {
            logger.info(String.format(key, args));
        }
    }
}
