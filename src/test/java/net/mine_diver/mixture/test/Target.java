package net.mine_diver.mixture.test;

import java.util.Random;

public class Target {

    public static final Target INSTANCE = new Target();

    public int testField = 42;

    private Target() {}
    
    public void test(boolean condition) {
        int neverusedagain = 40;
        int butwhatif = neverusedagain * 2;
        System.out.println("Invoked test!");
        if (condition) {
            butwhatif++;
            System.out.println("It's true!");
        } else {
            String oneFalse = "False!";
            StringBuilder message = new StringBuilder();
            for (int i = 0; i < 3; i++)
                message.append(oneFalse).append(" ");
            butwhatif += message.deleteCharAt(message.length() - 1).hashCode();
            System.out.println(message);
        }
        System.out.println("Oh, well, that's it I guess. " + butwhatif);
        System.out.println(testField);
        testField += 20;
        System.out.println(testField);
    }

    public String testReturnable() {
        Random random = new Random();
        int a = random.nextInt();
        int b = a ^ random.nextInt();
        return Integer.toHexString(a) + Integer.toBinaryString(a & ~b);
    }
}
