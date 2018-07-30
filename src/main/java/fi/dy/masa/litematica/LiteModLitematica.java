package fi.dy.masa.litematica;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mumfrey.liteloader.Configurable;
import com.mumfrey.liteloader.InitCompleteListener;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.gui.LitematicaConfigPanel;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.event.KeyCallbacks;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.minecraft.client.Minecraft;

public class LiteModLitematica implements LiteMod, Configurable, InitCompleteListener, Tickable
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public String getName()
    {
        return Reference.MOD_NAME;
    }

    @Override
    public String getVersion()
    {
        return Reference.MOD_VERSION;
    }

    @Override
    public Class<? extends ConfigPanel> getConfigPanelClass()
    {
        return LitematicaConfigPanel.class;
    }

    @Override
    public void init(File configPath)
    {
        Configs.loadFromFile();
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        InputEventHandler.getInstance().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInstance().registerKeyboardInputHandler(InputHandler.getInstance());
        InputEventHandler.getInstance().registerMouseInputHandler(InputHandler.getInstance());
    }

    @Override
    public void onInitCompleted(Minecraft mc, LiteLoader loader)
    {
        KeyCallbacks.init(mc);
    }

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath)
    {
    }

    @Override
    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock)
    {
        InputHandler.onTick();
    }

    public static void logInfo(String message, Object... args)
    {
        if (Configs.Generic.VERBOSE_LOGGING.getBooleanValue())
        {
            logger.info(message, args);
        }
    }
}
