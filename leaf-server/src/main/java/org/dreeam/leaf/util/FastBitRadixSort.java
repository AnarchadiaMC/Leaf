package org.dreeam.leaf.util;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Array; // Required for Array.newInstance
import java.util.Arrays;
import java.util.List;

public class FastBitRadixSort {

    private static final int SMALL_ARRAY_THRESHOLD = 2;
    private static final ThreadLocal<Entity[]> ENTITY_BUFFER = ThreadLocal.withInitial(() -> new Entity[256]);
    private static final ThreadLocal<long[]> BITS_BUFFER = ThreadLocal.withInitial(() -> new long[256]);

    @SuppressWarnings("unchecked")
    public <T extends Entity, T_REF extends Entity> T[] sort(List<T> entities, T_REF referenceEntity, Class<T> entityClass) {
        int size = entities.size();
        if (size <= 1) {
            T[] resultArray = (T[]) Array.newInstance(entityClass, size);
            return entities.toArray(resultArray);
        }

        if (ENTITY_BUFFER.get().length < size) {
            ENTITY_BUFFER.set(new Entity[size]);
            BITS_BUFFER.set(new long[size]);
        }

        var entitiesBuf = ENTITY_BUFFER.get();
        var bitsBuf = BITS_BUFFER.get();
        for (int i = 0; i < size; i++) {
            entitiesBuf[i] = entities.get(i);
            bitsBuf[i] = Double.doubleToRawLongBits(
                referenceEntity.distanceToSqr(entities.get(i))
            );
        }

        fastRadixSort(entitiesBuf, bitsBuf, 0, size - 1, 62);

        T[] resultArray = (T[]) Array.newInstance(entityClass, size);
        for (int i = 0; i < size; i++) {
            resultArray[i] = entityClass.cast(entitiesBuf[i]);
        }
        Arrays.fill(entitiesBuf, 0, size, null);
        return resultArray;
    }

    private void fastRadixSort(
        Entity[] ents,
        long[] bits,
        int low,
        int high,
        int bit
    ) {
        if (bit < 0 || low >= high) {
            return;
        }

        if (high - low <= SMALL_ARRAY_THRESHOLD) {
            insertionSort(ents, bits, low, high);
            return;
        }

        int i = low;
        int j = high;
        final long mask = 1L << bit;

        while (i <= j) {
            while (i <= j && (bits[i] & mask) == 0) {
                i++;
            }
            while (i <= j && (bits[j] & mask) != 0) {
                j--;
            }
            if (i < j) {
                swap(ents, bits, i++, j--);
            }
        }

        if (low < j) {
            fastRadixSort(ents, bits, low, j, bit - 1);
        }
        if (i < high) {
            fastRadixSort(ents, bits, i, high, bit - 1);
        }
    }

    private void insertionSort(
        Entity[] ents,
        long[] bits,
        int low,
        int high
    ) {
        for (int i = low + 1; i <= high; i++) {
            int j = i;
            Entity currentEntity = ents[j];
            long currentBits = bits[j];

            while (j > low && bits[j - 1] > currentBits) {
                ents[j] = ents[j - 1];
                bits[j] = bits[j - 1];
                j--;
            }
            ents[j] = currentEntity;
            bits[j] = currentBits;
        }
    }

    private void swap(Entity[] ents, long[] bits, int a, int b) {
        Entity tempEntity = ents[a];
        ents[a] = ents[b];
        ents[b] = tempEntity;

        long tempBits = bits[a];
        bits[a] = bits[b];
        bits[b] = tempBits;
    }
}
