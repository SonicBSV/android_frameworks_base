/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.dreams.complication.dagger;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dreams.complication.ComplicationLayoutParams;
import com.android.systemui.dreams.complication.DreamHomeControlsComplication;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Named;
import javax.inject.Scope;

import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;

/**
 * Responsible for generating dependencies for the {@link DreamHomeControlsComplication}.
 */
@Subcomponent(modules = DreamHomeControlsComplicationComponent.DreamHomeControlsModule.class)
@DreamHomeControlsComplicationComponent.DreamHomeControlsComplicationScope
public interface DreamHomeControlsComplicationComponent {
    /**
     * Creates a view holder for the home controls complication.
     */
    DreamHomeControlsComplication.DreamHomeControlsChipViewHolder getViewHolder();

    /**
     * Scope of the home controls complication.
     */
    @Documented
    @Retention(RUNTIME)
    @Scope
    @interface DreamHomeControlsComplicationScope {}

    /**
     * Factory that generates a {@link DreamHomeControlsComplicationComponent}.
     */
    @Subcomponent.Factory
    interface Factory {
        DreamHomeControlsComplicationComponent create();
    }

    /**
     * Scoped injected values for the {@link DreamHomeControlsComplicationComponent}.
     */
    @Module
    interface DreamHomeControlsModule {
        String DREAM_HOME_CONTROLS_CHIP_VIEW = "dream_home_controls_chip_view";
        String DREAM_HOME_CONTROLS_CHIP_LAYOUT_PARAMS = "home_controls_chip_layout_params";

        // TODO(b/217199227): move to a single location.
        // Weight of order in the parent container. The home controls complication should have low
        // weight and be placed at the end.
        int INSERT_ORDER_WEIGHT = 0;

        /**
         * Provides the dream home controls chip view.
         */
        @Provides
        @DreamHomeControlsComplicationScope
        @Named(DREAM_HOME_CONTROLS_CHIP_VIEW)
        static ImageView provideHomeControlsChipView(LayoutInflater layoutInflater) {
            return (ImageView) layoutInflater.inflate(R.layout.dream_overlay_home_controls_chip,
                    null, false);
        }

        /**
         * Provides the layout parameters for the dream home controls complication.
         */
        @Provides
        @DreamHomeControlsComplicationScope
        @Named(DREAM_HOME_CONTROLS_CHIP_LAYOUT_PARAMS)
        static ComplicationLayoutParams provideLayoutParams(@Main Resources res) {
            return new ComplicationLayoutParams(
                    res.getDimensionPixelSize(R.dimen.keyguard_affordance_fixed_width),
                    res.getDimensionPixelSize(R.dimen.keyguard_affordance_fixed_height),
                    ComplicationLayoutParams.POSITION_BOTTOM
                            | ComplicationLayoutParams.POSITION_START,
                    ComplicationLayoutParams.DIRECTION_END,
                    INSERT_ORDER_WEIGHT);
        }
    }

}
