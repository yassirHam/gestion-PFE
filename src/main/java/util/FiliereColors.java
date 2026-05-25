package util;

import java.util.Arrays;
import java.util.List;

/**
 * Stable, deterministic colour assignment for filières. The user no longer
 * has to declare colours per filière: each filière code is hashed to a
 * fixed slot in a curated palette so the same filière always gets the same
 * colour across PDF, DOCX and HTML rendering.
 */
public final class FiliereColors {

    /** RGB triplets (0..255) used for "strong" backgrounds (planning view). */
    private static final int[][] PALETTE_RGB = new int[][]{
            {0x4F, 0x8A, 0xFF}, // blue
            {0xFF, 0xC1, 0x07}, // amber
            {0x68, 0x9F, 0x38}, // green
            {0xB3, 0x88, 0xFF}, // purple
            {0xFD, 0xA1, 0x72}, // peach
            {0x00, 0x9B, 0x9B}, // teal
            {0xE6, 0x4A, 0x19}, // deep orange
            {0xC2, 0x18, 0x5B}, // pink
            {0x5D, 0x40, 0x37}, // brown
            {0x37, 0x47, 0x4F}  // blue grey
    };

    /** Hex strings (uppercase, no #) matching {@link #PALETTE_RGB}. */
    private static final List<String> PALETTE_HEX = Arrays.asList(
            "4F8AFF", "FFC107", "689F38", "B388FF", "FDA172",
            "009B9B", "E64A19", "C2185B", "5D4037", "37474F"
    );

    private FiliereColors() {}

    private static int slot(String filiere) {
        if (filiere == null || filiere.isEmpty()) return 0;
        int h = 0;
        for (int i = 0; i < filiere.length(); i++) {
            h = h * 31 + Character.toUpperCase(filiere.charAt(i));
        }
        h = h & 0x7fffffff;
        return h % PALETTE_RGB.length;
    }

    public static String hex(String filiere) {
        if (filiere != null) {
            String f = filiere.trim().toUpperCase();
            if (f.equals("TDIA")) return "C6EFCE";
            if (f.equals("ID")) return "F4B183";
            if (f.equals("GI")) return "BDD7EE";
        }
        return PALETTE_HEX.get(slot(filiere));
    }

    public static int[] rgb(String filiere) {
        if (filiere != null) {
            String f = filiere.trim().toUpperCase();
            if (f.equals("TDIA")) return new int[]{0xC6, 0xEF, 0xCE};
            if (f.equals("ID")) return new int[]{0xF4, 0xB1, 0x83};
            if (f.equals("GI")) return new int[]{0xBD, 0xD7, 0xEE};
        }
        return PALETTE_RGB[slot(filiere)];
    }

    /** Returns the float-triplet expected by iText's DeviceRgb constructor. */
    public static float[] rgbFloat(String filiere) {
        int[] c = rgb(filiere);
        return new float[]{c[0] / 255.0f, c[1] / 255.0f, c[2] / 255.0f};
    }

    /** True if the colour is dark enough that the foreground text should be white. */
    public static boolean prefersWhiteText(String filiere) {
        int[] c = rgb(filiere);
        // Perceived luminance (Rec. 601)
        double lum = 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2];
        return lum < 150;
    }
}
