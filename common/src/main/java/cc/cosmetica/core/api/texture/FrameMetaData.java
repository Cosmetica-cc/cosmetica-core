/*
 * Copyright 2024, 2025 Cosmetica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.cosmetica.core.api.texture;

import java.awt.*;

/**
 * Frame metadata. Can be supplied by add-ons for converting other formats to PNG tilesheets.
 */
public class FrameMetaData {
    public FrameMetaData(Rectangle bounds, boolean blend, boolean dispose) {
        this.bounds = bounds;
        this.blend = blend;
        this.dispose = dispose;
    }

    public final Rectangle bounds;
    public final boolean blend;
    public final boolean dispose;
}
