package fi.dy.masa.litematica.schematic.conversion;

import fi.dy.masa.litematica.mixin.IMixinBlockRedstoneWire;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChorusPlant;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFourWay;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public class SchematicConversionFixers
{
    private static final BooleanProperty[] FENCE_WALL_PROP_MAP = new BooleanProperty[] { null, null, BlockFourWay.NORTH, BlockFourWay.SOUTH, BlockFourWay.WEST, BlockFourWay.EAST };

    public static final IStateFixer FIXER_CHRORUS_PLANT = (reader, state, pos) -> {
        return ((BlockChorusPlant) state.getBlock()).makeConnections(reader, pos);
    };

    public static final IStateFixer FIXER_DIRT_SNOWY = (reader, state, pos) -> {
        Block block = reader.getBlockState(pos.up()).getBlock();
        return state.with(BlockDirtSnowy.SNOWY, Boolean.valueOf(block == Blocks.SNOW_BLOCK || block == Blocks.SNOW));
    };

    public static final IStateFixer FIXER_FENCE = (reader, state, pos) -> {
        BlockFence fence = (BlockFence) state.getBlock();

        for (EnumFacing side : PositionUtils.FACING_HORIZONTALS)
        {
            BlockPos posAdj = pos.offset(side);
            IBlockState stateAdj = reader.getBlockState(posAdj);
            state = state.with(FENCE_WALL_PROP_MAP[side.getIndex()], fence.attachesTo(stateAdj, stateAdj.getBlockFaceShape(reader, posAdj, side.getOpposite())));
        }

        return state;
    };

    public static final IStateFixer FIXER_FIRE = (reader, state, pos) -> {
        BlockFire fire = (BlockFire) state.getBlock();
        return fire.getStateForPlacement(reader, pos);
    };

    public static final IStateFixer FIXER_PANE = (reader, state, pos) -> {
        BlockPane pane = (BlockPane) state.getBlock();

        for (EnumFacing side : PositionUtils.FACING_HORIZONTALS)
        {
            BlockPos posAdj = pos.offset(side);
            IBlockState stateAdj = reader.getBlockState(posAdj);
            state = state.with(FENCE_WALL_PROP_MAP[side.getIndex()], pane.attachesTo(stateAdj, stateAdj.getBlockFaceShape(reader, posAdj, side.getOpposite())));
        }

        return state;
    };

    public static final IStateFixer FIXER_REDSTONE_WIRE = (reader, state, pos) -> {
        BlockRedstoneWire wire = (BlockRedstoneWire) state.getBlock();

        return state
            .with(BlockRedstoneWire.WEST, ((IMixinBlockRedstoneWire) wire).invokeGetSide(reader, pos, EnumFacing.WEST))
            .with(BlockRedstoneWire.EAST, ((IMixinBlockRedstoneWire) wire).invokeGetSide(reader, pos, EnumFacing.EAST))
            .with(BlockRedstoneWire.NORTH, ((IMixinBlockRedstoneWire) wire).invokeGetSide(reader, pos, EnumFacing.NORTH))
            .with(BlockRedstoneWire.SOUTH, ((IMixinBlockRedstoneWire) wire).invokeGetSide(reader, pos, EnumFacing.SOUTH));
    };

    public static final IStateFixer FIXER_WALL = (reader, state, pos) -> {
        boolean[] sides = new boolean[6];

        for (EnumFacing side : PositionUtils.FACING_HORIZONTALS)
        {
            BlockPos posAdj = pos.offset(side);
            IBlockState stateAdj = reader.getBlockState(posAdj);

            boolean val = wallAttachesTo(stateAdj, stateAdj.getBlockFaceShape(reader, posAdj, side.getOpposite()));
            state = state.with(FENCE_WALL_PROP_MAP[side.getIndex()], val);
            sides[side.getIndex()] = val;
        }

        boolean south = sides[EnumFacing.SOUTH.getIndex()];
        boolean west = sides[EnumFacing.WEST.getIndex()];
        boolean north = sides[EnumFacing.NORTH.getIndex()];
        boolean east = sides[EnumFacing.EAST.getIndex()];
        boolean up = ((! south || west || ! north || east) && (south || ! west || north || ! east)) || reader.getBlockState(pos.up()).isAir() == false;

        return state.with(BlockWall.UP, up);
    };

    private static boolean wallAttachesTo(IBlockState state, BlockFaceShape shape)
    {
        Block block = state.getBlock();
        boolean flag = shape == BlockFaceShape.MIDDLE_POLE_THICK || shape == BlockFaceShape.MIDDLE_POLE && block instanceof BlockFenceGate;
        return ! BlockWall.isExcepBlockForAttachWithPiston(block) && shape == BlockFaceShape.SOLID || flag;
    }
    
    public interface IStateFixer
    {
        IBlockState fixState(IBlockReader reader, IBlockState state, BlockPos pos);
    }
}
