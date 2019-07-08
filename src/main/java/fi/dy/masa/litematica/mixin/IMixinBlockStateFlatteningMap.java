package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;

@Mixin(BlockStateFlatteningMap.class)
public interface IMixinBlockStateFlatteningMap
{
    @Accessor("NAME_TO_ID")
    public static Object2IntMap<String> getOldNameToShiftedOldBlockIdMap() {return null; }
}
