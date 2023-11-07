package gg.norisk.securitycam

import net.fabricmc.api.ModInitializer

class SecurityCamMod : ModInitializer {
    override fun onInitialize() {
        Config.init()
        PlayerDetector.init()
    }
}
