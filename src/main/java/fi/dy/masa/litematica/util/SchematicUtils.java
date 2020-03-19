package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.GuiSchematicSave.InMemorySchematicCreator;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskDeleteArea;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicDirect;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicSetblock;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;

public class SchematicUtils
{
    private static long areaMovedTime;

    public static boolean saveSchematic(boolean inMemoryOnly)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null)
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                String title = "litematica.gui.title.schematic_projects.save_new_version";
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();
                GuiTextInput gui = new GuiTextInput(512, title, project.getCurrentVersionName(), GuiUtils.getCurrentScreen(), new SchematicVersionCreator());
                GuiBase.openGui(gui);
            }
            else if (inMemoryOnly)
            {
                String title = "litematica.gui.title.create_in_memory_schematic";
                GuiTextInput gui = new GuiTextInput(512, title, area.getName(), GuiUtils.getCurrentScreen(), new InMemorySchematicCreator(area));
                GuiBase.openGui(gui);
            }
            else
            {
                GuiSchematicSave gui = new GuiSchematicSave();
                gui.setParent(GuiUtils.getCurrentScreen());
                GuiBase.openGui(gui);
            }

            return true;
        }

        return false;
    }

    public static void unloadCurrentlySelectedSchematic()
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null)
        {
            SchematicHolder.getInstance().removeSchematic(placement.getSchematic());
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
        }
    }

    public static boolean breakSchematicBlock(MinecraftClient mc)
    {
        return setTargetedSchematicBlockState(mc, Blocks.AIR.getDefaultState());
    }

    public static boolean placeSchematicBlock(MinecraftClient mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            BlockPos pos = info.pos.offset(info.side);

            if (DataManager.getRenderLayerRange().isPositionWithinRange(pos))
            {
                return setTargetedSchematicBlockState(pos, info.stateNew);
            }
        }

        return false;
    }

    public static boolean replaceSchematicBlocksInDirection(MinecraftClient mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            Direction playerFacingH = mc.player.getHorizontalFacing();
            Direction direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);

            // Center region
            if (direction == info.side)
            {
                direction = direction.getOpposite();
            }

            BlockPos posEnd = getReplacementBoxEndPos(info.pos, direction);
            return setSchematicBlockStates(info.pos, posEnd, info.stateNew);
        }

        return false;
    }

    public static boolean replaceAllIdenticalSchematicBlocks(MinecraftClient mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            return setAllIdenticalSchematicBlockStates(info.pos, info.stateOriginal, info.stateNew);
        }

        return false;
    }

    public static boolean breakSchematicBlocks(MinecraftClient mc)
    {
        RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, mc.player, 10);

        if (wrapper != null && wrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockHitResult trace = wrapper.getBlockHitResult();
            BlockPos pos = trace.getBlockPos();
            Direction playerFacingH = mc.player.getHorizontalFacing();
            Direction direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(trace.getSide(), playerFacingH, pos, trace.getPos());

            // Center region
            if (direction == trace.getSide())
            {
                direction = direction.getOpposite();
            }

            BlockPos posEnd = getReplacementBoxEndPos(pos, direction);

            return setSchematicBlockStates(pos, posEnd, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    public static boolean breakAllIdenticalSchematicBlocks(MinecraftClient mc)
    {
        RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, mc.player, 10);

        if (wrapper != null && wrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockHitResult trace = wrapper.getBlockHitResult();
            BlockPos pos = trace.getBlockPos();
            BlockState stateOriginal = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);

            return setAllIdenticalSchematicBlockStates(pos, stateOriginal, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    public static boolean placeSchematicBlocksInDirection(MinecraftClient mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            Direction playerFacingH = mc.player.getHorizontalFacing();
            Direction direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);
            BlockPos posStart = info.pos.offset(info.side); // offset to the adjacent air block

            if (SchematicWorldHandler.getSchematicWorld().getBlockState(posStart).isAir())
            {
                BlockPos posEnd = getReplacementBoxEndPos(posStart, direction);
                return setSchematicBlockStates(posStart, posEnd, info.stateNew);
            }
        }

        return false;
    }

    public static boolean fillAirWithBlocks(MinecraftClient mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            BlockPos posStart = info.pos.offset(info.side); // offset to the adjacent air block

            if (SchematicWorldHandler.getSchematicWorld().getBlockState(posStart).isAir())
            {
                return setAllIdenticalSchematicBlockStates(posStart, Blocks.AIR.getDefaultState(), info.stateNew);
            }
        }

        return false;
    }

    @Nullable
    private static ReplacementInfo getTargetInfo(MinecraftClient mc)
    {
        ItemStack stack = mc.player.getMainHandStack();

        if ((stack.isEmpty() == false && (stack.getItem() instanceof BlockItem)) ||
            (stack.isEmpty() && ToolMode.REBUILD.getPrimaryBlock() != null))
        {
            WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
            RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 10, true);

            if (worldSchematic != null && traceWrapper != null &&
                traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
            {
                BlockHitResult trace = traceWrapper.getBlockHitResult();
                Direction side = trace.getSide();
                Vec3d hitVec = trace.getPos();
                BlockPos pos = trace.getBlockPos();
                BlockState stateOriginal = worldSchematic.getBlockState(pos);
                BlockState stateNew = Blocks.AIR.getDefaultState();

                if (stack.getItem() instanceof BlockItem)
                {
                    // Smuggle in a reference to the Schematic world to the use context
                    World worldClient = mc.player.world;
                    mc.player.world = worldSchematic;

                    BlockHitResult hit = new BlockHitResult(trace.getPos(), side, pos.offset(side), false);
                    ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, Hand.MAIN_HAND, hit));

                    mc.player.world = worldClient;

                    stateNew = ((BlockItem) stack.getItem()).getBlock().getPlacementState(ctx);
                }
                else if (ToolMode.REBUILD.getPrimaryBlock() != null)
                {
                    stateNew = ToolMode.REBUILD.getPrimaryBlock();
                }

                return new ReplacementInfo(pos, side, hitVec, stateOriginal, stateNew);
            }
        }

        return null;
    }

    private static BlockPos getReplacementBoxEndPos(BlockPos startPos, Direction direction)
    {
        return getReplacementBoxEndPos(startPos, direction, 10000);
    }

    private static BlockPos getReplacementBoxEndPos(BlockPos startPos, Direction direction, int maxBlocks)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        LayerRange range = DataManager.getRenderLayerRange();
        BlockState stateStart = world.getBlockState(startPos);
        BlockPos.Mutable posMutable = new BlockPos.Mutable();
        posMutable.set(startPos);

        while (maxBlocks-- > 0)
        {
            posMutable.move(direction);

            if (range.isPositionWithinRange(posMutable) == false ||
                world.getChunkProvider().isChunkLoaded(posMutable.getX() >> 4, posMutable.getZ() >> 4) == false ||
                world.getBlockState(posMutable) != stateStart)
            {
                posMutable.move(direction.getOpposite());
                break;
            }
        }

        return posMutable.toImmutable();
    }

    public static boolean setTargetedSchematicBlockState(MinecraftClient mc, BlockState state)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true);

        if (world != null && traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockHitResult trace = traceWrapper.getBlockHitResult();
            BlockPos pos = trace.getBlockPos();
            return setTargetedSchematicBlockState(pos, state);
        }

        return false;
    }

    private static boolean setTargetedSchematicBlockState(BlockPos pos, BlockState state)
    {
        if (pos != null)
        {
            SubChunkPos cpos = new SubChunkPos(pos);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().containsPos(pos))
                    {
                        SchematicPlacement placement = part.getPlacement();
                        String regionName = part.getSubRegionName();
                        LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(regionName);
                        BlockPos posSchematic = getSchematicContainerPositionFromWorldPosition(pos, placement.getSchematic(),
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);

                        if (posSchematic != null)
                        {
                            state = getUntransformedBlockState(state, placement, regionName);

                            BlockState stateOriginal = container.get(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ());

                            int totalBlocks = part.getPlacement().getSchematic().getMetadata().getTotalBlocks();
                            int increment = 0;

                            if (stateOriginal.isAir() == false)
                            {
                                increment = state.isAir() == false ? 0 : -1;
                            }
                            else
                            {
                                increment = state.isAir() == false ? 1 : 0;
                            }

                            totalBlocks += increment;

                            container.set(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ(), state);
                            part.getPlacement().getSchematic().getMetadata().setTotalBlocks(totalBlocks);
                            DataManager.getSchematicPlacementManager().markChunkForRebuild(new ChunkPos(cpos.getX(), cpos.getZ()));

                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setSchematicBlockStates(BlockPos posStart, BlockPos posEnd, BlockState state)
    {
        if (posStart != null && posEnd != null)
        {
            SubChunkPos cpos = new SubChunkPos(posStart);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().containsPos(posStart))
                    {
                        SchematicPlacement placement = part.getPlacement();
                        String regionName = part.getSubRegionName();
                        LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(regionName);
                        BlockPos posStartSchematic = getSchematicContainerPositionFromWorldPosition(posStart, placement.getSchematic(),
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);
                        BlockPos posEndSchematic = getSchematicContainerPositionFromWorldPosition(posEnd, placement.getSchematic(),
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);

                        if (posStartSchematic != null && posEndSchematic != null)
                        {
                            BlockPos posMin = PositionUtils.getMinCorner(posStartSchematic, posEndSchematic);
                            BlockPos posMax = PositionUtils.getMaxCorner(posStartSchematic, posEndSchematic);
                            final int minX = Math.max(posMin.getX(), 0);
                            final int minY = Math.max(posMin.getY(), 0);
                            final int minZ = Math.max(posMin.getZ(), 0);
                            final int maxX = Math.min(posMax.getX(), container.getSize().getX() - 1);
                            final int maxY = Math.min(posMax.getY(), container.getSize().getY() - 1);
                            final int maxZ = Math.min(posMax.getZ(), container.getSize().getZ() - 1);
                            int totalBlocks = part.getPlacement().getSchematic().getMetadata().getTotalBlocks();
                            int increment = 0;

                            state = getUntransformedBlockState(state, placement, regionName);

                            for (int y = minY; y <= maxY; ++y)
                            {
                                for (int z = minZ; z <= maxZ; ++z)
                                {
                                    for (int x = minX; x <= maxX; ++x)
                                    {
                                        BlockState stateOriginal = container.get(x, y, z);

                                        if (stateOriginal.isAir() == false)
                                        {
                                            increment = state.isAir() == false ? 0 : -1;
                                        }
                                        else
                                        {
                                            increment = state.isAir() == false ? 1 : 0;
                                        }

                                        totalBlocks += increment;

                                        container.set(x, y, z, state);
                                    }
                                }
                            }

                            part.getPlacement().getSchematic().getMetadata().setTotalBlocks(totalBlocks);
                            DataManager.getSchematicPlacementManager().markAllPlacementsOfSchematicForRebuild(placement.getSchematic());

                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setAllIdenticalSchematicBlockStates(BlockPos posStart, BlockState stateOriginal, BlockState stateNew)
    {
        if (posStart != null)
        {
            SubChunkPos cpos = new SubChunkPos(posStart);
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            List<PlacementPart> list = manager.getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().containsPos(posStart))
                    {
                        if (replaceAllIdenticalBlocks(manager, part, stateOriginal, stateNew))
                        {
                            manager.markAllPlacementsOfSchematicForRebuild(part.getPlacement().getSchematic());
                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean replaceAllIdenticalBlocks(SchematicPlacementManager manager, PlacementPart part,
            BlockState stateOriginalIn, BlockState stateNewIn)
    {
        SchematicPlacement schematicPlacement = part.getPlacement();
        String selected = schematicPlacement.getSelectedSubRegionName();
        List<String> regions = new ArrayList<>();

        // Some sub-region selected, only replace in that region
        if (selected != null)
        {
            regions.add(selected);
        }
        // The entire placement is selected, replace in all sub-regions
        else if (manager.getSelectedSchematicPlacement() == schematicPlacement)
        {
            regions.addAll(schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).keySet());
        }
        // Nothing from the targeted placement is selected, don't replace anything
        else
        {
            InfoUtils.showInGameMessage(MessageType.WARNING, 20000, "litematica.message.warn.schematic_rebuild_placement_not_selected");
            return false;
        }

        LayerRange range = DataManager.getRenderLayerRange();

        int totalBlocks = schematicPlacement.getSchematic().getMetadata().getTotalBlocks();
        int increment = 0;

        if (stateOriginalIn.isAir() == false)
        {
            increment = stateNewIn.isAir() == false ? 0 : -1;
        }
        else
        {
            increment = stateNewIn.isAir() == false ? 1 : 0;
        }

        for (String regionName : regions)
        {
            LitematicaBlockStateContainer container = schematicPlacement.getSchematic().getSubRegionContainer(regionName);
            SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);

            if (container == null || placement == null)
            {
                continue;
            }

            int minX = range.getClampedValue(LayerRange.getWorldMinValueForAxis(Direction.Axis.X), Direction.Axis.X);
            int minY = range.getClampedValue(LayerRange.getWorldMinValueForAxis(Direction.Axis.Y), Direction.Axis.Y);
            int minZ = range.getClampedValue(LayerRange.getWorldMinValueForAxis(Direction.Axis.Z), Direction.Axis.Z);
            int maxX = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(Direction.Axis.X), Direction.Axis.X);
            int maxY = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(Direction.Axis.Y), Direction.Axis.Y);
            int maxZ = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(Direction.Axis.Z), Direction.Axis.Z);

            BlockPos posStart = new BlockPos(minX, minY, minZ);
            BlockPos posEnd = new BlockPos(maxX, maxY, maxZ);

            BlockPos pos1 = getReverserTransformedWorldPosition(posStart, schematicPlacement.getSchematic(),
                    regionName, schematicPlacement, schematicPlacement.getRelativeSubRegionPlacement(regionName));
            BlockPos pos2 = getReverserTransformedWorldPosition(posEnd, schematicPlacement.getSchematic(),
                    regionName, schematicPlacement, schematicPlacement.getRelativeSubRegionPlacement(regionName));

            if (pos1 == null || pos2 == null)
            {
                return false;
            }

            BlockPos posStartWorld = PositionUtils.getMinCorner(pos1, pos2);
            BlockPos posEndWorld   = PositionUtils.getMaxCorner(pos1, pos2);

            Vec3i size = container.getSize();
            final int startX = Math.max(posStartWorld.getX(), 0);
            final int startY = Math.max(posStartWorld.getY(), 0);
            final int startZ = Math.max(posStartWorld.getZ(), 0);
            final int endX = Math.min(posEndWorld.getX(), size.getX() - 1);
            final int endY = Math.min(posEndWorld.getY(), size.getY() - 1);
            final int endZ = Math.min(posEndWorld.getZ(), size.getZ() - 1);

            //System.out.printf("DEBUG == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
            //        regionName, startX, startY, startZ, endX, endY, endZ, container.getSize().getX(), container.getSize().getY(), container.getSize().getZ());

            if (startX < 0 || startY < 0 || startZ < 0 ||
                endX >= size.getX() ||
                endY >= size.getY() ||
                endZ >= size.getZ())
            {
                System.out.printf("OUT OF BOUNDS == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
                        regionName, startX, startY, startZ, endX, endY, endZ, size.getX(), size.getY(), size.getZ());
                return false;
            }

            //System.out.printf("DEBUG == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
            //        regionName, startX, startY, startZ, endX, endY, endZ, size.getX(), size.getY(), size.getZ());

            BlockState stateOriginal = getUntransformedBlockState(stateOriginalIn, schematicPlacement, regionName);
            BlockState stateNew = getUntransformedBlockState(stateNewIn, schematicPlacement, regionName);

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        if (container.get(x, y, z) == stateOriginal)
                        {
                            container.set(x, y, z, stateNew);
                            totalBlocks += increment;
                        }
                    }
                }
            }
        }

        schematicPlacement.getSchematic().getMetadata().setTotalBlocks(totalBlocks);

        return true;
    }

    public static void moveCurrentlySelectedWorldRegionToLookingDirection(int amount, PlayerEntity player, MinecraftClient mc)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            BlockPos pos = area.getEffectiveOrigin().offset(EntityUtils.getClosestLookingDirection(player), amount);
            moveCurrentlySelectedWorldRegionTo(pos, mc);
        }
    }

    public static void moveCurrentlySelectedWorldRegionTo(BlockPos pos, MinecraftClient mc)
    {
        if (mc.player == null || mc.player.abilities.creativeMode == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
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
            scheduler.hasTask(TaskPasteSchematicSetblock.class) ||
            scheduler.hasTask(TaskPasteSchematicDirect.class))
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.move.pending_tasks");
            return;
        }

        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "");
            TaskSaveSchematic taskSave = new TaskSaveSchematic(schematic, area, true);
            taskSave.disableCompletionMessage();
            areaMovedTime = System.currentTimeMillis();

            taskSave.setCompletionListener(() ->
            {
                SchematicPlacement placement = SchematicPlacement.createFor(schematic, pos, "-", true, true);
                DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);

                TaskDeleteArea taskDelete = new TaskDeleteArea(area.getAllSubRegionBoxes(), true);
                taskDelete.disableCompletionMessage();
                areaMovedTime = System.currentTimeMillis();

                taskDelete.setCompletionListener(() ->
                {
                    TaskBase taskPaste;

                    if (mc.isIntegratedServerRunning())
                    {
                        taskPaste = new TaskPasteSchematicDirect(placement);
                    }
                    else
                    {
                        taskPaste = new TaskPasteSchematicSetblock(placement, false);
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
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
        }
    }

    public static void cloneSelectionArea(MinecraftClient mc)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, mc.player.getName().getString());
            TaskSaveSchematic taskSave = new TaskSaveSchematic(schematic, area, true);
            taskSave.disableCompletionMessage();

            taskSave.setCompletionListener(() ->
            {
                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                String name = schematic.getMetadata().getName();
                BlockPos origin = RayTraceUtils.getTargetedPosition(mc.world, mc.player, 6, false);

                if (origin == null)
                {
                    origin = fi.dy.masa.malilib.util.PositionUtils.getEntityBlockPos(mc.player);
                }

                SchematicPlacement placement = SchematicPlacement.createFor(schematic, origin, name, true, true);

                manager.addSchematicPlacement(placement, false);
                manager.setSelectedSchematicPlacement(placement);

                if (mc.player.abilities.creativeMode)
                {
                    DataManager.setToolMode(ToolMode.PASTE_SCHEMATIC);
                }
            });

            TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(taskSave, 10);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
        }
    }

    @Nullable
    public static BlockPos getSchematicContainerPositionFromWorldPosition(BlockPos worldPos, LitematicaSchematic schematic, String regionName,
            SchematicPlacement schematicPlacement, SubRegionPlacement regionPlacement, LitematicaBlockStateContainer container)
    {
        BlockPos boxMinRel = getReverserTransformedWorldPosition(worldPos, schematic, regionName, schematicPlacement, regionPlacement);

        if (boxMinRel == null)
        {
            return null;
        }

        final int startX = boxMinRel.getX();
        final int startY = boxMinRel.getY();
        final int startZ = boxMinRel.getZ();
        Vec3i size = container.getSize();

        /*
        if (startX < 0 || startY < 0 || startZ < 0 || startX >= size.getX() || startY >= size.getY() || startZ >= size.getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, startX: %d, startY %s, startZ: %d - size x: %d y: %s z: %d =============\n",
                    regionName, startX, startY, startZ, size.getX(), size.getY(), size.getZ());
            return null;
        }

        return boxMinRel;
        */

        return new BlockPos(MathHelper.clamp(startX, 0, size.getX() - 1),
                            MathHelper.clamp(startY, 0, size.getY() - 1),
                            MathHelper.clamp(startZ, 0, size.getZ() - 1));
    }

    @Nullable
    private static BlockPos getReverserTransformedWorldPosition(BlockPos worldPos, LitematicaSchematic schematic,
            String regionName, SchematicPlacement schematicPlacement, SubRegionPlacement regionPlacement)
    {
        BlockPos origin = schematicPlacement.getOrigin();
        BlockPos regionPos = regionPlacement.getPos();
        BlockPos regionSize = schematic.getAreaSize(regionName);

        if (regionSize == null)
        {
            return null;
        }

        // These are the untransformed relative positions
        BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        // The transformed sub-region origin position
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // The relative offset of the affected region's corners, to the sub-region's origin corner
        BlockPos relPos = new BlockPos(worldPos.getX() - origin.getX() - regionPosTransformed.getX(),
                                       worldPos.getY() - origin.getY() - regionPosTransformed.getY(),
                                       worldPos.getZ() - origin.getZ() - regionPosTransformed.getZ());

        // Reverse transform that relative offset, to get the untransformed orientation's offsets
        relPos = PositionUtils.getReverseTransformedBlockPos(relPos, regionPlacement.getMirror(), regionPlacement.getRotation());

        relPos = PositionUtils.getReverseTransformedBlockPos(relPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // Get the offset relative to the sub-region's minimum corner, instead of the origin corner (which can be at any corner)
        relPos = relPos.subtract(posMinRel.subtract(regionPos));

        return relPos;
    }

    public static BlockState getUntransformedBlockState(BlockState state, SchematicPlacement schematicPlacement, String subRegionName)
    {
        SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(subRegionName);

        if (placement != null)
        {
            final BlockRotation rotationCombined = PositionUtils.getReverseRotation(schematicPlacement.getRotation().rotate(placement.getRotation()));
            final BlockMirror mirrorMain = schematicPlacement.getMirror();
            BlockMirror mirrorSub = placement.getMirror();

            if (mirrorSub != BlockMirror.NONE &&
                (schematicPlacement.getRotation() == BlockRotation.CLOCKWISE_90 ||
                 schematicPlacement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90))
            {
                mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
            }

            if (rotationCombined != BlockRotation.NONE)
            {
                state = state.rotate(rotationCombined);
            }

            if (mirrorSub != BlockMirror.NONE)
            {
                state = state.mirror(mirrorSub);
            }

            if (mirrorMain != BlockMirror.NONE)
            {
                state = state.mirror(mirrorMain);
            }
        }

        return state;
    }

    private static class ReplacementInfo
    {
        public final BlockPos pos;
        public final Direction side;
        public final Vec3d hitVec;
        public final BlockState stateOriginal;
        public final BlockState stateNew;

        public ReplacementInfo(BlockPos pos, Direction side, Vec3d hitVec, BlockState stateOriginal, BlockState stateNew)
        {
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.stateOriginal = stateOriginal;
            this.stateNew = stateNew;
        }
    }

    public static class SchematicVersionCreator implements IStringConsumerFeedback
    {
        @Override
        public boolean setString(String string)
        {
            return DataManager.getSchematicProjectsManager().commitNewVersion(string);
        }
    }
}
