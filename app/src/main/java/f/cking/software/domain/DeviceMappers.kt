package f.cking.software.domain

import f.cking.software.data.DeviceEntity

fun DeviceEntity.toDomain(): DeviceData {
    return DeviceData(
        address = address,
        name = name,
        lastDetectTimeMs = lastDetectTimeMs,
        firstDetectTimeMs = firstDetectTimeMs,
        detectCount = detectCount,
        customName = customName,
        favorite = favorite,
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
    )
}