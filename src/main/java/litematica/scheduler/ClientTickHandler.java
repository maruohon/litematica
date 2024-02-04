package litematica.scheduler;

import malilib.gui.util.GuiUtils;
import malilib.util.game.wrap.GameWrap;
import litematica.data.DataManager;
import litematica.schematic.verifier.SchematicVerifierManager;
import litematica.util.EasyPlaceUtils;

public class ClientTickHandler implements malilib.event.ClientTickHandler
{
    protected int tickCounter;

    @Override
    public void onClientTick()
    {
        if (GameWrap.getClientPlayer() == null || GameWrap.getClientWorld() == null)
        {
            return;
        }

        DataManager.getRenderLayerRange().followPlayerIfEnabled(GameWrap.getClientPlayer());
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
