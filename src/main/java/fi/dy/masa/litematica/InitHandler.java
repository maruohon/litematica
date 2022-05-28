package fi.dy.masa.litematica;

import net.minecraft.client.Minecraft;
import fi.dy.masa.malilib.config.JsonModConfig;
import fi.dy.masa.malilib.config.JsonModConfig.ConfigDataUpdater;
import fi.dy.masa.malilib.config.util.ConfigUpdateUtils.KeyBindSettingsResetter;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.HotkeyCallbacks;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.FileMigrationUtils;
import fi.dy.masa.litematica.event.ClientWorldChangeHandler;
import fi.dy.masa.litematica.event.RenderHandler;
import fi.dy.masa.litematica.gui.ConfigScreen;
import fi.dy.masa.litematica.input.LitematicaHotkeyProvider;
import fi.dy.masa.litematica.input.MouseScrollHandlerImpl;
import fi.dy.masa.litematica.network.SchematicSavePacketHandler;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.scheduler.ClientTickHandler;

public class InitHandler implements InitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        // Reset all KeyBindSettings when updating to the first post-malilib-refactor version
        ConfigDataUpdater updater = new KeyBindSettingsResetter(LitematicaHotkeyProvider.INSTANCE::getAllHotkeys, 0);
        Registry.CONFIG_MANAGER.registerConfigHandler(JsonModConfig.createJsonModConfig(Reference.MOD_INFO, Configs.CURRENT_VERSION, Configs.CATEGORIES, updater));

        Registry.CONFIG_SCREEN.registerConfigScreenFactory(Reference.MOD_INFO, ConfigScreen::create);
        Registry.CONFIG_TAB.registerConfigTabProvider(Reference.MOD_INFO, ConfigScreen::getConfigTabs);

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

        HotkeyCallbacks.init(Minecraft.getMinecraft());
        StatusInfoRenderer.init();

        DataManager.getAreaSelectionsBaseDirectory();
        DataManager.getSchematicsBaseDirectory();

        Registry.CLIENT_PACKET_CHANNEL_HANDLER.registerClientChannelHandler(SchematicSavePacketHandler.INSTANCE);
    }
}
