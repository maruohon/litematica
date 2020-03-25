package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.util.SchematicEditUtils;
import fi.dy.masa.litematica.util.EasyPlaceUtils;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MixinMinecraft
{
    @Inject(method = "processKeyBinds", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/Minecraft;rightClickMouse()V"))
    private void onRightClickMouse(CallbackInfo ci)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld())
        {
            EasyPlaceUtils.setIsFirstClick(true);
        }
    }

    @Inject(method = "rightClickMouse", cancellable = true, at = @At(value = "FIELD", ordinal = 0,
            target = "Lnet/minecraft/client/Minecraft;objectMouseOver:Lnet/minecraft/util/math/RayTraceResult;"))
    private void onHandleRightClickPre(CallbackInfo ci)
    {
        if (SchematicEditUtils.rebuildHandleBlockPlace((net.minecraft.client.Minecraft)(Object) this))
        {
            ci.cancel();
        }
    }

    @Inject(method = "rightClickMouse", at = @At("TAIL"))
    private void onRightClickMouseTail(CallbackInfo ci)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld())
        {
            EasyPlaceUtils.onRightClickTail((net.minecraft.client.Minecraft)(Object) this);
        }
    }

    @Inject(method = "clickMouse", cancellable = true, at = @At(value = "FIELD", ordinal = 0,
            target = "Lnet/minecraft/util/math/RayTraceResult;typeOfHit:Lnet/minecraft/util/math/RayTraceResult$Type;"))
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
