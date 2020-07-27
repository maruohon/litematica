package fi.dy.masa.litematica;

import java.io.File;
import com.mumfrey.liteloader.Configurable;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import fi.dy.masa.litematica.config.gui.LitematicaConfigPanel;
import fi.dy.masa.malilib.event.dispatch.InitializationDispatcher;

public class LiteModLitematica implements LiteMod, Configurable
{
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
        InitializationDispatcher.INSTANCE.registerInitializationHandler(new InitHandler());
    }

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath)
    {
    }
}
