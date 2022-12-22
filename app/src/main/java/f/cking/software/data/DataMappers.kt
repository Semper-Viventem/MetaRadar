package f.cking.software.domain

import android.location.Location
import f.cking.software.data.database.AppleContactEntity
import f.cking.software.data.database.DeviceEntity
import f.cking.software.data.database.LocationEntity
import f.cking.software.data.database.RadarProfileEntity
import f.cking.software.domain.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Location.toDomain(time: Long): LocationModel {
    return LocationModel(
        lat = this.latitude,
        lng = this.longitude,
        time = time,
    )
}

fun LocationModel.toData(): LocationEntity {
    return LocationEntity(
        time = time,
        lat = lat,
        lng = lng,
    )
}

fun LocationEntity.toDomain(): LocationModel {
    return LocationModel(lat, lng, time)
}

fun DeviceEntity.toDomain(appleAirDrop: AppleAirDrop?): DeviceData {
    return DeviceData(
        address = address,
        name = name,
        lastDetectTimeMs = lastDetectTimeMs,
        firstDetectTimeMs = firstDetectTimeMs,
        detectCount = detectCount,
        customName = customName,
        favorite = favorite,
        manufacturerInfo = manufacturerId?.let { id ->
            manufacturerName?.let { name -> ManufacturerInfo(id, name, appleAirDrop) }
        },
        lastFollowingDetectionTimeMs = lastFollowingDetectionMs,
    )
}

fun DeviceData.toData(): DeviceEntity {
    return DeviceEntity(
        address = address,
        name = name,
        lastDetectTimeMs = lastDetectTimeMs,
        firstDetectTimeMs = firstDetectTimeMs,
        detectCount = detectCount,
        customName = customName,
        favorite = favorite,
        manufacturerId = manufacturerInfo?.id,
        manufacturerName = manufacturerInfo?.name,
        lastFollowingDetectionMs = lastFollowingDetectionTimeMs,
    )
}

fun RadarProfile.toData(): RadarProfileEntity {
    return RadarProfileEntity(
        id = id,
        name = name,
        description = description,
        isActive = isActive,
        detectFilter = Json.encodeToString(detectFilter)
    )
}

fun RadarProfileEntity.toDomain(): RadarProfile {
    return RadarProfile(
        id = id,
        name = name,
        description = description,
        isActive = isActive,
        detectFilter = detectFilter?.let { Json.decodeFromString(detectFilter) }
    )
}

fun AppleAirDrop.AppleContact.toData(associatedAddress: String): AppleContactEntity {
    return AppleContactEntity(
        sha256,
        associatedAddress,
        lastDetectTimeMs = lastDetectionTimeMs,
        firstDetectTimeMs = firstDetectionTimeMs
    )
}

fun AppleContactEntity.toDomain(): AppleAirDrop.AppleContact {
    return AppleAirDrop.AppleContact(
        sha256,
        lastDetectionTimeMs = lastDetectTimeMs,
        firstDetectionTimeMs = firstDetectTimeMs
    )
}