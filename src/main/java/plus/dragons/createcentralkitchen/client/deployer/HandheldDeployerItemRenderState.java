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

package plus.dragons.createcentralkitchen.client.deployer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zurrtum.create.client.content.kinetics.deployer.DeployerRenderer.DeployerItemPunchRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The deployer's punch-mode drawing of a held item, tilted a little. It lives outside the mixin package on purpose:
 * Mixin refuses to load ordinary classes from the packages of a mixin config.
 */
public class HandheldDeployerItemRenderState extends DeployerItemPunchRenderState {
    public HandheldDeployerItemRenderState(ItemModelResolver itemModelManager, Level world, ItemStack heldItem, Vec3 offset, Direction facing) {
        super(itemModelManager, world, heldItem, offset, facing);
    }

    @Override
    protected void transform(PoseStack matrices) {
        matrices.translate(0, -1 / 8f, 0);
        matrices.mulPose(Axis.XP.rotationDegrees(-22.5f));
        super.transform(matrices);
    }
}
