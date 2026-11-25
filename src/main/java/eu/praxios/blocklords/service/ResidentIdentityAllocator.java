package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.Resident;
import eu.praxios.blocklords.util.ResidentNames;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

final class ResidentIdentityAllocator {
    private final Set<String> usedNames = new HashSet<>();
    private int nextNumber;

    ResidentIdentityAllocator(Collection<Resident> residents) {
        int highest = 0;
        for (Resident resident : residents) {
            highest = Math.max(highest, resident.number());
            if (resident.displayName() != null && !resident.displayName().isBlank()) {
                usedNames.add(resident.displayName());
            }
        }
        nextNumber = highest + 1;
    }

    Identity next(Resident old) {
        int number = old != null && old.number() > 0 ? old.number() : nextNumber;
        nextNumber = Math.max(nextNumber, number + 1);
        if (old != null && old.displayName() != null && !old.displayName().isBlank()) {
            usedNames.add(old.displayName());
            return new Identity(old.displayName(), number);
        }
        String name = ResidentNames.random();
        for (int attempt = 0; attempt < 50 && usedNames.contains(name); attempt++) {
            name = ResidentNames.random();
        }
        usedNames.add(name);
        return new Identity(name, number);
    }

    record Identity(String name, int number) {
    }
}
