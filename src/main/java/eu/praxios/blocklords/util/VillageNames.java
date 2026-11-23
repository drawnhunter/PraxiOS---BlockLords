package eu.praxios.blocklords.util;

import java.text.Normalizer;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class VillageNames {
    private static final List<String> PREFIXES = List.of("Eich", "Finken", "Birken", "Sonnen", "Nebel", "Wild", "Silber", "Rosen");
    private static final List<String> SUFFIXES = List.of("hain", "dorf", "furt", "tal", "brück", "feld", "wacht", "heim");

    private VillageNames() {
    }

    public static String normalize(String input) {
        return Normalizer.normalize(input == null ? "" : input.trim().replaceAll("\\s+", " "), Normalizer.Form.NFC);
    }

    /** @return null bei gültigem Namen, sonst eine spielerlesbare Fehlerursache. */
    public static String validate(String input) {
        String name = normalize(input);
        int length = name.codePointCount(0, name.length());
        if (length < 3 || length > 24) {
            return "Der Dorfname muss 3–24 Zeichen lang sein.";
        }
        if (!name.codePoints().allMatch(codePoint -> Character.isLetterOrDigit(codePoint)
                || Character.isSpaceChar(codePoint) || codePoint == '\'' || codePoint == '-' || codePoint == '_')) {
            return "Erlaubt sind Buchstaben, Zahlen, Leerzeichen, Apostroph, Binde- und Unterstrich.";
        }
        if (!Character.isLetterOrDigit(name.codePointAt(0))
                || !Character.isLetterOrDigit(name.codePointBefore(name.length()))) {
            return "Der Name muss mit einem Buchstaben oder einer Zahl beginnen und enden.";
        }
        return null;
    }

    public static List<String> suggestions() {
        return ThreadLocalRandom.current().ints(0, PREFIXES.size()).distinct().limit(3)
                .mapToObj(index -> PREFIXES.get(index) + SUFFIXES.get(ThreadLocalRandom.current().nextInt(SUFFIXES.size())))
                .toList();
    }
}
