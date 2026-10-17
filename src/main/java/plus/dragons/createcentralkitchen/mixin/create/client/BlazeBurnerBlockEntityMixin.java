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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerClientExtension;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerRenderOverride;

/** Gives every blaze burner a place to keep its current render override, and lets the override fake the block above. */
@Mixin(BlazeBurnerBlockEntity.class)
public abstract class BlazeBurnerBlockEntityMixin extends SmartBlockEntity implements BlazeBurnerClientExtension {
    @Unique
    private @Nullable BlazeBurnerRenderOverride createCentralKitchen$renderOverride;

    protected BlazeBurnerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    @Override
    public BlazeBurnerRenderOverride getRenderOverride() {
        return createCentralKitchen$renderOverride;
    }

    @Override
    public void setRenderOverride(@Nullable BlazeBurnerRenderOverride renderOverride) {
        this.createCentralKitchen$renderOverride = renderOverride;
    }

    @ModifyReturnValue(method = "isValidBlockAbove", at = @At("RETURN"))
    private boolean createCentralKitchen$overrideValidBlockAbove(boolean original) {
        var override = createCentralKitchen$renderOverride;
        return override == null ? original : override.isValidBlockAbove(original);
    }
}
