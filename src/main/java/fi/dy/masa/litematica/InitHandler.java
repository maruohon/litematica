package fi.dy.masa.litematica;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.HotkeyCallbacks;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.FileMigrationUtils;
import fi.dy.masa.litematica.event.ClientWorldChangeHandler;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.event.RenderHandler;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.scheduler.ClientTickHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.dispatch.ClientWorldChangeEventDispatcher;
import fi.dy.masa.malilib.event.dispatch.InputEventDispatcher;
import fi.dy.masa.malilib.event.dispatch.RenderEventDispatcher;
import fi.dy.masa.malilib.event.dispatch.TickEventDispatcher;
import fi.dy.masa.malilib.event.IInitializationHandler;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.INSTANCE.registerConfigHandler(Reference.MOD_ID, new Configs());

        InputEventDispatcher.getKeyBindManager().registerKeyBindProvider(InputHandler.getInstance());
        InputEventDispatcher.getInputManager().registerMouseInputHandler(InputHandler.getInstance());

        RenderHandler renderer = new RenderHandler();
        RenderEventDispatcher.INSTANCE.registerGameOverlayRenderer(renderer);
        RenderEventDispatcher.INSTANCE.registerWorldPostRenderer(renderer);

        TickEventDispatcher.INSTANCE.registerClientTickHandler(new ClientTickHandler());

        ClientWorldChangeEventDispatcher.INSTANCE.registerClientWorldChangeHandler(new ClientWorldChangeHandler());

        FileMigrationUtils.tryMigrateOldPerWorldData();
        FileMigrationUtils.tryMigrateOldAreaSelections();

        HotkeyCallbacks.init(Minecraft.getMinecraft());
        StatusInfoRenderer.init();

        DataManager.getAreaSelectionsBaseDirectory();
        DataManager.getSchematicsBaseDirectory();
    }
}
