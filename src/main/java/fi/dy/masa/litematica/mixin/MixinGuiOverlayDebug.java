package fi.dy.masa.litematica.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.text.TextFormatting;

@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug
{
    @Inject(method = "renderDebugInfoLeft()V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/String;)Z",
                     shift = Shift.AFTER, ordinal = 2, remap = false),
            locals = LocalCapture.CAPTURE_FAILSOFT, require = 0)
    private void onRenderDebugInfoLeft(CallbackInfo ci, List<String> list)
    {
        this.addInfoLines(list);
    }

    private void addInfoLines(List<String> list)
    {
        WorldClient world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            String pre = TextFormatting.GOLD.toString();
            String rst = TextFormatting.RESET.toString();

            WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();

            list.add("");
            list.add(String.format("%s[Litematica]%s %s", pre, rst, renderer.getDebugInfoRenders()));

            String str = String.format("E %s TE: %d", world.getDebugLoadedEntities(), world.loadedTileEntityList.size());
            list.add(String.format("%s[Litematica]%s %s %s", pre, rst, renderer.getDebugInfoEntities(), str));
        }
    }
}
