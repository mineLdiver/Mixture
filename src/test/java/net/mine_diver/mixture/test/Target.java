package net.mine_diver.mixture.test;

public class Target {

    public static final Target INSTANCE = new Target();

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
    }
}
