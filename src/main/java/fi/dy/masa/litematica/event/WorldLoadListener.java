package fi.dy.masa.litematica.event;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadPre(@Nullable WorldClient world, Minecraft mc)
    {
        // Save the settings before the integrated server gets shut down
        if (Minecraft.getMinecraft().world != null)
        {
            DataManager.save();
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable WorldClient world, Minecraft mc)
    {
        SchematicWorldHandler.recreateSchematicWorld(world == null);

        if (world != null)
        {
            DataManager.load();
        }
        else
        {
            TaskScheduler.getInstance().clearTasks();
            SchematicHolder.getInstance().clearLoadedSchematics();
            InfoHud.getInstance().reset(); // remove the line providers and clear the data
        }
    }
}
