package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class TaskCountBlocksPlacement extends TaskCountBlocksBase
{
    protected final WorldSchematic worldSchematic;

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList)
    {
        this(Collections.singletonList(schematicPlacement), materialList);
    }

    public TaskCountBlocksPlacement(List<SchematicPlacement> placements, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.material_list");

        ArrayList<Box> boxes = new ArrayList<>();

        for (SchematicPlacement placement : placements)
        {
            boxes.addAll(placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values());
        }

        this.worldSchematic = SchematicWorldHandler.getSchematicWorld();
        this.addBoxesPerChunks(boxes);
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.worldSchematic != null;
    }

    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateSchematic = this.worldSchematic.getBlockState(pos);

        if (stateSchematic.isAir() == false)
        {
            BlockState stateClient = this.worldClient.getBlockState(pos);

            this.countsTotal.addTo(stateSchematic, 1);

            if (stateClient.isAir())
            {
                this.countsMissing.addTo(stateSchematic, 1);
            }
            else if (stateClient != stateSchematic)
            {
                this.countsMissing.addTo(stateSchematic, 1);
                this.countsMismatch.addTo(stateSchematic, 1);
            }
        }
    }
}
