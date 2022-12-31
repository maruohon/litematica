package litematica.scheduler.tasks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import litematica.materials.IMaterialList;
import litematica.selection.AreaSelection;

public class TaskCountBlocksArea extends TaskCountBlocksMaterialList
{
    protected final AreaSelection selection;

    public TaskCountBlocksArea(AreaSelection selection, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.area_analyzer");

        this.selection = selection;
        this.addPerChunkBoxes(selection.getAllSelectionBoxes());
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        IBlockState stateClient = this.worldClient.getBlockState(pos).getActualState(this.worldClient, pos);
        this.countsTotal.addTo(stateClient, 1);
    }
}
