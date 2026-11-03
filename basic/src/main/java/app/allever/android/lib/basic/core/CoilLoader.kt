package app.allever.android.lib.basic.core

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.widget.ImageView
import coil.Coil
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import java.io.File

object CoilLoader {

    private val TAG = "ImageLoader｜CoilLoader"
    private var isInit = false;

    fun init(context: Context) {
        if (isInit) {
            return
        }
        if (context is Application) {
            Coil.setImageLoader(context.initCoil())
            isInit = true
        }
    }

    fun load(resource: Any, imageView: ImageView, errorResId: Int?, placeholder: Int?) {
        imageView.load(resource) {
            applyDefault(this, errorResId, placeholder)
        }
    }

    fun loadCircle(
        resource: Any,
        imageView: ImageView,
        borderWidthDp: Int?,
        borderColor: Int?,
        errorResId: Int?,
        placeholder: Int?
    ) {
        imageView.load(resource) {
            applyDefault(this, errorResId, placeholder)
            transformations(
                BorderCircleTransformation(
                    DisplayHelper.dip2px(borderWidthDp ?: 0),
                    borderColor ?: Color.parseColor("#00000000")
                )
            )
        }
    }

    fun loadRound(
        resource: Any,
        imageView: ImageView,
        radiusDp: Float?,
        errorResId: Int?,
        placeholder: Int?
    ) {
        imageView.load(resource) {
            applyDefault(this, errorResId, placeholder)
            transformations(
                RoundedCornersTransformation(
                    radius = DisplayHelper.dip2px(radiusDp ?: 0f).toFloat()
                )
            )
        }
    }

    fun loadGif(resource: Any, imageView: ImageView) {
        load(resource, imageView, null, null)
    }

    fun loadBlur(resource: Any, imageView: ImageView, radius: Float?) {
        imageView.load(resource) {
            transformations(BlurTransformation(imageView.context, radius ?: 0f))
        }
    }

    fun download(url: String, block: ((Boolean, File?) -> Unit)) {

    }


    private fun applyDefault(
        imageRequestBuilder: ImageRequest.Builder,
        errorResId: Int?,
        placeholder: Int?
    ): ImageRequest.Builder {
        errorResId?.let {
            imageRequestBuilder.error(it)
        }
        placeholder?.let {
            imageRequestBuilder.placeholder(it)
        }
        imageRequestBuilder.crossfade(true)
        return imageRequestBuilder
    }
}

fun Application.initCoil(): ImageLoader {
    return ImageLoader.Builder(this)
        .components {
            if (SDK_INT >= 28) {
//                add(ImageDecoderDecoder.Factory())
            } else {
//                add(GifDecoder.Factory())
            }

//            add(SvgDecoder.Factory())
//            add(VideoFrameDecoder.Factory())
        }
        .crossfade(true)
        .build()
}
