package com.zhufu.opencraft.special_block

import org.bukkit.Location
import java.util.*

/**
 * A special block without NBT access
 */
abstract class StatelessSpecialBlock(location: Location, id: UUID) : SpecialBlock(location, id)