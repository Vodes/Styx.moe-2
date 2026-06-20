package moe.styx.web.util

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.ScaleMethod
import com.sksamuel.scrimage.webp.WebpWriter
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.Image
import moe.styx.common.http.httpClient
import moe.styx.common.isWindows
import java.io.File
import java.util.*

fun downloadImageForStyx(url: String, thumbnail: Boolean): Image? = runBlocking {
    val response = httpClient.get(url)
    if (response.status != HttpStatusCode.OK)
        return@runBlocking null

    val guid = UUID.randomUUID().toString().uppercase()

    if (isWindows) {
        return@runBlocking Image(guid, externalURL = url, type = if (thumbnail) 0 else 1)
    }
    val stream = response.bodyAsChannel().toInputStream()
    var image = ImmutableImage.loader().fromStream(stream).also {
        runCatching {
            stream.close()
        }.onFailure {
            return@runBlocking null
        }
    }

    // Make thumbnails smaller
    if (image.ratio() < 1 && image.height > 700)
        image = image.scaleToHeight(700, ScaleMethod.Bicubic)
    // Make banners smaller and since they're in landscape, go by width
    else if (image.ratio() > 1 && image.width > 1600)
        image = image.scaleToWidth(1600, ScaleMethod.Bicubic)

    val output = File(UnifiedConfig.current.base.imageDir(), "$guid.webp")
    image.output(WebpWriter.DEFAULT.withQ(100).withM(6), output)
    return@runBlocking Image(guid, hasWEBP = 1, type = if (thumbnail) 0 else 1)
}