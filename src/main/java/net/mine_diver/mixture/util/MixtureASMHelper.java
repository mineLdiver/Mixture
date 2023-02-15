package net.mine_diver.mixture.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

public class MixtureASMHelper {

    public static String getOwner(AbstractInsnNode node) {
        switch (node.getType()) {
            case FIELD_INSN:
                return ((FieldInsnNode) node).owner;
            case METHOD_INSN:
                return ((MethodInsnNode) node).owner;
            default:
                throw new IllegalArgumentException("No owner in insn type " + node.getClass());
        }
    }
}
