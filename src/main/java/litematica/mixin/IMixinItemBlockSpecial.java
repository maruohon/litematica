package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockSpecial;

@Mixin(ItemBlockSpecial.class)
public interface IMixinItemBlockSpecial
{
    @Accessor("block")
    Block getBlock();
}
