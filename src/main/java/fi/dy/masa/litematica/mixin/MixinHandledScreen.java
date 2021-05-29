package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.text.ITextComponent;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;

@Mixin(ContainerScreen.class)
public abstract class MixinHandledScreen extends Screen
{
    private MixinHandledScreen(ITextComponent title)
    {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screen/inventory/ContainerScreen;drawBackground(Lcom/mojang/blaze3d/matrix/MatrixStack;FII)V"))
    private void renderSlotHighlights(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        MaterialListHudRenderer.renderLookedAtBlockInInventory((ContainerScreen<?>) (Object) this, this.client);
    }
}
