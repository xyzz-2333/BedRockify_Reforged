package dev.bedrockify.forge.platform;

import net.minecraftforge.fml.loading.LoadingModList;

/** Loader queries must also work before ModList is constructed, during Mixin selection. */
public final class Platform {
    private Platform() {}
    public static boolean isModLoaded(String id) {
        LoadingModList list = LoadingModList.get();
        return list != null && list.getModFileById(id) != null;
    }
}
