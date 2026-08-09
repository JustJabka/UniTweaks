package net.danygames2014.unitweaks.mixin.tweaks.debugoverlay;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.danygames2014.unitweaks.UniTweaks;
import net.danygames2014.unitweaks.util.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Shadow
    private Minecraft minecraft;

    @Unique private static final int TEXT_OFFSET_X = 2;
    @Unique private static final int TEXT_OFFSET_Y = 8;
    @Unique private static final int TEXT_COLOR = 14737632;
    @Unique private static final String UNKNOW_BIOME_FALLBACK = "Unknown";
    @Unique private static int row = 0;

    @Unique
    private Config.UserInterfaceConfig.DebugOverlayConfig getDebugOverlayConfig() {
        return UniTweaks.USER_INTERFACE_CONFIG.debugOverlayConfig;
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
            )
    )
    private void roundPlayerCoordinates(InGameHud instance, TextRenderer renderer, String text, int x, int y, int z, Operation<Void> original) {
        if (!getDebugOverlayConfig().roundedDebugCoordinates) {
            original.call(instance, renderer, text, x, y, z);
            return;
        }

        if (text.startsWith("x: ")) text = getFormattedCoordinate('x');
        else if (text.startsWith("y: ")) text = getFormattedCoordinate('y');
        else if (text.startsWith("z: ")) text = getFormattedCoordinate('z');

        original.call(instance, renderer, text, x, y, z);
    }

    @Unique
    private String getFormattedCoordinate(char sign) {
        ClientPlayerEntity player = this.minecraft.player;
        if (player == null) return "%s: 0.0".formatted(sign);

        double coordinate = switch (sign) {
            case 'x' -> player.x;
            case 'y' -> player.boundingBox.minY;
            case 'z' -> player.z;
            default -> 0.0;
        };

        return String.format("%s: %.1f", sign, coordinate);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;III)V"
            )
    )
    private void renderAdditionalDebugInfo(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
        ClientPlayerEntity player = this.minecraft.player;
        if (player == null) return;

        World world = player.world;
        if (world == null) return;

        if (!minecraft.options.debugHud) return;

        Config.UserInterfaceConfig.DebugOverlayConfig config = getDebugOverlayConfig();

        int x = MathHelper.floor(player.x);
        int y = MathHelper.floor(player.y);
        int z = MathHelper.floor(player.z);

        row = 0;

        if (config.showDebugDayCounter) {
            renderDebugInfo("Day: %s".formatted(getDayCount(world)));
        }

        if (config.showDebugLightLevel) {
            renderDebugInfo("Light Level: %s".formatted(world.getLightLevel(x, y, z)));
        }

        if (config.showDebugBiome) {
            renderDebugInfo("Biome: %s".formatted(getBiome(world, x, z)));
        }

        if (config.showDebugSlimeChunk) {
            renderDebugInfo("Slime Chunk: %s".formatted(isSlimeChunk(world, x, z)));
        }

        if (config.showDebugDimension) {
            renderDebugInfo("Dimension: %s".formatted(world.dimension.id));
        }

        if (config.showDebugWorldSeed) {
            renderDebugInfo("World Seed: %s".formatted(world.getSeed()));
        }
    }

    @Unique
    private void renderDebugInfo(String text) {
        TextRenderer renderer = this.minecraft.textRenderer;
        renderer.drawWithShadow(
                text,
                TEXT_OFFSET_X,
                calcTextOffsetY(row++),
                TEXT_COLOR
        );
    }

    @Unique
    private int calcTextOffsetY(int row) {
        return getDebugOverlayConfig().overlayAdditionsYOffset + (row * TEXT_OFFSET_Y);
    }

    @Unique
    private long getDayCount(World world) {
        long time = world.getProperties().getTime();
        final long dayCycle = 24000;

        return time / dayCycle;
    }

    @Unique
    private String getBiome(World world, int x, int z) {
        BiomeSource biomeSource = world.method_1781();
        if (biomeSource == null) return UNKNOW_BIOME_FALLBACK;

        Biome biome = biomeSource.getBiome(x, z);
        return biome != null ? biome.name : UNKNOW_BIOME_FALLBACK;
    }

    @Unique
    private boolean isSlimeChunk(World world, int x, int z) {
        Chunk chunk = world.getChunkFromPos(x, z);
        return (chunk.getSlimeRandom(987234911L).nextInt(10) == 0);
    }
}
