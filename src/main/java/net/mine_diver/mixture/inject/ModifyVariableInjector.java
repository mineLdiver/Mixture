package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.CommonInjector;
import net.mine_diver.mixture.handler.ModifyVariable;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.sarcasm.util.Locals;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class ModifyVariableInjector<T extends ModifyVariable & CommonInjector> implements Injector<T> {

    @Override
    public void inject(ClassNode mixedClass, MethodNode mixedMethod, AbstractInsnNode injectionPoint, Set<MixtureInfo.HandlerInfo<T>> handlers) {
        handlers.forEach(handler -> {
            InsnList insns = new InsnList();
            boolean isStatic = Modifier.isStatic(handler.methodNode.access);
            if (!isStatic)
                insns.add(new VarInsnNode(ALOAD, 0));
            int varIndex = handler.annotation.index();
            boolean argsOnly = handler.annotation.argsOnly();
            Type varType = argsOnly ? Type.getArgumentTypes(mixedMethod.desc)[varIndex - 1] : Type.getType(Locals.getLocalVariableAt(mixedClass, mixedMethod, injectionPoint, varIndex).desc);
            int curSize = varType.getSize();
            insns.add(new VarInsnNode(varType.getOpcode(Opcodes.ILOAD), varIndex));
            Type[] methodArgs = Type.getArgumentTypes(mixedMethod.desc);
            Type[] actualHandlerArgs = Type.getArgumentTypes(handler.methodNode.desc);
            boolean hasMethodArgs = actualHandlerArgs.length - 1 > methodArgs.length;
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
            insns.add(new VarInsnNode(varType.getOpcode(ISTORE), varIndex));
            if (handler.annotation.at().shift() == At.Shift.AFTER)
                mixedMethod.instructions.insert(injectionPoint, insns);
            else
                mixedMethod.instructions.insertBefore(injectionPoint, insns);
        });
    }
}
