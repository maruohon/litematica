package fi.dy.masa.litematica.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.hud.DebugHud;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;

@Mixin(DebugHud.class)
public abstract class MixinDebugHud
{
    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void addDebugLines(CallbackInfoReturnable<List<String>> cir)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            List<String> list = cir.getReturnValue();
            String pre = GuiBase.TXT_GOLD;
            String rst = GuiBase.TXT_RST;

            WorldRendererSchematic renderer = LitematicaRenderer.getInstance().getWorldRenderer();

            list.add(String.format("%s[Litematica]%s %s",
                                   pre, rst, renderer.getDebugInfoRenders()));

            String str = String.format("E: %d TE: TODO 1.17+ C: %d, CT: %d, CV: %d",
                                       world.getRegularEntityCount(),
                                       world.getChunkProvider().getLoadedChunkCount(),
                                       DataManager.getSchematicPlacementManager().getTouchedChunksCount(),
                                       DataManager.getSchematicPlacementManager().getLastVisibleChunksCount());
            list.add(String.format("%s[Litematica]%s %s %s", pre, rst, renderer.getDebugInfoEntities(), str));
        }
    }
}
