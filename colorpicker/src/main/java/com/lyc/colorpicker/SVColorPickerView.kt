package com.lyc.colorpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Size
import androidx.core.content.res.use
import androidx.core.graphics.withSave
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by Liu Yuchuan on 2020/2/15.
 */
class SVColorPickerView @JvmOverloads @SuppressLint("Recycle") constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bounds = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paddingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawablePosition = Rect()
    private var currentPointerId = -1

    private var colorPickerViewListener: ColorPickerViewListener? = null
    // 0 ~ 1/3 -> red
    // 1/3 ~ 2/3 -> green
    // 2/3 ~ 1 -> blue
    var hue: Float = 0f
        set(value) {
            val validateValue = value.coerceIn(0f, 360f)
            if (validateValue != field) {
                field = validateValue
                onSizeUpdate()
                changeColorAndNotify {
                    currentColorHSV[0] = validateValue
                }
                invalidate()
            }
        }
    var paddingColor: Int = Color.TRANSPARENT
        set(value) {
            if (field != value) {
                field = value
                paddingPaint.color = value
                invalidate()
            }
        }
    var thumbDrawable: Drawable? = null
        // actually non-null
        get() {
            return field ?: defaultThumbDrawable
        }
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private val currentColorHSV = floatArrayOf(hue, 0.5f, 0.5f)
    private val defaultThumbStroke =
        (context.resources.displayMetrics.density * 2 + 0.5f).roundToInt()
    private val defaultThumbSize = (context.resources.displayMetrics.density * 24).roundToInt()
    var thumbWidth = defaultThumbSize
        set(value) {
            if (value != field && value > 0) {
                field = value
                updateDrawablePositionByColorHSV(currentColorHSV)
                invalidate()
            }
        }
    var thumbHeight = defaultThumbSize
        set(value) {
            if (value != field && value > 0) {
                field = value
                updateDrawablePositionByColorHSV(currentColorHSV)
                invalidate()
            }
        }
    private val defaultThumbDrawable by lazy {
        GradientDrawable().apply {
            setStroke(
                defaultThumbStroke,
                Color.WHITE
            )
        }
    }
    private val thumbDrawableInternal
        get() = thumbDrawable ?: defaultThumbDrawable

    init {
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.SVColorPickerView).use { ta ->
                paddingColor = ta.getColor(R.styleable.SVColorPickerView_paddingColor, paddingColor)
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
        if (left != bounds.left || top != bounds.top || right != bounds.right || bottom != bounds.bottom) {
            bounds.set(left, top, right, bottom)
            onSizeUpdate()
            updateDrawablePositionByColorHSV(currentColorHSV)
        }
    }

    private fun updateDrawablePositionByColorHSV(@Size(3) hsvColor: FloatArray) {
        val x = bounds.width() * hsvColor[1] + bounds.left
        val y = bounds.bottom - bounds.height() * hsvColor[2]
        updateDrawablePosition(x, y)
    }

    private fun updateDrawablePosition(x: Float, y: Float) {
        drawablePosition.left = (x - thumbWidth * 0.5f).roundToInt()
        drawablePosition.top = (y - thumbHeight * 0.5f).roundToInt()
        drawablePosition.right = (x + thumbWidth * 0.5f).roundToInt()
        drawablePosition.bottom = (y + thumbHeight * 0.5f).roundToInt()
    }

    @ColorInt
    private fun getColorAtLocation(x: Float, y: Float): Int {
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0 || h <= 0) return 0
        val saturation = (x / w).coerceIn(0f, 1f)
        val value = (1f - y / h).coerceIn(0f, 1f)
        return Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    private fun getColorHSVAtLocation(@Size(3) hsvColor: FloatArray, x: Float, y: Float) {
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0 || h <= 0) return
        val saturation = ((x - bounds.left) / w).coerceIn(0f, 1f)
        val value = (1f - (y - bounds.top) / h).coerceIn(0f, 1f)
        hsvColor[0] = hue
        hsvColor[1] = saturation
        hsvColor[2] = value
    }

    private fun onSizeUpdate() {
        // 饱和度
        val saturationShader = LinearGradient(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.top,
            0xffffffff.toInt(),
            Color.HSVToColor(floatArrayOf(hue, 1f, 1f)),
            Shader.TileMode.CLAMP
        )
        // 明度
        val valueShader = LinearGradient(
            bounds.left,
            bounds.top,
            bounds.left,
            bounds.bottom,
            0xffffffff.toInt(),
            0xff000000.toInt(),
            Shader.TileMode.CLAMP
        )
        paint.shader = ComposeShader(valueShader, saturationShader, PorterDuff.Mode.MULTIPLY)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val width =
            if (wMode == MeasureSpec.AT_MOST || wMode == MeasureSpec.UNSPECIFIED) {
                min(
                    w,
                    (context.resources.displayMetrics.density * 200).toInt() + paddingLeft + paddingRight
                )
            } else {
                w
            }
        val height =
            if (hMode == MeasureSpec.AT_MOST || hMode == MeasureSpec.UNSPECIFIED) {
                min(
                    h,
                    (context.resources.displayMetrics.density * 200).toInt() + paddingTop + paddingBottom
                )
            } else {
                h
            }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (paddingColor and 0xff000000.toInt() != 0) {
            canvas.withSave {
                if (Build.VERSION.SDK_INT >= 26) {
                    clipOutRect(bounds)
                } else {
                    clipRect(bounds, Region.Op.DIFFERENCE)
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paddingPaint)
            }
        }
        canvas.drawRect(bounds, paint)
        thumbDrawableInternal.bounds = drawablePosition
        thumbDrawableInternal.draw(canvas)
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
        val xInt = x.roundToInt()
        val yInt = y.roundToInt()

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentPointerId = pointerId
                if (!drawablePosition.contains(xInt, yInt)) {
                    changeColorAndNotify {
                        getColorHSVAtLocation(currentColorHSV, x, y)
                        updateDrawablePosition(x, y)
                        postInvalidate()
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val newX = x.coerceIn(bounds.left, bounds.right)
                val newY = y.coerceIn(bounds.top, bounds.bottom)
                changeColorAndNotify {
                    getColorHSVAtLocation(currentColorHSV, newX, newY)
                    updateDrawablePosition(newX, newY)
                    postInvalidate()
                }
            }
        }


        return true
    }

    private inline fun changeColorAndNotify(func: () -> Unit) {
        val lastColor = Color.HSVToColor(currentColorHSV)
        func()
        val newColor = Color.HSVToColor(currentColorHSV)
        if (lastColor != newColor) {
            colorPickerViewListener?.onColorChange(newColor, currentColorHSV.clone())
        }
    }

    fun setColorPickerViewListener(listener: ColorPickerViewListener?) {
        this.colorPickerViewListener = listener
    }

    @ColorInt
    fun getCurrentColor(): Int {
        return Color.HSVToColor(currentColorHSV)
    }

    private fun getColorHSV(@Size(3) colorHSV: FloatArray) {
        System.arraycopy(currentColorHSV, 0, colorHSV, 0, 3)
    }

    fun setCurrentColor(color: Int) {
        Color.colorToHSV(color, currentColorHSV)
        hue = currentColorHSV[0]
    }

    fun setCurrentColorHSV(@Size(3) colorHSV: FloatArray) {
        changeColorAndNotify {
            System.arraycopy(colorHSV, 0, currentColorHSV, 0, 3)
            hue = currentColorHSV[0]
        }
    }

    interface ColorPickerViewListener {
        fun onColorChange(newColor: Int, colorHSV: FloatArray)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return State(currentColorHSV, super.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? State)?.run {
            super.onRestoreInstanceState(superState)
            setCurrentColorHSV(state.currentColor)
        }
    }

    private class State(
        @Size(3)
        val currentColor: FloatArray,
        val superState: Parcelable?
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            FloatArray(3).also { parcel.readFloatArray(it) }, parcel.readParcelable(
                ClassLoader.getSystemClassLoader()
            )
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeFloatArray(currentColor)
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
