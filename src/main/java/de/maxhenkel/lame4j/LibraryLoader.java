package de.maxhenkel.lame4j;

import com.sun.jna.Platform;

class LibraryLoader {

    public static String getPath() {
        String platform = Platform.RESOURCE_PREFIX;
        return String.format("/natives/%s/libmp3lame.%s", platform, getExtension(platform));
    }

    private static String getExtension(String platform) {
        switch (platform) {
            case "darwin":
            case "darwin-x86-64":
            case "darwin-aarch64":
                return "dylib";
            case "win32-x86":
            case "win32-x86-64":
                return "dll";
            default:
                return "so";
        }
    }

}
