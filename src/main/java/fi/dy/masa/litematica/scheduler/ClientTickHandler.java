package fi.dy.masa.litematica.scheduler;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.render.DebugScreenMessages;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(Minecraft mc)
    {
        if (mc.world != null && mc.player != null)
        {
            InputHandler.onTick();
            WorldUtils.easyPlaceOnUseTick(mc);
            DebugScreenMessages.update(mc);

            DataManager.getSchematicPlacementManager().processQueuedChunks();
            TaskScheduler.getInstance().runTasks();
        }
    }
}
