package shop.chobbar.app

import android.app.ActivityManager
import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import shop.chobbar.app.di.AppContainer

class ChobbarApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    /** True on low-RAM phones: smaller image cache, RGB_565 bitmaps, fewer animations. */
    val isLowEnd: Boolean by lazy {
        val am = getSystemService(ActivityManager::class.java)
        am.isLowRamDevice || am.memoryClass <= 192
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { container.client.http }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(if (isLowEnd) 0.12 else 0.2).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(80L * 1024 * 1024).build() }
        .allowRgb565(isLowEnd)
        .crossfade(!isLowEnd)
        .respectCacheHeaders(false)
        .build()
}
