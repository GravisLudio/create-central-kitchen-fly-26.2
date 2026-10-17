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

package plus.dragons.createcentralkitchen.mixin.farmersdelight;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import com.zurrtum.create.content.kinetics.deployer.ItemApplicationInput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import plus.dragons.createcentralkitchen.config.CCKConfig;
import plus.dragons.createcentralkitchen.integration.farmersdelight.recipe.CuttingBoardRecipeConverters;

/**
 * Create-Fly has no event for add-ons to offer deployer recipes (NeoForge's {@code DeployerRecipeSearchEvent}), so
 * when Create finds nothing, the Cutting Board recipes seen as deployer recipes are tried.
 */
@Mixin(DeployerBlockEntity.class)
public abstract class DeployerBlockEntityMixin {
    @ModifyReturnValue(method = "getRecipe", at = @At("RETURN"))
    private @Nullable Recipe<? extends RecipeInput> addCuttingBoardRecipes(@Nullable Recipe<? extends RecipeInput> original, ItemStack stack) {
        if (original != null || !CCKConfig.recipes().convertCuttingBoardRecipesToDeployingRecipes.get())
            return original;
        var deployer = (DeployerBlockEntity) (Object) this;
        var player = deployer.getPlayer();
        if (player == null || !(deployer.getLevel() instanceof ServerLevel level))
            return null;
        var input = new ItemApplicationInput(stack, player.cast().getMainHandItem());
        for (var holder : CuttingBoardRecipeConverters.index(level.recipeAccess()).deploying()) {
            if (holder.value().matches(input, level))
                return holder.value();
        }
        return null;
    }
}
