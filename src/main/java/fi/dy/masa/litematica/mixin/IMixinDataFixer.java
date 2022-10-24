package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.datafix.DataFixer;

@Mixin(DataFixer.class)
public interface IMixinDataFixer
{
    @Accessor("version")
    int getVersion();
}
