package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.CommonInjector;
import net.mine_diver.mixture.handler.Redirect;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.mixture.util.MixtureASMHelper;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Bytecode;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

@SuppressWarnings("ClassExplicitlyAnnotation")
public final class RedirectInjector<T extends Redirect & CommonInjector> implements Injector<T> {

    @Override
    public void inject(ClassNode mixedClass, MethodNode mixedMethod, AbstractInsnNode injectionPoint, Set<MixtureInfo.HandlerInfo<T>> handlers) {
        if (handlers.size() > 1)
            throw new IllegalStateException("More than one redirects on the same injection point!");
        MixtureInfo.HandlerInfo<T> handler = handlers.iterator().next();
        boolean isStatic = Modifier.isStatic(handler.methodNode.access);
        int opcode = injectionPoint.getOpcode();
        boolean isStaticInsn = opcode == GETSTATIC || opcode == PUTSTATIC || opcode == INVOKESTATIC;
        InsnList insns = new InsnList();
        Type[] argumentTypes;
        switch (injectionPoint.getType()) {
            case FIELD_INSN:
                switch (opcode) {
                    case PUTSTATIC:
                    case PUTFIELD:
                        argumentTypes = new Type[]{Type.getType(((FieldInsnNode) injectionPoint).desc)};
                        break;
                    default:
                        argumentTypes = new Type[0];
                        break;
                }
                break;
            case METHOD_INSN:
                argumentTypes = Type.getArgumentTypes(((MethodInsnNode) injectionPoint).desc);
                break;
            default:
                throw new IllegalArgumentException("Unknown injection point type \"" + injectionPoint.getClass().getSimpleName() + "\" for Redirect injector!");
        }
        if (!isStaticInsn)
            argumentTypes = Util.concat(Type.getObjectType(MixtureASMHelper.getOwner(injectionPoint)), argumentTypes);
        int redirectedArg = mixedMethod.maxLocals;
        for (Type argumentType : argumentTypes)
            ASMHelper.addLocalVariable(mixedMethod, argumentType.getDescriptor());
        for (int i = argumentTypes.length - 1; i >= 0; i--)
            insns.add(new VarInsnNode(argumentTypes[i].getOpcode(ISTORE), mixedMethod.maxLocals - Bytecode.getArgsSize(argumentTypes) + i));
        if (!isStatic)
            insns.add(new VarInsnNode(ALOAD, 0));
        int curSize = 0;
        for (Type argumentType : argumentTypes) {
            insns.add(new VarInsnNode(argumentType.getOpcode(ILOAD), redirectedArg));
            redirectedArg += argumentType.getSize();
            curSize += argumentType.getSize();
        }
        Type[] actualHandlerArgs = Type.getArgumentTypes(handler.methodNode.desc);
        boolean hasMethodArgs = actualHandlerArgs.length > argumentTypes.length;
        Type[] methodArgs = Type.getArgumentTypes(mixedMethod.desc);
        if (hasMethodArgs) {
            int methodArg = isStatic ? 0 : 1;
            for (Type argType : methodArgs) {
                insns.add(new VarInsnNode(argType.getOpcode(ILOAD), methodArg));
                methodArg += argType.getSize();
                curSize += argType.getSize();
            }
        }
        Injectors.locals(handler, insns, mixedClass, mixedMethod, injectionPoint, curSize);
        insns.add(new MethodInsnNode(isStatic ? INVOKESTATIC : INVOKESPECIAL, mixedClass.name, handler.methodNode.name, handler.methodNode.desc));
        mixedMethod.instructions.insertBefore(injectionPoint, insns);
        mixedMethod.instructions.remove(injectionPoint);
    }
}
