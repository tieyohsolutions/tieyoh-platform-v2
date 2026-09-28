package com.tieyoh.mobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient

// Coil auto-discovers an Application implementing ImageLoaderFactory and
// uses it to build its singleton ImageLoader -- so this one client backs
// every AsyncImage in the app (MerchantLogoThumb in MainActivity.kt,
// PartneredMerchantSearchScreen.kt, MerchantSelectionScreen.kt, and the
// logo in WalletOverviewScreen.kt's MerchantBalanceRow), not just the
// call site that happened to need it first.
//
// Why this exists: merchant logos are hotlinked straight from whatever
// CDN a merchant's own website/listing happens to use -- right now
// that's Google's Business Profile photo CDN for the Outscraper-imported
// rows (see the V28/V29 migrations for The Wolf Cafe), eventually
// wherever the merchant onboarding site stores an uploaded file. Google's
// photo CDN specifically was confirmed to serve/block a request
// differently depending on the client's User-Agent -- the exact same URL
// loads fine from a normal browser-like fetch, but Coil's default OkHttp
// client sends its own generic "okhttp/..." UA and gets nothing back, no
// error surfaced, just a permanently loading/blank image. Setting a
// browser-like UA on every request here fixes that at the root instead
// of special-casing one merchant's URL.
class TieyohApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .build()
    }
}
