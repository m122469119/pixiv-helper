package xyz.cssxsh.mirai.plugin.command

import net.mamoe.mirai.console.command.*
import xyz.cssxsh.mirai.plugin.*

object PixivArticleCommand : SimpleCommand(
    owner = PixivHelperPlugin,
    "article", "特辑",
    description = "PIXIV特辑指令"
) {

    override val prefixOptional: Boolean = true

    @Handler
    suspend fun CommandSenderOnMessage<*>.load() = withHelper {
        randomArticles().let { data ->
            data.articles.forEach {
                addCacheJob(name = "ARTICLE[${it.aid}]", reply = false) { getArticle(article = it).eros() }
            }
            buildMessageByArticle(data = data)
        }
    }
}