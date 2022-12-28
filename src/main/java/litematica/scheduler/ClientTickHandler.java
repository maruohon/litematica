package litematica.scheduler;

import malilib.util.game.wrap.GameUtils;
import litematica.data.DataManager;
import litematica.input.MouseScrollHandlerImpl;
import litematica.schematic.verifier.SchematicVerifierManager;

public class ClientTickHandler implements malilib.event.ClientTickHandler
{
    protected int tickCounter;

    @Override
    public void onClientTick()
    {
        MouseScrollHandlerImpl.onTick();
        DataManager.getRenderLayerRange().followPlayerIfEnabled(GameUtils.getClientPlayer());
        DataManager.getSchematicPlacementManager().processQueuedChunks();
        TaskScheduler.getInstanceClient().runTasks();

        if ((this.tickCounter) % 10 == 0)
        {
            SchematicVerifierManager.INSTANCE.scheduleReChecks();
        }

        ++this.tickCounter;
    }
}
