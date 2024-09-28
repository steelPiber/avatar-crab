package com.example.avatar_crab.data.exercise

import android.os.Parcel
import android.os.Parcelable

data class SegmentDataEntity(
    val avgHeartRate: Float,
    val minHeartRate: Float,
    val maxHeartRate: Float,
    val latitude: Float,
    val longitude: Float
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(avgHeartRate)
        parcel.writeFloat(minHeartRate)
        parcel.writeFloat(maxHeartRate)
        parcel.writeFloat(latitude)
        parcel.writeFloat(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SegmentDataEntity> {
        override fun createFromParcel(parcel: Parcel): SegmentDataEntity {
            return SegmentDataEntity(parcel)
        }

        override fun newArray(size: Int): Array<SegmentDataEntity?> {
            return arrayOfNulls(size)
        }
    }
}
