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

package plus.dragons.createcentralkitchen.integration.farmersdelight.jei;

import com.zurrtum.create.AllItems;
import com.zurrtum.create.client.compat.jei.category.DeployingCategory;
import com.zurrtum.create.client.compat.jei.category.SawingCategory;
import com.zurrtum.create.client.compat.jei.renderer.TwoIconRenderer;
import com.zurrtum.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.zurrtum.create.content.kinetics.saw.CuttingRecipe;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.Internal;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import plus.dragons.createcentralkitchen.config.CCKConfig;
import plus.dragons.createcentralkitchen.integration.farmersdelight.recipe.CuttingBoardRecipeConverters;
import plus.dragons.createcentralkitchen.integration.jei.CCKJeiExtension;
import plus.dragons.createcentralkitchen.integration.jei.CCKJeiPlugin;
import vectorwing.farmersdelight.common.registry.ModItems;

/**
 * Shows the Cutting Board recipes that a saw and a deployer can now do, in two categories that look like Create's own
 * sawing and deploying ones.
 */
public class FDJeiExtension implements CCKJeiExtension {
    private static final String SAWING_KEY = "create_central_kitchen.recipe.farmersdelight.automatic_food_sawing";
    private static final String DEPLOYING_KEY = "create_central_kitchen.recipe.farmersdelight.automatic_food_cutting";
    public static final IRecipeType<RecipeHolder<CuttingRecipe>> SAWING = CCKJeiPlugin.recipeHolderType("farmersdelight.automatic_food_sawing");
    public static final IRecipeType<RecipeHolder<? extends ItemApplicationRecipe>> DEPLOYING = CCKJeiPlugin.recipeHolderType("farmersdelight.automatic_food_cutting");

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (CCKConfig.recipes().convertCuttingBoardRecipesToSawingRecipes.get()) {
            registration.addRecipeCategories(new SawingCategory() {
                @Override
                public IRecipeType<RecipeHolder<CuttingRecipe>> getRecipeType() {
                    return SAWING;
                }

                @Override
                public Component getTitle() {
                    return Component.translatable(SAWING_KEY);
                }

                @Override
                public IDrawable getIcon() {
                    return new TwoIconRenderer(AllItems.MECHANICAL_SAW, ModItems.CUTTING_BOARD.get());
                }
            });
        }
        if (CCKConfig.recipes().convertCuttingBoardRecipesToDeployingRecipes.get()) {
            registration.addRecipeCategories(new DeployingCategory() {
                @Override
                public IRecipeType<RecipeHolder<? extends ItemApplicationRecipe>> getRecipeType() {
                    return DEPLOYING;
                }

                @Override
                public Component getTitle() {
                    return Component.translatable(DEPLOYING_KEY);
                }

                @Override
                public IDrawable getIcon() {
                    return new TwoIconRenderer(AllItems.DEPLOYER, ModItems.CUTTING_BOARD.get());
                }
            });
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // JEI keeps its own copy of the server's recipes on the client.
        var index = CuttingBoardRecipeConverters.buildIndex(Internal.getClientSyncedRecipes().values());
        if (CCKConfig.recipes().convertCuttingBoardRecipesToSawingRecipes.get())
            registration.addRecipes(SAWING, index.sawing());
        if (CCKConfig.recipes().convertCuttingBoardRecipesToDeployingRecipes.get())
            registration.addRecipes(DEPLOYING, index.deploying().stream()
                    .<RecipeHolder<? extends ItemApplicationRecipe>>map(holder -> holder)
                    .toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (CCKConfig.recipes().convertCuttingBoardRecipesToSawingRecipes.get())
            registration.addCraftingStation(SAWING, AllItems.MECHANICAL_SAW);
        if (CCKConfig.recipes().convertCuttingBoardRecipesToDeployingRecipes.get())
            registration.addCraftingStation(DEPLOYING, AllItems.DEPLOYER, AllItems.DEPOT, AllItems.BELT_CONNECTOR);
    }
}
