package us.timinc.mc.cobblemon.capturexp.handler

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.pokemon.stats.SidemodEvSource
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import us.timinc.mc.cobblemon.capturexp.CaptureXp
import us.timinc.mc.cobblemon.capturexp.CaptureXp.config
import us.timinc.mc.cobblemon.capturexp.CaptureXp.debugger
import us.timinc.mc.cobblemon.timcore.getIdentifier
import us.timinc.mc.cobblemon.timcore.hasExpAllFor

object CaptureInBattleHandler {
    fun handle(event: PokemonCapturedEvent) {
        val caseDebugger = debugger.getCaseDebugger()

        val battle = Cobblemon.battleRegistry.getBattleByParticipatingPlayer(event.player) ?: return
        val caughtBattleMon = battle.actors.flatMap { it.pokemonList }.find { it.uuid == event.pokemon.uuid } ?: return
        val caughtBattleMonActor = caughtBattleMon.actor

        caseDebugger.debug("Battle ${battle.battleId} resulted in wild Pokémon ${caughtBattleMon.effectedPokemon.getIdentifier()} being captured.")

        caughtBattleMonActor.getSide().getOppositeSide().actors.forEach { opponentActor ->
            opponentActor.pokemonList.filter {
                (it.health > 0 || config.inBattleAwardExperienceToFaintedPokemon)
                        && ((config.inBattleExpAll && it.effectedPokemon.getOwnerPlayer()
                    ?.hasExpAllFor(it.effectedPokemon) == true)
                        || caughtBattleMon.facedOpponents.contains(it)
                        || it.effectedPokemon.heldItem()
                    .`is`(CobblemonItemTags.EXPERIENCE_SHARE))
            }.forEach { opponentMon ->
                val xpShareOnly = !caughtBattleMon.facedOpponents.contains(opponentMon)
                val xpShareOnlyModifier =
                    (if (xpShareOnly) Cobblemon.config.experienceShareMultiplier else 1).toDouble()
                val experience = Cobblemon.experienceCalculator.calculate(
                    opponentMon, caughtBattleMon, config.inBattleExpMultiplier * xpShareOnlyModifier
                )
                if (experience > 0) {
                    caseDebugger.debug("Granting ${opponentMon.effectedPokemon.getIdentifier()} $experience experience.")

                    opponentActor.awardExperience(opponentMon, experience)
                }

                if (config.inBattleGrantEvs) {
                    val grantedEvs = Cobblemon.evYieldCalculator.calculate(opponentMon, caughtBattleMon)

                    caseDebugger.debug("Granting ${opponentMon.effectedPokemon.getIdentifier()} $grantedEvs EVs.")

                    grantedEvs.forEach { (k, v) ->
                        opponentMon.effectedPokemon.evs.add(
                            k,
                            v,
                            SidemodEvSource(CaptureXp.modId, opponentMon.effectedPokemon)
                        )
                    }
                }
            }
        }
    }
}