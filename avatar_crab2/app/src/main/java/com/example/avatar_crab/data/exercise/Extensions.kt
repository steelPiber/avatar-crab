package com.example.avatar_crab.data.exercise

import com.google.android.gms.maps.model.LatLng

// Convert LatLngEntity to LatLng
fun LatLngEntity.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}

// Convert LatLng to LatLngEntity
fun LatLng.toLatLngEntity(): LatLngEntity {
    return LatLngEntity(latitude = this.latitude, longitude = this.longitude)
}

// Convert List<LatLngEntity> to List<LatLng>
fun List<LatLngEntity>.toLatLngList(): List<LatLng> {
    return this.map { it.toLatLng() }
}

// Convert List<LatLng> to List<LatLngEntity>
fun List<LatLng>.toLatLngEntityList(): List<LatLngEntity> {
    return this.map { it.toLatLngEntity() }
}

// Convert SegmentDataEntity to SegmentData
fun SegmentDataEntity.toSegmentData(): SegmentData {
    return SegmentData(
        avgHeartRate = this.avgHeartRate,
        minHeartRate = this.minHeartRate,
        maxHeartRate = this.maxHeartRate,
        latitude = this.latitude,
        longitude = this.longitude
    )
}

// Convert SegmentData to SegmentDataEntity
fun SegmentData.toSegmentDataEntity(): SegmentDataEntity {
    return SegmentDataEntity(
        avgHeartRate = this.avgHeartRate,
        minHeartRate = this.minHeartRate,
        maxHeartRate = this.maxHeartRate,
        latitude = this.latitude,
        longitude = this.longitude
    )
}

// Convert ExerciseRecordEntity to ExerciseRecord
fun ExerciseRecordEntity.toExerciseRecord(email: String = this.email): ExerciseRecord {
    return ExerciseRecord(
        id = this.id.toInt(),
        distance = this.distance,
        elapsedTime = this.elapsedTime,
        calories = this.calories,
        avgPace = this.avgPace,
        date = this.date,
        segments = this.segments.toMutableList(),  // 새롭게 생성된 리스트
        pathPoints = this.pathPoints.toLatLngList(),
        email = email
    )
}

// Convert ExerciseRecord to ExerciseRecordEntity
fun ExerciseRecord.toExerciseRecordEntity(): ExerciseRecordEntity {
    return ExerciseRecordEntity(
        id = this.id.toLong(),
        distance = this.distance,
        elapsedTime = this.elapsedTime,
        calories = this.calories,
        avgPace = this.avgPace,
        date = this.date,
        segments = this.segments.toList(),  // 새롭게 생성된 리스트
        pathPoints = this.pathPoints.toLatLngEntityList(),
        email = this.email
    )
}
