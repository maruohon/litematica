package litematica;

import net.ornithemc.osl.entrypoints.api.client.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import malilib.registry.Registry;
import litematica.config.Configs;

public class Litematica implements ClientModInitializer
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public static void printDebug(String key, Object... args)
    {
        if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue())
        {
            LOGGER.info(String.format(key, args));
        }
    }

    @Override
    public void initClient()
    {
        Registry.INITIALIZATION_DISPATCHER.registerInitializationHandler(new InitHandler());
    }
}
