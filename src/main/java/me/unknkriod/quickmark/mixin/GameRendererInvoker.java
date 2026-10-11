package me.unknkriod.quickmark.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker accessors for the private view transforms {@link GameRenderer} applies
 * while rendering the world.
 *
 * <p>{@code GameRenderer#project(Vec3d)} only accounts for the camera rotation
 * and the perspective projection, so it silently ignores the hurt tilt and the
 * view bobbing. HUD elements projected with it therefore drift relative to the
 * world while the player is moving. These invokers let the mod reproduce the
 * exact projection the world itself is rendered with.</p>
 */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {

    @Invoker("getFov")
    float invokeGetFov(Camera camera, float tickProgress, boolean changingFov);

    @Invoker("tiltViewWhenHurt")
    void invokeTiltViewWhenHurt(MatrixStack matrices, float tickProgress);

    @Invoker("bobView")
    void invokeBobView(MatrixStack matrices, float tickProgress);
}
