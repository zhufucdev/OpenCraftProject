package tw.davy.minecraft.skinny.providers

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

import tw.davy.minecraft.skinny.SignedSkin
import tw.davy.minecraft.skinny.Skinny

/**
 * @author Davy
 */
open class LegacyProvider : Provider {

    protected open val skinFolder: File
        get() = File(Skinny.instance.dataFolder, "skins")

    init {
        createSkinFolder()
    }

    override fun getSkinData(name: String): SignedSkin? {
        if (!getSkinDir(name).exists())
            return null

        val value = getSkinValue(name)
        val signature = getSkinSignature(name)
        return if (value != null && signature != null) SignedSkin(value, signature) else null

    }

    private fun getSkinValue(name: String): String? {
        return readData(File(getSkinDir(name), "value.dat"))
    }

    private fun getSkinSignature(name: String): String? {
        return readData(File(getSkinDir(name), "signature.dat"))
    }

    protected fun getSkinDir(name: String): File {
        return File(skinFolder, name.toLowerCase())
    }

    private fun createSkinFolder() {
        if (!skinFolder.exists())
            skinFolder.mkdir()
    }

    protected fun readData(file: File): String? {
        if (file.exists()) {
            try {
                val buf = BufferedReader(FileReader(file))
                val data = buf.readLine()

                buf.close()
                return data
            } catch (ignored: IOException) {
            }

        }

        return null
    }
}
