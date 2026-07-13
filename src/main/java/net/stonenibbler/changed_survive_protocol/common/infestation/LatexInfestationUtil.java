package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @SuppressWarnings("unchecked")
    static <T> List<T> randomSample(Collection<T> values, int limit, RandomSource random) {
        if (limit <= 0 || values.isEmpty()) {
            return List.of();
        }
        if (values instanceof List<?> list) {
            return randomSample((List<T>)list, limit, random);
        }

        List<T> sample = new ArrayList<>(Math.min(limit, values.size()));
        int seen = 0;
        for (T value : values) {
            seen++;
            if (sample.size() < limit) {
                sample.add(value);
                continue;
            }

            int replace = random.nextInt(seen);
            if (replace < limit) {
                sample.set(replace, value);
            }
        }
        shuffle(sample, random);
        return sample;
    }

    static <T> List<T> randomSample(List<T> values, int limit, RandomSource random) {
        if (limit <= 0 || values.isEmpty()) {
            return List.of();
        }

        int sampleSize = Math.min(limit, values.size());
        if (sampleSize == values.size()) {
            List<T> sample = new ArrayList<>(values);
            shuffle(sample, random);
            return sample;
        }

        Set<Integer> selected = new HashSet<>(sampleSize);
        List<T> sample = new ArrayList<>(sampleSize);
        for (int i = values.size() - sampleSize; i < values.size(); i++) {
            int candidate = random.nextInt(i + 1);
            int selectedIndex = selected.add(candidate) ? candidate : i;
            selected.add(selectedIndex);
            sample.add(values.get(selectedIndex));
        }
        shuffle(sample, random);
        return sample;
    }
}
