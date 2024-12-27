package io.github.masamune.combat.action

import io.github.masamune.asset.SoundAsset
import io.github.masamune.combat.ActionExecutorService
import io.github.masamune.tiledmap.ActionType

class ScrollInfernoAction : Action(ActionType.SCROLL_INFERNO, ActionTargetType.ALL) {

    override fun ActionExecutorService.onUpdate(): Boolean {
        addSfx(allTargets, "fire3", 1f, 2f)
        dealMagicDamage(source.stats.damage, allTargets)
        play(SoundAsset.EXPLOSION1, 1f)
        return true
    }

}
