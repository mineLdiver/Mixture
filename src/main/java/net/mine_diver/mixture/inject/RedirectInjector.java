package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.LocalCapture;
import net.mine_diver.mixture.mixin.ASMHelper;
import net.mine_diver.mixture.mixin.Bytecode;
import net.mine_diver.mixture.mixin.Locals;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.mixture.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

public final class RedirectInjector implements Injector {

    @Override
    public void inject(ClassNode mixedClass, MethodNode mixedMethod, MixtureInfo.HandlerInfo handlerInfo, AbstractInsnNode injectionPoint) {
        switch (injectionPoint.getType()) {
            case AbstractInsnNode.METHOD_INSN:
                MethodInsnNode insn = (MethodInsnNode) injectionPoint;
                boolean isStatic = Modifier.isStatic(handlerInfo.methodNode.access);
                boolean isStaticInvoke = insn.getOpcode() == INVOKESTATIC;
                InsnList insns = new InsnList();
                Type[] argumentTypes = Type.getArgumentTypes(insn.desc);
                if (!isStaticInvoke)
                    argumentTypes = Util.concat(Type.getObjectType(insn.owner), argumentTypes);
                int curArg = mixedMethod.maxLocals;
                for (Type argumentType : argumentTypes)
                    ASMHelper.addLocalVariable(mixedMethod, argumentType.getDescriptor());
                for (int i = argumentTypes.length - 1; i >= 0; i--)
                    insns.add(new VarInsnNode(argumentTypes[i].getOpcode(ISTORE), mixedMethod.maxLocals - argumentTypes.length + i));
                if (!isStatic)
                    insns.add(new VarInsnNode(ALOAD, 0));
                for (Type argumentType : argumentTypes)
                    insns.add(new VarInsnNode(argumentType.getOpcode(ILOAD), curArg++));
                Type[] actualHandlerArgs = Type.getArgumentTypes(handlerInfo.methodNode.desc);
                boolean hasMethodArgs = actualHandlerArgs.length > argumentTypes.length;
                Type[] methodArgs = Type.getArgumentTypes(mixedMethod.desc);
                if (hasMethodArgs)
                    for (int i = 0; i < methodArgs.length; i++)
                        insns.add(new VarInsnNode(methodArgs[i].getOpcode(ILOAD), isStatic ? i : i + 1));
                if (handlerInfo.annotation.getEnum("locals", LocalCapture.NO_CAPTURE).isCaptureLocals()) {
                    LocalVariableNode[] locals = Arrays.stream(Locals.getLocalsAt(mixedClass, mixedMethod, injectionPoint, Locals.Settings.DEFAULT)).filter(Objects::nonNull).toArray(LocalVariableNode[]::new);
                    LocalVariableNode[] requestedLocals = new LocalVariableNode[actualHandlerArgs.length - (hasMethodArgs ? argumentTypes.length + methodArgs.length : argumentTypes.length)];
                    System.arraycopy(locals, Bytecode.getFirstNonArgLocalIndex(mixedMethod), requestedLocals, 0, requestedLocals.length);
                    for (LocalVariableNode requestedLocal : requestedLocals)
                        insns.add(new VarInsnNode(Type.getType(requestedLocal.desc).getOpcode(ILOAD), requestedLocal.index));
                }
                insns.add(new MethodInsnNode(isStatic ? INVOKESTATIC : INVOKESPECIAL, mixedClass.name, handlerInfo.methodNode.name, handlerInfo.methodNode.desc));
                mixedMethod.instructions.insertBefore(injectionPoint, insns);
                mixedMethod.instructions.remove(injectionPoint);
                break;
            default:
                throw new IllegalArgumentException("Unknown injection point type \"" + injectionPoint.getClass().getSimpleName() + "\" for Redirect injector!");
        }
    }
}
