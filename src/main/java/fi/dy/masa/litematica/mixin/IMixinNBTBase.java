package fi.dy.masa.litematica.mixin;

import java.io.DataOutput;
import java.io.IOException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.nbt.NBTBase;

@Mixin(NBTBase.class)
public interface IMixinNBTBase
{
    @Invoker("write")
    void invokeWrite(DataOutput output) throws IOException;
}
