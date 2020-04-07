package fi.dy.masa.litematica.scheduler.tasks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.selection.AreaSelection;

public class TaskCountBlocksArea extends TaskCountBlocksMaterialList
{
    protected final AreaSelection selection;

    public TaskCountBlocksArea(AreaSelection selection, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.area_analyzer");

        this.selection = selection;
        this.addBoxesPerChunks(selection.getAllSubRegionBoxes());
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        IBlockState stateClient = this.worldClient.getBlockState(pos).getActualState(this.worldClient, pos);
        this.countsTotal.addTo(stateClient, 1);
    }
}
