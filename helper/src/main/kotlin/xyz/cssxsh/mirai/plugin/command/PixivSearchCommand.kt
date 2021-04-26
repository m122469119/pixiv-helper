package xyz.cssxsh.mirai.plugin.command

import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.descriptor.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.plugin.*
import xyz.cssxsh.mirai.plugin.tools.ImageSearcher

object PixivSearchCommand : SimpleCommand(
    owner = PixivHelperPlugin,
    "search", "搜索", "搜图",
    description = "PIXIV搜索指令"
) {

    @ExperimentalCommandDescriptors
    @ConsoleExperimentalApi
    override val prefixOptional: Boolean = true

    private fun MessageChain.getQuoteImage(): Image = requireNotNull(findIsInstance<QuoteReply>()?.let { quote ->
        PixivHelperListener.images.entries.find { (source, _) -> source.toString() == quote.source.toString() }
    }) { "找不到图片" }.value

    private fun CommandSenderOnMessage<*>.findTwitterImage(url: String) = launch {
        ImageSearcher.getTwitterImage(url = url).maxByOrNull { it.similarity }?.let {
            quoteReply(buildMessageChain {
                appendLine("推特图源")
                appendLine("相似度: ${it.similarity}")
                appendLine("Tweet: ${it.tweet}")
                appendLine("原图: ${it.image}")
            })
        }
    }

    @Handler
    suspend fun CommandSenderOnMessage<*>.search(image: Image = fromEvent.message.getQuoteImage()) = withHelper {
        logger.info { "搜索 ${image.queryUrl()}" }
        useMappers {
            it.statistic.findSearchResult(image.md5.hex())
        } ?: ImageSearcher.getSearchResults(url = image.queryUrl()).run {
            requireNotNull(maxByOrNull { it.similarity }) {
                findTwitterImage(url = image.queryUrl())
                "没有PIXIV搜索结果"
            }
        }.also { result ->
            if (result.similarity > MIN_SIMILARITY) {
                useMappers { it.statistic.replaceSearchResult(result.copy(md5 = image.md5.hex())) }
            } else {
                findTwitterImage(url = image.queryUrl())
            }
        }.getContent()
    }
}