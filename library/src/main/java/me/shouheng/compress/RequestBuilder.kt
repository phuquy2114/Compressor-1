package me.shouheng.compress

import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.os.Message
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * The request builder object. Used to build the compress request.
 * It contains many useful methods like [.notifyCompressSuccess].
 * This class has two children, [AbstractStrategy] and
 * [me.shouheng.compress.request.BitmapBuilder]
 *
 * @param <T> the required result type
 */
abstract class RequestBuilder<T> : Handler.Callback {

    private var compressListener: Callback<T>? = null
    private var abstractStrategy: AbstractStrategy? = null

    private val handler = Handler(Looper.getMainLooper(), this)

    /**
     * Get bitmap from given strategy. The strategy must implement this method.
     * Mainly this method is used to get bitmap in [RequestBuilder]
     * like [me.shouheng.compress.request.BitmapBuilder] to get bitmap from real compressor
     * like luban and compressor and transform the bitmap to required type.
     *
     * @return the bitmap
     */
    open fun getBitmap(): Bitmap? {
        abstractStrategy?: throw IllegalStateException("The real compress strategy is null.")
        return abstractStrategy!!.getBitmap()
    }

    /**
     * Blocking method used to get the compressed result in current thread.
     */
    abstract fun get(): T?

    /**
     * Get the result using kotlin coroutines, for example [Dispatchers.IO]
     */
    abstract suspend fun get(coroutineContext: CoroutineContext): T?

    /**
     * Use RxJava to get the result
     */
    abstract fun asFlowable(): Flowable<T>

    /**
     * Launch the compressor task in [AsyncTask]
     */
    abstract fun launch()

    override fun handleMessage(msg: Message): Boolean {
        if (compressListener == null) return false
        when (msg.what) {
            MSG_COMPRESS_START -> compressListener?.onStart()
            MSG_COMPRESS_SUCCESS -> compressListener?.onSuccess(msg.obj as T)
            MSG_COMPRESS_ERROR -> compressListener?.onError(msg.obj as Throwable)
            else -> { /* noop */ }
        }
        return false
    }

    fun setCompressListener(compressListener: Callback<T>?): RequestBuilder<T> {
        this.compressListener = compressListener
        return this
    }

    fun setAbstractStrategy(abstractStrategy: AbstractStrategy) {
        this.abstractStrategy = abstractStrategy
    }

    protected fun notifyCompressStart() {
        handler.sendMessage(handler.obtainMessage(MSG_COMPRESS_START))
    }

    protected fun notifyCompressSuccess(result: T) {
        handler.sendMessage(handler.obtainMessage(MSG_COMPRESS_SUCCESS, result))
    }

    protected fun notifyCompressError(throwable: Throwable) {
        handler.sendMessage(handler.obtainMessage(MSG_COMPRESS_ERROR, throwable))
    }

    interface Callback<T> {

        /**
         * Will be called when start to compress.
         */
        fun onStart()

        /**
         * Will be called when finish compress.
         *
         * @param result the compressed image
         */
        fun onSuccess(result: T)

        /**
         * Will be called when error occurred.
         *
         * @param throwable the throwable exception
         */
        fun onError(throwable: Throwable)
    }

    companion object {
        private const val MSG_COMPRESS_SUCCESS  = 0
        private const val MSG_COMPRESS_START    = 1
        private const val MSG_COMPRESS_ERROR    = 2
    }
}
