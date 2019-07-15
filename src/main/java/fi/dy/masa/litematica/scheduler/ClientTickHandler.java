package fi.dy.masa.litematica.scheduler;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.MinecraftClient;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (mc.world != null && mc.player != null)
        {
            InputHandler.onTick(mc);
            WorldUtils.easyPlaceOnUseTick(mc);

            if (Configs.Generic.LAYER_MODE_DYNAMIC.getBooleanValue())
            {
                DataManager.getRenderLayerRange().setToPosition(mc.player);
            }

            DataManager.getSchematicPlacementManager().processQueuedChunks();
            TaskScheduler.getInstanceClient().runTasks();
        }
    }
}
