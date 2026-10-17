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

import com.zurrtum.create.client.content.kinetics.deployer.DeployerRenderer.DeployerItemRenderState;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plus.dragons.createcentralkitchen.client.deployer.HandheldDeployerItemRenderState;
import plus.dragons.createcentralkitchen.config.CCKConfig;
import plus.dragons.createcentralkitchen.data.CCKItemTags;

/**
 * Items in {@code #create:handheld_in_deployer_use} (tools, bows...) are drawn like in the deployer's punch mode, tilted
 * a little, even when it is in use mode.
 */
@Mixin(DeployerItemRenderState.class)
public abstract class DeployerItemRenderStateMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void createCentralKitchen$handheldInUse(
            DeployerBlockEntity be, ItemModelResolver itemModelManager, Level world, ItemStack heldItem,
            Vec3 offset, Direction facing, CallbackInfoReturnable<DeployerItemRenderState> cir) {
        if (be.mode == DeployerBlockEntity.Mode.PUNCH || !heldItem.is(CCKItemTags.HANDHELD_IN_DEPLOYER_USE))
            return;
        if (!CCKConfig.client().renderDeployerUsingItemWithCustomTransform.get())
            return;
        cir.setReturnValue(new HandheldDeployerItemRenderState(itemModelManager, world, heldItem, offset, facing));
    }
}
