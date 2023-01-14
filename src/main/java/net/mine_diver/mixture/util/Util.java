package net.mine_diver.mixture.util;

import java.util.Arrays;

public class Util {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static <T> T[] concat(T[] array, T element) {
        array = Arrays.copyOf(array, array.length + 1);
        array[array.length - 1] = element;
        return array;
    }
}
