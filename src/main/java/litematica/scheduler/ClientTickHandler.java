package litematica.scheduler;

import malilib.gui.util.GuiUtils;
import malilib.util.game.wrap.GameUtils;
import litematica.data.DataManager;
import litematica.schematic.verifier.SchematicVerifierManager;
import litematica.util.EasyPlaceUtils;

public class ClientTickHandler implements malilib.event.ClientTickHandler
{
    protected int tickCounter;

    @Override
    public void onClientTick()
    {
        if (GameUtils.getClientPlayer() == null || GameUtils.getClientWorld() == null)
        {
            return;
        }

        DataManager.getRenderLayerRange().followPlayerIfEnabled(GameUtils.getClientPlayer());
        DataManager.getSchematicPlacementManager().processQueuedChunks();
        TaskScheduler.getInstanceClient().runTasks();

        if ((this.tickCounter) % 10 == 0)
        {
            SchematicVerifierManager.INSTANCE.scheduleReChecks();
        }

        if (GuiUtils.noScreenOpen())
        {
            EasyPlaceUtils.easyPlaceOnUseTick();
        }

        ++this.tickCounter;
    }
}
