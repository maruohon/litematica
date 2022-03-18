package fi.dy.masa.litematica.scheduler;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.render.DebugScreenMessages;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifierManager;

public class ClientTickHandler implements fi.dy.masa.malilib.event.ClientTickHandler
{
    protected int tickCounter;

    @Override
    public void onClientTick(Minecraft mc)
    {
        InputHandler.onTick(mc);
        DebugScreenMessages.update(mc);
        DataManager.getRenderLayerRange().followPlayerIfEnabled(mc.player);
        DataManager.getSchematicPlacementManager().processQueuedChunks();
        TaskScheduler.getInstanceClient().runTasks();

        if ((this.tickCounter) % 10 == 0)
        {
            SchematicVerifierManager.INSTANCE.scheduleReChecks();
        }

        ++this.tickCounter;
    }
}
