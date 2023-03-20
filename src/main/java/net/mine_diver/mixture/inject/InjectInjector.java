package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.CallbackInfo;
import net.mine_diver.mixture.handler.CallbackInfoReturnable;
import net.mine_diver.mixture.transform.AnnotationInfo;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.mixture.util.MixtureUtils;
import net.mine_diver.sarcasm.util.ASMHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
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
        int index = 0;
        for (Type methodArg : argumentTypes) {
            int opcode = methodArg.getOpcode(ILOAD);
            insns.add(new VarInsnNode(opcode, isStatic ? index : index + 1));
            index += methodArg.getSize();
        }
        Type returnType = Type.getReturnType(mixedMethod.desc);
        boolean returnVoid = Type.VOID_TYPE == returnType;
        int opcode = injectionPoint.getOpcode();
        boolean isAtReturn = opcode >= IRETURN && opcode < RETURN;
        Type callbackInfoType = returnVoid ? CALLBACKINFO_TYPE : CALLBACKINFORETURNABLE_TYPE;
        Type[] handlerArgs = MixtureUtils.concat(argumentTypes, callbackInfoType);
        int callbackOrdinal = handlerArgs.length;
        boolean usesCallbackInfo = StreamSupport.stream(handlerInfo.methodNode.instructions.spliterator(), false).anyMatch(abstractInsnNode1 -> abstractInsnNode1 instanceof VarInsnNode && abstractInsnNode1.getOpcode() == ALOAD && ((VarInsnNode) abstractInsnNode1).var == callbackOrdinal);
        LocalVariableNode returnVar = null;
        LocalVariableNode callbackInfoVar = null;
        if (usesCallbackInfo) {
            insns.add(new TypeInsnNode(NEW, callbackInfoType.getInternalName()));
            insns.add(new InsnNode(DUP));
            if (isAtReturn) {
                returnVar = ASMHelper.addLocalVariable(mixedMethod, returnType.getDescriptor());
                insns.insert(new VarInsnNode(returnType.getOpcode(ISTORE), returnVar.index));
                insns.add(new VarInsnNode(returnType.getOpcode(ILOAD), returnVar.index));
            }
            insns.add(new MethodInsnNode(INVOKESPECIAL, callbackInfoType.getInternalName(), "<init>", "(" + (isAtReturn ? returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY ? Type.getDescriptor(Object.class) : returnType.getDescriptor() : "") + ")V"));
            callbackInfoVar = ASMHelper.addLocalVariable(mixedMethod, varIndex -> "callbackInfo" + varIndex, callbackInfoType.getDescriptor());
            insns.add(new VarInsnNode(ASTORE, callbackInfoVar.index));
            insns.add(new VarInsnNode(ALOAD, callbackInfoVar.index));
        } else
            insns.add(new InsnNode(ACONST_NULL));
        Injectors.locals(handlerInfo, insns, mixedClass, mixedMethod, injectionPoint, index + 1);
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
            if (isAtReturn)
                insns.add(new VarInsnNode(returnType.getOpcode(ILOAD), returnVar.index));
        }
        if (handlerInfo.annotation.<AnnotationInfo>get("at").getEnum("shift", At.Shift.UNSET) == At.Shift.AFTER)
            mixedMethod.instructions.insert(injectionPoint, insns);
        else
            mixedMethod.instructions.insertBefore(injectionPoint, insns);
    }
}
