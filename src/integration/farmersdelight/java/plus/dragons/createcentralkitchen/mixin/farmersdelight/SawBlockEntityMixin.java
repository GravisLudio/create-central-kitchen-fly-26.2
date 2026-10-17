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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.kinetics.saw.SawBlockEntity;
import com.zurrtum.create.foundation.recipe.RecipeFinder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import plus.dragons.createcentralkitchen.config.CCKConfig;
import plus.dragons.createcentralkitchen.integration.farmersdelight.recipe.CuttingBoardRecipeConverters;

/**
 * Adds the Cutting Board recipes that accept an iron knife to the list of recipes a saw can run. Create caches that
 * list, so the merged list is cached too (by the identity of Create's list, which changes on every reload).
 */
@Mixin(SawBlockEntity.class)
public abstract class SawBlockEntityMixin {
    private record Merged(List<?> sawing, List<RecipeHolder<?>> recipes) {}

    private static final Cache<List<RecipeHolder<?>>, Merged> MERGED = CacheBuilder.newBuilder().weakKeys().build();

    @WrapOperation(method = "updateRecipe", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/foundation/recipe/RecipeFinder;get(Ljava/lang/Object;Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<RecipeHolder<?>> addCuttingBoardRecipes(Object cacheKey, ServerLevel level, Predicate<RecipeHolder<?>> conditions, Operation<List<RecipeHolder<?>>> original) {
        var found = original.call(cacheKey, level, conditions);
        if (!CCKConfig.recipes().convertCuttingBoardRecipesToSawingRecipes.get())
            return found;
        var sawing = CuttingBoardRecipeConverters.index(level.recipeAccess()).sawing();
        if (sawing.isEmpty())
            return found;
        var cached = MERGED.getIfPresent(found);
        if (cached != null && cached.sawing() == sawing)
            return cached.recipes();
        var merged = new ArrayList<RecipeHolder<?>>(found);
        merged.addAll(sawing);
        var result = List.copyOf(merged);
        MERGED.put(found, new Merged(sawing, result));
        return result;
    }
}
