package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(HandledScreen.class)
public interface IMixinHandledScreen
{
    @Accessor("field_2776") // x
    int litematica_getX();

    @Accessor("field_2800") // y
    int litematica_getY();
}
