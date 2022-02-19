package fi.dy.masa.litematica;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.HotkeyCallbacks;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.FileMigrationUtils;
import fi.dy.masa.litematica.event.ClientWorldChangeHandler;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.event.RenderHandler;
import fi.dy.masa.litematica.gui.ConfigScreen;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.scheduler.ClientTickHandler;
import fi.dy.masa.malilib.config.BaseModConfig;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.registry.Registry;

public class InitHandler implements InitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        Registry.CONFIG_MANAGER.registerConfigHandler(BaseModConfig.createDefaultModConfig(Reference.MOD_INFO, 1, Configs.CATEGORIES));
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(Reference.MOD_INFO, ConfigScreen::create);
        Registry.CONFIG_TAB.registerConfigTabProvider(Reference.MOD_INFO, ConfigScreen::getConfigTabs);

        Registry.HOTKEY_MANAGER.registerHotkeyProvider(InputHandler.getInstance());
        Registry.INPUT_DISPATCHER.registerMouseInputHandler(InputHandler.getInstance());

        RenderHandler renderer = new RenderHandler();
        Registry.RENDER_EVENT_DISPATCHER.registerGameOverlayRenderer(renderer);
        Registry.RENDER_EVENT_DISPATCHER.registerWorldPostRenderer(renderer);

        Registry.TICK_EVENT_DISPATCHER.registerClientTickHandler(new ClientTickHandler());

        Registry.CLIENT_WORLD_CHANGE_EVENT_DISPATCHER.registerClientWorldChangeHandler(new ClientWorldChangeHandler());

        FileMigrationUtils.tryMigrateOldPerWorldData();
        FileMigrationUtils.tryMigrateOldAreaSelections();

        HotkeyCallbacks.init(Minecraft.getMinecraft());
        StatusInfoRenderer.init();

        DataManager.getAreaSelectionsBaseDirectory();
        DataManager.getSchematicsBaseDirectory();
    }
}
