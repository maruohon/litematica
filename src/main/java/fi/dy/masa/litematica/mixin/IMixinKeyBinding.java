package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;

@Mixin(KeyBinding.class)
public interface IMixinKeyBinding
{
    @Accessor("keyCode")
    InputMappings.Input getInput();
}
