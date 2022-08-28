package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapFont
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import org.bukkit.map.MinecraftFont
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import java.io.File
import java.io.FileNotFoundException
import javax.imageio.ImageIO

class ImageRender(private val file: File) : MapRenderer() {
    private var image: Image = ImageIO.read(file)
    private var isRendered = false

    init {
        if (!file.exists()) throw FileNotFoundException(file.absolutePath)
    }

    override fun render(map: MapView, canvas: MapCanvas, player: Player) {
        if (isRendered) return

        val pg = PixelGrabber(image, 0, 0, -1, -1, false)
        try {
            pg.grabPixels()
        } catch (e: Exception) {
            e.printStackTrace()
            canvas.drawText(0, 56, MinecraftFont(), "${e::class.simpleName}: ${e.message}")
            return
        }
        val height = pg.height
        val width = pg.width
        Bukkit.getLogger().info("Rendering an image with $height in height and $width in width.")
        if (height > 128 || width > 128) {
            Bukkit.getLogger().warning("Scaling the image to 128*128.")
            image = image.getScaledInstance(128, 128, Image.SCALE_SMOOTH)
        }
        canvas.drawImage(0, 0, image)
        isRendered = true
    }

    override fun equals(other: Any?): Boolean {
        return other is ImageRender && other.file == this.file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}