/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
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
package com.google.android.apps.santatracker.presentquest.ui;

import android.content.Intent;

import com.google.android.apps.santatracker.presentquest.MapsActivity;
import com.google.android.apps.santatracker.presentquest.R;

public class PlayWorkshopDialog extends PlayGameDialog {

    public static final String TAG = "PlayWorkshopDialog";

    @Override
    public int getImageResourceId() {
        return R.drawable.present_throw_card_small;
    }

    @Override
    public void launchGame() {
        Intent intent = new Intent("com.google.android.apps.santatracker.ACTION_DOODLE");
        intent.putExtra("startUp", "waterpolo");

        getActivity().startActivityForResult(intent, MapsActivity.RC_WORKSHOP_GAME);

    }
}
