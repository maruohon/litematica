package fi.dy.masa.litematica.render;

import com.mumfrey.liteloader.util.debug.DebugMessage;
import com.mumfrey.liteloader.util.debug.DebugMessage.Position;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;

public class DebugScreenMessages
{
    public static final DebugMessage MESSAGE_EMPTY_LINE = DebugMessage.create(Position.LEFT_BOTTOM, "");
    public static final DebugMessage MESSAGE_RENDERER = DebugMessage.create(Position.LEFT_BOTTOM, "");
    public static final DebugMessage MESSAGE_ENTITIES = DebugMessage.create(Position.LEFT_BOTTOM, "");

    public static void update(Minecraft mc)
    {
        if (mc.gameSettings.showDebugInfo)
        {
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

            if (world != null)
            {
                RenderGlobal render = LitematicaRenderer.getInstance().getWorldRenderer();

                String pre = GuiBase.TXT_GOLD;
                String rst = GuiBase.TXT_RST;

                MESSAGE_RENDERER.setMessage(String.format("%s[Litematica]%s %s", pre, rst, render.getDebugInfoRenders()));

                String str = String.format("E %s TE: %d", world.getDebugLoadedEntities(), world.loadedTileEntityList.size());
                MESSAGE_ENTITIES.setMessage(String.format("%s[Litematica]%s %s %s", pre, rst, render.getDebugInfoEntities(), str));
            }
        }
    }
}
