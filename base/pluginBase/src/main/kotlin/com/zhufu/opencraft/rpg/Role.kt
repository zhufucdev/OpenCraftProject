package com.zhufu.opencraft.rpg

enum class Role {
    FIGHTER, MAGICIAN, RANGER, PRIEST, SUMMONER, NO_ROLE, NULL;

    val nameCode get() = if (this != NULL) "rpg.role.${name.toLowerCase()}.name" else error("Null role.")
    val description
        get() = if (this != NULL) "rpg.role.${name.toLowerCase()}.subtitle"
        else error("Null role.")
}