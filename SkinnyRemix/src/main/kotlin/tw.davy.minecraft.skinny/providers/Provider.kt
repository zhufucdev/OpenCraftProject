package tw.davy.minecraft.skinny.providers

import tw.davy.minecraft.skinny.SignedSkin

/**
 * @author Davy
 */
interface Provider {
    fun getSkinData(name: String): SignedSkin?
}
