package fi.dy.masa.litematica.schematic.conversion;

import fi.dy.masa.litematica.mixin.IMixinBlockFenceGate;
import fi.dy.masa.litematica.mixin.IMixinBlockRedstoneWire;
import fi.dy.masa.litematica.mixin.IMixinBlockStairs;
import fi.dy.masa.litematica.mixin.IMixinBlockVine;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.Constants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAbstractBanner;
import net.minecraft.block.BlockAbstractSkull;
import net.minecraft.block.BlockAttachedStem;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBannerWall;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChorusPlant;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFourWay;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSkullWall;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockStemGrown;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BedPart;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.state.properties.NoteBlockInstrument;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;

public class SchematicConversionFixers
{
    private static final BooleanProperty[] FENCE_WALL_PROP_MAP = new BooleanProperty[] { null, null, BlockFourWay.NORTH, BlockFourWay.SOUTH, BlockFourWay.WEST, BlockFourWay.EAST };

    public static final IStateFixer FIXER_BANNER = (reader, state, pos) -> {
        NBTTagCompound tag = reader.getBlockEntityData(pos);

        if (tag != null)
        {
            EnumDyeColor colorOrig = ((BlockAbstractBanner) state.getBlock()).getColor();
            EnumDyeColor colorFromData = EnumDyeColor.byId(15 - tag.getInt("Base"));

            if (colorOrig != colorFromData)
            {
                Integer rotation = state.get(BlockBanner.ROTATION);

                switch (colorFromData)
                {
                    case WHITE:         state = Blocks.WHITE_BANNER.getDefaultState();      break;
                    case ORANGE:        state = Blocks.ORANGE_BANNER.getDefaultState();     break;
                    case MAGENTA:       state = Blocks.MAGENTA_BANNER.getDefaultState();    break;
                    case LIGHT_BLUE:    state = Blocks.LIGHT_BLUE_BANNER.getDefaultState(); break;
                    case YELLOW:        state = Blocks.YELLOW_BANNER.getDefaultState();     break;
                    case LIME:          state = Blocks.LIME_BANNER.getDefaultState();       break;
                    case PINK:          state = Blocks.PINK_BANNER.getDefaultState();       break;
                    case GRAY:          state = Blocks.GRAY_BANNER.getDefaultState();       break;
                    case LIGHT_GRAY:    state = Blocks.LIGHT_GRAY_BANNER.getDefaultState(); break;
                    case CYAN:          state = Blocks.CYAN_BANNER.getDefaultState();       break;
                    case PURPLE:        state = Blocks.PURPLE_BANNER.getDefaultState();     break;
                    case BLUE:          state = Blocks.BLUE_BANNER.getDefaultState();       break;
                    case BROWN:         state = Blocks.BROWN_BANNER.getDefaultState();      break;
                    case GREEN:         state = Blocks.GREEN_BANNER.getDefaultState();      break;
                    case RED:           state = Blocks.RED_BANNER.getDefaultState();        break;
                    case BLACK:         state = Blocks.BLACK_BANNER.getDefaultState();      break;
                }

                state = state.with(BlockBanner.ROTATION, rotation);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_BANNER_WALL = (reader, state, pos) -> {
        NBTTagCompound tag = reader.getBlockEntityData(pos);

        if (tag != null)
        {
            EnumDyeColor colorOrig = ((BlockAbstractBanner) state.getBlock()).getColor();
            EnumDyeColor colorFromData = EnumDyeColor.byId(15 - tag.getInt("Base"));

            if (colorOrig != colorFromData)
            {
                EnumFacing facing = state.get(BlockBannerWall.HORIZONTAL_FACING);

                switch (colorFromData)
                {
                    case WHITE:         state = Blocks.WHITE_WALL_BANNER.getDefaultState();      break;
                    case ORANGE:        state = Blocks.ORANGE_WALL_BANNER.getDefaultState();     break;
                    case MAGENTA:       state = Blocks.MAGENTA_WALL_BANNER.getDefaultState();    break;
                    case LIGHT_BLUE:    state = Blocks.LIGHT_BLUE_WALL_BANNER.getDefaultState(); break;
                    case YELLOW:        state = Blocks.YELLOW_WALL_BANNER.getDefaultState();     break;
                    case LIME:          state = Blocks.LIME_WALL_BANNER.getDefaultState();       break;
                    case PINK:          state = Blocks.PINK_WALL_BANNER.getDefaultState();       break;
                    case GRAY:          state = Blocks.GRAY_WALL_BANNER.getDefaultState();       break;
                    case LIGHT_GRAY:    state = Blocks.LIGHT_GRAY_WALL_BANNER.getDefaultState(); break;
                    case CYAN:          state = Blocks.CYAN_WALL_BANNER.getDefaultState();       break;
                    case PURPLE:        state = Blocks.PURPLE_WALL_BANNER.getDefaultState();     break;
                    case BLUE:          state = Blocks.BLUE_WALL_BANNER.getDefaultState();       break;
                    case BROWN:         state = Blocks.BROWN_WALL_BANNER.getDefaultState();      break;
                    case GREEN:         state = Blocks.GREEN_WALL_BANNER.getDefaultState();      break;
                    case RED:           state = Blocks.RED_WALL_BANNER.getDefaultState();        break;
                    case BLACK:         state = Blocks.BLACK_WALL_BANNER.getDefaultState();      break;
                }

                state = state.with(BlockBannerWall.HORIZONTAL_FACING, facing);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_BED = (reader, state, pos) -> {
        NBTTagCompound tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("color", Constants.NBT.TAG_INT))
        {
            int colorId = tag.getInt("color");
            EnumFacing facing = state.get(BlockBed.HORIZONTAL_FACING);
            BedPart part = state.get(BlockBed.PART);
            Boolean occupied = state.get(BlockBed.OCCUPIED);

            switch (colorId)
            {
                case  0: state = Blocks.WHITE_BED.getDefaultState(); break;
                case  1: state = Blocks.ORANGE_BED.getDefaultState(); break;
                case  2: state = Blocks.MAGENTA_BED.getDefaultState(); break;
                case  3: state = Blocks.LIGHT_BLUE_BED.getDefaultState(); break;
                case  4: state = Blocks.YELLOW_BED.getDefaultState(); break;
                case  5: state = Blocks.LIME_BED.getDefaultState(); break;
                case  6: state = Blocks.PINK_BED.getDefaultState(); break;
                case  7: state = Blocks.GRAY_BED.getDefaultState(); break;
                case  8: state = Blocks.LIGHT_GRAY_BED.getDefaultState(); break;
                case  9: state = Blocks.CYAN_BED.getDefaultState(); break;
                case 10: state = Blocks.PURPLE_BED.getDefaultState(); break;
                case 11: state = Blocks.BLUE_BED.getDefaultState(); break;
                case 12: state = Blocks.BROWN_BED.getDefaultState(); break;
                case 13: state =  Blocks.GREEN_BED.getDefaultState(); break;
                case 14: state = Blocks.RED_BED.getDefaultState(); break;
                case 15: state = Blocks.BLACK_BED.getDefaultState(); break;
                default: return state;
            }

            state = state.with(BlockBed.HORIZONTAL_FACING, facing)
                         .with(BlockBed.PART, part)
                         .with(BlockBed.OCCUPIED, occupied);
        }

        return state;
    };

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

    public static final IStateFixer FIXER_FLOWER_POT = (reader, state, pos) -> {
        NBTTagCompound tag = reader.getBlockEntityData(pos);

        if (tag != null)
        {
            String itemName = tag.getString("Item");

            if (itemName.length() > 0)
            {
                int meta = tag.getInt("Data");

                switch (itemName)
                {
                    case "minecraft:sapling":
                        if (meta == 0)      return Blocks.POTTED_OAK_SAPLING.getDefaultState();
                        if (meta == 1)      return Blocks.POTTED_SPRUCE_SAPLING.getDefaultState();
                        if (meta == 2)      return Blocks.POTTED_BIRCH_SAPLING.getDefaultState();
                        if (meta == 3)      return Blocks.POTTED_JUNGLE_SAPLING.getDefaultState();
                        if (meta == 4)      return Blocks.POTTED_ACACIA_SAPLING.getDefaultState();
                        if (meta == 5)      return Blocks.POTTED_DARK_OAK_SAPLING.getDefaultState();
                        break;
                    case "minecraft:tallgrass":
                        if (meta == 0)      return Blocks.POTTED_DEAD_BUSH.getDefaultState();
                        if (meta == 2)      return Blocks.POTTED_FERN.getDefaultState();
                        break;
                    case "minecraft:red_flower":
                        if (meta == 0)      return Blocks.POTTED_POPPY.getDefaultState();
                        if (meta == 1)      return Blocks.POTTED_BLUE_ORCHID.getDefaultState();
                        if (meta == 2)      return Blocks.POTTED_ALLIUM.getDefaultState();
                        if (meta == 3)      return Blocks.POTTED_AZURE_BLUET.getDefaultState();
                        if (meta == 4)      return Blocks.POTTED_RED_TULIP.getDefaultState();
                        if (meta == 5)      return Blocks.POTTED_ORANGE_TULIP.getDefaultState();
                        if (meta == 6)      return Blocks.POTTED_WHITE_TULIP.getDefaultState();
                        if (meta == 7)      return Blocks.POTTED_PINK_TULIP.getDefaultState();
                        if (meta == 8)      return Blocks.POTTED_OXEYE_DAISY.getDefaultState();
                        break;
                    case "minecraft:yellow_flower":     return Blocks.POTTED_DANDELION.getDefaultState();
                    case "minecraft:brown_mushroom":    return Blocks.POTTED_BROWN_MUSHROOM.getDefaultState();
                    case "minecraft:red_mushroom":      return Blocks.POTTED_RED_MUSHROOM.getDefaultState();
                    case "minecraft:deadbush":          return Blocks.POTTED_DEAD_BUSH.getDefaultState();
                    case "minecraft:cactus":            return Blocks.POTTED_CACTUS.getDefaultState();
                    default:                            return state;
                }
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_NOTE_BLOCK = (reader, state, pos) -> {
        NBTTagCompound tag = reader.getBlockEntityData(pos);

        if (tag != null)
        {
            state = state
                        .with(BlockNote.POWERED, Boolean.valueOf(tag.getBoolean("powered")))
                        .with(BlockNote.NOTE, Integer.valueOf(MathHelper.clamp(tag.getByte("note"), 0, 24)))
                        .with(BlockNote.INSTRUMENT, NoteBlockInstrument.byState(reader.getBlockState(pos.down())));
        }

        return state;
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

    public static final IStateFixer FIXER_SKULL = (reader, state, pos) -> {
        NBTTagCompound tag = reader.getBlockEntityData(pos);

        if (tag != null)
        {
            BlockSkull.ISkullType typeOrig = ((BlockAbstractSkull) state.getBlock()).getSkullType();
            BlockSkull.ISkullType typeFromData = BlockSkull.Types.values()[MathHelper.clamp(tag.getByte("SkullType"), 0, 5)];

            if (typeOrig != typeFromData)
            {
                if (typeFromData == BlockSkull.Types.SKELETON)
                {
                    state = Blocks.SKELETON_SKULL.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.WITHER_SKELETON)
                {
                    state = Blocks.WITHER_SKELETON_SKULL.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.PLAYER)
                {
                    state = Blocks.PLAYER_HEAD.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.ZOMBIE)
                {
                    state = Blocks.ZOMBIE_HEAD.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.CREEPER)
                {
                    state = Blocks.CREEPER_HEAD.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.DRAGON)
                {
                    state = Blocks.DRAGON_HEAD.getDefaultState();
                }
            }

            state = state.with(BlockBanner.ROTATION, MathHelper.clamp(tag.getByte("Rot"), 0, 15));
        }

        return state;
    };

    public static final IStateFixer FIXER_SKULL_WALL = (reader, state, pos) -> {
        NBTTagCompound tag = reader.getBlockEntityData(pos);

        if (tag != null)
        {
            BlockSkull.ISkullType typeOrig = ((BlockAbstractSkull) state.getBlock()).getSkullType();
            BlockSkull.ISkullType typeFromData = BlockSkull.Types.values()[MathHelper.clamp(tag.getByte("SkullType"), 0, 5)];

            if (typeOrig != typeFromData)
            {
                EnumFacing facing = state.get(BlockSkullWall.FACING);

                if (typeFromData == BlockSkull.Types.SKELETON)
                {
                    state = Blocks.SKELETON_WALL_SKULL.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.WITHER_SKELETON)
                {
                    state = Blocks.WITHER_SKELETON_WALL_SKULL.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.PLAYER)
                {
                    state = Blocks.PLAYER_WALL_HEAD.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.ZOMBIE)
                {
                    state = Blocks.ZOMBIE_WALL_HEAD.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.CREEPER)
                {
                    state = Blocks.CREEPER_WALL_HEAD.getDefaultState();
                }
                else if (typeFromData == BlockSkull.Types.DRAGON)
                {
                    state = Blocks.DRAGON_WALL_HEAD.getDefaultState();
                }

                state = state.with(BlockSkullWall.FACING, facing);
            }
        }

        return state;
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
        IBlockState fixState(IBlockReaderWithData reader, IBlockState state, BlockPos pos);
    }
}
