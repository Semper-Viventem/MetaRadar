package f.cking.software.domain

import f.cking.software.data.database.AppleContactEntity
import f.cking.software.data.database.DeviceEntity
import f.cking.software.data.database.RadarProfileEntity
import f.cking.software.domain.model.AppleAirDrop
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        manufacturerName = manufacturerInfo?.name
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

fun AppleAirDrop.AppleContact.toData(associatedAddress: String, lastSeenTime: Long): AppleContactEntity {
    return AppleContactEntity(sha256, associatedAddress, lastSeenTime)
}

fun AppleContactEntity.toDomain(): AppleAirDrop.AppleContact {
    return AppleAirDrop.AppleContact(sha256)
}