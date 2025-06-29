package us.timinc.mc.cobblemon.capturexp

import com.cobblemon.mod.common.api.Priority
import us.timinc.mc.cobblemon.capturexp.event.CaptureXpEvents
import us.timinc.mc.cobblemon.capturexp.event.handler.CaptureInBattleHandler
import us.timinc.mc.cobblemon.capturexp.event.handler.CaptureOutOfBattleHandler
import us.timinc.mc.cobblemon.timcore.AbstractConfig
import us.timinc.mc.cobblemon.timcore.AbstractMod

const val MOD_ID: String = "capture_xp"

object CaptureXp : AbstractMod<CaptureXp.CaptureXpConfig>(MOD_ID, CaptureXpConfig::class.java) {

    class CaptureXpConfig : AbstractConfig() {
        val inBattleExpAll: Boolean = false
        val inBattleExpMultiplier = 1.0
        val inBattleGrantEvs = false

        val outOfBattleExpAll: Boolean = false
        val outOfBattleExpMultiplier = 1.0
        val outOfBattleGrantEvs = false
    }

    init {
        CaptureXpEvents.POKEMON_CAPTURED_IN_BATTLE.subscribe(Priority.LOWEST, CaptureInBattleHandler::handle)
        CaptureXpEvents.POKEMON_CAPTURE_OUT_OF_BATTLE.subscribe(Priority.LOWEST, CaptureOutOfBattleHandler::handle)
    }
}