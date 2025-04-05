package f.cking.software.domain

import android.location.Location
import f.cking.software.data.database.entity.AppleContactEntity
import f.cking.software.data.database.entity.DeviceEntity
import f.cking.software.data.database.entity.JournalEntryEntity
import f.cking.software.data.database.entity.LocationEntity
import f.cking.software.data.database.entity.ProfileDetectEntity
import f.cking.software.data.database.entity.RadarProfileEntity
import f.cking.software.domain.model.AppleAirDrop
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.JournalEntry
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.ProfileDetect
import f.cking.software.domain.model.RadarProfile
import kotlinx.serialization.json.Json
import timber.log.Timber

fun Location.toDomain(time: Long): LocationModel =
    LocationModel(
        lat = this.latitude,
        lng = this.longitude,
        time = time,
    )

fun LocationModel.toData(): LocationEntity =
    LocationEntity(
        time = time,
        lat = lat,
        lng = lng,
    )

fun LocationEntity.toDomain(): LocationModel = LocationModel(lat, lng, time)

fun DeviceEntity.toDomain(appleAirDrop: AppleAirDrop?): DeviceData =
    DeviceData(
        address = address,
        name = name,
        lastDetectTimeMs = lastDetectTimeMs,
        firstDetectTimeMs = firstDetectTimeMs,
        detectCount = detectCount,
        customName = customName,
        favorite = favorite,
        manufacturerInfo =
            manufacturerId?.let { id ->
                manufacturerName?.let { name -> ManufacturerInfo(id, name, appleAirDrop) }
            },
        lastFollowingDetectionTimeMs = lastFollowingDetectionMs,
        tags = tags.toSet(),
        rssi = lastSeenRssi,
        systemAddressType = systemAddressType,
        deviceClass = deviceClass,
        isPaired = isPaired,
        servicesUuids = serviceUuids,
        rowDataEncoded = rowDataEncoded,
        metadata = metadata?.let { json.decodeFromStringOrNull(it) },
        isConnectable = isConnectable,
    )

fun DeviceData.toData(): DeviceEntity =
    DeviceEntity(
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
        tags = tags.toList(),
        lastSeenRssi = rssi,
        systemAddressType = systemAddressType,
        deviceClass = deviceClass,
        isPaired = isPaired,
        serviceUuids = servicesUuids,
        rowDataEncoded = rowDataEncoded,
        metadata = metadata?.let { json.encodeToString(it) },
        isConnectable = isConnectable,
    )

fun RadarProfile.toData(): RadarProfileEntity =
    RadarProfileEntity(
        id = id,
        name = name,
        description = description,
        isActive = isActive,
        detectFilter = json.encodeToString(detectFilter),
        cooldown = cooldownMs,
    )

fun RadarProfileEntity.toDomain(): RadarProfile =
    RadarProfile(
        id = id,
        name = name,
        description = description,
        isActive = isActive,
        detectFilter = detectFilter?.let { json.decodeFromStringOrNull(detectFilter) },
        cooldownMs = cooldown,
    )

fun ProfileDetectEntity.toDomain(): ProfileDetect =
    ProfileDetect(
        id = id,
        profileId = profileId,
        triggerTime = triggerTime,
        deviceAddress = deviceAddress,
    )

fun ProfileDetect.toData(): ProfileDetectEntity =
    ProfileDetectEntity(
        id = id,
        profileId = profileId,
        triggerTime = triggerTime,
        deviceAddress = deviceAddress,
    )

fun AppleAirDrop.AppleContact.toData(associatedAddress: String): AppleContactEntity =
    AppleContactEntity(
        sha256,
        associatedAddress,
        lastDetectTimeMs = lastDetectionTimeMs,
        firstDetectTimeMs = firstDetectionTimeMs,
    )

fun AppleContactEntity.toDomain(): AppleAirDrop.AppleContact =
    AppleAirDrop.AppleContact(
        sha256,
        lastDetectionTimeMs = lastDetectTimeMs,
        firstDetectionTimeMs = firstDetectTimeMs,
    )

fun JournalEntryEntity.toDomain(): JournalEntry =
    JournalEntry(
        id = id,
        timestamp = timestamp,
        report = json.decodeFromString(report),
    )

fun JournalEntry.toData(): JournalEntryEntity =
    JournalEntryEntity(
        id = id,
        timestamp = timestamp,
        report = json.encodeToString(report),
    )

private val json = Json { ignoreUnknownKeys = true }

private inline fun <reified T> Json.decodeFromStringOrNull(
    str: String,
    ignoreUnknown: Boolean = true,
): T? =
    try {
        decodeFromString<T>(str)
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
