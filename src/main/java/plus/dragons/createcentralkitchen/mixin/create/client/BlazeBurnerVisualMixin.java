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

import com.zurrtum.create.client.content.processing.burner.BlazeBurnerVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.lib.instance.InstanceTypes;
import com.zurrtum.create.client.flywheel.lib.instance.TransformedInstance;
import com.zurrtum.create.client.flywheel.lib.model.Models;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.flywheel.lib.visual.AbstractBlockEntityVisual;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerClientExtension;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerRenderOverride;

/**
 * Hat of the override on the Flywheel visual. Create keeps one {@code hat} instance (train or logistics hat); when the
 * override's hat is wanted instead, this keeps that instance on the override's model and tells Create a hat is present
 * so it does not replace it.
 */
@Mixin(BlazeBurnerVisual.class)
public abstract class BlazeBurnerVisualMixin extends AbstractBlockEntityVisual<BlazeBurnerBlockEntity> {
    @Shadow
    private HeatLevel heatLevel;
    @Shadow
    private @Nullable TransformedInstance hat;

    @Unique
    private @Nullable PartialModel createCentralKitchen$hatModelOverride;

    private BlazeBurnerVisualMixin(VisualizationContext ctx, BlazeBurnerBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
    }

    @ModifyVariable(method = "animate", at = @At("STORE"), name = "hatPresent")
    private boolean createCentralKitchen$updateChefHat(boolean hatPresent) {
        var renderOverride = ((BlazeBurnerClientExtension) blockEntity).getRenderOverride();
        // The visual does not scale hats down, so the small head gets the small hat.
        boolean inert = !heatLevel.isAtLeast(HeatLevel.FADING);
        PartialModel wanted = renderOverride == null || hatPresent ? null : renderOverride.getHatModel(inert);
        if (!BlazeBurnerRenderOverride.isModelLoaded(wanted))
            wanted = null;
        if (wanted == null) {
            // Back to Create's own hat (or none): drop the instance that carried the override's model.
            if (createCentralKitchen$hatModelOverride != null && hat != null) {
                hat.delete();
                hat = null;
            }
            createCentralKitchen$hatModelOverride = null;
            return hatPresent;
        }
        if (createCentralKitchen$hatModelOverride != wanted) {
            var instancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(wanted));
            if (hat != null)
                instancer.stealInstance(hat);
            else
                hat = instancer.createInstance();
            createCentralKitchen$hatModelOverride = wanted;
        }
        return true;
    }
}
