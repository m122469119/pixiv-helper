package xyz.cssxsh.mirai.pixiv.data

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(SendModel.Companion::class)
public sealed class SendModel {
    override fun toString(): String = this::class.simpleName!!

    public object Normal : SendModel()
    public object Flash : SendModel()
    public data class Recall(val ms: Long) : SendModel()
    public object Forward : SendModel()

    @Serializable
    public data class Info(
        val type: String,
        val ms: Long = 60_000L
    )

    public companion object : KSerializer<SendModel> {
        @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            buildSerialDescriptor(SendModel::class.qualifiedName!!, StructureKind.OBJECT)

        public operator fun invoke(type: String, ms: Long = 60_000L): SendModel {
            return when (type.uppercase()) {
                "NORMAL" -> Normal
                "FLASH" -> Flash
                "RECALL" -> Recall(ms)
                "FORWARD" -> Forward
                else -> throw IllegalArgumentException("不支持的发送类型 $type")
            }
        }

        override fun deserialize(decoder: Decoder): SendModel {
            return decoder.decodeSerializableValue(Info.serializer()).let { info -> invoke(info.type, info.ms) }
        }

        override fun serialize(encoder: Encoder, value: SendModel) {
            encoder.encodeSerializableValue(
                Info.serializer(),
                when (value) {
                    is Normal -> Info("NORMAL")
                    is Flash -> Info("FLASH")
                    is Recall -> Info("RECALL", value.ms)
                    is Forward -> Info("FORWARD")
                }
            )
        }
    }
}