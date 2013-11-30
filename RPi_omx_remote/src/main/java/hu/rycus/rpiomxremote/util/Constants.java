package hu.rycus.rpiomxremote.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for various constants.
 *
 * <br/>
 * Created by Viktor Adam on 10/31/13.
 *
 * @author rycus
 */
public interface Constants {

    // TODO add more recognised extensions
    /** The set of recognised video extensions. */
    Set<String> VIDEO_EXTENSIONS    = new HashSet<String>(Arrays.asList("avi", "mp4", "mkv"));
    /** The set of recognised subtitle extensions. */
    Set<String> SUBTITLE_EXTENSIONS = new HashSet<String>(Arrays.asList("srt"));

    /** Helper class for file extensions. */
    public static class Extensions {

        /** Returns true if the given extension is considered a video file extension. */
        public static boolean isVideo(String extension) {
            return VIDEO_EXTENSIONS.contains(extension.toLowerCase());
        }

        /** Returns true if the given extension is considered a subtitle file extension. */
        public static boolean isSubtitle(String extension) {
            return SUBTITLE_EXTENSIONS.contains(extension.toLowerCase());
        }

    }

}
