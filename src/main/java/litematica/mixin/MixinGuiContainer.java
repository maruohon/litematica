package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import litematica.config.Configs;
import litematica.materials.MaterialListHudRenderer;

@Mixin(net.minecraft.client.gui.inventory.GuiContainer.class)
public abstract class MixinGuiContainer extends net.minecraft.client.gui.GuiScreen
{
    @Inject(method = "drawScreen", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V"))
    private void hilightSlots(int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        if (Configs.InfoOverlays.MATERIAL_LIST_SLOT_HIGHLIGHT.getBooleanValue())
        {
            MaterialListHudRenderer.renderSlotHighlights((net.minecraft.client.gui.inventory.GuiContainer) (Object) this);
        }
    }
}
