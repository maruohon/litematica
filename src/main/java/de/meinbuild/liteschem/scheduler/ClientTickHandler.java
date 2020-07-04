package de.meinbuild.liteschem.scheduler;

import de.meinbuild.liteschem.data.DataManager;
import de.meinbuild.liteschem.event.InputHandler;
import de.meinbuild.liteschem.util.WorldUtils;
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

            DataManager.getSchematicPlacementManager().processQueuedChunks();
            TaskScheduler.getInstanceClient().runTasks();
        }
    }
}
