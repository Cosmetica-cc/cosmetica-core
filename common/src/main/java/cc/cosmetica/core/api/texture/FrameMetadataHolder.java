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
import java.util.List;
import java.util.Optional;

/**
 * Implement on an {@link javax.imageio.ImageReader} to apply custom frame metadata.
 */
public interface FrameMetadataHolder {
    /**
     * Get the frame metadata for this object.
     * @return a list of metadata for each frame.
     */
    List<FrameMetaData> getFrameMetadata();

    /**
     * Get the canvas dimensions.
     * @return the canvas dimensions, if known.
     */
    Optional<int[]> getCanvasDimensions();
}
