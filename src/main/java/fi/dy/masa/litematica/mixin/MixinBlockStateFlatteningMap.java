package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;

@Mixin(BlockStateFlatteningMap.class)
public abstract class MixinBlockStateFlatteningMap
{
    @Inject(method = "addEntry", at = @At("HEAD"))
    private static void onAddEntry(int id, String fixedNBT, String[] sourceNBTs, CallbackInfo ci)
    {
        SchematicConversionMaps.addEntry(id, fixedNBT, sourceNBTs);
    }
}
