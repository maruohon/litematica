package fi.dy.masa.litematica;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.rift.listener.client.ClientTickable;
import org.dimdev.riftloader.listener.InitializationListener;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.event.KeyCallbacks;
import fi.dy.masa.litematica.event.RenderHandler;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import net.minecraft.client.Minecraft;

public class Litematica implements ClientTickable, InitializationListener
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitialization()
    {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.litematica.json");

        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    @Override
    public void clientTick(Minecraft mc)
    {
        InputHandler.onTick();
        WorldUtils.easyPlaceOnUseTick(mc);
    }

    private static class InitHandler implements IInitializationHandler
    {
        @Override
        public void registerModHandlers()
        {
            ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());

            InputEventHandler.getInstance().registerKeybindProvider(InputHandler.getInstance());
            InputEventHandler.getInstance().registerKeyboardInputHandler(InputHandler.getInstance());
            InputEventHandler.getInstance().registerMouseInputHandler(InputHandler.getInstance());

            IRenderer renderer = new RenderHandler();
            RenderEventHandler.getInstance().registerGameOverlayRenderer(renderer);
            RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

            DataManager.getAreaSelectionsBaseDirectory();
            DataManager.getSchematicsBaseDirectory();
            KeyCallbacks.init();
        }
    }

    public static void logInfo(String message, Object... args)
    {
        if (Configs.Generic.VERBOSE_LOGGING.getBooleanValue())
        {
            logger.info(message, args);
        }
    }
}
