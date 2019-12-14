package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;

@Mixin(SignBlockEntity.class)
public interface IMixinSignBlockEntity
{
    @Accessor("texts")
    Text[] litematica_getText();
}
