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

package plus.dragons.createcentralkitchen.common.packager;

import com.zurrtum.create.api.packager.unpacking.UnpackingHandler;
import com.zurrtum.create.infrastructure.component.PackageOrderWithCrafts;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class ShapelessUnpackingHandler implements UnpackingHandler {
    protected final int slotStart;
    protected final int slotCount;

    protected ShapelessUnpackingHandler(int slotStart, int slotCount) {
        this.slotStart = slotStart;
        this.slotCount = slotCount;
    }

    @Override
    public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        // Recipes only exist on the logical server.
        if (!(level instanceof ServerLevel serverLevel))
            return false;
        var inventory = getInventory(level, pos, side);
        if (inventory == null)
            return false;
        var device = new DeviceAdapter(inventory, slotStart, slotCount,
                UnpackingRecipeCache.get(serverLevel.recipeAccess(), getRecipeType()),
                recipe -> acceptsRecipe(level, pos, recipe));
        var plan = RecipeUnpacking.plan(device, items, orderContext);
        return plan != null && plan.apply(inventory, simulate);
    }

    protected abstract @Nullable Container getInventory(Level level, BlockPos pos, Direction side);

    protected abstract RecipeType<?> getRecipeType();

    protected boolean acceptsRecipe(Level level, BlockPos pos, Recipe<?> recipe) {
        // Fuel, fluid, molds and serving containers remain the machine's responsibility.
        return true;
    }

    private record DeviceAdapter(Container inventory, int slotStart, int slotCount,
            List<RecipeUnpacking.Candidate> recipes, Predicate<Recipe<?>> acceptsRecipe) implements RecipeUnpacking.Device {}
}
