package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.util.SchematicEditUtils;
import fi.dy.masa.litematica.util.EasyPlaceUtils;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MixinMinecraft
{
    @Shadow private int rightClickDelayTimer;

    @Inject(method = "processKeyBinds", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/Minecraft;rightClickMouse()V", ordinal = 0))
    private void preRightClickMouse(CallbackInfo ci)
    {
        EasyPlaceUtils.setIsFirstClick(true);
    }

    @Inject(method = "processKeyBinds", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/Minecraft;rightClickMouse()V", ordinal = 0))
    private void postRightClickMouse(CallbackInfo ci)
    {
        EasyPlaceUtils.setIsFirstClick(false);
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void onRightClickMouseStart(CallbackInfo ci)
    {
        if (SchematicEditUtils.rebuildHandleBlockPlace((net.minecraft.client.Minecraft)(Object) this))
        {
            this.rightClickDelayTimer = 4;
            ci.cancel();
        }
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void onLeftClickMouseStart(CallbackInfo ci)
    {
        if (SchematicEditUtils.rebuildHandleBlockBreak((net.minecraft.client.Minecraft)(Object) this))
        {
            ci.cancel();
        }
    }

    @Inject(method = "runTick()V", at = @At("HEAD"))
    private void onRunTickStart(CallbackInfo ci)
    {
        DataManager.onClientTickStart();
    }
}
