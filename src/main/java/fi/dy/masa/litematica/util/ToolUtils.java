package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskDeleteArea;
import fi.dy.masa.litematica.scheduler.tasks.TaskFillArea;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicDirect;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.scheduler.tasks.TaskUpdateBlocks;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.util.SchematicCreationUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.tool.ToolModeData;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.message.MessageType;
import fi.dy.masa.malilib.listener.ICompletionListener;
import fi.dy.masa.malilib.message.MessageUtils;
import fi.dy.masa.malilib.util.position.LayerRange;

public class ToolUtils
{
    private static long areaMovedTime;

    public static void setToolModeBlockState(ToolMode mode, boolean primary, Minecraft mc)
    {
        IBlockState state = Blocks.AIR.getDefaultState();
        double reach = mc.playerController.getBlockReachDistance();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(mc.world, entity, reach, true);

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
                    state = mc.world.getBlockState(pos).getActualState(mc.world, pos);
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

    public static void fillSelectionVolumes(Minecraft mc, IBlockState state, @Nullable IBlockState stateToReplace)
    {
        if (mc.player != null && mc.player.capabilities.isCreativeMode)
        {
            final AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();

            if (area == null)
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
                return;
            }

            if (area.getAllSubRegionBoxes().size() > 0)
            {
                SelectionBox currentBox = area.getSelectedSubRegionBox();
                final ImmutableList<SelectionBox> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSubRegionBoxes());

                TaskFillArea task = new TaskFillArea(boxes, state, stateToReplace, false);
                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 20);

                MessageUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
            }
            else
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.empty_area_selection");
            }
        }
        else
        {
            MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    public static void deleteSelectionVolumes(boolean removeEntities, Minecraft mc)
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
            area = DataManager.getSelectionManager().getCurrentSelection();
        }

        deleteSelectionVolumes(area, removeEntities, mc);
    }

    public static void deleteSelectionVolumes(@Nullable final AreaSelection area, boolean removeEntities, Minecraft mc)
    {
        deleteSelectionVolumes(area, removeEntities, null, mc);
    }

    public static void deleteSelectionVolumes(@Nullable final AreaSelection area, boolean removeEntities,
            @Nullable ICompletionListener listener, Minecraft mc)
    {
        if (mc.player != null && mc.player.capabilities.isCreativeMode)
        {
            if (area == null)
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
                return;
            }

            if (area.getAllSubRegionBoxes().size() > 0)
            {
                SelectionBox currentBox = area.getSelectedSubRegionBox();
                final ImmutableList<SelectionBox> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSubRegionBoxes());

                TaskDeleteArea task = new TaskDeleteArea(boxes, removeEntities);

                if (listener != null)
                {
                    task.setCompletionListener(listener);
                }

                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 20);

                MessageUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
            }
            else
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.empty_area_selection");
            }
        }
        else
        {
            MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    public static void updateSelectionVolumes(Minecraft mc)
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
            area = DataManager.getSelectionManager().getCurrentSelection();
        }

        updateSelectionVolumes(area, mc);
    }

    public static void updateSelectionVolumes(@Nullable final AreaSelection area, Minecraft mc)
    {
        if (mc.player != null && mc.player.capabilities.isCreativeMode && mc.isSingleplayer())
        {
            if (area == null)
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
                return;
            }

            if (area.getAllSubRegionBoxes().size() > 0)
            {
                SelectionBox currentBox = area.getSelectedSubRegionBox();
                final ImmutableList<SelectionBox> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSubRegionBoxes());
                TaskUpdateBlocks task = new TaskUpdateBlocks(boxes);
                TaskScheduler.getInstanceServer().scheduleTask(task, 20);

                MessageUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
            }
            else
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.empty_area_selection");
            }
        }
        else
        {
            MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    public static void moveCurrentlySelectedWorldRegionToLookingDirection(int amount, EntityPlayer player, Minecraft mc)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            BlockPos pos = area.getEffectiveOrigin().offset(EntityUtils.getClosestLookingDirection(player), amount);
            moveCurrentlySelectedWorldRegionTo(pos, mc);
        }
    }

    public static void moveCurrentlySelectedWorldRegionTo(BlockPos pos, Minecraft mc)
    {
        if (mc.player == null || mc.player.capabilities.isCreativeMode == false)
        {
            MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
            return;
        }

        TaskScheduler scheduler = TaskScheduler.getServerInstanceIfExistsOrClient();
        long currentTime = System.currentTimeMillis();

        // Add a delay from the previous move operation, to allow time for
        // server -> client chunk/block syncing, otherwise a subsequent move
        // might wipe the area before the new blocks have arrived on the
        // client and thus the new move schematic would just be air.
        if ((currentTime - areaMovedTime) < 400 ||
            scheduler.hasTask(TaskSaveSchematic.class) ||
            scheduler.hasTask(TaskDeleteArea.class) ||
            scheduler.hasTask(TaskPasteSchematicPerChunkBase.class) ||
            scheduler.hasTask(TaskPasteSchematicDirect.class))
        {
            MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.move.pending_tasks");
            return;
        }

        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            final LayerRange range = DataManager.getRenderLayerRange().copy();
            LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(area, "");
            TaskSaveSchematic taskSave = new TaskSaveSchematic(schematic, area, true);
            taskSave.disableCompletionMessage();
            areaMovedTime = System.currentTimeMillis();

            taskSave.setCompletionListener(() ->
            {
                SchematicPlacement placement = SchematicPlacement.createFor(schematic, pos, "-", true);
                DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);

                TaskDeleteArea taskDelete = new TaskDeleteArea(area.getAllSubRegionBoxes(), true);
                taskDelete.disableCompletionMessage();
                areaMovedTime = System.currentTimeMillis();

                taskDelete.setCompletionListener(() ->
                {
                    TaskBase taskPaste;

                    if (mc.isSingleplayer())
                    {
                        taskPaste = new TaskPasteSchematicDirect(placement, range);
                    }
                    else
                    {
                        taskPaste = new TaskPasteSchematicPerChunkCommand(ImmutableList.of(placement), range, false);
                    }

                    taskPaste.disableCompletionMessage();
                    areaMovedTime = System.currentTimeMillis();

                    taskPaste.setCompletionListener(() ->
                    {
                        SchematicHolder.getInstance().removeSchematic(schematic);
                        area.moveEntireSelectionTo(pos, false);
                        areaMovedTime = System.currentTimeMillis();
                    });

                    scheduler.scheduleTask(taskPaste, 1);
                });

                scheduler.scheduleTask(taskDelete, 1);
            });

            scheduler.scheduleTask(taskSave, 1);
        }
        else
        {
            MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
        }
    }

    public static boolean cloneSelectionArea(final Minecraft mc)
    {
        final SelectionManager sm = DataManager.getSelectionManager();
        final AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            final LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(area, mc.player.getName());
            final TaskSaveSchematic taskSave = new TaskSaveSchematic(schematic, area, true);
            taskSave.disableCompletionMessage();

            taskSave.setCompletionListener(() ->
            {
                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                String name = schematic.getMetadata().getName();
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                BlockPos origin;

                if (Configs.Generic.CLONE_AT_ORIGINAL_POS.getBooleanValue())
                {
                    origin = area.getEffectiveOrigin();
                }
                else
                {
                    origin = RayTraceUtils.getTargetedPosition(mc.world, entity, 6, false);

                    if (origin == null)
                    {
                        origin = new BlockPos(entity);
                    }
                }

                SchematicPlacement placement = SchematicPlacement.createFor(schematic, origin, name, true, false);

                manager.addSchematicPlacement(placement, false);
                manager.setSelectedSchematicPlacement(placement);

                if (mc.player.capabilities.isCreativeMode)
                {
                    DataManager.setToolMode(ToolMode.PASTE_SCHEMATIC);
                }
            });

            TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(taskSave, 10);

            return true;
        }
        else
        {
            MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
        }

        return false;
    }
}
