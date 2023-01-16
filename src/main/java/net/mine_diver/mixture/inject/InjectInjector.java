package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.CallbackInfo;
import net.mine_diver.mixture.transform.AnnotationInfo;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.mixture.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.stream.StreamSupport;

import static org.objectweb.asm.Opcodes.*;

public final class InjectInjector implements Injector {

    private static final Type CALLBACKINFO_TYPE = Type.getType(CallbackInfo.class);

    @Override
    public void inject(ClassNode mixedClass, MethodNode mixedMethod, MixtureInfo.HandlerInfo handlerInfo, AbstractInsnNode injectionPoint) {
        InsnList insns = new InsnList();
        boolean isStatic = Modifier.isStatic(handlerInfo.methodNode.access);
        if (!isStatic)
            insns.add(new VarInsnNode(ALOAD, 0));
        Type[] argumentTypes = Type.getArgumentTypes(mixedMethod.desc);
        for (int i = 0, argumentTypesLength = argumentTypes.length; i < argumentTypesLength; i++) {
            Type argumentType = argumentTypes[i];
            insns.add(new VarInsnNode(argumentType.getOpcode(ILOAD), i + 1));
        }
        Type[] handlerArgs = Util.concat(argumentTypes, CALLBACKINFO_TYPE);
        int callbackOrdinal = handlerArgs.length;
        boolean usesCallbackInfo = StreamSupport.stream(handlerInfo.methodNode.instructions.spliterator(), false).anyMatch(abstractInsnNode1 -> abstractInsnNode1 instanceof VarInsnNode && abstractInsnNode1.getOpcode() == ALOAD && ((VarInsnNode) abstractInsnNode1).var == callbackOrdinal);
        int callbackInfoVar = -1;
        if (usesCallbackInfo) {
            insns.add(new TypeInsnNode(NEW, CALLBACKINFO_TYPE.getInternalName()));
            insns.add(new InsnNode(DUP));
            insns.add(new MethodInsnNode(INVOKESPECIAL, CALLBACKINFO_TYPE.getInternalName(), "<init>", "()V"));
            callbackInfoVar = mixedMethod.maxLocals++;
            insns.add(new VarInsnNode(ASTORE, callbackInfoVar));
            insns.add(new VarInsnNode(ALOAD, callbackInfoVar));
        } else
            insns.add(new InsnNode(ACONST_NULL));
        insns.add(new MethodInsnNode(isStatic ? INVOKESTATIC : INVOKESPECIAL, mixedClass.name, handlerInfo.methodNode.name, Type.getMethodDescriptor(Type.VOID_TYPE, handlerArgs)));
        if (usesCallbackInfo) {
            insns.add(new VarInsnNode(ALOAD, callbackInfoVar));
            insns.add(new MethodInsnNode(INVOKEVIRTUAL, CALLBACKINFO_TYPE.getInternalName(), "isCanceled", "()Z"));
            LabelNode elseLabel = new LabelNode();
            insns.add(new JumpInsnNode(IFEQ, elseLabel));
            insns.add(new InsnNode(Type.getReturnType(mixedMethod.desc).getOpcode(IRETURN)));
            insns.add(elseLabel);
        }
        if (handlerInfo.annotation.<AnnotationInfo>get("at").getEnum("shift", At.Shift.UNSET) == At.Shift.AFTER)
            mixedMethod.instructions.insert(injectionPoint, insns);
        else
            mixedMethod.instructions.insertBefore(injectionPoint, insns);
    }
}
