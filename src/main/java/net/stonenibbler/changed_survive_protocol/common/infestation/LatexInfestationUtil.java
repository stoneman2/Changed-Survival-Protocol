package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
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
        List<T> sample = new ArrayList<>(sampleSize);
        for (int i = 0; i < sampleSize; i++) {
            sample.add(values.get(random.nextInt(values.size())));
        }
        return sample;
    }
}
