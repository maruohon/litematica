package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class TaskCountBlocksPlacement extends TaskCountBlocksMaterialList
{
    protected final SchematicPlacement schematicPlacement;
    protected final WorldSchematic worldSchematic;

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.material_list");

        this.worldSchematic = SchematicWorldHandler.getSchematicWorld();
        this.schematicPlacement = schematicPlacement;
        Collection<SelectionBox> boxes = schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values();
        this.addBoxesPerChunks(boxes);

        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.worldSchematic != null;
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        IBlockState stateSchematic = this.worldSchematic.getBlockState(pos).getActualState(this.worldSchematic, pos);

        if (stateSchematic.getBlock() != Blocks.AIR)
        {
            IBlockState stateClient = this.worldClient.getBlockState(pos).getActualState(this.worldClient, pos);

            this.countsTotal.addTo(stateSchematic, 1);

            if (stateClient.getBlock() == Blocks.AIR)
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
