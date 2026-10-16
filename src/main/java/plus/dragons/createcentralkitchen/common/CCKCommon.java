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

package plus.dragons.createcentralkitchen.common;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.dragons.createcentralkitchen.common.packager.UnpackingRecipeCache;
import plus.dragons.createcentralkitchen.config.CCKConfig;

public class CCKCommon implements ModInitializer {
    public static final String ID = "create_central_kitchen";
    public static final String NAME = "Create: Central Kitchen";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitialize() {
        CCKConfig.registerCommon();
        UnpackingRecipeCache.register();
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }
}
