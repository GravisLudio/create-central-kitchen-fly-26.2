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

package plus.dragons.createcentralkitchen.integration.jei;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import plus.dragons.createcentralkitchen.common.CCKCommon;

/**
 * JEI plugin of Central Kitchen. It has no categories of its own: each integration registers a
 * {@link CCKJeiExtension} from its client initializer, before JEI loads its plugins.
 */
@JeiPlugin
public class CCKJeiPlugin implements IModPlugin {
    public static final Identifier ID = CCKCommon.asResource("jei");
    private static final List<CCKJeiExtension> EXTENSIONS = new CopyOnWriteArrayList<>();

    public static void register(CCKJeiExtension extension) {
        EXTENSIONS.add(extension);
    }

    /** A JEI recipe type whose recipes are recipe holders, like Create's own. */
    @SuppressWarnings("unchecked")
    public static <R> IRecipeType<R> recipeHolderType(String path) {
        return (IRecipeType<R>) IRecipeType.create(CCKCommon.asResource(path), RecipeHolder.class);
    }

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        EXTENSIONS.forEach(extension -> extension.registerCategories(registration));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        EXTENSIONS.forEach(extension -> extension.registerRecipes(registration));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        EXTENSIONS.forEach(extension -> extension.registerRecipeCatalysts(registration));
    }
}
