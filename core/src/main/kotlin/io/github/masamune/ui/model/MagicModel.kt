package io.github.masamune.ui.model

import io.github.masamune.tiledmap.ActionType

data class MagicModel(
    val type: ActionType,
    val name: String,
    val targetDescriptor: String,
    val mana: Int,
)
