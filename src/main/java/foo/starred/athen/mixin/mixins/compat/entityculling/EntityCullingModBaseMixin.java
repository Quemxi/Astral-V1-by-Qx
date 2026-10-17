package foo.starred.athen.mixin.mixins.compat.entityculling;

import dev.tr7zw.entityculling.EntityCullingModBase;
import foo.starred.athen.api.messaging.impl.MessagingAPI;
import foo.starred.athen.modules.impl.ModSettings;
import foo.starred.snowbird.api.text.parser.impl.ParserKt;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityCullingModBase.class, remap = false)
public class EntityCullingModBaseMixin {
    @Inject(method = "clientTick", at = @At("HEAD"))
    private void disableTickCulling(CallbackInfo ci) {
        if (!ModSettings.getDisableTickCulling()) return;

        EntityCullingModBase instance = (EntityCullingModBase) (Object) this;
        if (instance.config == null) return;
        if (!instance.config.tickCulling) return;

        instance.config.tickCulling = false;
        Component literal = ParserKt.parse("<hover:This was done to improve compatibility with Astral's slayer features!>Disabled tick culling in the mod \"Entity culling\"!", true);
        MessagingAPI.mod(literal);
    }
}
