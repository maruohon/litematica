package fi.dy.masa.litematica.mixin;

import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Mixin(ModelOverrideList.class)
public abstract class MixinModelOverrideList
{
    @SuppressWarnings("deprecation")
    @Redirect(method = "apply", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/client/item/ModelPredicateProvider;call(" +
                       "Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/world/ClientWorld;" +
                       "Lnet/minecraft/entity/LivingEntity;I)F"))
    private float fixCrashWithNullWorld(ModelPredicateProvider provider,
                                        ItemStack stack,
                                        @Nullable ClientWorld world,
                                        @Nullable LivingEntity entity,
                                        int i)
    {
        if (world == null)
        {
            return provider.call(stack, MinecraftClient.getInstance().world, entity, i);
        }

        return provider.call(stack, world, entity, i);
    }
}
