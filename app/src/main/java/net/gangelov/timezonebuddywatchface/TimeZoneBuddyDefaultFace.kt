package net.gangelov.timezonebuddywatchface

import android.graphics.*
import android.os.Bundle
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import androidx.core.math.MathUtils
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

data class TimeZoneConfig(
    val timeZone: TimeZone,
    val color: Int,
    val mutedColor: Int,
    val dayTime: TimeInterval,
    val workTime: TimeInterval
)

data class TimeInterval(
    val start: LocalTime,
    val end: LocalTime
) {
    fun totalSeconds(): Int {
        return end.toSecondOfDay() - start.toSecondOfDay()
    }
}

class DefaultWatchFace : CanvasWatchFaceService() {
    override fun onCreateEngine(): Engine {
        return Engine()
    }

    inner class Engine : CanvasWatchFaceService.Engine() {
        private val primaryTimeZoneConfig = TimeZoneConfig(
            TimeZone.getTimeZone(ZoneId.of("Europe/Sofia")),
            color = Color.rgb(250, 194, 97),
            mutedColor = Color.rgb(178, 115, 6),
            dayTime = TimeInterval(
                LocalTime.of(8, 0),
                LocalTime.of(21, 0)
            ),
            workTime = TimeInterval(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0)
            )
        )

        private val secondaryTimeZoneConfig = TimeZoneConfig(
            TimeZone.getTimeZone(ZoneId.of("America/New_York")),
            color = Color.rgb(176, 144, 223),
            mutedColor = Color.rgb(119, 65, 200),
            dayTime = TimeInterval(
                LocalTime.of(8, 0),
                LocalTime.of(21, 0)
            ),
            workTime = TimeInterval(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0)
            )
        )

        private lateinit var calendar: Calendar

        private val painter = WatchFacePainter(
            primaryTimeZoneConfig,
            secondaryTimeZoneConfig
        )

        override fun onCreate(holder: SurfaceHolder?) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@DefaultWatchFace).build()
            )

            calendar = Calendar.getInstance()
        }

        override fun onPropertiesChanged(properties: Bundle?) {
            super.onPropertiesChanged(properties)

//            properties.getBoolean(
//                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
//            )

//            properties.getBoolean(
//                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
//            )
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            invalidate()
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        private var lastBounds = Rect(0, 0, 1, 1)
        override fun onDraw(canvas: Canvas, bounds: Rect) {
            calendar.timeInMillis = System.currentTimeMillis()

            if (lastBounds != bounds) {
                painter.boundsUpdated(bounds)
            }

            painter.drawOn(canvas, calendar)
        }
    }
}

class WatchFacePainter(
    private val primaryTimeZone: TimeZoneConfig,
    private val secondaryTimeZone: TimeZoneConfig
) {
    private val arcInset = 35f
    private val primaryArcInset = arcInset + 0f
    private val secondaryArcInset = arcInset + 15f

    private val baseArcPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.rgb(40, 40, 40)
        strokeWidth = 10f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val primaryPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = primaryTimeZone.color
        strokeWidth = 10f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val primaryMutedPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = primaryTimeZone.mutedColor
        strokeWidth = 10f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val secondaryPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = secondaryTimeZone.color
        strokeWidth = 10f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val secondaryMutedPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = secondaryTimeZone.mutedColor
        strokeWidth = 10f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    val hourMarkersPaint = Paint().apply {
        color = Color.rgb(255, 255, 255)
        isAntiAlias = true
        alpha = 200
        textSize = 18f
    }

    private lateinit var center: PointF
    private lateinit var primaryArcRect: RectF
    private lateinit var secondaryArcRect: RectF
    fun boundsUpdated(bounds: Rect) {
        center = PointF(bounds.exactCenterX(), bounds.exactCenterY())
        primaryArcRect = RectF(bounds).apply {
            inset(primaryArcInset, primaryArcInset)
        }
        secondaryArcRect = RectF(bounds).apply {
            inset(secondaryArcInset, secondaryArcInset)
        }
    }

    fun drawOn(canvas: Canvas, calendar: Calendar) {
        canvas.drawColor(Color.BLACK)

        // TODO: Cache these calculations?
        val now = calendar.toInstant()
        val offsetOfCurrentZone = now.atZone(TimeZone.getDefault().toZoneId()).offset.totalSeconds
//        val offsetOfCurrentZone = now.atZone(TimeZone.getTimeZone("France/Paris").toZoneId()).offset.totalSeconds
//        val offsetOfCurrentZone = now.atZone(TimeZone.getTimeZone("America/New_York").toZoneId()).offset.totalSeconds
        val offsetOfPrimaryZone = now.atZone(primaryTimeZone.timeZone.toZoneId()).offset.totalSeconds
        val offsetOfSecondaryZone = now.atZone(secondaryTimeZone.timeZone.toZoneId()).offset.totalSeconds

        val primaryOffset = offsetOfCurrentZone - offsetOfPrimaryZone
        val secondaryOffset = offsetOfCurrentZone - offsetOfSecondaryZone

        canvas.drawArc(primaryArcRect, 0f, 360f, false, baseArcPaint)
        drawArcForTimeInterval(canvas, primaryTimeZone.dayTime, primaryOffset, primaryArcRect, primaryMutedPaint)
        drawArcForTimeInterval(canvas, primaryTimeZone.workTime, primaryOffset, primaryArcRect, primaryPaint)

        canvas.drawArc(secondaryArcRect, 0f, 360f, false, baseArcPaint)
        drawArcForTimeInterval(canvas, secondaryTimeZone.dayTime, secondaryOffset, secondaryArcRect, secondaryMutedPaint)
        drawArcForTimeInterval(canvas, secondaryTimeZone.workTime, secondaryOffset, secondaryArcRect, secondaryPaint)

        val currentTime = now.atZone(TimeZone.getDefault().toZoneId())
        val currentTimeSeconds = currentTime.toLocalTime().toSecondOfDay()

        drawHourLabels(canvas, center, primaryArcInset - 20f, primaryOffset, currentTimeSeconds)
        drawHourLabels(canvas, center, secondaryArcInset + 20f, secondaryOffset, currentTimeSeconds)

        drawCurrentTimeIndicator(
            canvas, center,
            primaryArcInset - 5f, secondaryArcInset + 5f,
            currentTimeSeconds
        )

        if (primaryOffset != 0) {
            drawDigitalClock(
                canvas, center - PointF(0f, 60f),
                now.atZone(primaryTimeZone.timeZone.toZoneId()).toLocalTime(),
                color = primaryTimeZone.color,
                fontSize = 42f
            )
        }

        val currentTimeColor =
            if (primaryOffset == 0) primaryTimeZone.color
            else if (secondaryOffset == 0) secondaryTimeZone.color
            else Color.rgb(255, 255, 255)

        drawDigitalClock(
            canvas, center,
            currentTime.toLocalTime(),
            color = currentTimeColor,
            fontSize = 72f
        )

        if (secondaryOffset != 0) {
            drawDigitalClock(
                canvas, center + PointF(0f, 60f),
                now.atZone(secondaryTimeZone.timeZone.toZoneId()).toLocalTime(),
                color = secondaryTimeZone.color,
                fontSize = 42f
            )
        }
    }

    private fun drawArcForTimeInterval(
        canvas: Canvas,
        interval: TimeInterval,
        offsetInSeconds: Int,
        rect: RectF,
        paint: Paint
    ) {
        val timeStartAngle = (interval.start.toSecondOfDay() + offsetInSeconds) / 240f
        val sweepAngle = interval.totalSeconds() / 240f

        canvas.drawArc(
            rect,
            timeStartAngle - 90f,
            sweepAngle,
            false,
            paint
        )
    }

    private inline fun lerp(t: Float, from: Float, to: Float): Float {
        return from + t * (to - from)
    }

    private inline fun fraction(x: Float, min: Float, max: Float): Float {
        return (x - min) / (max - min)
    }

    private fun drawHourLabels(
        canvas: Canvas,
        center: PointF,
        inset: Float,
        timeZoneOffsetInSeconds: Int,
        nowSeconds: Int
    ) {
        val textRadius = center.x - inset
        val angleOffset = timeZoneOffsetInSeconds / 240f
        val currentTimeAngle = nowSeconds / 240f

        // TODO: Do not initialize on each call?
        val textBounds = Rect()

        for (tickIndex in 0..23) {
            val tickRotationDegrees = tickIndex.toDouble() * 360 / 24 + angleOffset

            val angleDifference = abs(currentTimeAngle - tickRotationDegrees) % 360
            val angleDifferenceAbs = Math.abs(if (angleDifference > 180) 360 - angleDifference else angleDifference).toFloat()

            hourMarkersPaint.alpha = lerp(fraction(MathUtils.clamp(angleDifferenceAbs, 0f, 120f), 0f, 120f), 255f, 50f).roundToInt()

            val tickRotationRadians = tickRotationDegrees * Math.PI / 180f

            val textX = Math.sin(tickRotationRadians).toFloat() * textRadius
            val textY = (-Math.cos(tickRotationRadians)).toFloat() * textRadius

            val hourLabel = tickIndex.toString()

            hourMarkersPaint.getTextBounds(hourLabel, 0, hourLabel.length, textBounds)

            canvas.drawText(
                hourLabel,
                center.x + textX - textBounds.exactCenterX(),
                center.y + textY - textBounds.exactCenterY(),
                hourMarkersPaint
            )
        }
    }

    private fun drawCurrentTimeIndicator(
        canvas: Canvas,
        center: PointF,
        outerInset: Float,
        innerInset: Float,
        nowSeconds: Int
    ) {
        val rotationDegrees = nowSeconds / 240f
        val rotationRadians = rotationDegrees * Math.PI / 180f

        val innerRadius = center.x - innerInset
        val outerRadius = center.x - outerInset

        val insideX = Math.sin(rotationRadians).toFloat() * innerRadius
        val insideY = (-Math.cos(rotationRadians)).toFloat() * innerRadius

        val outsideX = Math.sin(rotationRadians).toFloat() * outerRadius
        val outsideY = (-Math.cos(rotationRadians)).toFloat() * outerRadius

        val currentTimeIndicatorPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = 5f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        canvas.drawLine(
            center.x + insideX, center.y + insideY,
            center.x + outsideX, center.y + outsideY,
            currentTimeIndicatorPaint
        )
    }

    private fun drawDigitalClock(canvas: Canvas, center: PointF, time: LocalTime, fontSize: Float, color: Int) {
        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
            textSize = fontSize
        }

        val text = time.format(DateTimeFormatter.ofPattern("HH:mm"))
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)

        canvas.drawText(
            text,
            center.x - textBounds.exactCenterX(), center.y - textBounds.exactCenterY(),
            paint
        )
    }
}