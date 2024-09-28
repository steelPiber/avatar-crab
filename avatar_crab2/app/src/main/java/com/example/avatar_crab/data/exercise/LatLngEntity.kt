package com.example.avatar_crab.data.exercise

import android.os.Parcel
import android.os.Parcelable

data class LatLngEntity(
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LatLngEntity> {
        override fun createFromParcel(parcel: Parcel): LatLngEntity {
            return LatLngEntity(parcel)
        }

        override fun newArray(size: Int): Array<LatLngEntity?> {
            return arrayOfNulls(size)
        }
    }
}
