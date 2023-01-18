package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.CallbackInfo;
import net.mine_diver.mixture.handler.CallbackInfoReturnable;
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
    private static final Type CALLBACKINFORETURNABLE_TYPE = Type.getType(CallbackInfoReturnable.class);

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
        Type returnType = Type.getReturnType(mixedMethod.desc);
        boolean returnVoid = Type.VOID_TYPE == returnType;
        Type callbackInfoType = returnVoid ? CALLBACKINFO_TYPE : CALLBACKINFORETURNABLE_TYPE;
        Type[] handlerArgs = Util.concat(argumentTypes, callbackInfoType);
        int callbackOrdinal = handlerArgs.length;
        boolean usesCallbackInfo = StreamSupport.stream(handlerInfo.methodNode.instructions.spliterator(), false).anyMatch(abstractInsnNode1 -> abstractInsnNode1 instanceof VarInsnNode && abstractInsnNode1.getOpcode() == ALOAD && ((VarInsnNode) abstractInsnNode1).var == callbackOrdinal);
        LocalVariableNode callbackInfoVar = null;
        if (usesCallbackInfo) {
            insns.add(new TypeInsnNode(NEW, callbackInfoType.getInternalName()));
            insns.add(new InsnNode(DUP));
            insns.add(new MethodInsnNode(INVOKESPECIAL, callbackInfoType.getInternalName(), "<init>", "()V"));
            callbackInfoVar = ASMHelper.addLocalVariable(mixedMethod, index -> "callbackInfo" + index, callbackInfoType.getDescriptor());
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
            insns.add(new MethodInsnNode(INVOKEVIRTUAL, callbackInfoType.getInternalName(), "isCanceled", "()Z"));
            LabelNode elseLabel = new LabelNode();
            insns.add(new JumpInsnNode(IFEQ, elseLabel));
            if (!returnVoid) {
                insns.add(new VarInsnNode(ALOAD, callbackInfoVar.index));
                boolean object = returnType.getSort() == Type.OBJECT;
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, callbackInfoType.getInternalName(), "getReturnValue" + (object ? "" : returnType.getDescriptor()), "()" + (object ? Type.getDescriptor(Object.class) : returnType.getDescriptor())));
                if (object)
                    insns.add(new TypeInsnNode(CHECKCAST, returnType.getInternalName()));
            }
            insns.add(new InsnNode(returnType.getOpcode(IRETURN)));
            insns.add(elseLabel);
        }
        if (handlerInfo.annotation.<AnnotationInfo>get("at").getEnum("shift", At.Shift.UNSET) == At.Shift.AFTER)
            mixedMethod.instructions.insert(injectionPoint, insns);
        else
            mixedMethod.instructions.insertBefore(injectionPoint, insns);
    }
}
