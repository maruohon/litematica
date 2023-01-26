package litematica.util;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import malilib.listener.TaskCompletionListener;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameUtils;
import malilib.util.position.LayerRange;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.data.SchematicHolder;
import litematica.scheduler.TaskScheduler;
import litematica.scheduler.tasks.TaskBase;
import litematica.scheduler.tasks.TaskDeleteArea;
import litematica.scheduler.tasks.TaskFillArea;
import litematica.scheduler.tasks.TaskPasteSchematicDirect;
import litematica.scheduler.tasks.TaskPasteSchematicPerChunkBase;
import litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand;
import litematica.scheduler.tasks.TaskUpdateBlocks;
import litematica.schematic.ISchematic;
import litematica.schematic.LitematicaSchematic;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.util.SchematicCreationUtils;
import litematica.selection.AreaSelection;
import litematica.selection.SelectionBox;
import litematica.selection.AreaSelectionManager;
import litematica.task.CreateSchematicTask;
import litematica.tool.ToolMode;
import litematica.tool.ToolModeData;
import litematica.util.RayTraceUtils.RayTraceWrapper;
import litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import litematica.world.SchematicWorldHandler;

public class ToolUtils
{
    private static long areaMovedTime;

    public static void setToolModeBlockState(ToolMode mode, boolean primary)
    {
        IBlockState state = Blocks.AIR.getDefaultState();
        Entity entity = GameUtils.getCameraEntity();
        World world = GameUtils.getClientWorld();
        double reach = GameUtils.getInteractionManager().getBlockReachDistance();
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(world, entity, reach, true);

        if (wrapper != null)
        {
            RayTraceResult trace = wrapper.getRayTraceResult();

            if (trace != null)
            {
                BlockPos pos = trace.getBlockPos();

                if (wrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
                {
                    state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
                }
                else if (wrapper.getHitType() == HitType.VANILLA)
                {
                    state = world.getBlockState(pos).getActualState(world, pos);
                }
            }
        }

        if (primary)
        {
            mode.setPrimaryBlock(state);
        }
        else
        {
            mode.setSecondaryBlock(state);
        }
    }

    public static void fillSelectionVolumes(IBlockState state, @Nullable IBlockState stateToReplace)
    {
        if (GameUtils.getClientPlayer() != null && GameUtils.isCreativeMode())
        {
            final AreaSelection area = DataManager.getAreaSelectionManager().getCurrentSelection();

            if (area == null)
            {
                MessageDispatcher.error("litematica.message.error.no_area_selected");
                return;
            }

            if (area.getAllSelectionBoxes().size() > 0)
            {
                SelectionBox currentBox = area.getSelectedSelectionBox();
                final ImmutableList<SelectionBox> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSelectionBoxes());

                TaskFillArea task = new TaskFillArea(boxes, state, stateToReplace, false);
                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 20);

                MessageDispatcher.generic("litematica.message.scheduled_task_added");
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.empty_area_selection");
            }
        }
        else
        {
            MessageDispatcher.error("litematica.error.generic.creative_mode_only");
        }
    }

    public static void deleteSelectionVolumes(boolean removeEntities)
    {
        AreaSelection area = null;

        if (DataManager.getToolMode() == ToolMode.DELETE && ToolModeData.DELETE.getUsePlacement())
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                area = AreaSelection.fromPlacement(placement);
            }
        }
        else
        {
            area = DataManager.getAreaSelectionManager().getCurrentSelection();
        }

        deleteSelectionVolumes(area, removeEntities);
    }

    public static void deleteSelectionVolumes(@Nullable final AreaSelection area, boolean removeEntities)
    {
        deleteSelectionVolumes(area, removeEntities, null);
    }

    public static void deleteSelectionVolumes(@Nullable final AreaSelection area, boolean removeEntities,
                                              @Nullable TaskCompletionListener listener)
    {
        if (GameUtils.getClientPlayer() != null && GameUtils.isCreativeMode())
        {
            if (area == null)
            {
                MessageDispatcher.error("litematica.message.error.no_area_selected");
                return;
            }

            if (area.getAllSelectionBoxes().size() > 0)
            {
                SelectionBox currentBox = area.getSelectedSelectionBox();
                final ImmutableList<SelectionBox> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSelectionBoxes());

                TaskDeleteArea task = new TaskDeleteArea(boxes, removeEntities);

                if (listener != null)
                {
                    task.setCompletionListener(listener);
                }

                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 20);

                MessageDispatcher.generic("litematica.message.scheduled_task_added");
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.empty_area_selection");
            }
        }
        else
        {
            MessageDispatcher.error("litematica.error.generic.creative_mode_only");
        }
    }

    public static void updateSelectionVolumes()
    {
        AreaSelection area = null;

        if (ToolModeData.UPDATE_BLOCKS.getUsePlacement())
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                area = AreaSelection.fromPlacement(placement);
            }
        }
        else
        {
            area = DataManager.getAreaSelectionManager().getCurrentSelection();
        }

        updateSelectionVolumes(area);
    }

    public static void updateSelectionVolumes(@Nullable final AreaSelection area)
    {
        if (GameUtils.getClientPlayer() != null && GameUtils.isCreativeMode() && GameUtils.isSinglePlayer())
        {
            if (area == null)
            {
                MessageDispatcher.error("litematica.message.error.no_area_selected");
                return;
            }

            if (area.getAllSelectionBoxes().size() > 0)
            {
                SelectionBox currentBox = area.getSelectedSelectionBox();
                final ImmutableList<SelectionBox> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSelectionBoxes());
                TaskUpdateBlocks task = new TaskUpdateBlocks(boxes);
                TaskScheduler.getInstanceServer().scheduleTask(task, 20);

                MessageDispatcher.generic("litematica.message.scheduled_task_added");
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.empty_area_selection");
            }
        }
        else
        {
            MessageDispatcher.error("litematica.error.generic.creative_mode_only");
        }
    }

    public static void moveCurrentlySelectedWorldRegionToLookingDirection(int amount, Entity cameraEntity)
    {
        AreaSelectionManager sm = DataManager.getAreaSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSelectionBoxes().size() > 0)
        {
            BlockPos pos = area.getEffectiveOrigin().offset(EntityUtils.getClosestLookingDirection(cameraEntity), amount);
            moveCurrentlySelectedWorldRegionTo(pos);
        }
    }

    public static void moveCurrentlySelectedWorldRegionTo(BlockPos pos)
    {
        if (GameUtils.isCreativeMode() == false)
        {
            MessageDispatcher.error("litematica.error.generic.creative_mode_only");
            return;
        }

        TaskScheduler scheduler = TaskScheduler.getServerInstanceIfExistsOrClient();
        long currentTime = System.currentTimeMillis();

        // Add a delay from the previous move operation, to allow time for
        // server -> client chunk/block syncing, otherwise a subsequent move
        // might wipe the area before the new blocks have arrived on the
        // client and thus the new move schematic would just be air.
        if ((currentTime - areaMovedTime) < 1000 ||
            scheduler.hasTask(CreateSchematicTask.class) ||
            scheduler.hasTask(TaskDeleteArea.class) ||
            scheduler.hasTask(TaskPasteSchematicPerChunkBase.class) ||
            scheduler.hasTask(TaskPasteSchematicDirect.class))
        {
            MessageDispatcher.error("litematica.message.error.move.pending_tasks");
            return;
        }

        AreaSelectionManager sm = DataManager.getAreaSelectionManager();
        AreaSelection selection = sm.getCurrentSelection();

        if (selection != null && selection.getAllSelectionBoxes().size() > 0)
        {
            LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(selection);
            CreateSchematicTask taskSave = new CreateSchematicTask(schematic, selection, false,
                                                () -> onAreaSavedForMove(schematic, selection, scheduler, pos));
            areaMovedTime = System.currentTimeMillis();
            taskSave.disableCompletionMessage();
            scheduler.scheduleTask(taskSave, 1);
        }
        else
        {
            MessageDispatcher.error("litematica.message.error.no_area_selected");
        }
    }

    private static void onAreaSavedForMove(ISchematic schematic,
                                           AreaSelection selection,
                                           TaskScheduler scheduler,
                                           BlockPos pos)
    {
        SchematicPlacement placement = SchematicPlacement.create(schematic, pos, "-", true);
        DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);

        areaMovedTime = System.currentTimeMillis();

        TaskDeleteArea taskDelete = new TaskDeleteArea(selection.getAllSelectionBoxes(), true);
        taskDelete.disableCompletionMessage();
        taskDelete.setCompletionListener(() -> onAreaDeletedBeforeMove(schematic, placement, selection, scheduler, pos));
        scheduler.scheduleTask(taskDelete, 1);
    }

    private static void onAreaDeletedBeforeMove(ISchematic schematic,
                                                SchematicPlacement placement,
                                                AreaSelection selection,
                                                TaskScheduler scheduler,
                                                BlockPos pos)
    {
        LayerRange range = DataManager.getRenderLayerRange().copy();
        TaskBase taskPaste;

        if (GameUtils.isSinglePlayer())
        {
            taskPaste = new TaskPasteSchematicDirect(placement, range);
        }
        else
        {
            taskPaste = new TaskPasteSchematicPerChunkCommand(ImmutableList.of(placement), range, false);
        }

        areaMovedTime = System.currentTimeMillis();

        taskPaste.disableCompletionMessage();
        taskPaste.setCompletionListener(() -> onMovedAreaPasted(schematic, selection, pos));
        scheduler.scheduleTask(taskPaste, 1);
    }

    private static void onMovedAreaPasted(ISchematic schematic,
                                          AreaSelection selection,
                                          BlockPos pos)
    {
        SchematicHolder.getInstance().removeSchematic(schematic);
        selection.moveEntireAreaSelectionTo(pos, false);
        areaMovedTime = System.currentTimeMillis();
    }

    public static boolean cloneSelectionArea()
    {
        AreaSelectionManager sm = DataManager.getAreaSelectionManager();
        AreaSelection selection = sm.getCurrentSelection();

        if (selection != null && selection.getAllSelectionBoxes().size() > 0)
        {
            LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(selection);
            CreateSchematicTask taskSave = new CreateSchematicTask(schematic, selection, false,
                                                                         () -> placeClonedSchematic(schematic, selection));
            taskSave.disableCompletionMessage();

            TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(taskSave, 10);

            return true;
        }
        else
        {
            MessageDispatcher.error("litematica.message.error.no_area_selected");
        }

        return false;
    }

    private static void placeClonedSchematic(ISchematic schematic, AreaSelection selection)
    {
        String name = selection.getName();
        BlockPos origin;

        if (Configs.Generic.CLONE_AT_ORIGINAL_POS.getBooleanValue())
        {
            origin = selection.getEffectiveOrigin();
        }
        else
        {
            Entity entity = GameUtils.getCameraEntity();
            origin = RayTraceUtils.getTargetedPosition(GameUtils.getClientWorld(), entity, 6, false);

            if (origin == null)
            {
                origin = EntityWrap.getEntityBlockPos(entity);
            }
        }

        SchematicCreationUtils.setSchematicMetadataOnCreation(schematic, name);
        SchematicHolder.getInstance().addSchematic(schematic, true);

        SchematicPlacement placement = SchematicPlacement.create(schematic, origin, name, true, false);
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();

        manager.addSchematicPlacement(placement, false);
        manager.setSelectedSchematicPlacement(placement);

        if (GameUtils.isCreativeMode())
        {
            DataManager.setToolMode(ToolMode.PASTE_SCHEMATIC);
        }
    }
}
