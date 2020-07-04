package de.meinbuild.liteschem.scheduler.tasks;

import java.util.Collection;
import de.meinbuild.liteschem.materials.IMaterialList;
import de.meinbuild.liteschem.schematic.placement.SchematicPlacement;
import de.meinbuild.liteschem.schematic.placement.SubRegionPlacement.RequiredEnabled;
import de.meinbuild.liteschem.selection.Box;
import de.meinbuild.liteschem.world.SchematicWorldHandler;
import de.meinbuild.liteschem.world.WorldSchematic;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class TaskCountBlocksPlacement extends TaskCountBlocksBase
{
    protected final SchematicPlacement schematicPlacement;
    protected final WorldSchematic worldSchematic;

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.material_list");

        this.worldSchematic = SchematicWorldHandler.getSchematicWorld();
        this.schematicPlacement = schematicPlacement;
        Collection<Box> boxes = schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values();
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
