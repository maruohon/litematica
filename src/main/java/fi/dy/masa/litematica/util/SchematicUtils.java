package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.GuiSchematicSave.InMemorySchematicCreator;
import fi.dy.masa.litematica.mixin.IMixinItemBlockSpecial;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBlockSpecial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class SchematicUtils
{
    public static boolean saveSchematic(boolean inMemoryOnly)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null)
        {
            Minecraft mc = Minecraft.getMinecraft();

            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                String title = "litematica.gui.title.schematic_projects.save_new_version";
                GuiTextInput gui = new GuiTextInput(512, title, area.getName(), mc.currentScreen, new SchematicVersionCreator());
                mc.displayGuiScreen(gui);
            }
            else if (inMemoryOnly)
            {
                String title = "litematica.gui.title.create_in_memory_schematic";
                GuiTextInput gui = new GuiTextInput(512, title, area.getName(), mc.currentScreen, new InMemorySchematicCreator(area));
                mc.displayGuiScreen(gui);
            }
            else
            {
                GuiSchematicSave gui = new GuiSchematicSave();
                gui.setParent(mc.currentScreen);
                mc.displayGuiScreen(gui);
            }

            return true;
        }

        return false;
    }

    public static boolean breakSchematicBlock(Minecraft mc)
    {
        return setTargetedSchematicBlockState(mc, Blocks.AIR.getDefaultState());
    }

    public static boolean placeSchematicBlock(Minecraft mc)
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

    public static boolean replaceSchematicBlocks(Minecraft mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            EnumFacing playerFacingH = mc.player.getHorizontalFacing();
            EnumFacing direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);

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

    public static boolean replaceAllIdenticalSchematicBlocks(Minecraft mc)
    {
        ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            return setAllIdenticalSchematicBlockStates(info.pos, info.stateOriginal, info.stateNew);
        }

        return false;
    }

    public static boolean breakSchematicBlocks(Minecraft mc)
    {
        RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, mc.player, 10);

        if (wrapper != null)
        {
            RayTraceResult trace = wrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();
            EnumFacing playerFacingH = mc.player.getHorizontalFacing();
            EnumFacing direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(trace.sideHit, playerFacingH, pos, trace.hitVec);

            // Center region
            if (direction == trace.sideHit)
            {
                direction = direction.getOpposite();
            }

            BlockPos posEnd = getReplacementBoxEndPos(pos, direction);

            return setSchematicBlockStates(pos, posEnd, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    public static boolean breakAllIdenticalSchematicBlocks(Minecraft mc)
    {
        RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, mc.player, 10);

        // The state can be null in 1.13+
        if (wrapper != null)
        {
            RayTraceResult trace = wrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();
            IBlockState stateOriginal = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);

            return setAllIdenticalSchematicBlockStates(pos, stateOriginal, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    @Nullable
    private static ReplacementInfo getTargetInfo(Minecraft mc)
    {
        ItemStack stack = mc.player.getHeldItemMainhand();

        if (stack.isEmpty() == false && (stack.getItem() instanceof ItemBlock || stack.getItem() instanceof ItemBlockSpecial))
        {
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
            RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 10, true);

            if (world != null && traceWrapper != null &&
                traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
            {
                RayTraceResult trace = traceWrapper.getRayTraceResult();
                EnumFacing side = trace.sideHit;
                Vec3d hitVec = trace.hitVec;
                int meta = stack.getItem().getMetadata(stack.getMetadata());
                BlockPos pos = trace.getBlockPos();
                IBlockState stateOriginal = world.getBlockState(pos);
                IBlockState stateNew = Blocks.AIR.getDefaultState();

                if (stack.getItem() instanceof ItemBlock)
                {
                    stateNew = ((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(world, pos.offset(side),
                                    side, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, meta, mc.player);
                }
                else if (stack.getItem() instanceof ItemBlockSpecial)
                {
                    stateNew = ((IMixinItemBlockSpecial) stack.getItem()).getBlock().getStateForPlacement(world, pos.offset(side),
                                    side, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, 0, mc.player);
                }

                return new ReplacementInfo(pos, side, hitVec, stateOriginal, stateNew);
            }
        }

        return null;
    }

    private static BlockPos getReplacementBoxEndPos(BlockPos startPos, EnumFacing direction)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        IBlockState stateStart = world.getBlockState(startPos);
        BlockPos posEnd = startPos;
        int failsafe = 10000;
        LayerRange range = DataManager.getRenderLayerRange();

        while (failsafe-- > 0)
        {
            BlockPos posTmp = posEnd.offset(direction);

            if (range.isPositionWithinRange(posTmp) == false)
            {
                break;
            }

            if (world.getBlockState(posTmp) == stateStart)
            {
                posEnd = posTmp;
            }
        }

        return posEnd;
    }

    public static boolean setTargetedSchematicBlockState(Minecraft mc, IBlockState state)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true);

        if (world != null && traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            RayTraceResult trace = traceWrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();
            return setTargetedSchematicBlockState(pos, state);
        }

        return false;
    }

    private static boolean setTargetedSchematicBlockState(BlockPos pos, IBlockState state)
    {
        if (pos != null)
        {
            SubChunkPos cpos = new SubChunkPos(pos);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().isVecInside(pos))
                    {
                        SchematicPlacement placement = part.getPlacement();
                        String regionName = part.getSubRegionName();
                        LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(regionName);
                        BlockPos posSchematic = getSchematicContainerPositionFromWorldPosition(pos, placement.getSchematic(),
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);

                        if (posSchematic != null)
                        {
                            IBlockState stateOriginal = container.get(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ());

                            int totalBlocks = part.getPlacement().getSchematic().getMetadata().getTotalBlocks();
                            int increment = 0;

                            if (stateOriginal.getBlock() != Blocks.AIR)
                            {
                                increment = state.getBlock() != Blocks.AIR ? 0 : -1;
                            }
                            else
                            {
                                increment = state.getBlock() != Blocks.AIR ? 1 : 0;
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

    private static boolean setSchematicBlockStates(BlockPos posStart, BlockPos posEnd, IBlockState state)
    {
        if (posStart != null && posEnd != null)
        {
            SubChunkPos cpos = new SubChunkPos(posStart);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().isVecInside(posStart))
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

                            for (int y = minY; y <= maxY; ++y)
                            {
                                for (int z = minZ; z <= maxZ; ++z)
                                {
                                    for (int x = minX; x <= maxX; ++x)
                                    {
                                        IBlockState stateOriginal = container.get(x, y, z);

                                        if (stateOriginal.getBlock() != Blocks.AIR)
                                        {
                                            increment = state.getBlock() != Blocks.AIR ? 0 : -1;
                                        }
                                        else
                                        {
                                            increment = state.getBlock() != Blocks.AIR ? 1 : 0;
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

    private static boolean setAllIdenticalSchematicBlockStates(BlockPos posStart, IBlockState stateOriginal, IBlockState stateNew)
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
                    if (part.getBox().isVecInside(posStart))
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
            IBlockState stateOriginal, IBlockState stateNew)
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

        if (stateOriginal.getBlock() != Blocks.AIR)
        {
            increment = stateNew.getBlock() != Blocks.AIR ? 0 : -1;
        }
        else
        {
            increment = stateNew.getBlock() != Blocks.AIR ? 1 : 0;
        }

        for (String regionName : regions)
        {
            LitematicaBlockStateContainer container = schematicPlacement.getSchematic().getSubRegionContainer(regionName);
            SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);

            if (container == null || placement == null)
            {
                continue;
            }

            int minX = range.getClampedValue(LayerRange.getWorldMinValueForAxis(EnumFacing.Axis.X), EnumFacing.Axis.X);
            int minY = range.getClampedValue(LayerRange.getWorldMinValueForAxis(EnumFacing.Axis.Y), EnumFacing.Axis.Y);
            int minZ = range.getClampedValue(LayerRange.getWorldMinValueForAxis(EnumFacing.Axis.Z), EnumFacing.Axis.Z);
            int maxX = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(EnumFacing.Axis.X), EnumFacing.Axis.X);
            int maxY = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(EnumFacing.Axis.Y), EnumFacing.Axis.Y);
            int maxZ = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(EnumFacing.Axis.Z), EnumFacing.Axis.Z);

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

        if (startX < 0 || startY < 0 || startZ < 0 || startX >= size.getX() || startY >= size.getY() || startZ >= size.getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, startX: %d, startY %s, startZ: %d - size x: %d y: %s z: %d =============\n",
                    regionName, startX, startY, startZ, size.getX(), size.getY(), size.getZ());
            return null;
        }

        return boxMinRel;
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

    private static class ReplacementInfo
    {
        public final BlockPos pos;
        public final EnumFacing side;
        public final Vec3d hitVec;
        public final IBlockState stateOriginal;
        public final IBlockState stateNew;

        public ReplacementInfo(BlockPos pos, EnumFacing side, Vec3d hitVec, IBlockState stateOriginal, IBlockState stateNew)
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
