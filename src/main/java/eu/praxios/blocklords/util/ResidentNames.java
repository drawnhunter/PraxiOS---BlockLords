package eu.praxios.blocklords.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Lesbare Namen für Bewohner statt UUID-Fragmenten. */
public final class ResidentNames {
    public static final List<String> NAMES = List.of(
            "Alaric", "Anselm", "Astrid", "Beatrix", "Bernd", "Bjorn", "Brunhild", "Cäcilia",
            "Clemens", "Dagmar", "Dietrich", "Edda", "Egon", "Elke", "Emil", "Erdmann", "Estrid",
            "Falk", "Fenja", "Florian", "Freya", "Frida", "Gero", "Greta", "Gunnar", "Hagen",
            "Halvar", "Hannelore", "Hedwig", "Helga", "Hendrik", "Hilde", "Holger", "Ida", "Ingrid",
            "Ivo", "Jorunn", "Jutta", "Karl", "Katrin", "Kendra", "Konrad", "Leif", "Lena",
            "Liesel", "Lorenz", "Lotte", "Ludwig", "Madlen", "Magnus", "Margarete", "Mathias",
            "Meike", "Mette", "Nils", "Norbert", "Odilia", "Olaf", "Ortwin", "Ottilie", "Rainer",
            "Ragna", "Rebekka", "Roald", "Roswitha", "Runa", "Sigrid", "Sönke", "Solveig", "Sten",
            "Swanhild", "Tilda", "Torben", "Ulrich", "Ursa", "Valentin", "Vera", "Viggo", "Volker",
            "Wanda", "Wendel", "Wilfried", "Ylva", "Yngvar", "Zelda", "Zora");

    private ResidentNames() {
    }

    public static String random() {
        return NAMES.get(ThreadLocalRandom.current().nextInt(NAMES.size()));
    }
}
