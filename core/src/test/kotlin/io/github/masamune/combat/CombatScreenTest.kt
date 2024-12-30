package io.github.masamune.combat

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.collection.MutableEntityBag
import com.github.quillraven.fleks.collection.mutableEntityBagOf
import io.github.masamune.Masamune
import io.github.masamune.asset.TiledMapAsset
import io.github.masamune.combat.buff.NullifyBuff
import io.github.masamune.component.Combat
import io.github.masamune.component.Enemy
import io.github.masamune.component.Equipment
import io.github.masamune.component.Inventory
import io.github.masamune.component.Player
import io.github.masamune.event.PlayerInteractCombatBeginEvent
import io.github.masamune.gdxTest
import io.github.masamune.screen.CombatScreen
import io.github.masamune.screen.GameScreen
import io.github.masamune.screen.LoadingScreen
import io.github.masamune.tiledmap.ItemType
import io.github.masamune.tiledmap.TiledObjectType
import io.mockk.every
import io.mockk.mockkObject
import ktx.app.KtxGame
import ktx.app.KtxScreen

/**
 * This test simulates a real combat like it is in the real game.
 * It loads assets similar to the LoadingScreen, loads the GameScreen and triggers
 * the combat start event.
 * Combat entities are taken from the combatTest.tmx map and its embedded tileset.
 *
 * Special tests:
 * - Press '1' to add an attack nullify buff for one turn to all enemies
 */

fun main() {
    // we misuse the VILLAGE constant for this test to avoid adding the test maps to our
    // TiledMapAsset enum. By mocking the VILLAGE's path value we can transition to an arbitrary map.
    mockkObject(TiledMapAsset.VILLAGE)
    every { TiledMapAsset.VILLAGE.path } returns "maps/combatTest.tmx"
    gdxTest("Combat Test", CombatScreenTest())
}

private class CombatScreenTest : KtxGame<KtxScreen>() {
    private val masamune = Masamune()

    override fun create() {
        // load game assets
        masamune.create()
        masamune.asset.finishLoading()
        masamune.shader.loadAllShader()
        masamune.getScreen<LoadingScreen>().dispose()
        masamune.removeScreen<LoadingScreen>()

        // add game and combat screen
        masamune.addScreen(GameScreen(masamune))
        masamune.addScreen(CombatScreen(masamune))
        masamune.setScreen<GameScreen>()
        val gameScreen = masamune.getScreen<GameScreen>()
        gameScreen.setMap(TiledMapAsset.VILLAGE)

        // trigger combat event like it is done in real game
        val player = spawnPlayer(gameScreen.world)
        val enemy = spawnEnemy(gameScreen.world)
        masamune.event.fire(PlayerInteractCombatBeginEvent)
        val combatScreen = masamune.getScreen<CombatScreen>()
        masamune.setScreen<CombatScreen>()
        with(gameScreen.world) {
            combatScreen.spawnEnemies(enemy, enemy[Enemy].combatEntities)
            combatScreen.spawnPlayer(gameScreen.world, player)
        }
    }

    private fun spawnPlayer(world: World): Entity {
        val player = masamune.tiled.loadEnemy(world, TiledObjectType.HERO, 0f, 0f)
        with(world) {
            val items = spawnPlayerItems(world)
            val equipment = spawnPlayerEquipment(world)
            player.configure {
                it += Player(gameProgress = 0)
                it += Inventory(items = items)
                it += Equipment(items = equipment)
            }
        }
        return player
    }

    private fun spawnPlayerItems(world: World): MutableEntityBag {
        return mutableEntityBagOf(
            masamune.tiled.loadItem(world, ItemType.SCROLL_INFERNO, 3),
            masamune.tiled.loadItem(world, ItemType.SMALL_MANA_POTION, 1),
        )
    }

    // elder sword gives bonus damage and heal action
    private fun spawnPlayerEquipment(world: World): MutableEntityBag {
        return mutableEntityBagOf(
            masamune.tiled.loadItem(world, ItemType.ELDER_SWORD, 1),
        )
    }

    private fun spawnEnemy(world: World): Entity = world.entity {
        it += Enemy(combatEntities = mapOf(TiledObjectType.BUTTERFLY to 1, TiledObjectType.MUSHROOM to 1))
    }

    override fun resize(width: Int, height: Int) {
        masamune.resize(width, height)
    }

    override fun render() {
        masamune.render()

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            val combatWorld = masamune.getScreen<CombatScreen>().world
            combatWorld.family { all(Combat).none(Player) }.forEach { enemy ->
                combatWorld.inject<ActionExecutorService>().addBuff(NullifyBuff(enemy, 1))
            }
        }
    }

    override fun dispose() {
        masamune.dispose()
    }
}
