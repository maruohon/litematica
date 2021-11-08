package fi.dy.masa.litematica.util;


import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.RailShape;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.CallbackI;

import java.util.LinkedHashMap;
import java.util.Map;

public class RotationUtils {
    public static Long currentHandlePos = null;
    private static int countTick = 0;
    private static Vec3d tracedPos = null;
    private static YawPitch originYawPitch = null;
    private static YawPitch rotateYawPitch;
    private static BlockState stateSchematic = null;
    private static final int waitTick = 8;
    public static boolean isHandling(){
        return currentHandlePos!=null;
    }
    private static Map< Class<? extends Block>, Integer> FacingMap = new LinkedHashMap<>();
    public static boolean requestRotate(BlockPos blockPos, BlockState blockState, Vec3d tracePos){
        if (isHandling()){
            return false;
        }
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(blockState);
        if (blockState.getBlock() instanceof PoweredRailBlock || blockState.getBlock() instanceof DetectorRailBlock){
            if (blockState.get(PoweredRailBlock.SHAPE) == RailShape.EAST_WEST){
                facing = Direction.EAST;
            } else {
                facing = Direction.NORTH;
            }
        }
        if (facing != null){
            Integer integer = getRequiredRotation(blockState.getBlock(), blockPos, tracedPos);
            //0 - none, 3 - opposite, 1 - counter, 2 - clockwise from wanted direction
            if (integer == 0){
            } else if (integer == 3){
                facing = facing.getOpposite();
            } else if (integer == 1){
                facing = facing.rotateYClockwise();
            } else if (integer == 2){
                facing = facing.rotateYCounterclockwise();
            }
            PlayerEntity player = MinecraftClient.getInstance().player;
            rotateYawPitch = convertDirectionToYawPitch(facing, MinecraftClient.getInstance().player);
            originYawPitch = new YawPitch(player.getYaw(), player.getPitch());
            currentHandlePos = blockPos.asLong();
            stateSchematic = blockState;
            tracedPos = tracePos;
            sendRotateEveryTick();
        }

        return true;
    }
    public static Direction calculateFacing(BlockState blockState){
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(blockState);
        if (facing == null){return Direction.NORTH;}
        return shouldReverse(blockState.getBlock()) ? facing.getOpposite() : facing;
    }
    private static void sendRotateEveryTick(){
        if (rotateYawPitch == null || currentHandlePos == null){
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.player.setYaw(rotateYawPitch.yaw);
        mc.player.setPitch(rotateYawPitch.pitch);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotateYawPitch.yaw, rotateYawPitch.pitch, mc.player.isOnGround()));

    }
    private static void resetYawPitch(){
        rotateYawPitch = null;
        currentHandlePos = null;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.player.setYaw(originYawPitch.yaw);
        mc.player.setPitch(originYawPitch.pitch);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(originYawPitch.yaw, originYawPitch.pitch, mc.player.isOnGround()));
        originYawPitch = null;
        countTick = 0;
        tracedPos = null;
        stateSchematic = null;
    }
    public static void tick(){
        if (MinecraftClient.getInstance().player == null || currentHandlePos == null){
            return;
        }
        countTick++;
        if (countTick >= waitTick){
            countTick = 0;
            resetYawPitch();
        }
        else {
            if (countTick == 3){ //n*50ms ping
                scheduledBlockPlacement();
            }
            if (countTick == 5){
                rotateYawPitch = originYawPitch;
            }
            sendRotateEveryTick();
        }
    }
    private static Vec3d applyTopBottomHitVec(BlockPos pos, BlockState state, Vec3d hitVecIn){
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        y = pos.getY();
        if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) != SlabType.DOUBLE)
        {
            if (state.get(SlabBlock.TYPE) == SlabType.TOP){
                y += 0.9;
            }
        }
        if (block instanceof TrapdoorBlock && state.get(TrapdoorBlock.HALF) != BlockHalf.TOP)
        {
            if (state.get(TrapdoorBlock.HALF) == BlockHalf.TOP){
                y += 0.9;
            }
        }
        return new Vec3d(x, y, z);
    }

    private static void scheduledBlockPlacement(){
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockPos pos = BlockPos.fromLong(currentHandlePos);
        Direction side = RotationUtils.calculateFacing(stateSchematic);
        //assume item didn't changed and items are at main hand
        Vec3d modifyPos = applyTopBottomHitVec(pos, stateSchematic, tracedPos);
        BlockHitResult hitResult = new BlockHitResult(modifyPos, side, pos, false);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hitResult);
    }
    private static boolean shouldReverse(Block block){
        // critical block lists
        return block instanceof PistonBlock || block instanceof DispenserBlock || block instanceof DropperBlock ||
                block instanceof ComparatorBlock || block instanceof RepeaterBlock || block instanceof BeehiveBlock || block instanceof HopperBlock;
    }

    private static Integer getRequiredRotation(Block block, BlockPos blockPos, Vec3d hitVec){
        if (FacingMap.containsKey(block.getClass())){
            return FacingMap.get(block.getClass());
        }
        if (fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(block.getDefaultState()) == null){
            FacingMap.put(block.getClass(), 0);
            return 0;
        }
        // int 0 : none, 1 : clockwise, 2 : counterclockwise, 3 : reverse
        PlayerEntity player = MinecraftClient.getInstance().player;
        Direction playerHorizontalFacing = player.getHorizontalFacing();
        Direction playerFacing = Direction.getEntityFacingOrder(player)[0];
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.NORTH, blockPos, false);
        ItemPlacementContext ctx = new ItemPlacementContext(player, Hand.MAIN_HAND, block.asItem().getDefaultStack(), hitResult);
        BlockState testState;
        try{
            testState = block.getPlacementState(ctx);
        }
        catch (Exception e){ //doors wtf
            return 0;
        }
        if (testState == null){
            return 0;
        }
        Direction testFacing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(testState);
        if (testFacing == null){
            FacingMap.put(block.getClass(), 0);
            return 0;
        }
        if (testFacing == playerFacing){
            FacingMap.put(block.getClass(), 0);
            return 0;
        } else if (testFacing == playerFacing.getOpposite()){
            FacingMap.put(block.getClass(), 3);
            return 3;
        } else if (testFacing == playerHorizontalFacing.rotateYClockwise()){
            FacingMap.put(block.getClass(), 2);
            return 1;
        } else if (testFacing == playerHorizontalFacing.rotateYCounterclockwise()){
            FacingMap.put(block.getClass(), 1);
            return 2;
        } else if (testFacing == playerHorizontalFacing.getOpposite()){
            FacingMap.put(block.getClass(), 3);
            return 3;}
        FacingMap.put(block.getClass(), 0);
        return 0;
    }
    private static YawPitch convertDirectionToYawPitch(Direction direction, PlayerEntity player){
        float originPitch = player.getPitch();
        float originYaw = player.getYaw();
        return switch (direction) {
            case UP -> new YawPitch(originYaw, -90.0f);
            case DOWN -> new YawPitch(originYaw, 90.0f);
            case EAST -> new YawPitch(-90.0f, originPitch);
            case WEST -> new YawPitch(90.0f, originPitch);
            case NORTH -> new YawPitch(180.0f, originPitch);
            case SOUTH -> new YawPitch(0.0f, originPitch);
        };
    }

    public static class YawPitch{
        public float yaw;
        public float pitch;
        public YawPitch(float yaw, float pitch){
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}