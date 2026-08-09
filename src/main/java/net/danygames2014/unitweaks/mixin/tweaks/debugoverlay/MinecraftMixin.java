package net.danygames2014.unitweaks.mixin.tweaks.debugoverlay;

import net.danygames2014.unitweaks.UniTweaks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "renderProfilerChart", at = @At("HEAD"), cancellable = true)
    private void disableProfilerChart(long tickTime, CallbackInfo ci) {
        if (!UniTweaks.USER_INTERFACE_CONFIG.debugOverlayConfig.disableDebugProfilerChart) return;
        ci.cancel();
    }
}
