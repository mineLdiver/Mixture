package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.CallbackInfo;
import net.mine_diver.mixture.handler.LocalCapture;
import net.mine_diver.mixture.mixin.ASMHelper;
import net.mine_diver.mixture.mixin.Bytecode;
import net.mine_diver.mixture.mixin.Locals;
import net.mine_diver.mixture.transform.AnnotationInfo;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.mixture.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
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
        LocalVariableNode callbackInfoVar = null;
        if (usesCallbackInfo) {
            insns.add(new TypeInsnNode(NEW, CALLBACKINFO_TYPE.getInternalName()));
            insns.add(new InsnNode(DUP));
            insns.add(new MethodInsnNode(INVOKESPECIAL, CALLBACKINFO_TYPE.getInternalName(), "<init>", "()V"));
            callbackInfoVar = ASMHelper.addLocalVariable(mixedMethod, index -> "callbackInfo" + index, Type.getDescriptor(CallbackInfo.class));
            insns.add(new VarInsnNode(ASTORE, callbackInfoVar.index));
            insns.add(new VarInsnNode(ALOAD, callbackInfoVar.index));
        } else
            insns.add(new InsnNode(ACONST_NULL));
        if (handlerInfo.annotation.getEnum("locals", LocalCapture.NO_CAPTURE).isCaptureLocals()) {
            LocalVariableNode[] locals = Arrays.stream(Locals.getLocalsAt(mixedClass, mixedMethod, injectionPoint, Locals.Settings.DEFAULT)).filter(Objects::nonNull).toArray(LocalVariableNode[]::new);
            Type[] actualHandlerArgs = Type.getArgumentTypes(handlerInfo.methodNode.desc);
            LocalVariableNode[] requestedLocals = new LocalVariableNode[actualHandlerArgs.length - handlerArgs.length];
            System.arraycopy(locals, Bytecode.getFirstNonArgLocalIndex(mixedMethod), requestedLocals, 0, requestedLocals.length);
            for (LocalVariableNode requestedLocal : requestedLocals)
                insns.add(new VarInsnNode(Type.getType(requestedLocal.desc).getOpcode(ILOAD), requestedLocal.index));
        }
        insns.add(new MethodInsnNode(isStatic ? INVOKESTATIC : INVOKESPECIAL, mixedClass.name, handlerInfo.methodNode.name, handlerInfo.methodNode.desc));
        if (usesCallbackInfo) {
            insns.add(new VarInsnNode(ALOAD, callbackInfoVar.index));
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
