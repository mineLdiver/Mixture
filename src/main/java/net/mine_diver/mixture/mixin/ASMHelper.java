package net.mine_diver.mixture.mixin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.IntFunction;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ASMHelper {

    public static LocalVariableNode addLocalVariable(MethodNode method, String desc) {
        return addLocalVariable(method, index -> "var" + index, desc);
    }

    public static LocalVariableNode addLocalVariable(MethodNode method, IntFunction<String> nameFunction, String desc) {
        int index = allocateLocal(method);
        return addLocalVariable(method, index, nameFunction.apply(index), desc);
    }

    /**
     * Allocate a new local variable for the method
     *
     * @return the allocated local index
     */
    public static int allocateLocal(MethodNode method) {
        return allocateLocals(method, 1);
    }

    /**
     * Allocate a number of new local variables for this method, returns the
     * first local variable index of the allocated range.
     *
     * @param method method node
     * @param locals number of locals to allocate
     * @return the first local variable index of the allocated range
     */
    public static int allocateLocals(MethodNode method, int locals) {
        int nextLocal = method.maxLocals;
        method.maxLocals += locals;
        return nextLocal;
    }

    /**
     * Add an entry to the target LVT
     *
     * @param method method node
     * @param index  local variable index
     * @param name   local variable name
     * @param desc   local variable type
     */
    public static LocalVariableNode addLocalVariable(MethodNode method, int index, String name, String desc) {
        return addLocalVariable(method, index, name, desc, null, null);
    }

    /**
     * Add an entry to the target LVT between the specified start and end labels
     *
     * @param method method node
     * @param index  local variable index
     * @param name   local variable name
     * @param desc   local variable type
     * @param from   start of range
     * @param to     end of range
     */
    public static LocalVariableNode addLocalVariable(MethodNode method, int index, String name, String desc, LabelNode from, LabelNode to) {
        if (from == null) {
            from = getStartLabel(method);
        }

        if (to == null) {
            to = getEndLabel(method);
        }

        if (method.localVariables == null) {
            method.localVariables = new ArrayList<>();
        }

        for (Iterator<LocalVariableNode> iter = method.localVariables.iterator(); iter.hasNext();) {
            LocalVariableNode local = iter.next();
            if (local != null && local.index == index && from == local.start && to == local.end) {
                iter.remove();
            }
        }

        LocalVariableNode local = new Locals.SyntheticLocalVariableNode(name, desc, null, from, to, index);
        method.localVariables.add(local);
        return local;
    }

    /**
     * Get a label which marks the very start of the method
     */
    private static LabelNode getStartLabel(MethodNode method) {
        LabelNode start;
        AbstractInsnNode insn = method.instructions.getFirst();
        if (insn.getType() == AbstractInsnNode.LABEL)
            start = (LabelNode) insn;
        else
            method.instructions.insert(start = new LabelNode());
        return start;
    }

    /**
     * Get a label which marks the very end of the method
     */
    private static LabelNode getEndLabel(MethodNode method) {
        LabelNode end;
        AbstractInsnNode insn = method.instructions.getLast();
        if (insn.getType() == AbstractInsnNode.LABEL)
            end = (LabelNode) insn;
        else
            method.instructions.add(end = new LabelNode());
        return end;
    }
}
