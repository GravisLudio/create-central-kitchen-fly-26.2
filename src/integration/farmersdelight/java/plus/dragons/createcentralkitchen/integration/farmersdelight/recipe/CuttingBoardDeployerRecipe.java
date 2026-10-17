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

import com.zurrtum.create.AllRecipeSerializers;
import com.zurrtum.create.AllRecipeTypes;
import com.zurrtum.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.zurrtum.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * A Cutting Board recipe seen as a Deployer recipe. Create-Fly's recipes are records and cannot be extended, so this
 * record implements {@link ItemApplicationRecipe} itself and borrows the serializer and type of
 * {@link DeployerApplicationRecipe}. It only ever lives in memory (it is never registered or sent to clients).
 */
public record CuttingBoardDeployerRecipe(List<ProcessingOutput> results, boolean keepHeldItem, Ingredient target,
        Ingredient ingredient) implements ItemApplicationRecipe {
    @Override
    public RecipeSerializer<DeployerApplicationRecipe> getSerializer() {
        return AllRecipeSerializers.DEPLOYING;
    }

    @Override
    public RecipeType<DeployerApplicationRecipe> getType() {
        return AllRecipeTypes.DEPLOYING;
    }

    /** Knives are damageable: they must wear down instead of being kept for free. */
    public boolean shouldKeepHeldTool(ItemStack heldTool) {
        return keepHeldItem() && !heldTool.isDamageableItem();
    }
}
