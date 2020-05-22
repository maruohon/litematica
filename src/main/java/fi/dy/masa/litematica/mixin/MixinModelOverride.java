package fi.dy.masa.litematica.mixin;

import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.render.model.json.ModelOverride;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Mixin(ModelOverride.class)
public abstract class MixinModelOverride
{
    @Redirect(method = "matches", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/client/item/ModelPredicateProvider;call(" +
                       "Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/world/ClientWorld;" +
                       "Lnet/minecraft/entity/LivingEntity;)F"))
    private float fixCrashWithNullWorld(ModelPredicateProvider provider, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity)
    {
        if (world == null)
        {
            return provider.call(stack, MinecraftClient.getInstance().world, entity);
        }

        return provider.call(stack, world, entity);
    }
}
