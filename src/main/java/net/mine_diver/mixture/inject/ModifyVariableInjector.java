package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.transform.AnnotationInfo;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.sarcasm.util.Locals;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

import static org.objectweb.asm.Opcodes.*;

public class ModifyVariableInjector implements Injector {

    @Override
    public void inject(ClassNode mixedClass, MethodNode mixedMethod, MixtureInfo.HandlerInfo handlerInfo, AbstractInsnNode injectionPoint) {
        InsnList insns = new InsnList();
        boolean isStatic = Modifier.isStatic(handlerInfo.methodNode.access);
        if (!isStatic)
            insns.add(new VarInsnNode(ALOAD, 0));
        int varIndex = handlerInfo.annotation.<Integer>get("index");
        boolean argsOnly = handlerInfo.annotation.<Boolean>get("argsOnly", false);
        Type varType = argsOnly ? Type.getArgumentTypes(mixedMethod.desc)[varIndex - 1] : Type.getType(Locals.getLocalVariableAt(mixedClass, mixedMethod, injectionPoint, varIndex).desc);
        int curSize = varType.getSize();
        insns.add(new VarInsnNode(varType.getOpcode(Opcodes.ILOAD), varIndex));
        Type[] methodArgs = Type.getArgumentTypes(mixedMethod.desc);
        Type[] actualHandlerArgs = Type.getArgumentTypes(handlerInfo.methodNode.desc);
        boolean hasMethodArgs = actualHandlerArgs.length - 1 > methodArgs.length;
        if (hasMethodArgs) {
            int methodArg = isStatic ? 0 : 1;
            for (Type argType : methodArgs) {
                insns.add(new VarInsnNode(argType.getOpcode(ILOAD), methodArg));
                methodArg += argType.getSize();
                curSize += argType.getSize();
            }
        }
        Injectors.locals(handlerInfo, insns, mixedClass, mixedMethod, injectionPoint, curSize);
        insns.add(new MethodInsnNode(isStatic ? INVOKESTATIC : INVOKESPECIAL, mixedClass.name, handlerInfo.methodNode.name, handlerInfo.methodNode.desc));
        insns.add(new VarInsnNode(varType.getOpcode(ISTORE), varIndex));
        if (handlerInfo.annotation.<AnnotationInfo>get("at").getEnum("shift", At.Shift.UNSET) == At.Shift.AFTER)
            mixedMethod.instructions.insert(injectionPoint, insns);
        else
            mixedMethod.instructions.insertBefore(injectionPoint, insns);
    }
}
