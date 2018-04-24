package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.nbt.NBTTagLongArray;

@Mixin(NBTTagLongArray.class)
public interface IMixinNBTTagLongArray
{
    @Accessor("data")
    long[] getArray();
}
