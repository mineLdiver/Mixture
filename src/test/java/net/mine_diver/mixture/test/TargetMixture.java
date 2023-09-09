package net.mine_diver.mixture.test;

import net.mine_diver.mixture.handler.*;
import org.objectweb.asm.Opcodes;

import java.io.PrintStream;
import java.util.Random;

@Mixture(Target.class)
public abstract class TargetMixture {
    @Shadow(overrides = {
            "test:test_predicate", "secret"
    })
    private String test_shadow;

    @Shadow(overrides = {
            "test:test_predicate", "superSecretMethod"
    })
    abstract void test_shadowMethod();

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
        System.out.println("Secret string! " + test_shadow);
        test_shadowMethod();
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
    private void redirectTest(PrintStream stream, Object message, boolean condition, int neverusedagain, int butwhatif, String oneFalse) {
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

//    @Inject(
//            method = @Reference("testReturnable()Ljava/lang/String;"),
//            at = @At(
//                    value = "mixture:injection_points/invoke",
//                    target = @Reference("Ljava/util/Random;nextInt()I"),
//                    ordinal = 0
//            )
//    )
//    private void cirTest(CallbackInfoReturnable<String> cir) {
//        cir.setReturnValue("Success!");
//    }

    @Redirect(
            method = @Reference("test(Z)V"),
            at = @At(
                    value = "mixture:injection_points/field",
                    target = @Reference("Lnet/mine_diver/mixture/test/Target;testField:I"),
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            )
    )
    private int redirectTestField(Target instance) {
        return 20;
    }

    @Redirect(
            method = @Reference("test(Z)V"),
            at = @At(
                    value = "mixture:injection_points/field",
                    target = @Reference("Lnet/mine_diver/mixture/test/Target;testField:I"),
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void redirectTestField(Target instance, int value) {
    }

    @Inject(
            method = {
                    @Reference("test(Z)V")
            },
            target = @Desc(
                    value = @Reference("altTest"),
                    args = boolean.class
            ),
            at = @At("mixture:injection_points/head")
    )
    private void onStart(boolean condition, CallbackInfo ci) {
        System.out.println("Ayy, start of a method!");
    }

    @Inject(
            method = {
                    @Reference("altTest(Z)V"),
                    @Reference("test(Z)V")
            },
            at = @At("mixture:injection_points/return")
    )
    private void onReturn(boolean condition, CallbackInfo ci) {
        System.out.println("Booo, the end of the method...");
    }

    @Inject(
            method = @Reference("testReturnable()Ljava/lang/String;"),
            at = @At("mixture:injection_points/return")
    )
    private void onNonVoidReturn(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + " loooool");
    }

    @ModifyVariable(
            target = @Desc(
                    value = @Reference("testReturnable"),
                    ret = String.class
            ),
            at = @At(
                    value = "mixture:injection_points/invoke",
                    desc = @Desc(
                            owner = Random.class,
                            value = @Reference("nextInt"),
                            ret = int.class
                    ),
                    ordinal = 1,
                    shift = At.Shift.AFTER
            ),
            index = 2
    )
    private int modifyVarAfterXor(int a) {
        return 0xCAFEBABE;
    }
}
