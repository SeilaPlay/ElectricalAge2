package org.eln2.mc.common

import com.charleskorn.kaml.Yaml
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.entity.player.Player
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.server.ServerLifecycleHooks
import org.eln2.mc.Eln2
import org.eln2.mc.Eln2.LOGGER
import org.eln2.mc.common.cells.foundation.CellGraphManager
import org.eln2.mc.utility.AnalyticsAcknowledgementsData
import org.eln2.mc.utility.AveragingList
import java.io.IOException

@Mod.EventBusSubscriber
object CommonEvents {
    private const val THIRTY_DAYS_AS_MILLISECONDS: Long = 2_592_000_000L
    private val upsAveragingList = AveragingList(100)
    private val tickTimeAveragingList = AveragingList(100)
    private var logCountdown = 0
    private const val logInterval = 100

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase == TickEvent.Phase.END) {
            var tickRate = 0.0
            var tickTime = 0.0

            ServerLifecycleHooks.getCurrentServer().allLevels.forEach {
                val graph = CellGraphManager.getFor(it)

                tickRate += graph.sampleTickRate()
                tickTime += graph.totalSpentTime
            }

            upsAveragingList.addSample(tickRate)
            tickTimeAveragingList.addSample(tickTime)

            if (logCountdown-- == 0) {
                logCountdown = logInterval

                LOGGER.info("Total simulation rate: ${upsAveragingList.calculate()} Updates/Second")
                LOGGER.info("Total simulation time: ${tickTimeAveragingList.calculate()}")
            }
        }
    }

    @SubscribeEvent
    fun onEntityJoinedWorld(event: EntityJoinWorldEvent) {
        //Warn new users that we collect analytics
        if (event.world.isClientSide && event.entity is Player) {
            if (Eln2.config.enableAnalytics) {
                val acknowledgementFile =
                    FMLPaths.CONFIGDIR.get().resolve("ElectricalAge/acknowledgements.yml").toFile()
                if (!acknowledgementFile.exists()) {
                    try {
                        acknowledgementFile.parentFile.mkdirs()
                        acknowledgementFile.createNewFile()
                        acknowledgementFile.writeText(
                            Yaml.default.encodeToString(
                                AnalyticsAcknowledgementsData.serializer(),
                                AnalyticsAcknowledgementsData(mutableMapOf())
                            )
                        )
                    } catch (ex: Exception) {
                        when (ex) {
                            is IOException, is SecurityException -> {
                                LOGGER.warn("Unable to write default analytics acknowledgements config")
                                LOGGER.warn(ex.localizedMessage)
                            }

                            else -> throw ex
                        }
                    }
                }
                val uuid: String = event.entity.uuid.toString()
                val acknowledgements: AnalyticsAcknowledgementsData = try {
                    Yaml.default.decodeFromStream(
                        AnalyticsAcknowledgementsData.serializer(),
                        acknowledgementFile.inputStream()
                    )
                } catch (ex: IOException) {
                    AnalyticsAcknowledgementsData(mutableMapOf())
                }
                if (acknowledgements.entries.none { (k, _) -> k == uuid } || acknowledgements.entries[uuid]!! < (System.currentTimeMillis()) - THIRTY_DAYS_AS_MILLISECONDS) {
                    event.entity.sendMessage(
                        TranslatableComponent("misc.eln2.acknowledge_analytics").withStyle(
                            ChatFormatting.RED
                        ), Util.NIL_UUID
                    )
                    acknowledgements.entries[uuid] = System.currentTimeMillis()
                }

                try {
                    acknowledgementFile.writeText(
                        Yaml.default.encodeToString(
                            AnalyticsAcknowledgementsData.serializer(),
                            acknowledgements
                        )
                    )
                } catch (ex: Exception) {
                    when (ex) {
                        is IOException, is SecurityException -> {
                            LOGGER.warn("Unable to save analytics acknowledgements")
                            LOGGER.warn(ex.localizedMessage)

                            event.entity.sendMessage(
                                TranslatableComponent("misc.eln2.analytics_save_failure").withStyle(
                                    ChatFormatting.ITALIC
                                ), Util.NIL_UUID
                            )
                        }

                        else -> throw ex
                    }
                }
            }
        }
    }
}
