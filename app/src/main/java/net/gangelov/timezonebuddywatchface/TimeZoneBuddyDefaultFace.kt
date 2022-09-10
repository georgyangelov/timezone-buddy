package net.gangelov.timezonebuddywatchface

import android.graphics.*
import android.os.Bundle
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import androidx.core.math.MathUtils
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")

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

    private val hourMarkersPaint = Paint().apply {
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

        val now = calendar.toInstant()
        val nowAtDefaultZone = now.atZone(TimeZone.getDefault().toZoneId())
        val nowAtPrimaryZone = now.atZone(primaryTimeZone.timeZone.toZoneId())
        val nowAtSecondaryZone = now.atZone(secondaryTimeZone.timeZone.toZoneId())

        val offsetOfCurrentZone = nowAtDefaultZone.offset.totalSeconds
        val offsetOfPrimaryZone = nowAtPrimaryZone.offset.totalSeconds
        val offsetOfSecondaryZone = nowAtSecondaryZone.offset.totalSeconds

        val primaryOffset = offsetOfCurrentZone - offsetOfPrimaryZone
        val secondaryOffset = offsetOfCurrentZone - offsetOfSecondaryZone

        canvas.drawArc(primaryArcRect, 0f, 360f, false, baseArcPaint)
        drawArcForTimeInterval(canvas, primaryTimeZone.dayTime, primaryOffset, primaryArcRect, primaryMutedPaint)
        drawArcForTimeInterval(canvas, primaryTimeZone.workTime, primaryOffset, primaryArcRect, primaryPaint)

        canvas.drawArc(secondaryArcRect, 0f, 360f, false, baseArcPaint)
        drawArcForTimeInterval(canvas, secondaryTimeZone.dayTime, secondaryOffset, secondaryArcRect, secondaryMutedPaint)
        drawArcForTimeInterval(canvas, secondaryTimeZone.workTime, secondaryOffset, secondaryArcRect, secondaryPaint)

        val localTimeAtDefaultZone = nowAtDefaultZone.toLocalTime()

        drawHourLabels(canvas, center, primaryArcInset - 20f, primaryOffset, localTimeAtDefaultZone)
        drawHourLabels(canvas, center, secondaryArcInset + 20f, secondaryOffset, localTimeAtDefaultZone)

        drawCurrentTimeIndicator(
            canvas, center,
            primaryArcInset - 5f, secondaryArcInset + 5f,
            localTimeAtDefaultZone
        )

        if (primaryOffset != 0) {
            drawDigitalClock(
                canvas, center - PointF(0f, 60f),
                nowAtPrimaryZone.toLocalTime(),
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
            localTimeAtDefaultZone,
            color = currentTimeColor,
            fontSize = 72f
        )

        if (secondaryOffset != 0) {
            drawDigitalClock(
                canvas, center + PointF(0f, 60f),
                nowAtSecondaryZone.toLocalTime(),
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

    private val textBoundsTmp = Rect()

    private fun drawHourLabels(
        canvas: Canvas,
        center: PointF,
        inset: Float,
        timeZoneOffsetInSeconds: Int,
        time: LocalTime
    ) {
        val textRadius = center.x - inset
        val angleOffset = timeZoneOffsetInSeconds / 240f
        val currentTimeAngle = time.toSecondOfDay() / 240f

        for (tickIndex in 0..23) {
            val tickRotationDegrees = tickIndex.toDouble() * 360 / 24 + angleOffset

            val angleDifference = abs(currentTimeAngle - tickRotationDegrees) % 360
            val angleDifferenceAbs = abs(if (angleDifference > 180) 360 - angleDifference else angleDifference).toFloat()

            hourMarkersPaint.alpha = lerp(fraction(MathUtils.clamp(angleDifferenceAbs, 0f, 120f), 0f, 120f), 255f, 50f).roundToInt()

            val tickRotationRadians = tickRotationDegrees * Math.PI / 180f

            val textX = sin(tickRotationRadians).toFloat() * textRadius
            val textY = (-cos(tickRotationRadians)).toFloat() * textRadius

            val hourLabel = tickIndex.toString()

            hourMarkersPaint.getTextBounds(hourLabel, 0, hourLabel.length, textBoundsTmp)

            canvas.drawText(
                hourLabel,
                center.x + textX - textBoundsTmp.exactCenterX(),
                center.y + textY - textBoundsTmp.exactCenterY(),
                hourMarkersPaint
            )
        }
    }

    private fun drawCurrentTimeIndicator(
        canvas: Canvas,
        center: PointF,
        outerInset: Float,
        innerInset: Float,
        time: LocalTime
    ) {
        val rotationDegrees = time.toSecondOfDay() / 240f
        val rotationRadians = rotationDegrees * Math.PI / 180f

        val innerRadius = center.x - innerInset
        val outerRadius = center.x - outerInset

        val insideX = sin(rotationRadians).toFloat() * innerRadius
        val insideY = (-cos(rotationRadians)).toFloat() * innerRadius

        val outsideX = sin(rotationRadians).toFloat() * outerRadius
        val outsideY = (-cos(rotationRadians)).toFloat() * outerRadius

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

    private val digitalClockPaintTmp = Paint().apply {
        isAntiAlias = true
    }

    private fun drawDigitalClock(canvas: Canvas, center: PointF, time: LocalTime, fontSize: Float, color: Int) {
        digitalClockPaintTmp.color = color
        digitalClockPaintTmp.textSize = fontSize

        val text = time.format(clockFormat)
        digitalClockPaintTmp.getTextBounds(text, 0, text.length, textBoundsTmp)

        canvas.drawText(
            text,
            center.x - textBoundsTmp.exactCenterX(), center.y - textBoundsTmp.exactCenterY(),
            digitalClockPaintTmp
        )
    }
}