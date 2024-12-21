package io.github.masamune.tiledmap

// This is an autogenerated class by gradle's 'genTiledEnumsAndExtensions' task. Do not touch it!
enum class AnimationType {
    DEAD,
    IDLE,
    ITEM,
    SPECIAL,
    UNDEFINED,
    WALK;

    val atlasKey: String = this.name.lowercase()
}
