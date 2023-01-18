package net.mine_diver.mixture.test;

import net.mine_diver.mixture.handler.*;

import java.io.PrintStream;

@Mixture(Target.class)
public class TargetMixture {

    @Inject(
            method = @Reference(value = "test(Z)V"),
            at = @At(
                    value = "mixture:injection_points/invoke",
                    target = @Reference("Ljava/io/PrintStream;println(Ljava/lang/String;)V"),
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectTest(boolean condition, CallbackInfo ci, int neverusedagain) {
        System.out.println("YOOO! " + condition);
        System.out.println("CallbackInfo: " + ci);
        System.out.println("Local: " + neverusedagain);
        ci.cancel();
    }

    @Redirect(
            method = @Reference(value = "test(Z)V"),
            at = @At(
                    value = "mixture:injection_points/invoke",
                    target = @Reference("Ljava/io/PrintStream;println(Ljava/lang/Object;)V")
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void redirectTest(PrintStream stream, Object message, int neverusedagain, int butwhatif, String oneFalse) {
        System.out.println(stream + " " + message.toString());
        System.out.println("Lol got a funny toString: " + message.getClass().getName() + "@" + Integer.toHexString(message.hashCode()));
        System.out.println("Local: " + oneFalse);
    }

    @Inject(
            method = @Reference("refhgfghgkfokgsd"),
            at = @At(
                    value = "gfsdgfsd",
                    target = @Reference("ghfkgjsdfkgjkdf"),
                    ordinal = 99,
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            predicate = "mixturetest:never_call_this_one"
    )
    private void predicateTest(int one, int two, CallbackInfo ci, String local) {
        System.out.println("what!??!?!");
    }

    @Inject(
            method = @Reference("testReturnable()Ljava/lang/String;"),
            at = @At(
                    value = "mixture:injection_points/invoke",
                    target = @Reference("Ljava/util/Random;nextInt()I"),
                    ordinal = 0
            )
    )
    private void cirTest(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Success!");
    }
}
