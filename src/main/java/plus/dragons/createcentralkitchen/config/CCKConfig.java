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

package plus.dragons.createcentralkitchen.config;

import com.zurrtum.create.catnip.config.Builder;
import plus.dragons.createcentralkitchen.common.CCKCommon;
import plus.dragons.createdragonsplus.util.FieldsNullabilityUnknownByDefault;

/**
 * Configuration lives in {@code config/create_central_kitchen/common.json} and {@code client.json}
 * (Create-Fly's JSON config; the NeoForge TOML files are not read).
 */
@FieldsNullabilityUnknownByDefault
public final class CCKConfig {
    private static volatile CCKCommonConfig common;
    private static volatile CCKClientConfig client;

    private CCKConfig() {}

    public static synchronized void registerCommon() {
        if (common == null)
            common = Builder.create(CCKCommonConfig::new, CCKCommon.ID, "common");
    }

    public static synchronized void registerClient() {
        if (client == null)
            client = Builder.create(CCKClientConfig::new, CCKCommon.ID, "client");
    }

    public static CCKCommonConfig common() {
        return common;
    }

    public static CCKClientConfig client() {
        return client;
    }

    public static CCKRecipesConfig recipes() {
        return common.recipes;
    }
}
