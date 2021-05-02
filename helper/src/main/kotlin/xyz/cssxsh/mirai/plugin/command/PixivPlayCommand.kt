package xyz.cssxsh.mirai.plugin.command

import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import xyz.cssxsh.mirai.plugin.*
import xyz.cssxsh.mirai.plugin.tools.*
import xyz.cssxsh.pixiv.AgeLimit
import xyz.cssxsh.pixiv.apps.*
import kotlin.time.seconds

object PixivPlayCommand : CompositeCommand(
    owner = PixivHelperPlugin,
    "play", "播放",
    description = "PIXIV播放指令"
) {

    @ExperimentalCommandDescriptors
    @ConsoleExperimentalApi
    override val prefixOptional: Boolean = true

    private var PixivHelper.play: Job by PixivHelperDelegate { Job().apply { complete() } }

    private var PixivHelper.duration by PixivHelperDelegate { (10).seconds }

    @SubCommand("rank", "排行")
    @Description("根据 Tag 播放特辑")
    suspend fun CommandSenderOnMessage<*>.rank(vararg words: String) = withHelper {
        check(play.isActive) { "其他列表播放着中" }
        val rank = NaviRank.getTagRank(words = words)
        play = launch {
            rank.records.cached().forEach {
                delay(duration)
                if (isActive) sendIllust { it }
            }
        }
        "开始播放NaviRank[${rank.title}]，共${rank.records.size}个作品，间隔 $duration"
    }

    @SubCommand("recommended", "推荐")
    @Description("根据 系统推荐 播放图集")
    suspend fun CommandSenderOnMessage<*>.recommended() = withHelper {
        check(play.isActive) { "其他列表播放着中" }
        val user = getAuthInfo().user
        val illusts = illustRecommended().illusts.filter { it.age == AgeLimit.ALL }
        play = launch {
            illusts.forEach {
                delay(duration)
                if (isActive) sendIllust(flush = true) { it }
            }
        }
        "开始播放用户[${user.name}]系统推荐，共${illusts.size}个作品，间隔 $duration"
    }

    @SubCommand("mark", "收藏")
    @Description("根据 tag 播放收藏")
    suspend fun CommandSenderOnMessage<*>.mark(tag: String? = null) = withHelper {
        check(play.isActive) { "其他列表播放着中" }
        val user = getAuthInfo().user
        val illusts = bookmarksRandom(uid = user.uid, tag = tag).illusts
        play = launch {
            illusts.forEach {
                delay(duration)
                if (isActive) sendIllust(flush = true) { it }
            }
        }
        "开始播放用户[${user.name}](${tag})收藏，共${illusts.size}个作品，间隔 $duration"
    }

    @SubCommand("article", "特辑")
    @Description("根据 AID 播放特辑")
    suspend fun CommandSenderOnMessage<*>.article(aid: Long) = withHelper {
        check(play.isActive) { "其他列表播放着中" }
        val article = Pixivision.getArticle(aid = aid)
        play = launch {
            article.illusts.forEach {
                delay(duration)
                if (isActive) sendIllust(flush = false) {
                    getIllustInfo(pid = it.pid, flush = false)
                }
            }
        }
        "开始播放特辑《${article.title}》，共${article.illusts.size}个作品，间隔 $duration"
    }

    @SubCommand("walkthrough", "random", "漫游", "随机")
    @Description("根据 AID 播放特辑")
    suspend fun CommandSenderOnMessage<*>.walkthrough() = withHelper {
        check(play.isActive) { "其他列表播放着中" }
        val illusts = illustWalkThrough().illusts.filter { it.age == AgeLimit.ALL && it.isEro() }
        play = launch {
            illusts.forEach {
                delay(duration)
                if (isActive) sendIllust(flush = true) { it }
            }
        }
        "开始播放漫游，共${illusts.size}个作品，间隔 $duration"
    }

    @SubCommand("complete", "补全", "自动补全")
    @Description("根据 AID 播放特辑")
    suspend fun CommandSenderOnMessage<*>.complete(vararg works: String) = withHelper {
        check(works.isNotEmpty())
        buildString {
            appendLine("自动补全，共${works.size}个")
            works.forEach { work ->
                appendLine("[$work] => ${searchAutoComplete(word = work).tags.map { it.getContent() }}")
            }
        }
    }

    @SubCommand("stop", "停止")
    @Description("停止播放当前列表")
    suspend fun CommandSenderOnMessage<*>.stop() = withHelper {
        if (play.isActive) {
            play.cancelAndJoin()
            "当前列表已停止播放"
        } else {
            "当前未播放"
        }
    }
}