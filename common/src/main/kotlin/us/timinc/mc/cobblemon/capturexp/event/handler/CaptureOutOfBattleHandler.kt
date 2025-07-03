package us.timinc.mc.cobblemon.capturexp.event.handler

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.pokemon.OriginalTrainerType
import com.cobblemon.mod.common.pokemon.evolution.requirements.LevelRequirement
import us.timinc.mc.cobblemon.capturexp.CaptureXp.config
import us.timinc.mc.cobblemon.capturexp.CaptureXp.debugger
import us.timinc.mc.cobblemon.capturexp.CaptureXp.modId
import us.timinc.mc.cobblemon.timcore.getIdentifier
import kotlin.math.pow
import kotlin.math.roundToInt

object CaptureOutOfBattleHandler {
    fun handle(event: PokemonCapturedEvent) {
        val caseDebugger = debugger.getCaseDebugger()

        val opponentPokemon = event.pokemon
        val playerParty = Cobblemon.storage.getParty(event.player)
        val source = SidemodExperienceSource(modId)
        val first = playerParty.firstOrNull { it != event.pokemon && it.currentHealth > 0 } ?: return

        caseDebugger.debug("${event.player.uuid} captured ${opponentPokemon.getIdentifier()} out of battle.")

        val playerMons = playerParty.filter {
            it != event.pokemon && it.currentHealth > 0 && (config.outOfBattleExpAll || it.uuid == first.uuid || it.heldItem()
                .`is`(CobblemonItemTags.EXPERIENCE_SHARE))
        }
        playerMons.forEach { playerMon ->
            val baseXp = opponentPokemon.form.baseExperienceYield
            val opponentLevel = opponentPokemon.level
            val term1 = (baseXp * opponentLevel) / 5.0

            val xpShareOnly = playerMon.uuid != first.uuid
            val xpShareModifier = Cobblemon.config.experienceShareMultiplier
            val captureModifier = config.outOfBattleExpMultiplier
            val term2 = (if (xpShareOnly) xpShareModifier else 1.0)

            val playerMonLevel = playerMon.level
            val term3 = (((2.0 * opponentLevel) + 10) / (opponentLevel + playerMonLevel + 10)).pow(2.5)

            val term4 = term1 * term2 * term3 + 1

            val isNonOt =
                playerMon.originalTrainerType == OriginalTrainerType.PLAYER && playerMon.originalTrainer != event.player.uuid.toString()
            val nonOtBonus = if (isNonOt) 1.5 else 1.0
            val hasLuckyEgg = playerMon.heldItem().`is`(CobblemonItemTags.LUCKY_EGG)
            val luckyEggBonus = if (hasLuckyEgg) Cobblemon.config.luckyEggMultiplier else 1.0
            val isAffectionate = playerMon.friendship >= 220
            val affectionateBonus = if (isAffectionate) 1.2 else 1.0
            val isCloseToEvolution = playerMon.evolutionProxy.server().any { evolution ->
                val requirements = evolution.requirements.asSequence()
                requirements.any { it is LevelRequirement } && requirements.all { it.check(playerMon) }
            }
            val closeToEvolutionBonus = if (isCloseToEvolution) 1.2 else 1.0

            val cobblemonModifier = Cobblemon.config.experienceMultiplier

            val experience =
                (term4 * nonOtBonus * luckyEggBonus * closeToEvolutionBonus * affectionateBonus * cobblemonModifier * captureModifier).roundToInt()

            caseDebugger.debug("Granting ${playerMon.getIdentifier()} $experience experience.")

            playerMon.addExperienceWithPlayer(event.player, source, experience)

            if (config.outOfBattleGrantEvs) {
                val grantedEvs = opponentPokemon.form.evYield

                caseDebugger.debug("Granting ${playerMon.getIdentifier()} $grantedEvs EVs.")

                grantedEvs.forEach(playerMon.evs::add)
            }
        }
    }
}