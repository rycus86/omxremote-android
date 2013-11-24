package hu.rycus.rpiomxremote.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by rycus on 10/31/13.
 */
public interface Constants {

    // TODO add more recognised extensions
    Set<String> VIDEO_EXTENSIONS    = new HashSet<String>(Arrays.asList("avi", "mp4", "mkv"));
    Set<String> SUBTITLE_EXTENSIONS = new HashSet<String>(Arrays.asList("srt"));

    public static class Extensions {

        public static boolean isVideo(String extension) {
            return VIDEO_EXTENSIONS.contains(extension.toLowerCase());
        }

        public static boolean isSubtitle(String extension) {
            return SUBTITLE_EXTENSIONS.contains(extension.toLowerCase());
        }

    }

}
