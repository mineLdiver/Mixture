package net.mine_diver.mixture.inject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.handler.LocalCapture;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Bytecode;
import net.mine_diver.sarcasm.util.Locals;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ILOAD;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Injectors {

    public static void locals(MixtureInfo.HandlerInfo info, InsnList out, ClassNode classNode, MethodNode methodNode, AbstractInsnNode injectionPoint, int lastArg) {
        LocalCapture localCapture = info.annotation.getEnum("locals", LocalCapture.NO_CAPTURE);
        final boolean
                print = localCapture.isPrintLocals(),
                capture = localCapture.isCaptureLocals();
        if (!print && !capture)
            return;
        MethodNode analyzeMethod = new MethodNode(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, methodNode.exceptions.toArray(new String[0]));
        methodNode.accept(analyzeMethod);
        LocalVariableNode[] locals = Locals.getLocalsAt(classNode, analyzeMethod, injectionPoint, Locals.Settings.DEFAULT);
        List<String> localClasses = print ? new ArrayList<>() : null;
        int firstLocal = Bytecode.getFirstNonArgLocalIndex(methodNode);
        int maxSize = capture ? firstLocal + Bytecode.getArgsSize(Type.getArgumentTypes(info.methodNode.desc)) - lastArg : 0;
        for (int curSize = firstLocal; curSize < locals.length; curSize += locals[curSize] == null ? 1 : Type.getType(locals[curSize].desc).getSize()) {
            LocalVariableNode local = locals[curSize];
            if (local != null) {
                Type localType = Type.getType(local.desc);
                if (print)
                    localClasses.add(localType.getClassName());
                if (capture && curSize < maxSize)
                    out.add(new VarInsnNode(localType.getOpcode(ILOAD), local.index));
            }
        }
        if (print)
            Mixtures.LOGGER.info(ASMHelper.toTarget(info.getMixtureInfo().classNode, info.methodNode) + " locals: " + localClasses);
    }
}
