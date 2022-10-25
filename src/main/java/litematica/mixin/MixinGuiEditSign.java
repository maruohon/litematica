package litematica.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import litematica.config.Configs;
import litematica.util.WorldUtils;

@Mixin(value = net.minecraft.client.gui.inventory.GuiEditSign.class, priority = 999)
public abstract class MixinGuiEditSign
{
    @Shadow @Final private net.minecraft.tileentity.TileEntitySign tileSign;

    @Inject(method = "initGui", at = @At("HEAD"))
    private void insertSignText(CallbackInfo ci)
    {
        if (Configs.Generic.SIGN_TEXT_PASTE.getBooleanValue())
        {
            WorldUtils.insertSignTextFromSchematic(this.tileSign);
        }
    }
}
