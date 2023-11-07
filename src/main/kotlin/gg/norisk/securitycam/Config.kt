package gg.norisk.securitycam

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.silkmc.silk.commands.clientCommand
import net.silkmc.silk.commands.player
import net.silkmc.silk.core.logging.logger
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import java.io.File

object Config {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val folder = File("security-cam").apply { mkdirs() }
    var isEnabled = false

    lateinit var data: ConfigData

    @Serializable
    data class ConfigData(var range: Double = 20.0, val whitelistedUser: MutableList<String> = mutableListOf())

    fun init() {
        runCatching {
            val folder = File("security-cam").apply { mkdirs() }
            val configFile = File(folder, "config.json")
            if (configFile.exists()) {
                data = json.decodeFromString(configFile.readText())
            } else {
                data = ConfigData()
                saveConfig().onSuccess {
                    logger().error("Saved Farm Security Config")
                }.onFailure {
                    logger().error("Error saving Farm Security Config")
                }
            }
            saveConfig()
        }.onFailure {
            logger().error(it.message)
        }

        configCommands()
    }

    fun configCommands() {
        clientCommand("securitycam") {
            runs {
                isEnabled = !isEnabled
                this.source.player.sendMessage("Security wurde ${if (isEnabled) "angeschaltet" else "ausgeschaltet"}".literal)
            }
            literal("radius") {
                runs {
                    this.source.player.sendMessage("Der Aktuelle Radius beträgt: ${data.range}".literal)
                }
                argument<Double>("radius", DoubleArgumentType.doubleArg(5.0)) { radius ->
                    runs {
                        data.range = radius()
                        saveConfig().onSuccess {
                            this.source.player.sendMessage("Der Radius wurde auf ${data.range} gesetzt".literal)
                        }.onFailure {
                            this.source.player.sendMessage("Fehler beim speichern ${it.message}".literal)
                        }
                    }
                }
            }
            literal("whitelist") {
                runs {
                    this.source.player.sendMessage(literalText {
                        text("Die aktuelle Whitelist besteht aus: ") { }
                        for (user in data.whitelistedUser) {
                            newLine()
                            text(user) { }
                        }
                    })
                }
                literal("remove") {
                    argument<String>("name", StringArgumentType.word()) { name ->
                        runs {
                            data.whitelistedUser.remove(name())
                            saveConfig().onSuccess {
                                this.source.player.sendMessage("$name wurde von der Whitelist entfernt.".literal)
                            }.onFailure {
                                this.source.player.sendMessage("Fehler beim speichern ${it.message}".literal)
                            }
                        }
                    }
                }
                literal("add") {
                    argument<String>("name", StringArgumentType.word()) { name ->
                        runs {
                            data.whitelistedUser.add(name())
                            saveConfig().onSuccess {
                                this.source.player.sendMessage("${name()} wurde gewhitelisted.".literal)
                            }.onFailure {
                                this.source.player.sendMessage("Fehler beim speichern ${it.message}".literal)
                            }
                        }
                    }
                }
            }
        }
    }

    fun saveConfig(): Result<Unit> {
        return runCatching {
            File(folder, "config.json").writeText(json.encodeToString(data))
        }
    }
}
