package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicVerifier;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient extends World
{
    protected MixinWorldClient(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client)
    {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    @Inject(method = "doPreChunk(IIZ)V", at = @At("RETURN"))
    private void onDoPreChunk(int chunkX, int chunkZ, boolean loadChunk, CallbackInfo ci)
    {
        if (this == (World) Minecraft.getMinecraft().world)
        {
            SchematicWorldHandler.getInstance().loadChunk(chunkX, chunkZ, loadChunk);
        }
    }

    @Inject(method = "invalidateRegionAndSetBlock", at = @At("HEAD"))
    private void onInvalidateRegionAndSetBlock(BlockPos pos, IBlockState state, CallbackInfoReturnable<Boolean> ci)
    {
        SchematicVerifier verifier = DataManager.getActiveSchematicVerifier();

        if (verifier != null)
        {
            verifier.markBlockChanged(pos);
        }
    }
}
