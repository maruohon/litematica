package fi.dy.masa.litematica.scheduler;

import net.minecraft.client.Minecraft;

import malilib.util.game.wrap.GameUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.input.MouseScrollHandlerImpl;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifierManager;

public class ClientTickHandler implements malilib.event.ClientTickHandler
{
    protected int tickCounter;

    @Override
    public void onClientTick()
    {
        Minecraft mc = GameUtils.getClient();
        MouseScrollHandlerImpl.onTick(mc);
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
