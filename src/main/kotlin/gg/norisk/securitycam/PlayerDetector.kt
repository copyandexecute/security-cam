package gg.norisk.securitycam

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.silkmc.silk.core.text.literalText
import java.text.SimpleDateFormat
import java.util.*

object PlayerDetector {
    private val nearbyEntities = mutableSetOf<UUID>()
    private val messageCooldown = mutableMapOf<UUID, Long>()

    fun init() {
        ClientTickEvents.END_WORLD_TICK.register(ClientTickEvents.EndWorldTick {
            val player = MinecraftClient.getInstance().player ?: return@EndWorldTick
            val entitiesInRange = mutableSetOf<UUID>()
            player.world
                .getOtherEntities(player, Box.from(player.pos).expand(Config.data.range))
                .filterIsInstance<PlayerEntity>()
                .filter { !it.isSpectator }
                .forEach {
                    entitiesInRange.add(it.uuid)
                    if (nearbyEntities.add(it.uuid) && Config.isEnabled) {
                        onPlayerEnter(it)
                    }
                }

            val entitiesToRemove = mutableSetOf<UUID>()
            nearbyEntities.forEach {
                if (!entitiesInRange.contains(it)) {
                    entitiesToRemove.add(it)
                    onPlayerLeave(it)
                }
            }
            nearbyEntities.removeAll(entitiesToRemove)
        })
    }

    private fun onPlayerEnter(player: PlayerEntity) {
        if (Config.data.whitelistedUser.none { it.equals(player.entityName, true) }) {
            val text =
                "ACHTUNG!!! Diese Raid Farm gehört Veni & NoRisk. Unbefugte Nutzung wird bei SparkOfPhoenix gemeldet."
            MinecraftClient.getInstance().networkHandler?.sendChatCommand("msg ${player.entityName} $text")
        }
    }

    private fun onPlayerLeave(uuid: UUID) {
        messageCooldown.remove(uuid)
    }

    fun onOpenInventory(blockState: BlockState, pos: BlockPos) {
        val player = MinecraftClient.getInstance().world?.players?.sortedByDescending {
            it.squaredDistanceTo(
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble()
            )
        }?.reversed()?.firstOrNull()

        if (Config.data.whitelistedUser.any { it.equals(player?.entityName, true) }) {
            return
        }

        if (!nearbyEntities.contains(player?.uuid)) {
            return
        }

        if (System.currentTimeMillis() > (messageCooldown[player?.uuid] ?: 0)) {
            MinecraftClient.getInstance().messageHandler.onGameMessage(literalText {
                text("[") { }
                text(SimpleDateFormat("HH:mm:ss").format(Date())) { }
                text("] ") { }
                text(player?.entityName ?: "Unbekannter Spieler") { }
                if (blockState.isOf(Blocks.CHEST)) {
                    text(" hat eine Kiste geöffnet") { }
                } else if (blockState.isOf(Blocks.HOPPER)) {
                    text(" hat einen Hopper geöffnet") { }
                }
                text(" ")
                text("[${pos.toShortString()}]") { }
            }, false)

            val text = "Diebstahl wird bestraft. Bitte kaufen Sie es über den Shop am Spawn. :) Mit freundlichen GHGrüßen"
            if (player != null) {
                messageCooldown[player.uuid] = System.currentTimeMillis() + 5000L
                MinecraftClient.getInstance().networkHandler?.sendChatCommand("msg ${player.entityName} $text")
            }
        }
    }
}
