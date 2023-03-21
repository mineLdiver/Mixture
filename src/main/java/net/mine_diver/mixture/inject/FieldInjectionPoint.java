package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.Reference;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class FieldInjectionPoint implements InjectionPoint<FieldInsnNode> {

    @Override
    public Set<FieldInsnNode> find(InsnList insns, At at) {
        String target = Reference.Parser.get(at.target());
        int ordinal = at.ordinal();
        int opcode = at.opcode();
        Set<FieldInsnNode> found = Util.newIdentitySet();
        Iterator<AbstractInsnNode> iter = insns.iterator();
        int cur = 0;
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (target.equals(ASMHelper.toTarget(fieldInsn)) && (opcode == -1 || opcode == fieldInsn.getOpcode()) && (ordinal == -1 || ordinal == cur++))
                    found.add(fieldInsn);
            }
        }
        return Collections.unmodifiableSet(found);
    }
}
