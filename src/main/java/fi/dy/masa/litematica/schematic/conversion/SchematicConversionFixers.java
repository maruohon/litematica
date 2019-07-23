package fi.dy.masa.litematica.schematic.conversion;

import fi.dy.masa.litematica.mixin.IMixinBlockFenceGate;
import fi.dy.masa.litematica.mixin.IMixinBlockRedstoneWire;
import fi.dy.masa.litematica.mixin.IMixinBlockStairs;
import fi.dy.masa.litematica.mixin.IMixinBlockVine;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAttachedStem;
import net.minecraft.block.BlockChorusPlant;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFourWay;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockStemGrown;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.DoubleBlockHalf;
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

    public static final IStateFixer FIXER_DOOR = (reader, state, pos) -> {
        if (state.get(BlockDoor.HALF) == DoubleBlockHalf.UPPER)
        {
            IBlockState stateLower = reader.getBlockState(pos.down());

            if (stateLower.getBlock() == state.getBlock())
            {
                state = state.with(BlockDoor.FACING, stateLower.get(BlockDoor.FACING));
                state = state.with(BlockDoor.OPEN,   stateLower.get(BlockDoor.OPEN));
            }
        }
        else
        {
            IBlockState stateUpper = reader.getBlockState(pos.up());

            if (stateUpper.getBlock() == state.getBlock())
            {
                state = state.with(BlockDoor.HINGE,   stateUpper.get(BlockDoor.HINGE));
                state = state.with(BlockDoor.POWERED, stateUpper.get(BlockDoor.POWERED));
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_DOUBLE_PLANT = (reader, state, pos) -> {
        if (state.get(BlockDoublePlant.HALF) == DoubleBlockHalf.UPPER)
        {
            IBlockState stateLower = reader.getBlockState(pos.down());

            if (stateLower.getBlock() instanceof BlockDoublePlant)
            {
                state = stateLower.with(BlockDoublePlant.HALF, DoubleBlockHalf.UPPER);
            }
        }

        return state;
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

    public static final IStateFixer FIXER_FENCE_GATE = (reader, state, pos) -> {
        BlockFenceGate gate = (BlockFenceGate) state.getBlock();
        EnumFacing facing = state.get(BlockFenceGate.HORIZONTAL_FACING);
        boolean inWall = false;

        if (facing.getAxis() == EnumFacing.Axis.X)
        {
            inWall = (((IMixinBlockFenceGate) gate).invokeIsWall(reader.getBlockState(pos.offset(EnumFacing.NORTH)))
                   || ((IMixinBlockFenceGate) gate).invokeIsWall(reader.getBlockState(pos.offset(EnumFacing.SOUTH))));
        }
        else
        {
            inWall = (((IMixinBlockFenceGate) gate).invokeIsWall(reader.getBlockState(pos.offset(EnumFacing.WEST)))
                   || ((IMixinBlockFenceGate) gate).invokeIsWall(reader.getBlockState(pos.offset(EnumFacing.EAST))));
        }

        return state.with(BlockFenceGate.IN_WALL, inWall);
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

    public static final IStateFixer FIXER_REDSTONE_REPEATER = (reader, state, pos) -> {
        return state.with(BlockRedstoneRepeater.LOCKED, Boolean.valueOf(getIsRepeaterPoweredOnSide(reader, pos, state)));
    };

    public static final IStateFixer FIXER_REDSTONE_WIRE = (reader, state, pos) -> {
        BlockRedstoneWire wire = (BlockRedstoneWire) state.getBlock();

        return state
            .with(BlockRedstoneWire.WEST, ((IMixinBlockRedstoneWire) wire).invokeGetSide(reader, pos, EnumFacing.WEST))
            .with(BlockRedstoneWire.EAST, ((IMixinBlockRedstoneWire) wire).invokeGetSide(reader, pos, EnumFacing.EAST))
            .with(BlockRedstoneWire.NORTH, ((IMixinBlockRedstoneWire) wire).invokeGetSide(reader, pos, EnumFacing.NORTH))
            .with(BlockRedstoneWire.SOUTH, ((IMixinBlockRedstoneWire) wire).invokeGetSide(reader, pos, EnumFacing.SOUTH));
    };

    public static final IStateFixer FIXER_STAIRS = (reader, state, pos) -> {
        return state.with(BlockStairs.SHAPE, IMixinBlockStairs.invokeGetStairShape(state, reader, pos));
    };

    public static final IStateFixer FIXER_STEM = (reader, state, pos) -> {
        BlockStem stem = (BlockStem) state.getBlock();
        BlockStemGrown crop = stem.getCrop();

        for (EnumFacing side : PositionUtils.FACING_HORIZONTALS)
        {
            BlockPos posAdj = pos.offset(side);
            IBlockState stateAdj = reader.getBlockState(posAdj);
            Block blockAdj = stateAdj.getBlock();

            if (blockAdj == crop || (stem == Blocks.PUMPKIN_STEM && blockAdj == Blocks.CARVED_PUMPKIN))
            {
                return crop.getAttachedStem().getDefaultState().with(BlockAttachedStem.FACING, side);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_TRIPWIRE = (reader, state, pos) -> {
        BlockTripWire wire = (BlockTripWire) state.getBlock();

        return state
                .with(BlockTripWire.NORTH, ((BlockTripWire) wire).shouldConnectTo(reader.getBlockState(pos.north()), EnumFacing.NORTH))
                .with(BlockTripWire.SOUTH, ((BlockTripWire) wire).shouldConnectTo(reader.getBlockState(pos.south()), EnumFacing.SOUTH))
                .with(BlockTripWire.WEST, ((BlockTripWire) wire).shouldConnectTo(reader.getBlockState(pos.west()), EnumFacing.WEST))
                .with(BlockTripWire.EAST, ((BlockTripWire) wire).shouldConnectTo(reader.getBlockState(pos.east()), EnumFacing.EAST));
    };

    public static final IStateFixer FIXER_VINE = (reader, state, pos) -> {
        BlockVine vine = (BlockVine) state.getBlock();
        return state.with(BlockVine.UP, ((IMixinBlockVine) vine).invokeShouldConnectUp(reader, pos.up(), EnumFacing.UP));
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

    private static boolean getIsRepeaterPoweredOnSide(IBlockReader reader, BlockPos pos, IBlockState stateRepeater)
    {
        EnumFacing facing = stateRepeater.get(BlockRedstoneRepeater.HORIZONTAL_FACING);
        EnumFacing sideLeft = facing.rotateYCCW();
        EnumFacing sideRight = facing.rotateY();

        return getRepeaterPowerOnSide(reader, pos.offset(sideLeft) , sideLeft ) > 0 ||
               getRepeaterPowerOnSide(reader, pos.offset(sideRight), sideRight) > 0;
    }

    private static int getRepeaterPowerOnSide(IBlockReader reader, BlockPos pos, EnumFacing side)
    {
        IBlockState state = reader.getBlockState(pos);
        Block block = state.getBlock();

        if (BlockRedstoneDiode.isDiode(state))
        {
            if (block == Blocks.REDSTONE_BLOCK)
            {
                return 15;
            }
            else
            {
                return block == Blocks.REDSTONE_WIRE ? state.get(BlockRedstoneWire.POWER) : state.getStrongPower(reader, pos, side);
            }
        }
        else
        {
            return 0;
        }
    }

    public interface IStateFixer
    {
        IBlockState fixState(IBlockReader reader, IBlockState state, BlockPos pos);
    }
}
