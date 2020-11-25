/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.location.timezone;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An event containing location time zone information.
 *
 * @hide
 */
public final class LocationTimeZoneEvent implements Parcelable {

    @IntDef({ EVENT_TYPE_UNKNOWN, EVENT_TYPE_PERMANENT_FAILURE, EVENT_TYPE_SUCCESS,
            EVENT_TYPE_UNCERTAIN })
    public @interface EventType {}

    /** Uninitialized value for {@link #mEventType} - must not be used for real events. */
    private static final int EVENT_TYPE_UNKNOWN = 0;

    /**
     * Indicates there was a permanent failure. This is not generally expected, and probably means a
     * required backend service has been turned down, or the client is unreasonably old.
     */
    public static final int EVENT_TYPE_PERMANENT_FAILURE = 1;

    /**
     * Indicates a successful geolocation time zone detection event. {@link #getTimeZoneIds()} will
     * be non-null but can legitimately be empty, e.g. for disputed areas, oceans.
     */
    public static final int EVENT_TYPE_SUCCESS = 2;

    /**
     * Indicates the time zone is not known because of an expected runtime state or error, e.g. when
     * the provider is unable to detect location, or there was a problem when resolving the location
     * to a time zone.
     */
    public static final int EVENT_TYPE_UNCERTAIN = 3;

    private static final int EVENT_TYPE_MAX = EVENT_TYPE_UNCERTAIN;

    @EventType
    private final int mEventType;

    @NonNull
    private final List<String> mTimeZoneIds;

    private final long mElapsedRealtimeMillis;

    private LocationTimeZoneEvent(@EventType int eventType, @NonNull List<String> timeZoneIds,
            long elapsedRealtimeMillis) {
        mEventType = checkValidEventType(eventType);
        mTimeZoneIds = immutableList(timeZoneIds);

        boolean emptyTimeZoneIdListExpected = eventType != EVENT_TYPE_SUCCESS;
        Preconditions.checkState(!emptyTimeZoneIdListExpected || timeZoneIds.isEmpty());

        mElapsedRealtimeMillis = elapsedRealtimeMillis;
    }

    /**
     * Returns the time of this fix, in elapsed real-time since system boot.
     *
     * <p>This value can be reliably compared to {@link
     * android.os.SystemClock#elapsedRealtime()}, to calculate the age of a fix and to compare
     * {@link LocationTimeZoneEvent} instances.
     *
     * @return elapsed real-time of fix, in milliseconds
     */
    public long getElapsedRealtimeMillis() {
        return mElapsedRealtimeMillis;
    }

    /**
     * Returns the event type.
     */
    @Nullable
    public @EventType int getEventType() {
        return mEventType;
    }

    /**
     * Gets the time zone IDs of this event. Contains zero or more IDs for a successful lookup.
     * The value is undefined for an unsuccessful lookup. See also {@link #getEventType()}.
     */
    @NonNull
    public List<String> getTimeZoneIds() {
        return mTimeZoneIds;
    }

    @Override
    public String toString() {
        return "LocationTimeZoneEvent{"
                + "mEventType=" + mEventType
                + ", mTimeZoneIds=" + mTimeZoneIds
                + ", mElapsedRealtimeMillis=" + mElapsedRealtimeMillis
                + "(" + Duration.ofMillis(mElapsedRealtimeMillis) + ")"
                + '}';
    }

    public static final @NonNull Parcelable.Creator<LocationTimeZoneEvent> CREATOR =
            new Parcelable.Creator<LocationTimeZoneEvent>() {
                @Override
                public LocationTimeZoneEvent createFromParcel(Parcel in) {
                    int eventType = in.readInt();
                    @SuppressWarnings("unchecked")
                    ArrayList<String> timeZoneIds =
                            (ArrayList<String>) in.readArrayList(null /* classLoader */);
                    long elapsedRealtimeMillis = in.readLong();
                    return new LocationTimeZoneEvent(eventType, timeZoneIds, elapsedRealtimeMillis);
                }

                @Override
                public LocationTimeZoneEvent[] newArray(int size) {
                    return new LocationTimeZoneEvent[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mEventType);
        parcel.writeList(mTimeZoneIds);
        parcel.writeLong(mElapsedRealtimeMillis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocationTimeZoneEvent that = (LocationTimeZoneEvent) o;
        return mEventType == that.mEventType
                && mElapsedRealtimeMillis == that.mElapsedRealtimeMillis
                && mTimeZoneIds.equals(that.mTimeZoneIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEventType, mTimeZoneIds, mElapsedRealtimeMillis);
    }

    /** @hide */
    public static final class Builder {

        private @EventType int mEventType = EVENT_TYPE_UNKNOWN;
        private @NonNull List<String> mTimeZoneIds = Collections.emptyList();
        private long mElapsedRealtimeMillis;

        public Builder() {
        }

        /**
         * Sets the contents of this from the supplied instance.
         */
        public Builder(@NonNull LocationTimeZoneEvent ltz) {
            mEventType = ltz.mEventType;
            mTimeZoneIds = ltz.mTimeZoneIds;
            mElapsedRealtimeMillis = ltz.mElapsedRealtimeMillis;
        }

        /**
         * Set the time zone ID of this event.
         */
        public Builder setEventType(@EventType int eventType) {
            checkValidEventType(eventType);
            mEventType = eventType;
            return this;
        }

        /**
         * Sets the time zone IDs of this event.
         */
        public Builder setTimeZoneIds(@NonNull List<String> timeZoneIds) {
            mTimeZoneIds = Objects.requireNonNull(timeZoneIds);
            return this;
        }

        /**
         * Sets the time of this event, in elapsed real-time since system boot.
         */
        public Builder setElapsedRealtimeMillis(long time) {
            mElapsedRealtimeMillis = time;
            return this;
        }

        /**
         * Builds a {@link LocationTimeZoneEvent} instance.
         */
        public LocationTimeZoneEvent build() {
            return new LocationTimeZoneEvent(mEventType, mTimeZoneIds, mElapsedRealtimeMillis);
        }
    }

    private static int checkValidEventType(int eventType) {
        if (eventType <= EVENT_TYPE_UNKNOWN || eventType > EVENT_TYPE_MAX) {
            throw new IllegalStateException("eventType " + eventType + " unknown");
        }
        return eventType;
    }

    @NonNull
    private static List<String> immutableList(@NonNull List<String> list) {
        Objects.requireNonNull(list);
        if (list.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(new ArrayList<>(list));
        }
    }
}
