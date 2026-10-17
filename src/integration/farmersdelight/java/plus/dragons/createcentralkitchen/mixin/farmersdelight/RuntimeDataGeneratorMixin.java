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

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.JsonOps;
import com.zurrtum.create.content.kinetics.saw.CuttingRecipe;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import com.zurrtum.create.foundation.pack.RuntimeDataGenerator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import plus.dragons.createcentralkitchen.config.CCKConfig;
import vectorwing.farmersdelight.common.registry.ModItems;

/**
 * Create generates sawing recipes that strip logs. With Farmer's Delight loaded, stripping also drops tree bark
 * (or straw for bamboo-like blocks).
 */
@Mixin(RuntimeDataGenerator.class)
public class RuntimeDataGeneratorMixin {
    @Shadow
    @Final
    private static Object2ObjectOpenHashMap<Identifier, JsonElement> JSON_FILES;

    @WrapOperation(method = "cuttingRecipes", at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/zurrtum/create/foundation/pack/RuntimeDataGenerator;simpleWoodRecipe(Lnet/minecraft/resources/Identifier;Lnet/minecraft/resources/Identifier;Lnet/minecraft/resources/Identifier;)V"))
    private static void addTreeBarkToStripCuttingRecipes(Identifier typeId, Identifier unstripped, Identifier stripped, Operation<Void> original, @Local(name = "type") String type) {
        if (!CCKConfig.recipes().addTreeBarkToStripSawingRecipes.get()) {
            original.call(typeId, unstripped, stripped);
            return;
        }
        if (!BuiltInRegistries.ITEM.containsKey(stripped))
            return;
        var recipe = new CuttingRecipe(
                50,
                List.of(
                        new ProcessingOutput(BuiltInRegistries.ITEM.getValue(stripped), 1),
                        new ProcessingOutput(type.contains("block") ? ModItems.STRAW.get() : ModItems.TREE_BARK.get(), 1)),
                Ingredient.of(BuiltInRegistries.ITEM.getValue(unstripped)));
        // Same path Create uses for its own runtime recipes, so this replaces the plain one.
        var path = Identifier.fromNamespaceAndPath(typeId.getNamespace(), "recipe/" + typeId.getPath()
                + "/runtime_generated/compat/" + unstripped.getNamespace() + "/" + unstripped.getPath() + "_to_" + stripped.getPath());
        Recipe.CODEC.encodeStart(JsonOps.INSTANCE, recipe).ifSuccess(json -> JSON_FILES.put(path, json));
    }
}
