package litematica;

import malilib.config.JsonModConfig;
import malilib.config.JsonModConfig.ConfigDataUpdater;
import malilib.config.util.ConfigUpdateUtils.KeyBindSettingsResetter;
import malilib.event.InitializationHandler;
import malilib.registry.Registry;
import litematica.config.Configs;
import litematica.config.HotkeyCallbacks;
import litematica.data.FileMigrationUtils;
import litematica.event.ClientWorldChangeHandler;
import litematica.event.RenderHandler;
import litematica.gui.ConfigScreen;
import litematica.input.LitematicaHotkeyProvider;
import litematica.input.MouseScrollHandlerImpl;
import litematica.network.SchematicSavePacketHandler;
import litematica.render.infohud.StatusInfoRenderer;
import litematica.scheduler.ClientTickHandler;
import litematica.util.LitematicaDirectories;

public class InitHandler implements InitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        // Reset all KeyBindSettings when updating to the first post-malilib-refactor version
        ConfigDataUpdater updater = new KeyBindSettingsResetter(LitematicaHotkeyProvider.INSTANCE::getAllHotkeys, 0);
        Registry.CONFIG_MANAGER.registerConfigHandler(JsonModConfig.createJsonModConfig(Reference.MOD_INFO, Configs.CURRENT_VERSION, Configs.CATEGORIES, updater));

        Registry.CONFIG_SCREEN.registerConfigScreenFactory(Reference.MOD_INFO, ConfigScreen::create);
        Registry.CONFIG_TAB.registerConfigTabSupplier(Reference.MOD_INFO, ConfigScreen::getConfigTabs);

        Registry.HOTKEY_MANAGER.registerHotkeyProvider(new LitematicaHotkeyProvider());
        Registry.INPUT_DISPATCHER.registerMouseScrollHandler(new MouseScrollHandlerImpl());

        RenderHandler renderer = new RenderHandler();
        Registry.RENDER_EVENT_DISPATCHER.registerGameOverlayRenderer(renderer);
        Registry.RENDER_EVENT_DISPATCHER.registerWorldPostRenderer(renderer);

        Registry.TICK_EVENT_DISPATCHER.registerClientTickHandler(new ClientTickHandler());

        Registry.CLIENT_WORLD_CHANGE_EVENT_DISPATCHER.registerClientWorldChangeHandler(new ClientWorldChangeHandler());

        Configs.init();
        FileMigrationUtils.tryMigrateOldPerWorldData();
        FileMigrationUtils.tryMigrateOldAreaSelections();

        HotkeyCallbacks.init();
        StatusInfoRenderer.init();

        // This creates the directories if they don't exist yet
        LitematicaDirectories.getAreaSelectionsBaseDirectory();
        LitematicaDirectories.getSchematicsBaseDirectory();

        Registry.CLIENT_PACKET_CHANNEL_HANDLER.registerClientChannelHandler(SchematicSavePacketHandler.INSTANCE);
    }
}
