package xyz.cssxsh.mirai.plugin.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.utils.minutesToMillis
import xyz.cssxsh.mirai.plugin.*
import xyz.cssxsh.mirai.plugin.data.PixivCacheData
import xyz.cssxsh.mirai.plugin.data.PixivHelperSettings.totalBookmarks
import xyz.cssxsh.mirai.plugin.data.PixivHelperSettings.delayTime
import xyz.cssxsh.pixiv.api.app.*

@Suppress("unused")
object PixivFollowCommand : CompositeCommand(
    PixivHelperPlugin,
    "follow",
    description = "关注指令",
    prefixOptional = true
), PixivHelperLogger {

    private suspend fun PixivHelper.getFollowed(uid: Long, maxNum: Long = 10_000): Set<Long> = buildList {
        (0L until maxNum step AppApi.PAGE_SIZE).forEach { offset ->
            runCatching {
                userFollowing(uid = uid, offset = offset).userPreviews.map { it.user.id }
            }.onSuccess {
                if (it.isEmpty()) return@buildList
                add(it)
                logger.verbose("加载关注用户作品预览第${offset / 30}页{${it.size}}成功")
            }.onFailure {
                logger.warning("加载关注用户作品预览第${offset / 30}页失败", it)
            }
        }
    }.flatten().toSet()

    /**
     * 关注色图作者
     */
    @SubCommand
    suspend fun CommandSenderOnMessage<MessageEvent>.ero() = getHelper().runCatching {
        check(followJob?.isActive != true) { "正在关注中, ${cacheJob}..." }
        launch(Dispatchers.IO) {
            val followed = getFollowed(uid = getAuthInfo().user.uid)
            PixivCacheData.caches().values.fold(mutableMapOf<Long, Pair<Int, Long>>()) { map, info ->
                map.apply {
                    compute(info.uid) { _, value ->
                        (value ?: (0 to 0L)).let { (num, total) ->
                            num + 1 to total + info.totalBookmarks
                        }
                    }
                }
            }.mapNotNull { (uid, count) ->
                uid.takeIf {
                    count.let { (num, total) -> total > totalBookmarks * num }
                }
            }.toSet().let {
                logger.verbose("共统计了${it.size}名画师")
                it - followed
            }.sorted().also {
                logger.info("用户(${getAuthInfo().user.uid})已关注${followed.size}, 共有${it.size}个用户等待关注")
            }.runCatching {
                quoteReply("共${size}个画师等待关注")
                size to count { uid ->
                    isActive && runCatching {
                        userFollowAdd(uid)
                    }.onSuccess {
                        logger.info("用户(${getAuthInfo().user.uid})添加关注(${uid})成功, $it")
                        delay(delayTime)
                    }.onFailure {
                        logger.warning("用户(${getAuthInfo().user.uid})添加关注(${uid})失败", it)
                        delay(1.minutesToMillis)
                    }.isSuccess
                }
            }.onSuccess { (total, success) ->
                quoteReply("关注完毕共${total}个画师, 关注成功${success}个")
            }.onFailure {
                quoteReply("关注失败, ${it.message}")
            }
        }.also {
            followJob = it
        }
    }.onSuccess { job ->
        quoteReply("关注任务添加完成${job}")
    }.onFailure {
        quoteReply("关注添加失败， ${it.message}")
    }.isSuccess
}