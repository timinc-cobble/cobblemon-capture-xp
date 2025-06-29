package us.timinc.mc.cobblemon.capturexp.event

import com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_CAPTURED
import com.cobblemon.mod.common.api.reactive.Observable.Companion.filter
import com.cobblemon.mod.common.util.isInBattle

object CaptureXpEvents {
    @JvmField
    val POKEMON_CAPTURED_IN_BATTLE = POKEMON_CAPTURED.pipe(
        filter { it.player.isInBattle() }
    )

    @JvmField
    val POKEMON_CAPTURE_OUT_OF_BATTLE = POKEMON_CAPTURED.pipe(
        filter { !it.player.isInBattle() }
    )
}