package fi.dy.masa.litematica;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mumfrey.liteloader.Configurable;
import com.mumfrey.liteloader.InitCompleteListener;
import com.mumfrey.liteloader.JoinGameListener;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.ScreenshotListener;
import com.mumfrey.liteloader.ShutdownListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderEventBroker.ReturnValue;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.gui.LitematicaConfigPanel;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputEventHandler;
import fi.dy.masa.litematica.event.KeyCallbacks;
import fi.dy.masa.malilib.hotkeys.KeybindEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.util.text.ITextComponent;

public class LiteModLitematica implements LiteMod, Configurable, InitCompleteListener, JoinGameListener, ScreenshotListener, ShutdownListener, Tickable
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
        Configs.load();
        KeybindEventHandler.getInstance().registerKeyEventHandler(InputEventHandler.getInstance());
    }

    @Override
    public void onInitCompleted(Minecraft minecraft, LiteLoader loader)
    {
        KeyCallbacks.init();
    }

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath)
    {
    }

    @Override
    public void onJoinGame(INetHandler netHandler, SPacketJoinGame joinGamePacket, ServerData serverData, RealmsServer realmsServer)
    {
        System.out.printf("onJoinGame -> DataManager.load()\n");
        DataManager.load();
    }

    @Override
    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock)
    {
        InputEventHandler.onTick();
    }

    @Override
    public void onShutDown()
    {
        Configs.save();
    }

    @Override
    public boolean onSaveScreenshot(String screenshotName, int width, int height, Framebuffer fbo, ReturnValue<ITextComponent> message)
    {
        return true;
    }

    public static void logInfo(String message, Object... args)
    {
        if (Configs.Generic.VERBOSE_LOGGING.getBooleanValue())
        {
            logger.info(message, args);
        }
    }
}
