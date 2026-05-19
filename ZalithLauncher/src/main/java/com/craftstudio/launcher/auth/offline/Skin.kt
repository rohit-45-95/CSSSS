package com.craftstudio.launcher.auth.offline

/**
 * Skin type definition for offline accounts.
 * Matches FCL's Skin enum structure.
 */
data class Skin(
    val type: Type,
    val textureModel: TextureModel = TextureModel.STEVE,
    val localSkinPath: String? = null,
    val localCapePath: String? = null
) {
    enum class Type {
        DEFAULT,        // No custom skin
        STEVE,          // Default Steve model
        ALEX,           // Default Alex (slim) model
        LOCAL_FILE      // Custom file-based skin
    }

    /**
     * Load this skin's textures asynchronously.
     * Returns null if type is DEFAULT (no custom loading).
     */
    fun loadTextures(): LoadedSkin? {
        return when (type) {
            Type.DEFAULT -> null
            Type.STEVE -> LoadedSkin(TextureModel.STEVE, null, null)
            Type.ALEX -> LoadedSkin(TextureModel.ALEX, null, null)
            Type.LOCAL_FILE -> {
                var skin: Texture? = null
                var cape: Texture? = null
                
                localSkinPath?.let { path ->
                    try {
                        val fis = java.io.FileInputStream(path)
                        skin = Texture.loadTexture(fis)
                        fis.close()
                    } catch (e: Exception) {
                        // Skin load failed, continue without it
                    }
                }
                
                localCapePath?.let { path ->
                    try {
                        val fis = java.io.FileInputStream(path)
                        cape = Texture.loadTexture(fis)
                        fis.close()
                    } catch (e: Exception) {
                        // Cape load failed, continue without it
                    }
                }
                
                LoadedSkin(textureModel, skin, cape)
            }
        }
    }
}
