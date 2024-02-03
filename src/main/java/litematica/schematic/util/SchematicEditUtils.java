package litematica.schematic.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBlockSpecial;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.EnabledCondition;
import malilib.util.game.RayTraceUtils.RayTraceFluidHandling;
import malilib.util.game.WorldUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameUtils;
import malilib.util.game.wrap.ItemWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.ChunkSectionPos;
import malilib.util.position.Direction;
import malilib.util.position.HitResult;
import malilib.util.position.LayerRange;
import malilib.util.position.PositionUtils;
import malilib.util.position.Vec3d;
import malilib.util.position.Vec3i;
import litematica.config.Hotkeys;
import litematica.data.DataManager;
import litematica.mixin.IMixinItemBlockSpecial;
import litematica.render.RenderUtils;
import litematica.schematic.ISchematic;
import litematica.schematic.ISchematicRegion;
import litematica.schematic.SchematicMetadata;
import litematica.schematic.container.ILitematicaBlockStateContainer;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import litematica.schematic.placement.SubRegionPlacement;
import litematica.tool.ToolMode;
import litematica.util.RayTraceUtils;
import litematica.util.RayTraceUtils.RayTraceWrapper;
import litematica.world.SchematicWorldHandler;
import litematica.world.WorldSchematic;

public class SchematicEditUtils
{
    public static boolean rebuildHandleBlockBreak()
    {
        if (GameUtils.getClientPlayer() != null &&
            DataManager.getToolMode() == ToolMode.SCHEMATIC_EDIT &&
            RenderUtils.areSchematicBlocksCurrentlyRendered())
        {
            if (Hotkeys.SCHEMATIC_EDIT_BREAK_DIRECTION.getKeyBind().isKeyBindHeld())
            {
                return breakSchematicBlocks();
            }
            else if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL.getKeyBind().isKeyBindHeld())
            {
                return breakAllIdenticalSchematicBlocks();
            }
            else
            {
                return breakSchematicBlock();
            }
        }

        return false;
    }

    public static boolean rebuildHandleBlockPlace()
    {
        if (GameUtils.getClientPlayer() != null &&
            DataManager.getToolMode() == ToolMode.SCHEMATIC_EDIT &&
            RenderUtils.areSchematicBlocksCurrentlyRendered())
        {
            if (Hotkeys.SCHEMATIC_EDIT_REPLACE_DIRECTION.getKeyBind().isKeyBindHeld())
            {
                return replaceSchematicBlocksInDirection();
            }
            else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_ALL.getKeyBind().isKeyBindHeld())
            {
                return replaceAllIdenticalSchematicBlocks();
            }
            else if (Hotkeys.SCHEMATIC_EDIT_BREAK_DIRECTION.getKeyBind().isKeyBindHeld())
            {
                return placeSchematicBlocksInDirection();
            }
            else if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL.getKeyBind().isKeyBindHeld())
            {
                return fillAirWithBlocks();
            }
            else
            {
                return placeSchematicBlock();
            }
        }

        return false;
    }

    private static boolean breakSchematicBlock()
    {
        return setTargetedSchematicBlockState(Blocks.AIR.getDefaultState());
    }

    private static boolean placeSchematicBlock()
    {
        ReplacementInfo info = getTargetInfo();

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            BlockPos pos = info.pos.offset(info.side);
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

            if (DataManager.getRenderLayerRange().isPositionWithinRange(pos) &&
                world != null && world.isAirBlock(pos))
            {
                return setTargetedSchematicBlockState(pos, info.stateNew);
            }
        }

        return true;
    }

    private static boolean replaceSchematicBlocksInDirection()
    {
        ReplacementInfo info = getTargetInfo();

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            Direction playerFacingH = EntityWrap.getClosestHorizontalLookingDirection(GameUtils.getClientPlayer());
            Direction direction = PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);

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

    private static boolean replaceAllIdenticalSchematicBlocks()
    {
        ReplacementInfo info = getTargetInfo();

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            return setAllIdenticalSchematicBlockStates(info.pos, info.stateOriginal, info.stateNew);
        }

        return false;
    }

    public static boolean rebuildAcceptReplacement()
    {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        Entity entity = GameUtils.getCameraEntity();
        World world = GameUtils.getClientWorld();
        HitResult trace = malilib.util.game.RayTraceUtils.getRayTraceFromEntity(world, entity, RayTraceFluidHandling.ANY, false, 5);

        if (schematicWorld != null && trace != null && trace.type == HitResult.Type.BLOCK)
        {
            BlockPos pos = trace.blockPos;
            IBlockState stateOriginal = schematicWorld.getBlockState(pos);
            IBlockState stateClient = world.getBlockState(pos).getActualState(world, pos);

            if (stateOriginal != stateClient)
            {
                if (setAllIdenticalSchematicBlockStates(pos, stateOriginal, stateClient))
                {
                    MessageDispatcher.success().translate("litematica.message.schematic_rebuild.accepted_replacement");
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean breakSchematicBlocks()
    {
        Entity entity = GameUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(GameUtils.getClientWorld(), entity, 20);

        if (wrapper != null)
        {
            HitResult trace = wrapper.getRayTraceResult();
            BlockPos pos = trace.blockPos;
            Direction playerFacingH = EntityWrap.getClosestHorizontalLookingDirection(entity);
            Direction direction = PositionUtils.getTargetedDirection(trace.side, playerFacingH, pos, trace.pos);

            // Center region
            if (direction == trace.side)
            {
                direction = direction.getOpposite();
            }

            BlockPos posEnd = getReplacementBoxEndPos(pos, direction);

            return setSchematicBlockStates(pos, posEnd, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    private static boolean breakAllIdenticalSchematicBlocks()
    {
        Entity entity = GameUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(GameUtils.getClientWorld(), entity, 20);

        // The state can be null in 1.13+
        if (wrapper != null)
        {
            HitResult trace = wrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();
            IBlockState stateOriginal = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);

            return setAllIdenticalSchematicBlockStates(pos, stateOriginal, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    private static boolean placeSchematicBlocksInDirection()
    {
        ReplacementInfo info = getTargetInfo();

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            Direction playerFacingH = EntityWrap.getClosestHorizontalLookingDirection(GameUtils.getClientPlayer());
            Direction direction = PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);
            BlockPos posStart = info.pos.offset(info.side); // offset to the adjacent air block

            if (SchematicWorldHandler.getSchematicWorld().getBlockState(posStart).getMaterial() == Material.AIR)
            {
                BlockPos posEnd = getReplacementBoxEndPos(posStart, direction);
                return setSchematicBlockStates(posStart, posEnd, info.stateNew);
            }
        }

        return false;
    }

    private static boolean fillAirWithBlocks()
    {
        ReplacementInfo info = getTargetInfo();

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null)
        {
            BlockPos posStart = info.pos.offset(info.side); // offset to the adjacent air block

            if (SchematicWorldHandler.getSchematicWorld().getBlockState(posStart).getMaterial() == Material.AIR)
            {
                return setAllIdenticalSchematicBlockStates(posStart, Blocks.AIR.getDefaultState(), info.stateNew);
            }
        }

        return false;
    }

    @Nullable
    private static ReplacementInfo getTargetInfo()
    {
        World clientWorld = GameUtils.getClientWorld();
        EntityPlayer player = GameUtils.getClientPlayer();
        ItemStack stack = player.getHeldItemMainhand();

        if ((ItemWrap.notEmpty(stack) && (stack.getItem() instanceof ItemBlock || stack.getItem() instanceof ItemBlockSpecial)) ||
            (ItemWrap.isEmpty(stack) && ToolMode.SCHEMATIC_EDIT.getPrimaryBlock() != null))
        {
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
            Entity entity = GameUtils.getCameraEntity();
            RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(clientWorld, entity, 20, true);

            if (world != null && traceWrapper != null &&
                traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
            {
                HitResult trace = traceWrapper.getRayTraceResult();
                Direction side = trace.side;
                Vec3d hitVec = trace.pos;
                int meta = stack.getItem().getMetadata(stack.getMetadata());
                BlockPos pos = trace.blockPos;
                IBlockState stateOriginal = world.getBlockState(pos);
                IBlockState stateNew = Blocks.AIR.getDefaultState();

                if (stack.getItem() instanceof ItemBlock)
                {
                    stateNew = ((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(world, pos.offset(side),
                                    side.getVanillaDirection(), (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, meta, player);
                }
                else if (stack.getItem() instanceof ItemBlockSpecial)
                {
                    stateNew = ((IMixinItemBlockSpecial) stack.getItem()).getBlock()
                                .getStateForPlacement(world, pos.offset(side).toVanillaPos(), side.getVanillaDirection(),
                                                      (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, 0, player);
                }
                else if (ToolMode.SCHEMATIC_EDIT.getPrimaryBlock() != null)
                {
                    stateNew = ToolMode.SCHEMATIC_EDIT.getPrimaryBlock();
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
        IBlockState stateStart = world.getBlockState(startPos);
        BlockPos.MutBlockPos posMutable = new BlockPos.MutBlockPos(startPos);

        while (maxBlocks-- > 0)
        {
            posMutable.offset(direction);

            if (range.isPositionWithinRange(posMutable) == false ||
                WorldUtils.isClientChunkLoaded(posMutable.getX() >> 4, posMutable.getZ() >> 4, world) == false ||
                world.getBlockState(posMutable) != stateStart)
            {
                posMutable.offset(direction.getOpposite());
                break;
            }
        }

        return posMutable.toImmutable();
    }

    private static boolean setTargetedSchematicBlockState(IBlockState state)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        Entity entity = GameUtils.getCameraEntity();
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(GameUtils.getClientWorld(), entity, 20, true);

        if (world != null && traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            HitResult trace = traceWrapper.getRayTraceResult();
            BlockPos pos = trace.blockPos;
            return setTargetedSchematicBlockState(pos, state);
        }

        return false;
    }

    private static boolean setTargetedSchematicBlockState(BlockPos pos, IBlockState state)
    {
        if (pos != null)
        {
            ChunkSectionPos cpos = new ChunkSectionPos(pos);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().contains(pos))
                    {
                        SchematicPlacement placement = part.getPlacement();
                        ISchematic schematic = placement.getSchematic();
                        String regionName = part.getSubRegionName();
                        ISchematicRegion region = schematic.getSchematicRegion(regionName);

                        if (region == null)
                        {
                            continue;
                        }

                        ILitematicaBlockStateContainer container = region.getBlockStateContainer();
                        BlockPos posSchematic = SchematicUtils.getSchematicContainerPositionFromWorldPosition(pos, schematic,
                                                                                                              regionName, placement, placement.getSubRegion(regionName), container);

                        if (posSchematic != null)
                        {
                            state = SchematicUtils.getUntransformedBlockState(state, placement, regionName);

                            IBlockState stateOriginal = container.getBlockState(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ());

                            SchematicMetadata metadata = schematic.getMetadata();
                            long totalBlocks = metadata.getTotalBlocks();
                            long increment;

                            if (stateOriginal.getBlock() != Blocks.AIR)
                            {
                                increment = state.getBlock() != Blocks.AIR ? 0 : -1;
                            }
                            else
                            {
                                increment = state.getBlock() != Blocks.AIR ? 1 : 0;
                            }

                            totalBlocks += increment;

                            container.setBlockState(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ(), state);

                            metadata.setTotalBlocks(totalBlocks);
                            metadata.setTimeModifiedToNow();
                            metadata.setModifiedSinceSaved();

                            DataManager.getSchematicPlacementManager().markChunkForRebuild(ChunkPos.asLong(cpos.getX(), cpos.getZ()));

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
            ChunkSectionPos cpos = ChunkSectionPos.ofBlockPos(posStart);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().contains(posStart))
                    {
                        String regionName = part.getSubRegionName();
                        SchematicPlacement schematicPlacement = part.getPlacement();
                        SubRegionPlacement placement = schematicPlacement.getSubRegion(regionName);
                        ISchematic schematic = schematicPlacement.getSchematic();
                        ISchematicRegion region = schematic.getSchematicRegion(regionName);

                        if (region == null)
                        {
                            continue;
                        }

                        ILitematicaBlockStateContainer container = region.getBlockStateContainer();
                        BlockPos posStartSchematic = SchematicUtils.getSchematicContainerPositionFromWorldPosition(posStart, schematic,
                                regionName, schematicPlacement, placement, container);
                        BlockPos posEndSchematic = SchematicUtils.getSchematicContainerPositionFromWorldPosition(posEnd, schematic,
                                regionName, schematicPlacement, placement, container);

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
                            long totalBlocks = schematic.getMetadata().getTotalBlocks();
                            long increment = 0;

                            state = SchematicUtils.getUntransformedBlockState(state, schematicPlacement, regionName);

                            for (int y = minY; y <= maxY; ++y)
                            {
                                for (int z = minZ; z <= maxZ; ++z)
                                {
                                    for (int x = minX; x <= maxX; ++x)
                                    {
                                        IBlockState stateOriginal = container.getBlockState(x, y, z);

                                        if (stateOriginal.getBlock() != Blocks.AIR)
                                        {
                                            increment = state.getBlock() != Blocks.AIR ? 0 : -1;
                                        }
                                        else
                                        {
                                            increment = state.getBlock() != Blocks.AIR ? 1 : 0;
                                        }

                                        totalBlocks += increment;

                                        container.setBlockState(x, y, z, state);
                                    }
                                }
                            }

                            SchematicMetadata metadata = schematic.getMetadata();
                            metadata.setTotalBlocks(totalBlocks);
                            metadata.setTimeModifiedToNow();
                            metadata.setModifiedSinceSaved();

                            DataManager.getSchematicPlacementManager().markAllPlacementsOfSchematicForRebuild(schematic);

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
            ChunkSectionPos cpos = ChunkSectionPos.ofBlockPos(posStart);
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            List<PlacementPart> list = manager.getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().contains(posStart))
                    {
                        if (replaceAllIdenticalBlocks(part, stateOriginal, stateNew))
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

    private static boolean replaceAllIdenticalBlocks(PlacementPart part, IBlockState stateOriginalIn, IBlockState stateNewIn)
    {
        SchematicPlacement schematicPlacement = part.getPlacement();
        ISchematic schematic = schematicPlacement.getSchematic();
        String selectedRegionName = schematicPlacement.getSelectedSubRegionName();
        List<String> regions = new ArrayList<>();

        // If there is a sub-region selected, check that it's the currently targeted region
        if (selectedRegionName != null)
        {
            if (selectedRegionName.equals(part.getSubRegionName()))
            {
                regions.add(selectedRegionName);
            }
            else
            {
                MessageDispatcher.warning(20000).translate("litematica.message.warn.schematic_rebuild.subregion_not_selected");
                return false;
            }
        }
        // No sub-regions selected, replace in the entire schematic
        else
        {
            regions.addAll(schematicPlacement.getSubRegionBoxes(EnabledCondition.ENABLED).keySet());
        }

        LayerRange range = DataManager.getRenderLayerRange();

        long totalBlocks = schematic.getMetadata().getTotalBlocks();
        long increment = 0;

        if (stateOriginalIn.getBlock() != Blocks.AIR)
        {
            increment = stateNewIn.getBlock() != Blocks.AIR ? 0 : -1;
        }
        else
        {
            increment = stateNewIn.getBlock() != Blocks.AIR ? 1 : 0;
        }

        for (String regionName : regions)
        {
            ISchematicRegion region = schematic.getSchematicRegion(regionName);

            if (region == null)
            {
                continue;
            }

            SubRegionPlacement placement = schematicPlacement.getSubRegion(regionName);
            ILitematicaBlockStateContainer container = region.getBlockStateContainer();
            Vec3i regionSize = region.getSize();

            if (container == null || placement == null || regionSize == null)
            {
                continue;
            }

            Pair<Vec3i, Vec3i> pair = SchematicUtils.getLayerRangeClampedSubRegion(range, schematicPlacement, placement, regionSize);

            if (pair == null)
            {
                return false;
            }

            Vec3i containerStart = pair.getLeft();
            Vec3i containerEnd = pair.getRight();
            Vec3i size = container.getSize();

            final int startX = containerStart.getX();
            final int startY = containerStart.getY();
            final int startZ = containerStart.getZ();
            final int endX = containerEnd.getX();
            final int endY = containerEnd.getY();
            final int endZ = containerEnd.getZ();

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

            IBlockState stateOriginal = SchematicUtils.getUntransformedBlockState(stateOriginalIn, schematicPlacement, regionName);
            IBlockState stateNew = SchematicUtils.getUntransformedBlockState(stateNewIn, schematicPlacement, regionName);

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        if (container.getBlockState(x, y, z) == stateOriginal)
                        {
                            container.setBlockState(x, y, z, stateNew);
                            totalBlocks += increment;
                        }
                    }
                }
            }
        }

        SchematicMetadata metadata = schematic.getMetadata();
        metadata.setTotalBlocks(totalBlocks);
        metadata.setTimeModifiedToNow();
        metadata.setModifiedSinceSaved();

        return true;
    }

    private static class ReplacementInfo
    {
        public final BlockPos pos;
        public final Direction side;
        public final Vec3d hitVec;
        public final IBlockState stateOriginal;
        public final IBlockState stateNew;

        public ReplacementInfo(BlockPos pos, Direction side, Vec3d hitVec, IBlockState stateOriginal, IBlockState stateNew)
        {
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.stateOriginal = stateOriginal;
            this.stateNew = stateNew;
        }
    }
}
