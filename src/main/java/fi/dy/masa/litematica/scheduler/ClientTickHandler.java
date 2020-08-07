package fi.dy.masa.litematica.scheduler;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.render.DebugScreenMessages;

public class ClientTickHandler implements fi.dy.masa.malilib.event.ClientTickHandler
{
    @Override
    public void onClientTick(Minecraft mc)
    {
        if (mc.world != null && mc.player != null)
        {
            InputHandler.onTick(mc);
            DebugScreenMessages.update(mc);
            DataManager.getRenderLayerRange().followPlayerIfEnabled(mc.player);
            DataManager.getSchematicPlacementManager().processQueuedChunks();
            TaskScheduler.getInstanceClient().runTasks();
        }
    }
}
