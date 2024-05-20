package io.github.masamune.tiledmap

import com.badlogic.gdx.maps.tiled.TiledMapTile
import ktx.tiled.property

// This is an autogenerated file by gradle's 'genTiledEnums' task. Do not touch it!

val TiledMapTile.atlas: String
    get() = this.property<String>("atlas", "UNDEFINED")

val TiledMapTile.atlasRegionKey: String
    get() = this.property<String>("atlasRegionKey")

val TiledMapTile.color: String
    get() = this.property<String>("color", "#ffffffff")

val TiledMapTile.hasAnimation: Boolean
    get() = this.property<Boolean>("hasAnimation", false)

val TiledMapTile.objType: String
    get() = this.property<String>("objType", "UNDEFINED")


