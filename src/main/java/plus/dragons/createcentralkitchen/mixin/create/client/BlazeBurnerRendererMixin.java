/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package plus.dragons.createcentralkitchen.mixin.create.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.processing.burner.BlazeBurnerRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerClientExtension;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerRenderOverride;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerRenderOverrides;

@Mixin(BlazeBurnerRenderer.class)
public class BlazeBurnerRendererMixin {
    /** Picks the override before the animation reads {@code isValidBlockAbove()}. */
    @Inject(method = "tickAnimation", at = @At("HEAD"))
    private static void createCentralKitchen$updateOverride(BlazeBurnerBlockEntity burner, CallbackInfo ci) {
        BlazeBurnerRenderOverrides.update(burner);
    }

    /**
     * The hat Create draws on its own (train hat, logistics hat) wins; otherwise the override's hat is used. The
     * renderer already scales a hat down on the small inert head, so it always gets the full-size model.
     */
    @ModifyVariable(method = "extractRenderState(Lcom/zurrtum/create/content/processing/burner/BlazeBurnerBlockEntity;Lcom/zurrtum/create/client/content/processing/burner/BlazeBurnerRenderer$BlazeBurnerRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("STORE"), name = "drawHat")
    private @Nullable PartialModel createCentralKitchen$overrideHat(
            @Nullable PartialModel drawHat, @Local(argsOnly = true) BlazeBurnerBlockEntity burner) {
        if (drawHat != null)
            return drawHat;
        BlazeBurnerRenderOverride override = ((BlazeBurnerClientExtension) burner).getRenderOverride();
        if (override == null)
            return null;
        PartialModel hat = override.getHatModel(false);
        return BlazeBurnerRenderOverride.isModelLoaded(hat) ? hat : null;
    }
}
