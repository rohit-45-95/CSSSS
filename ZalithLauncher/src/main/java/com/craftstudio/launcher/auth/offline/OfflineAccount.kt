package com.craftstudio.launcher.auth.offline

import android.content.Context
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.utils.OfflineUuidUtils
import com.craftstudio.launcher.utils.skin.SkinPreferenceStore
import java.io.File

/**
 * Manages the Yggdrasil server for offline account skins.
 * 
 * Lifecycle:
 * 1. initialize() - creates and starts server for current game launch
 * 2. injectArgs() - returns authlib-injector JVM args (if skin != DEFAULT)
 * 3. close() - stops server when game closes
 */
object OfflineAccount {
    private var server: YggdrasilServer? = null
    
    /**
     * Initialize the Yggdrasil server for an offline account.
     * Must be called before launching the game.
     * 
     * @param context Android context
     * @param username Offline username
     * @param uuid Offline UUID (generated from username)
     * @return YggdrasilServer if initialized successfully, null if skin is DEFAULT
     */
    fun initialize(context: Context, username: String, uuid: String): YggdrasilServer? {
        close() // Clean up any previous server
        
        val skin = loadSkin(context, username) ?: return null
        
        // If skin type is DEFAULT, no authlib-injector needed
        if (skin.type == Skin.Type.DEFAULT) {
            return null
        }
        
        // Create server on random port
        server = YggdrasilServer(0)
        
        // Load textures (handles LOCAL_FILE and CUSTOM_SKIN_LOADER_API)
        val loadedSkin = skin.loadTextures()
        
        // Add character to server
        server!!.addCharacter(
            YggdrasilServer.Character(
                uuid = uuid,
                username = username,
                loadedSkin = loadedSkin
            )
        )
        
        // Start server
        server!!.start()
        
        Logging.i("OfflineAccount", "Yggdrasil server started on port ${server!!.getListeningPort()}")
        
        return server
    }

    /**
     * Get authlib-injector JVM arguments for offline account.
     * Must call initialize() first.
     * Returns empty list if server is not running (DEFAULT skin).
     */
    fun getAuthlibArgs(): List<String> {
        val server = server ?: return emptyList()
        
        val port = server.getListeningPort()
        return listOf(
            "-Dauthlibinjector.side=client",
            "-javaagent:${getAuthlibInjectorPath()}=http://127.0.0.1:$port"
        )
    }

    /**
     * Stop the Yggdrasil server (call when game closes).
     */
    fun close() {
        server?.stop()
        server = null
        Logging.i("OfflineAccount", "Yggdrasil server stopped")
    }

    /**
     * Load skin preference from SharedPreferences.
     */
    private fun loadSkin(context: Context, username: String): Skin? {
        val skinUrl = SkinPreferenceStore.getSkinUrl(context, username)
        val skinPath = SkinPreferenceStore.getSkinPath(context, username)
        val capePath = SkinPreferenceStore.getCapePath(context, username)
        val textureModel = SkinPreferenceStore.getTextureModel(context, username)
        
        return when {
            skinUrl != null -> {
                // URL-based skin (CUSTOM_SKIN_LOADER_API)
                Skin(
                    type = Skin.Type.CUSTOM_SKIN_LOADER_API,
                    textureModel = textureModel,
                    localSkinPath = null,
                    localCapePath = capePath,
                    skinUrl = skinUrl
                )
            }
            skinPath != null && File(skinPath).exists() -> {
                // LOCAL_FILE skin
                Skin(
                    type = Skin.Type.LOCAL_FILE,
                    textureModel = textureModel,
                    localSkinPath = skinPath,
                    localCapePath = capePath
                )
            }
            else -> {
                // DEFAULT skin (no custom textures)
                Skin(
                    type = Skin.Type.DEFAULT,
                    textureModel = textureModel
                )
            }
        }
    }

    private fun getAuthlibInjectorPath(): String {
        // Path to authlib-injector JAR (should be bundled in assets or libs)
        // This will be resolved at launch time by LaunchGame
        return com.craftstudio.launcher.utils.path.LibPath.AUTHLIB_INJECTOR.absolutePath
    }
}
