package io.github.masamune.combat.state

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.collection.EntityBag
import io.github.masamune.combat.ActionExecutorService
import io.github.masamune.combat.action.Action
import io.github.masamune.component.Combat
import io.github.masamune.component.Player
import io.github.masamune.component.Stats
import io.github.masamune.event.CombatNextTurnEvent
import io.github.masamune.event.CombatTurnEndEvent
import io.github.masamune.event.EventService
import io.github.masamune.isEntityDead
import ktx.log.logger

class CombatStatePerformAction(
    private val world: World,
    private val eventService: EventService = world.inject(),
    private val actionExecutorService: ActionExecutorService = world.inject(),
) : CombatState {
    private val combatEntities = world.family { all(Combat) }
    private val actionStack = ArrayDeque<Triple<Entity, Action, EntityBag>>()
    private var turnEnd = false

    override fun onEnter() {
        log.debug { debugCombatEntities() }
        turnEnd = false
        actionStack.clear()
        combatEntities.forEach { entity ->
            if (world.isEntityDead(entity)) {
                return@forEach
            }

            val (_, action, targets) = entity[Combat]
            actionStack += Triple(entity, action, targets)
        }
        log.debug { "Performing ${actionStack.size} actions" }
        performNext()
    }

    private fun performNext() {
        if (actionStack.isEmpty()) {
            return
        }

        var (nextEntity, nextAction, nextTargets) = actionStack.first()
        while (world.isEntityDead(nextEntity)) {
            // entity died already -> remove its action from the stack and find
            // next executable action
            actionStack.removeFirst()
            if (actionStack.isEmpty()) {
                return
            }

            val topAction = actionStack.first()
            nextEntity = topAction.first
            nextAction = topAction.second
            nextTargets = topAction.third
        }
        actionExecutorService.perform(nextEntity, nextAction, nextTargets)
    }

    private fun debugCombatEntities() = buildString {
        append("Combat Stats debug:")
        appendLine()
        combatEntities.forEach { entity ->
            append("Entity ${entity.id} (")
            append(if (entity has Player) "Player" else "Enemy")
            append("): ")
            val stats = entity[Stats]
            val buffs = entity[Combat].buffs.joinToString { "${it::class.simpleName}" }
            append("life=${stats.life}, mana=${stats.mana}, agi=${stats.agility}, buffs=$buffs")
            appendLine()
        }
    }

    override fun onUpdate(deltaTime: Float) = with(world) {
        if (actionStack.isEmpty()) {
            if (!turnEnd) {
                // all actions of turn performed -> fire turn end event which might add some actions on the stack again
                log.debug { "Combat turn end" }
                turnEnd = true
                eventService.fire(CombatTurnEndEvent)
            } else {
                log.debug { "Combat trigger next turn" }
                val player = combatEntities.single { it has Player }
                val aliveEnemies = combatEntities.filter { it hasNo Player && it[Stats].life > 0 }
                eventService.fire(CombatNextTurnEvent(player, aliveEnemies))
            }
            return
        }

        actionExecutorService.update()
        if (actionExecutorService.isFinished) {
            actionStack.removeFirst()
            actionExecutorService.clearAction()
            performNext()
        }
    }

    override fun onExit() {
        log.debug { debugCombatEntities() }
    }

    companion object {
        private val log = logger<CombatStatePerformAction>()
    }
}
