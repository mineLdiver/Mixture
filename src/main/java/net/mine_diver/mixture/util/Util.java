package net.mine_diver.mixture.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Util {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static <T> T[] concat(T[] array, T element) {
        array = Arrays.copyOf(array, array.length + 1);
        array[array.length - 1] = element;
        return array;
    }

    public static <T> T[] concat(T element, T[] array) {
        //noinspection unchecked
        Class<T[]> arrayType = (Class<T[]>) array.getClass();
        //noinspection unchecked
        T[] newArray = (arrayType == (Object)Object[].class) ?
                (T[]) new Object[array.length + 1] :
                (T[]) Array.newInstance(arrayType.getComponentType(), array.length + 1);
        System.arraycopy(array, 0, newArray, 1, array.length);
        newArray[0] = element;
        return newArray;
    }
}
