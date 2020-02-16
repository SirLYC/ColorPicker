package com.lyc.colorpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Size
import androidx.core.content.res.use
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by Liu Yuchuan on 2020/2/16.
 */
class HueBarView @JvmOverloads @SuppressLint("Recycle") constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val bounds = RectF()
    private val drawablePosition = Rect()
    private var onHueBarChangeListener: OnHueBarChangeListener? = null
    private var isBroadCast = false

    // 0 ~ 1/3 -> red
    // 1/3 ~ 2/3 -> green
    // 2/3 ~ 1 -> blue
    var currentHue = 0f
        set(value) {
            val validateValue = value.coerceIn(0f, 360f)
            if (validateValue != field) {
                field = validateValue
                if (!isBroadCast) {
                    isBroadCast = true
                    onHueBarChangeListener?.onHueChange(validateValue)
                    isBroadCast = false
                }
                updateProgressInternal(validateValue)
                invalidate()
            }
        }
    private var currentPointerId = -1
    var orientation: Orientation = Orientation.HORIZONTAL
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val defaultWidth = (context.resources.displayMetrics.density * 180).roundToInt()
    private val defaultHeight = (context.resources.displayMetrics.density * 24).roundToInt()

    private val hsvBuffer = floatArrayOf(0f, 1f, 1f)
    private val gradientColors = IntArray(360) { index ->
        hsvBuffer[0] = index.toFloat()
        Color.HSVToColor(hsvBuffer)
    }

    private val defaultThumbSize = (context.resources.displayMetrics.density * 24).roundToInt()
    var thumbWidth = (context.resources.displayMetrics.density * 24 + 0.5f).toInt()
        set(value) {
            if (value != field && value > 0) {
                field = value
                updateProgressInternal(currentHue)
                invalidate()
            }
        }
    var thumbHeight = defaultThumbSize
        set(value) {
            if (value != field && value > 0) {
                field = value
                updateProgressInternal(currentHue)
                invalidate()
            }
        }
    private val defaultThumbDrawable by lazy {
        GradientDrawable().apply {
            setStroke(
                (context.resources.displayMetrics.density * 2 + 0.5f).toInt(),
                Color.WHITE
            )
        }
    }
    var thumbDrawable: Drawable? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private val thumbDrawableInternal
        get() = thumbDrawable ?: defaultThumbDrawable

    init {
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.HueBarView).use { ta ->
                orientation = Orientation.values()[ta.getInt(R.styleable.HueBarView_orientation, 0)]
                thumbWidth = ta.getDimensionPixelSize(
                    R.styleable.SVColorPickerView_thumbWidth,
                    defaultThumbSize
                )
                thumbHeight = ta.getDimensionPixelSize(
                    R.styleable.SVColorPickerView_thumbHeight,
                    defaultThumbSize
                )
                thumbDrawable = ta.getDrawable(R.styleable.SVColorPickerView_thumbDrawable)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = (w - paddingRight).toFloat()
        val bottom = (h - paddingBottom).toFloat()
        val xChange = left != bounds.left || right != bounds.right
        if (xChange || top != bounds.top || bottom != bounds.bottom) {
            bounds.set(left, top, right, bottom)
            if (xChange) {
                updateShader()
            }
            updateProgressInternal(currentHue)
        }
    }

    private fun updateShader() {
        paint.shader = LinearGradient(
            bounds.left,
            bounds.top,
            if (orientation == Orientation.HORIZONTAL) {
                bounds.right
            } else {
                bounds.left
            },
            if (orientation == Orientation.HORIZONTAL) {
                bounds.top
            } else {
                bounds.bottom
            },
            gradientColors,
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(bounds, paint)
        thumbDrawableInternal.bounds = drawablePosition
        thumbDrawableInternal.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)

        if (orientation == Orientation.HORIZONTAL) {
            val width =
                if (wMode == MeasureSpec.AT_MOST || wMode == MeasureSpec.UNSPECIFIED) {
                    min(w, defaultWidth + paddingLeft + paddingTop)
                } else {
                    w
                }
            val height =
                if (hMode == MeasureSpec.AT_MOST || hMode == MeasureSpec.UNSPECIFIED) {
                    min(h, defaultHeight + paddingLeft + paddingTop)
                } else {
                    h
                }
            setMeasuredDimension(width, height)
        } else {
            val width =
                if (wMode == MeasureSpec.AT_MOST || wMode == MeasureSpec.UNSPECIFIED) {
                    min(w, defaultHeight + paddingLeft + paddingTop)
                } else {
                    w
                }
            val height =
                if (hMode == MeasureSpec.AT_MOST || hMode == MeasureSpec.UNSPECIFIED) {
                    min(h, defaultWidth + paddingLeft + paddingTop)
                } else {
                    h
                }
            setMeasuredDimension(width, height)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }

        val index = event.actionIndex
        val actionMasked = event.actionMasked
        val pointerId = event.getPointerId(index)

        if (pointerId == -1) {
            return false
        }

        if (actionMasked != MotionEvent.ACTION_DOWN && (pointerId != currentPointerId || currentPointerId == -1)) {
            return false
        }

        val x = event.getX(index)
        val y = event.getY(index)

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            currentPointerId = pointerId
        }

        if (actionMasked == MotionEvent.ACTION_DOWN || actionMasked == MotionEvent.ACTION_MOVE) {
            currentHue = if (orientation == Orientation.HORIZONTAL) {
                (x / bounds.width() * 360f).coerceIn(0f, 360f)
            } else {
                (y / bounds.height() * 360f).coerceIn(0f, 360f)
            }
        }

        return true
    }


    private fun getProgressLength(total: Float, progress: Float): Float {
        val seg = total / 360
        return seg * progress.coerceIn(0f, 360f)
    }

    private fun updateProgressInternal(progress: Float) {
        val x = if (orientation == Orientation.HORIZONTAL) {
            getProgressLength(bounds.width(), progress) + bounds.left
        } else {
            bounds.centerX()
        }
        val y = if (orientation == Orientation.HORIZONTAL) {
            bounds.centerY()
        } else {
            getProgressLength(bounds.height(), progress) + bounds.top
        }
        drawablePosition.left = (x - thumbWidth * 0.5f).roundToInt()
        drawablePosition.top = (y - thumbHeight * 0.5f).roundToInt()
        drawablePosition.right = (x + thumbWidth * 0.5f).roundToInt()
        drawablePosition.bottom = (y + thumbHeight * 0.5f).roundToInt()
    }


    enum class Orientation {
        HORIZONTAL, VERTICAL
    }

    interface OnHueBarChangeListener {
        // 0 ~ 360
        fun onHueChange(newHue: Float)
    }

    fun setOnHueBarChangeListener(listener: OnHueBarChangeListener?) {
        this.onHueBarChangeListener = listener
    }

    override fun onSaveInstanceState(): Parcelable? {
        return State(currentHue, super.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? State)?.run {
            super.onRestoreInstanceState(superState)
            this@HueBarView.currentHue = state.currentHue
        }
    }

    private class State(
        @Size(3)
        val currentHue: Float,
        val superState: Parcelable?
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readFloat(), parcel.readParcelable(
                ClassLoader.getSystemClassLoader()
            )
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeFloat(currentHue)
            parcel.writeParcelable(superState, 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<State> {
            override fun createFromParcel(parcel: Parcel): State {
                return State(parcel)
            }

            override fun newArray(size: Int): Array<State?> {
                return arrayOfNulls(size)
            }
        }
    }
}
