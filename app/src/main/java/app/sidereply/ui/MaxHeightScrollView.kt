package app.sidereply.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * A ScrollView that grows with its content but never beyond a fraction of the display.
 * The reply sheet header (including the close button) stays fixed while long OCR text or
 * unusually long suggestions scroll inside this view on small screens.
 */
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ScrollView(context, attrs, defStyleAttr) {

    var maxHeightFraction: Float = 0.60f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val maxHeight = (resources.displayMetrics.heightPixels * maxHeightFraction).toInt()
        if (measuredHeight > maxHeight) {
            setMeasuredDimension(measuredWidth, maxHeight)
        }
    }
}
