package litematica.mixin.debug;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.GuiOverlayDebug;

import litematica.render.DebugScreenMessages;

@Mixin(GuiOverlayDebug.class)
public abstract class GuiOverlayDebugMixin
{
    @Inject(method = "call", at = @At("RETURN"))
    private void litematica_addDebugLines(CallbackInfoReturnable<List<String>> cir)
    {
        DebugScreenMessages.addDebugScreenMessages(cir.getReturnValue());
    }
}
