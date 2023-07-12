/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.flags;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.annotations.VisibleForTesting;

/**
 * @hide
 */
public final class SyncableFlag implements Parcelable {
    private final String mNamespace;
    private final String mName;
    private String mValue;
    private final boolean mDynamic;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public SyncableFlag(
            @NonNull String namespace,
            @NonNull String name,
            @NonNull String value,
            boolean dynamic) {
        mNamespace = namespace;
        mName = name;
        mValue = value;
        mDynamic = dynamic;
    }

    public void setValue(@NonNull String value) {
        mValue = value;
    }

    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public String getValue() {
        return mValue;
    }

    @NonNull
    public boolean isDynamic() {
        return mDynamic;
    }

    @NonNull
    public static final Parcelable.Creator<SyncableFlag> CREATOR = new Parcelable.Creator<>() {
        public SyncableFlag createFromParcel(Parcel in) {
            return new SyncableFlag(
                    in.readString(), in.readString(), in.readString(), in.readBoolean());
        }

        public SyncableFlag[] newArray(int size) {
            return new SyncableFlag[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mNamespace);
        dest.writeString(mName);
        dest.writeString(mValue);
        dest.writeBoolean(mDynamic);
    }

    @Override
    public String toString() {
        return getNamespace() + "." + getName() + "[" + getValue() + "]";
    }
}
