package net.mine_diver.mixture.test;

public class Target {

    public static final Target INSTANCE = new Target();

    private Target() {}
    
    public void test(boolean condition) {
        System.out.println("Invoked test!");
        if (condition)
            System.out.println("It's true!");
        else
            System.out.println("False! False! False!");
        System.out.println("Oh, well, that's it I guess");
    }
}
