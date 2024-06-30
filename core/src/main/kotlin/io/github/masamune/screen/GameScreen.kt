package io.github.masamune.screen

import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.fleks.configureWorld
import io.github.masamune.Masamune
import io.github.masamune.PhysicContactHandler
import io.github.masamune.asset.*
import io.github.masamune.dialog.DialogConfigurator
import io.github.masamune.event.EventService
import io.github.masamune.input.KeyboardController
import io.github.masamune.system.*
import io.github.masamune.tiledmap.TiledService
import io.github.masamune.ui.model.DialogViewModel
import io.github.masamune.ui.view.DialogView
import io.github.masamune.ui.view.dialogView
import ktx.app.KtxScreen
import ktx.box2d.createWorld
import ktx.log.logger
import ktx.scene2d.actors

class GameScreen(
    private val masamune: Masamune,
    private val batch: Batch = masamune.batch,
    private val inputProcessor: InputMultiplexer = masamune.inputProcessor,
    private val eventService: EventService = masamune.event,
    private val tiledService: TiledService = masamune.tiled,
    private val shaderService: ShaderService = masamune.shader,
    assetService: AssetService = masamune.asset,
) : KtxScreen {
    // viewports and stage
    private val gameViewport: Viewport = ExtendViewport(16f, 9f)
    private val uiViewport = FitViewport(928f, 522f)
    private val stage = Stage(uiViewport, batch)
    private val skin = assetService[SkinAsset.DEFAULT]

    // views and view models
    private val dialogViewModel = DialogViewModel(eventService)
    private lateinit var dialogView: DialogView

    // physic world
    private val physicWorld = createWorld(Vector2.Zero, true).apply {
        autoClearForces = false
    }

    // other stuff
    private val keyboardController = KeyboardController(eventService)
    private val dialogConfigurator = DialogConfigurator(assetService[I18NAsset.MESSAGES])

    // ecs world
    private val world = gameWorld()

    private fun gameWorld() = configureWorld {
        injectables {
            add(batch)
            add(gameViewport)
            add(physicWorld)
            add(shaderService)
            add(eventService)
            add(dialogConfigurator)
        }

        systems {
            add(MoveSystem())
            add(PhysicSystem())
            add(PlayerInteractSystem())
            add(CameraSystem())
            add(AnimationSystem())
            add(RenderSystem())
        }
    }

    override fun show() {
        // set controller
        inputProcessor.clear()
        inputProcessor.addProcessor(keyboardController)

        // set physic contact handler (needs to be done AFTER ECS world is created)
        physicWorld.setContactListener(PhysicContactHandler(eventService, world))

        // setup UI views
        stage.clear()
        stage.actors {
            dialogView = dialogView(dialogViewModel, skin) {
                isVisible = false
            }
        }

        // register all event listeners
        registerEventListeners()

        // load map AFTER event listeners are registered
        tiledService.setMap(TiledMapAsset.VILLAGE, world)
    }

    private fun registerEventListeners() {
        eventService += world
        eventService += dialogView
        eventService += dialogViewModel
        eventService += keyboardController
    }

    override fun hide() {
        eventService.clearListeners()
        physicWorld.setContactListener(null)
    }

    override fun resize(width: Int, height: Int) {
        gameViewport.update(width, height, true)
        uiViewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        world.update(delta)

        uiViewport.apply()
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        log.debug { "Disposing world with '${world.numEntities}' entities" }
        world.dispose()
        physicWorld.dispose()
        stage.dispose()
    }

    companion object {
        private val log = logger<GameScreen>()
    }
}
