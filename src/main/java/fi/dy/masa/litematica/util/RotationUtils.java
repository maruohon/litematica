package fi.dy.masa.litematica.util;


import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.CallbackI;

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
    public static boolean requestRotate(BlockPos blockPos, BlockState blockState, Vec3d tracePos){
        if (isHandling()){
            return false;
        }
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(blockState);
        if (facing != null){
            if (shouldReverse(blockState.getBlock())) {
                facing = facing.getOpposite();
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
            sendRotateEveryTick();
        }
    }
    private static void scheduledBlockPlacement(){
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockPos pos = BlockPos.fromLong(currentHandlePos);
        Direction side = RotationUtils.calculateFacing(stateSchematic);
        //assume item didn't changed and items are at main hand
        BlockHitResult hitResult = new BlockHitResult(tracedPos, side, pos, false);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hitResult);
    }
    private static boolean shouldReverse(Block block){
        // critical block lists
        return block instanceof PistonBlock || block instanceof DispenserBlock || block instanceof DropperBlock ||
                block instanceof RepeaterBlock || block instanceof ComparatorBlock || block instanceof BeehiveBlock;
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