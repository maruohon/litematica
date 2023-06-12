package fi.dy.masa.litematica.mixin;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SignBlockEntity.class)
public interface IMixinSignBlockEntity
{
    @Accessor("frontText")
    SignText litematica_getFrontText();
    @Accessor("backText")
    SignText litematica_getBackText();
}
