package f.cking.software.utils.graphic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.location.Location
import androidx.collection.LruCache
import androidx.core.graphics.createBitmap
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Most of the class is AI generated
 */
object HeatMapBitmapFactory {

    data class Position(val location: Location, val radiusMeters: Float)
    data class Tile(val topLeft: Location, val bottomRight: Location) {

        /**
         * True if this tile intersects (collides) with [other].
         *
         * @param inclusiveEdges if true, touching edges count as intersecting.
         *                       if false, requires positive-area overlap.
         */
        fun intersects(other: Tile, inclusiveEdges: Boolean = true): Boolean {
            val aTop = topLeft.latitude
            val aBottom = bottomRight.latitude
            val aLeft = topLeft.longitude
            val aRight = bottomRight.longitude

            val bTop = other.topLeft.latitude
            val bBottom = other.bottomRight.latitude
            val bLeft = other.topLeft.longitude
            val bRight = other.bottomRight.longitude

            return if (inclusiveEdges) {
                // Overlap in lat AND overlap in lng, allowing equality at edges.
                aLeft <= bRight &&
                        aRight >= bLeft &&
                        aBottom <= bTop &&
                        aTop >= bBottom
            } else {
                // Strict overlap (positive area)
                aLeft < bRight &&
                        aRight > bLeft &&
                        aBottom < bTop &&
                        aTop > bBottom
            }
        }

        /**
         * True if this tile fully contains [other] (including borders).
         */
        fun contains(other: Tile): Boolean {
            val aTop = topLeft.latitude
            val aBottom = bottomRight.latitude
            val aLeft = topLeft.longitude
            val aRight = bottomRight.longitude

            val bTop = other.topLeft.latitude
            val bBottom = other.bottomRight.latitude
            val bLeft = other.topLeft.longitude
            val bRight = other.bottomRight.longitude

            return bLeft >= aLeft &&
                    bRight <= aRight &&
                    bTop <= aTop &&
                    bBottom >= aBottom
        }

        fun contains(location: Location, paddingMeters: Double): Boolean {
            require(paddingMeters >= 0)

            val latTop = topLeft.latitude
            val latBottom = bottomRight.latitude
            val lngLeft = topLeft.longitude
            val lngRight = bottomRight.longitude

            // Quickly reject impossible lat (even before meter expansion).
            val locLat = location.latitude
            val locLng = location.longitude

            // If no padding, fast path:
            if (paddingMeters <= 0.0) {
                return locLat <= latTop &&
                        locLat >= latBottom &&
                        locLng >= lngLeft &&
                        locLng <= lngRight
            }

            // Expand tile bounds by paddingMeters in lat/lng
            val centerLat = (latTop + latBottom) / 2.0
            val mPerDegLat = 111_320.0
            val mPerDegLng = 111_320.0 * kotlin.math.cos(Math.toRadians(centerLat))

            val dLat = paddingMeters / mPerDegLat
            val dLng = paddingMeters / mPerDegLng

            val paddedTop = latTop + dLat
            val paddedBottom = latBottom - dLat
            val paddedLeft = lngLeft - dLng
            val paddedRight = lngRight + dLng

            return locLat <= paddedTop &&
                    locLat >= paddedBottom &&
                    locLng >= paddedLeft &&
                    locLng <= paddedRight
        }
    }

    private fun generateTileGradientBitmapFastNoNormalize(
        positions: List<Position>,
        tile: Tile,
        widthPx: Int,
        downsample: Int,
        blurSigmaPx: Float,
    ): Bitmap {
        fun metersPerDegLat(): Double = 111_320.0
        fun metersPerDegLng(atLatDeg: Double): Double =
            111_320.0 * cos(Math.toRadians(atLatDeg))

        val latTop = tile.topLeft.latitude
        val latBottom = tile.bottomRight.latitude
        val lngLeft = tile.topLeft.longitude
        val lngRight = tile.bottomRight.longitude
        val centerLat = (latTop + latBottom) / 2.0

        val tileWidthMeters = abs(lngRight - lngLeft) * metersPerDegLng(centerLat)
        val tileHeightMeters = abs(latTop - latBottom) * metersPerDegLat()
        val heightPx = max(1, round(widthPx * (tileHeightMeters / tileWidthMeters)).toInt())

        val ds = downsample.coerceAtLeast(1)
        val wL = max(1, widthPx / ds)
        val hL = max(1, heightPx / ds)
        val sizeL = wL * hL

        val pxPerMeterX_L = wL / tileWidthMeters
        val pxPerMeterY_L = hL / tileHeightMeters
        val pxPerMeter_L = min(pxPerMeterX_L, pxPerMeterY_L)

        fun latLngToPixelLow(lat: Double, lng: Double): Pair<Float, Float> {
            val xNorm = (lng - lngLeft) / (lngRight - lngLeft)
            val yNorm = (latTop - lat) / (latTop - latBottom)
            return (xNorm * wL).toFloat() to (yNorm * hL).toFloat()
        }

        fun idxL(x: Int, y: Int) = y * wL + x

        val intensityL = FloatArray(sizeL)

        fun smoothstep(t: Float): Float {
            val x = t.coerceIn(0f, 1f)
            return x * x * (3f - 2f * x)
        }

        val kernelCache = HashMap<Int, FloatArray>()
        fun getKernel(rPx: Int): FloatArray {
            return kernelCache.getOrPut(rPx) {
                val d = 2 * rPx + 1
                val k = FloatArray(d * d)
                val r = rPx.toFloat()
                val r2 = r * r
                var i = 0
                for (yy in -rPx..rPx) {
                    val dy = yy.toFloat() + 0.5f
                    val dy2 = dy * dy
                    for (xx in -rPx..rPx) {
                        val dx = xx.toFloat() + 0.5f
                        val d2 = dx * dx + dy2
                        k[i++] = if (d2 <= r2) {
                            val dist = sqrt(d2)
                            smoothstep(1f - dist / r)
                        } else 0f
                    }
                }
                k
            }
        }

        // MAX-union stamping
        for (pos in positions) {
            val (cx, cy) = latLngToPixelLow(pos.location.latitude, pos.location.longitude)
            val rPxF = max(1f, pos.radiusMeters * pxPerMeter_L.toFloat())
            val rPx = rPxF.roundToInt().coerceAtLeast(1)

            val kernel = getKernel(rPx)
            val d = 2 * rPx + 1

            val x0 = floor(cx - rPx).toInt()
            val y0 = floor(cy - rPx).toInt()

            for (ky in 0 until d) {
                val y = y0 + ky
                if (y !in 0 until hL) continue
                val rowBase = idxL(0, y)
                val kRowBase = ky * d
                for (kx in 0 until d) {
                    val x = x0 + kx
                    if (x !in 0 until wL) continue
                    val v = kernel[kRowBase + kx]
                    if (v <= 0f) continue
                    val id = rowBase + x
                    if (v > intensityL[id]) intensityL[id] = v
                }
            }
        }

        if ((intensityL.maxOrNull() ?: 0f) <= 0f) {
            return createBitmap(widthPx, heightPx)
        }

        val blurredL = gaussianBlurSeparable(intensityL, wL, hL, blurSigmaPx)

        // Upscale + colorize (proper gradient)
        val out = IntArray(widthPx * heightPx)

        fun sampleLow(x: Float, y: Float): Float {
            val x0i = floor(x).toInt().coerceIn(0, wL - 1)
            val y0i = floor(y).toInt().coerceIn(0, hL - 1)
            val x1i = (x0i + 1).coerceIn(0, wL - 1)
            val y1i = (y0i + 1).coerceIn(0, hL - 1)
            val fx = x - x0i
            val fy = y - y0i

            val v00 = blurredL[idxL(x0i, y0i)]
            val v10 = blurredL[idxL(x1i, y0i)]
            val v01 = blurredL[idxL(x0i, y1i)]
            val v11 = blurredL[idxL(x1i, y1i)]

            val vx0 = v00 * (1 - fx) + v10 * fx
            val vx1 = v01 * (1 - fx) + v11 * fx
            return vx0 * (1 - fy) + vx1 * fy
        }

        // visibility boost so lone blobs aren't too faint
        val alphaScale = 1.6f

        var iOut = 0
        for (y in 0 until heightPx) {
            val yL = y.toFloat() / ds
            for (x in 0 until widthPx) {
                val xL = x.toFloat() / ds
                val vRaw = sampleLow(xL, yL).coerceIn(0f, 1f)

                val v = min(1f, vRaw * alphaScale)

                val a = (v * 255f).toInt()
                val r = ((1f - v) * 255f).toInt()
                val g = (v * 255f).toInt()
                val b = 0

                out[iOut++] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(out, widthPx, heightPx, Bitmap.Config.ARGB_8888)
    }

    /**
     * Fast separable Gaussian blur for a float buffer.
     * Edge handling: clamp.
     */
    private fun gaussianBlurSeparable(
        src: FloatArray,
        w: Int,
        h: Int,
        sigma: Float
    ): FloatArray {
        if (sigma <= 0.1f) return src.copyOf()

        val radius = ceil(3f * sigma).toInt().coerceAtLeast(1)
        val kernel = FloatArray(2 * radius + 1)
        var sum = 0f
        val twoSigma2 = 2f * sigma * sigma
        for (i in -radius..radius) {
            val v = exp(-(i * i) / twoSigma2)
            kernel[i + radius] = v
            sum += v
        }
        // normalize
        for (i in kernel.indices) kernel[i] /= sum

        val tmp = FloatArray(w * h)
        val dst = FloatArray(w * h)

        fun idx(x: Int, y: Int) = y * w + x

        // horizontal
        for (y in 0 until h) {
            for (x in 0 until w) {
                var acc = 0f
                for (k in -radius..radius) {
                    val sx = (x + k).coerceIn(0, w - 1)
                    acc += src[idx(sx, y)] * kernel[k + radius]
                }
                tmp[idx(x, y)] = acc
            }
        }

        // vertical
        for (y in 0 until h) {
            for (x in 0 until w) {
                var acc = 0f
                for (k in -radius..radius) {
                    val sy = (y + k).coerceIn(0, h - 1)
                    acc += tmp[idx(x, sy)] * kernel[k + radius]
                }
                dst[idx(x, y)] = acc
            }
        }

        return dst
    }

    private fun computeWidthForRenderTile(core: Tile, render: Tile, widthPxCore: Int): Int {
        // keep px-per-meter consistent between core and render
        val metersPerDegLng = { lat: Double ->
            111_320.0 * cos(Math.toRadians(lat))
        }
        val latCoreC = (core.topLeft.latitude + core.bottomRight.latitude) / 2.0
        val latRenderC = (render.topLeft.latitude + render.bottomRight.latitude) / 2.0

        val coreWidthM =
            abs(core.bottomRight.longitude - core.topLeft.longitude) * metersPerDegLng(latCoreC)
        val renderWidthM =
            abs(render.bottomRight.longitude - render.topLeft.longitude) * metersPerDegLng(latRenderC)

        val pxPerM = widthPxCore / coreWidthM
        return max(1, round(renderWidthM * pxPerM).toInt())
    }

    fun paddedRenderTile(core: Tile, paddingMeters: Double): Tile {
        if (paddingMeters <= 0.0) return core

        val latTop = core.topLeft.latitude
        val latBottom = core.bottomRight.latitude
        val lngLeft = core.topLeft.longitude
        val lngRight = core.bottomRight.longitude

        val centerLat = (latTop + latBottom) / 2.0
        val mPerDegLat = 111_320.0
        val mPerDegLng = 111_320.0 * kotlin.math.cos(Math.toRadians(centerLat))

        val padLat = paddingMeters / mPerDegLat
        val padLng = paddingMeters / mPerDegLng

        fun make(lat: Double, lng: Double) =
            Location("tile_pad").apply { latitude = lat; longitude = lng }

        val paddedTopLeft = make(latTop + padLat, lngLeft - padLng)
        val paddedBottomRight = make(latBottom - padLat, lngRight + padLng)

        return Tile(paddedTopLeft, paddedBottomRight)
    }

    fun filterPointsForTile(
        positions: List<Position>,
        renderTile: Tile,
        extraMarginMeters: Double
    ): List<Position> {
        val latTop = renderTile.topLeft.latitude
        val latBottom = renderTile.bottomRight.latitude
        val lngLeft = renderTile.topLeft.longitude
        val lngRight = renderTile.bottomRight.longitude

        // expand bounds by extraMarginMeters (cheap approx)
        val centerLat = (latTop + latBottom) / 2.0
        val mPerDegLat = 111_320.0
        val mPerDegLng = 111_320.0 * kotlin.math.cos(Math.toRadians(centerLat))
        val dLat = extraMarginMeters / mPerDegLat
        val dLng = extraMarginMeters / mPerDegLng

        val top = latTop + dLat
        val bottom = latBottom - dLat
        val left = lngLeft - dLng
        val right = lngRight + dLng

        return positions.filter { p ->
            val lat = p.location.latitude
            val lng = p.location.longitude
            lat in bottom..top && lng in left..right
        }
    }

    private data class BitmapCacheKey(
        val positionsAll: List<Position>,
        val coreTile: Tile,
        val widthPxCore: Int,
        val renderPaddingMeters: Double,
        val downsample: Int = 5,
        val blurSigmaPxLow: Float = 4f,
        val debugBorderPx: Int = 0
    )

    private val bitmapCache = LruCache<BitmapCacheKey, Bitmap>(maxSize = 100)

    private var simpleDebugBitmap: Bitmap? = null

    fun simpleDebugBitmap(
        coreTile: Tile,
        widthPxCore: Int,
        renderPaddingMeters: Double,
    ): Bitmap {

        if (simpleDebugBitmap != null) return simpleDebugBitmap!!

        fun metersPerDegLat(): Double = 111_320.0
        fun metersPerDegLng(atLatDeg: Double): Double =
            111_320.0 * cos(Math.toRadians(atLatDeg))

        val latTop = coreTile.topLeft.latitude
        val latBottom = coreTile.bottomRight.latitude
        val lngLeft = coreTile.topLeft.longitude
        val lngRight = coreTile.bottomRight.longitude
        val centerLat = (latTop + latBottom) / 2.0

        // 1) Build render tile (padded)
        val renderTile = paddedRenderTile(coreTile, renderPaddingMeters)
        val widthPx = computeWidthForRenderTile(coreTile, renderTile, widthPxCore)

        val tileWidthMeters = abs(lngRight - lngLeft) * metersPerDegLng(centerLat)
        val tileHeightMeters = abs(latTop - latBottom) * metersPerDegLat()
        val heightPx = max(1, round(widthPx * (tileHeightMeters / tileWidthMeters)).toInt())

        val out = IntArray(widthPx * heightPx)
        val bmpPadded = Bitmap.createBitmap(out, widthPx, heightPx, Bitmap.Config.ARGB_8888)

        val cropRect = computeCoreCropRectPx(coreTile, renderTile, bmpPadded.width, bmpPadded.height)

        val emptyBitmap = Bitmap.createBitmap(
            bmpPadded,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        ).copy(Bitmap.Config.ARGB_8888, true)

        drawDebugBorder(emptyBitmap, 2)
        simpleDebugBitmap = emptyBitmap

        return simpleDebugBitmap!!
    }

    fun generateTileGradientBitmapFastSeamless(
        positionsAll: List<Position>,
        coreTile: Tile,
        widthPxCore: Int,
        renderPaddingMeters: Double,
        downsample: Int = 4,
        blurSigmaPxLow: Float = 4f,
        debugBorderPx: Int = 0
    ): Bitmap {
        require(widthPxCore > 0)

        val key = BitmapCacheKey(positionsAll, coreTile, widthPxCore, renderPaddingMeters, downsample, blurSigmaPxLow, debugBorderPx)

        val cachedBitmap = bitmapCache[key]
        if (cachedBitmap != null) return cachedBitmap

        // 1) Build render tile (padded)
        val renderTile = paddedRenderTile(coreTile, renderPaddingMeters)

        // 2) Filter points relevant to padded area (huge speed win)
        val maxRadius = positionsAll.maxOfOrNull { it.radiusMeters.toDouble() } ?: 0.0
        val positions = filterPointsForTile(positionsAll, renderTile, extraMarginMeters = maxRadius)

        // 3) Render padded bitmap
        val bmpPadded = generateTileGradientBitmapFastNoNormalize(
            positions = positions,
            tile = renderTile,
            widthPx = computeWidthForRenderTile(coreTile, renderTile, widthPxCore),
            downsample = downsample,
            blurSigmaPx = blurSigmaPxLow,
        )

        // 4) Crop padded bitmap back to core area
        val cropRect = computeCoreCropRectPx(coreTile, renderTile, bmpPadded.width, bmpPadded.height)

        val immutableBitmap = Bitmap.createBitmap(
            bmpPadded,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )

        val result = if (debugBorderPx == 0) {
            immutableBitmap
        } else {
            val bmpCore = immutableBitmap.copy(Bitmap.Config.ARGB_8888, true)
            drawDebugBorder(bmpCore, debugBorderPx)
            bmpCore
        }
        bitmapCache.put(key, result)
        return result
    }

    private fun computeCoreCropRectPx(
        core: Tile,
        render: Tile,
        renderWidthPx: Int,
        renderHeightPx: Int
    ): Rect {
        // map core bounds into render pixel space linearly
        val latTopR = render.topLeft.latitude
        val latBottomR = render.bottomRight.latitude
        val lngLeftR = render.topLeft.longitude
        val lngRightR = render.bottomRight.longitude

        fun xPx(lng: Double): Int {
            val t = (lng - lngLeftR) / (lngRightR - lngLeftR)
            return (t * renderWidthPx).roundToInt()
        }

        fun yPx(lat: Double): Int {
            val t = (latTopR - lat) / (latTopR - latBottomR)
            return (t * renderHeightPx).roundToInt()
        }

        val left = xPx(core.topLeft.longitude).coerceIn(0, renderWidthPx)
        val right = xPx(core.bottomRight.longitude).coerceIn(0, renderWidthPx)
        val top = yPx(core.topLeft.latitude).coerceIn(0, renderHeightPx)
        val bottom = yPx(core.bottomRight.latitude).coerceIn(0, renderHeightPx)

        return Rect(
            min(left, right),
            min(top, bottom),
            max(left, right),
            max(top, bottom)
        )
    }

    fun buildTilesWithRenderPaddingStable(
        points: List<Location>,
        tileSizeMeters: Double,
        paddingMeters: Double = 0.0
    ): List<Tile> {
        if (points.isEmpty() || tileSizeMeters <= 0.0 || paddingMeters < 0.0) return emptyList()

        val N = tileSizeMeters
        val P = paddingMeters

        // Web Mercator constants (EPSG:3857)
        val R = 6378137.0
        val MAX_LAT = 85.05112878

        fun clampLat(lat: Double) = lat.coerceIn(-MAX_LAT, MAX_LAT)

        fun mercatorX(lngDeg: Double): Double {
            val lonRad = Math.toRadians(lngDeg)
            return R * lonRad
        }

        fun mercatorY(latDeg: Double): Double {
            val latRad = Math.toRadians(clampLat(latDeg))
            return R * ln(tan(Math.PI / 4.0 + latRad / 2.0))
        }

        fun inverseMercator(x: Double, y: Double): Pair<Double, Double> {
            val lonRad = x / R
            val latRad = 2.0 * atan(exp(y / R)) - Math.PI / 2.0
            return Math.toDegrees(latRad) to Math.toDegrees(lonRad)
        }

        data class IJ(val i: Int, val j: Int)

        val tilesToRender = HashSet<IJ>(points.size * 4)

        fun outwardSteps(distToEdge: Double): Int {
            if (P <= distToEdge) return 0
            val extra = P - distToEdge
            return ceil(extra / N).toInt().coerceAtLeast(1)
        }

        for (p in points) {
            val x = mercatorX(p.longitude)
            val y = mercatorY(p.latitude)

            val i0 = floor(x / N).toInt()
            val j0 = floor(y / N).toInt()
            tilesToRender.add(IJ(i0, j0))

            val lx = x - i0 * N          // [0, N)
            val ly = y - j0 * N          // [0, N)

            val distLeft = lx
            val distRight = N - lx
            val distTop = ly
            val distBottom = N - ly

            val leftSteps = outwardSteps(distLeft)
            val rightSteps = outwardSteps(distRight)
            val topSteps = outwardSteps(distTop)
            val bottomSteps = outwardSteps(distBottom)

            // Side neighbors
            for (s in 1..leftSteps) tilesToRender.add(IJ(i0 - s, j0))
            for (s in 1..rightSteps) tilesToRender.add(IJ(i0 + s, j0))
            for (s in 1..topSteps) tilesToRender.add(IJ(i0, j0 - s))
            for (s in 1..bottomSteps) tilesToRender.add(IJ(i0, j0 + s))

            // Corner neighbors
            for (sx in 1..leftSteps) {
                for (sy in 1..topSteps) tilesToRender.add(IJ(i0 - sx, j0 - sy))
                for (sy in 1..bottomSteps) tilesToRender.add(IJ(i0 - sx, j0 + sy))
            }
            for (sx in 1..rightSteps) {
                for (sy in 1..topSteps) tilesToRender.add(IJ(i0 + sx, j0 - sy))
                for (sy in 1..bottomSteps) tilesToRender.add(IJ(i0 + sx, j0 + sy))
            }
        }

        fun makeLocation(lat: Double, lng: Double) =
            Location("tile").apply {
                latitude = lat
                longitude = lng
            }

        val result = ArrayList<Tile>(tilesToRender.size)
        for ((i, j) in tilesToRender) {
            val x0 = i * N
            val y0 = j * N
            val x1 = x0 + N
            val y1 = y0 + N

            // In Mercator, +y is north. Tile top-left uses (x0, y1).
            val (latTop, lngLeft) = inverseMercator(x0, y1)
            val (latBottom, lngRight) = inverseMercator(x1, y0)

            result.add(
                Tile(
                    topLeft = makeLocation(latTop, lngLeft),
                    bottomRight = makeLocation(latBottom, lngRight)
                )
            )
        }

        return result
    }

    private fun drawDebugBorder(
        bitmap: Bitmap,
        borderPx: Int = 4,
        color: Int = 0xFFFF0000.toInt()     // red
    ) {
        if (borderPx <= 0) return

        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = borderPx.toFloat()
        }

        val half = borderPx / 2f
        canvas.drawRect(
            half,
            half,
            bitmap.width - half,
            bitmap.height - half,
            paint
        )
    }
}