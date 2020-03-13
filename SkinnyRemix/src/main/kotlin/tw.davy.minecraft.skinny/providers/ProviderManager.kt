package tw.davy.minecraft.skinny.providers

import java.util.ArrayList
import java.util.logging.Logger

import tw.davy.minecraft.skinny.SignedSkin
import tw.davy.minecraft.skinny.Skinny

/**
 * @author Davy
 */
class ProviderManager(enableStorages: List<String>) {
    private val mProviders = ArrayList<Provider>()

    private val logger: Logger
        get() = Skinny.instance.logger

    init {
        enableStorages.forEach { storageName ->
            try {
                val klass = Class.forName(storageName) as Class<out Provider>
                mProviders.add(klass.newInstance())
            } catch (ignored: ClassNotFoundException) {
                logger.warning("Failed to load provider: $storageName")
            } catch (ignored: ClassCastException) {
                logger.warning("Failed to load provider: $storageName")
            } catch (ignored: IllegalAccessException) {
                logger.warning("Failed to initialize provider: $storageName")
            } catch (ignored: InstantiationException) {
                logger.warning("Failed to initialize provider: $storageName")
            }
        }

        if (mProviders.isEmpty())
            logger.warning("No skin providers loaded, this plugin may not works.")
        else {
            logger.info("Loaded providers:")
            mProviders.forEach { provider -> logger.info("* " + provider.javaClass.name) }
        }
    }

    fun getSkin(name: String): SignedSkin? {
        for (provider in mProviders) {
            try {
                val skin = provider.getSkinData(name)
                if (skin != null)
                    return skin
            } catch (ignored: Exception) {

            }
        }

        return null
    }
}
