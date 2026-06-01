package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.util.RandomSource;

import java.util.List;

final class LatexInfestationUtil {
    private LatexInfestationUtil() {
    }

    static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T value = list.get(i);
            list.set(i, list.get(j));
            list.set(j, value);
        }
    }
}
