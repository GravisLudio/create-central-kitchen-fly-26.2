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

package plus.dragons.createcentralkitchen.integration.farmersdelight.recipe;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zurrtum.create.AllRecipeTypes;
import com.zurrtum.create.content.kinetics.saw.CuttingRecipe;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import java.util.Collection;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import plus.dragons.createcentralkitchen.common.recipe.RecipeConverter;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.registry.ModItems;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

public class CuttingBoardRecipeConverters {
    private static final int SAWING_DURATION = 50;

    public static final RecipeConverter<CuttingBoardRecipe, CuttingRecipe> SAWING = RecipeConverter.cached(
            CacheBuilder.newBuilder(), holder -> {
                var recipe = holder.value();
                return new RecipeHolder<>(derivedId(holder, "_using_saw"),
                        new CuttingRecipe(SAWING_DURATION, outputs(recipe), recipe.getInput()));
            });

    public static final RecipeConverter<CuttingBoardRecipe, CuttingBoardDeployerRecipe> DEPLOYING = RecipeConverter.cached(
            CacheBuilder.newBuilder(), holder -> {
                var recipe = holder.value();
                // The deployer keeps its tool: a knife wears down only if it is damageable (see CuttingBoardDeployerRecipe).
                return new RecipeHolder<>(derivedId(holder, "_using_deployer"),
                        new CuttingBoardDeployerRecipe(outputs(recipe), true, recipe.getInput(), recipe.getTool()));
            });

    /** Everything derived from one RecipeManager, computed once per reload. */
    public record Index(List<RecipeHolder<CuttingBoardRecipe>> cutting,
            List<RecipeHolder<CuttingRecipe>> sawing,
            List<RecipeHolder<CuttingBoardDeployerRecipe>> deploying) {}

    private static final Cache<RecipeManager, Index> INDEXES = CacheBuilder.newBuilder().weakKeys().build();

    /** Drops the cached indexes whenever recipes or tags are reloaded. */
    public static void register() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> INDEXES.invalidateAll());
    }

    public static Index index(RecipeManager manager) {
        return INDEXES.asMap().computeIfAbsent(manager, CuttingBoardRecipeConverters::buildIndex);
    }

    private static Index buildIndex(RecipeManager manager) {
        return buildIndex(manager.getRecipes());
    }

    /** Builds the index from any set of recipes, e.g. the copy JEI keeps on the client. */
    @SuppressWarnings("unchecked")
    public static Index buildIndex(Collection<RecipeHolder<?>> recipes) {
        var cutting = recipes.stream()
                .filter(holder -> holder.value().getType() == ModRecipeTypes.CUTTING.get())
                .map(holder -> (RecipeHolder<CuttingBoardRecipe>) holder)
                .toList();
        var automated = cutting.stream().filter(AllRecipeTypes.CAN_BE_AUTOMATED).toList();
        var sawing = automated.stream().filter(CuttingBoardRecipeConverters::canSaw).map(SAWING).toList();
        var deploying = automated.stream().map(DEPLOYING).toList();
        return new Index(cutting, sawing, deploying);
    }

    private static ResourceKey<Recipe<?>> derivedId(RecipeHolder<CuttingBoardRecipe> holder, String suffix) {
        return ResourceKey.create(Registries.RECIPE, holder.id().identifier().withSuffix(suffix));
    }

    private static List<ProcessingOutput> outputs(CuttingBoardRecipe recipe) {
        return recipe.getRollableResults().stream()
                .map(result -> new ProcessingOutput(result.stack().item(), result.stack().count(),
                        result.stack().components(), result.chance()))
                .toList();
    }

    public static ItemStack sawAsKnife() {
        return new ItemStack(ModItems.IRON_KNIFE.get());
    }

    public static boolean canSaw(RecipeHolder<CuttingBoardRecipe> holder) {
        return holder.value().getTool().test(sawAsKnife());
    }
}
