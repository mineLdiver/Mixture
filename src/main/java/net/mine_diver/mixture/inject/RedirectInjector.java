package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.mixture.util.Util;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

public final class RedirectInjector implements Injector {

    @Override
    public void inject(ClassNode mixedClass, MethodNode mixedMethod, MixtureInfo.HandlerInfo handlerInfo, AbstractInsnNode injectionPoint) {
        switch (injectionPoint.getType()) {
            case AbstractInsnNode.METHOD_INSN:
                MethodInsnNode insn = (MethodInsnNode) injectionPoint;
                boolean isStatic = Modifier.isStatic(handlerInfo.methodNode.access);
                boolean isStaticInvoke = insn.getOpcode() == Opcodes.INVOKESTATIC;
                InsnList insns = new InsnList();
                Type[] argumentTypes = Type.getArgumentTypes(insn.desc);
                if (!isStaticInvoke)
                    argumentTypes = Util.concat(Type.getObjectType(insn.owner), argumentTypes);
                int curArg = mixedMethod.maxLocals;
                mixedMethod.maxLocals += argumentTypes.length;
                for (int i = argumentTypes.length - 1; i >= 0; i--) {
                    Type argumentType = argumentTypes[i];
                    insns.add(new VarInsnNode(argumentType.getOpcode(Opcodes.ISTORE), mixedMethod.maxLocals - argumentTypes.length + i));
                }
                if (!isStatic)
                    insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                for (Type argumentType : argumentTypes)
                    insns.add(new VarInsnNode(argumentType.getOpcode(Opcodes.ILOAD), curArg++));
                insns.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL, mixedClass.name, handlerInfo.methodNode.name, Type.getMethodDescriptor(Type.getReturnType(insn.desc), argumentTypes)));
                mixedMethod.instructions.insertBefore(injectionPoint, insns);
                mixedMethod.instructions.remove(injectionPoint);
                break;
            default:
                throw new IllegalArgumentException("Unknown injection point type \"" + injectionPoint.getClass().getSimpleName() + "\" for Redirect injector!");
        }
    }
}
