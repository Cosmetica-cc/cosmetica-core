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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.mixin.nametags.FeatureSubmitsAccessor;
import cc.cosmetica.core.mixin.nametags.SimpleFeatureRenderPhaseAccessor;
import cc.cosmetica.core.mixin.nametags.TranslucentFeatureRenderPhaseAccessor;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;

public class IconsMixinImpl {
    /**
     * Add an icon to nametags.
     * @param preparedIcon the prepared icon.
     * @param nameTags the nametags to render.
     * @param seeThroughNameTags the see through name tags to render.
     * @param seeThrough whether the see through nametags are being built.
     * @return whether the nametag should be cleared.
     */
    public static boolean addIcons(IconSubmitter.IconSubmission preparedIcon,
                                SimpleFeatureRenderPhase nameTags,
                                TranslucentFeatureRenderPhase seeThroughNameTags,
                                boolean seeThrough) {
        if (preparedIcon != null) {
            var submitsByFeature = ((SimpleFeatureRenderPhaseAccessor)nameTags).getSubmitsByFeature();
            var submit = ((FeatureSubmitsAccessor) (submitsByFeature[NameTagFeatureRenderer.TYPE.id()]))
                    .getUnbatched().getLast();

            IconSubmitter iconSubmitter0 = (IconSubmitter) submit;
            iconSubmitter0.cosmeticacore$submitIcon(preparedIcon);

            if (seeThrough) {
                var translucent = ((TranslucentFeatureRenderPhaseAccessor)seeThroughNameTags).getSubmits().getLast();
                IconSubmitter iconSubmitter1 = (IconSubmitter) translucent;
                iconSubmitter1.cosmeticacore$submitIcon(preparedIcon);
            }
            return true;
        }

        return false;
    }

    /**
     * Run on the nametag
     * @param submit the feature renderer submission.
     */
    public static void onNametagVisit(NameTagFeatureRenderer.Submit submit) {
        IconSubmitter.IconSubmission icon = ((IconSubmitter) (Object) submit).cosmeticacore$getPreparedIcon();
        if (icon != null) {
            NametagRenderer.prepareIcon(icon.icon(), icon.transparent(), icon.readjustTextPosition());
        }
    }
}
