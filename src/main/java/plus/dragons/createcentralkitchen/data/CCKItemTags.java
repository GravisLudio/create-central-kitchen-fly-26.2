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

package plus.dragons.createcentralkitchen.data;

import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Tag keys. The NeoForge build also generated tag contents when the game started; on Fabric the contents are
 * ordinary data files ({@code data/create/tags/item/handheld_in_deployer_use.json}).
 */
public final class CCKItemTags {
    /**
     * Items in this tag will render with {@link ItemDisplayContext#THIRD_PERSON_RIGHT_HAND}
     * when held by a {@link DeployerBlockEntity} in Use mode.
     */
    public static final TagKey<Item> HANDHELD_IN_DEPLOYER_USE = TagKey
            .create(Registries.ITEM, Identifier.fromNamespaceAndPath("create", "handheld_in_deployer_use"));

    private CCKItemTags() {}
}
