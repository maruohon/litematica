package fi.dy.masa.litematica.scheduler.tasks;

import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.selection.AreaSelection;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class TaskCountBlocksArea extends TaskCountBlocksBase
{
    protected final AreaSelection selection;

    public TaskCountBlocksArea(AreaSelection selection, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.area_analyzer");

        this.selection = selection;
        this.addBoxesPerChunks(selection.getAllSubRegionBoxes());
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateClient = this.clientWorld.getBlockState(pos);
        this.countsTotal.addTo(stateClient, 1);
    }
}
