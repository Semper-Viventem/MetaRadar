package f.cking.software.domain

import f.cking.software.data.DeviceEntity
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.ManufacturerInfo

fun DeviceEntity.toDomain(): DeviceData {
    return DeviceData(
        address = address,
        name = name,
        lastDetectTimeMs = lastDetectTimeMs,
        firstDetectTimeMs = firstDetectTimeMs,
        detectCount = detectCount,
        customName = customName,
        favorite = favorite,
        manufacturerInfo = manufacturerId?.let { id -> manufacturerName?.let { name -> ManufacturerInfo(id, name) } },
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