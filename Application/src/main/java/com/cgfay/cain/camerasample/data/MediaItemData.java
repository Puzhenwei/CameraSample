package com.cgfay.cain.camerasample.data;

import android.net.Uri;

import java.util.Date;



public class MediaItemData {

    public static final String MIME_TYPE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_GIF = "image/gif";
    public static final String MIME_TYPE_PHOTOSPHERE = "application/vnd.google.panorama360+jpg";
    public static final String MIME_TYPE_MP4 = "video/mp4";

    private final long mContentId;
    private final String mTitle;
    private final String mMimeType;
    private final Date mCreationDate;
    private final Date mLastModifiedDate;
    private final String mFilePath;
    private final Uri mUri;
    private final Size mDimensions;
    private final long mSizeInBytes;
    private final int mOrientation;
    private final Location mLocation;

    public MediaItemData(long contentId,
                         String title,
                         String mimeType,
                         Date creationDate,
                         Date lastModifiedDate,
                         String filePath,
                         Uri uri,
                         Size dimensions,
                         long sizeInBytes,
                         int orientation,
                         Location location) {
        this.mContentId = contentId;
        this.mTitle = title;
        this.mMimeType = mimeType;
        this.mCreationDate = creationDate;
        this.mLastModifiedDate = lastModifiedDate;
        this.mFilePath = filePath;
        this.mUri = uri;
        this.mDimensions = dimensions;
        this.mSizeInBytes = sizeInBytes;
        this.mOrientation = orientation;
        this.mLocation = location;
    }

    public long getContentId() {
        return mContentId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public Date getCreationDate() {
        return mCreationDate;
    }

    public Date getLastModifiedDate() {
        return mLastModifiedDate;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public Uri getUri() {
        return mUri;
    }

    public Size getDimensions() {
        return mDimensions;
    }

    public long getSizeInBytes() {
        return mSizeInBytes;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public Location getLocation() {
        return mLocation;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MediaItemData {");
        builder.append("id:");
        builder.append(mContentId);
        builder.append(", title:");
        builder.append(mTitle);
        builder.append(", mimeType:");
        builder.append(mMimeType);
        builder.append(", creationDate:");
        builder.append(mCreationDate);
        builder.append(", lastModifiedDate:");
        builder.append(mLastModifiedDate);
        builder.append(", filePath:");
        builder.append(mFilePath);
        builder.append(", uri:");
        builder.append(mUri);
        builder.append(", dimensions:");
        builder.append(mDimensions);
        builder.append(", sizeInBytes:");
        builder.append(mSizeInBytes);
        builder.append(", orientation:");
        builder.append(mOrientation);
        builder.append(", location:");
        builder.append(mLocation);
        builder.append("}");
        return builder.toString();
    }

    public static class Builder {
        public static final Date EMPTY = new Date(0);
        public static final Size ZERO = new Size(0, 0);

        private long mContentId = -1;
        private String mTitle = "";
        private  String mMimeType = "";
        private Date mCreationDate = EMPTY;
        private Date mLastModifiedDate = EMPTY;
        private String mFilePath = "";
        private final Uri mUri;
        private Size mDimensions;
        private long mSizeInBytes = 0;
        private int mOrientation = 0;
        private Location mLocation = Location.UNKNOWN;

        public Builder(Uri uri) {
            mUri = uri;
        }

        public MediaItemData build() {
            return new MediaItemData(
                    mContentId,
                    mTitle,
                    mMimeType,
                    mCreationDate,
                    mLastModifiedDate,
                    mFilePath,
                    mUri,
                    mDimensions,
                    mSizeInBytes,
                    mOrientation,
                    mLocation
            );
        }

        public static Builder from(MediaItemData data) {
            Builder builder = new Builder(data.getUri());
            builder.mContentId = data.getContentId();
            builder.mTitle = data.getTitle();
            builder.mMimeType = data.getMimeType();
            builder.mCreationDate = data.getCreationDate();
            builder.mLastModifiedDate = data.getLastModifiedDate();
            builder.mFilePath = data.getFilePath();
            builder.mDimensions = data.getDimensions();
            builder.mSizeInBytes = data.getSizeInBytes();
            builder.mOrientation = data.getOrientation();
            builder.mLocation = data.getLocation();
            return builder;
        }

        public Builder withContentId(long contentId) {
            mContentId = contentId;
            return this;
        }

        public Builder withTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder withMimeType(String mimeType) {
            mMimeType = mimeType;
            return this;
        }

        public Builder withCreationDate(Date creationDate) {
            mCreationDate = creationDate;
            return this;
        }

        public Builder withLasModifiedDate(Date lasModifiedDate) {
            mLastModifiedDate = lasModifiedDate;
            return this;
        }

        public Builder withFilePath(String filePath) {
            mFilePath = filePath;
            return this;
        }

        public Builder withDimensions(Size dimensions) {
            mDimensions = dimensions;
            return this;
        }

        public Builder withSizeInBytes(long sizeInBytes) {
            mSizeInBytes = sizeInBytes;
            return this;
        }

        public Builder withOrientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        public Builder withLocation(Location location) {
            mLocation = location;
            return this;
        }
    }
}
