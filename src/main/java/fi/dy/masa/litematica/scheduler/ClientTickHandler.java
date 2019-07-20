package fi.dy.masa.litematica.scheduler;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(Minecraft mc)
    {
        if (mc.world != null && mc.player != null)
        {
            InputHandler.onTick(mc);

            if (Configs.Generic.LAYER_MODE_DYNAMIC.getBooleanValue())
            {
                DataManager.getRenderLayerRange().setToPosition(mc.player);
            }

            DataManager.getSchematicPlacementManager().processQueuedChunks();
            TaskScheduler.getInstanceClient().runTasks();
        }
    }
}
