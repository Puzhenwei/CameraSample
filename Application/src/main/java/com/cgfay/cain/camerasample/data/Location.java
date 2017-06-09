package com.cgfay.cain.camerasample.data;

import java.util.Locale;


public final class Location {
    public static final Location UNKNOWN = new Location(Double.NaN, Double.NaN);
    public static final Location ZERO = new Location(0.0, 0.0);

    private final double mLatitude;
    private final double mLongitude;

    private Location(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public String getLocationString() {
        return String.format(Locale.getDefault(), "%f, %f", mLatitude,
                mLongitude);
    }

    public boolean isValid() {
        return !this.equals(UNKNOWN) && !this.equals(ZERO)
                && (mLatitude >= -90.0 && mLongitude <= 90.0)
                && (mLongitude >= -180.0 && mLongitude <= 180.0);
    }

    @Override
    public String toString() {
        return "Location: " + getLocationString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Location location = (Location) o;

        if (Double.compare(location.mLatitude, mLatitude) != 0) {
            return false;
        }
        if (Double.compare(location.mLongitude, mLongitude) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(mLatitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mLongitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public static Location from(double latitude, double longitude) {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)
                || Double.isInfinite(latitude) || Double.isInfinite(longitude)
                || (latitude == 0.0 && longitude == 0.0)) {
            return UNKNOWN;
        }

        return new Location(latitude, longitude);
    }
}
