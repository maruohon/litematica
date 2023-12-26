package litematica;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import litematica.config.Configs;

public class Litematica
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public static void printDebug(String key, Object... args)
    {
        if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue())
        {
            LOGGER.info(String.format(key, args));
        }
    }
}
