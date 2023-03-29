package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.Matcher;
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
        final Matcher matcher = Matcher.of(at);
        final int ordinal = at.ordinal();
        final int opcode = at.opcode();
        final Set<FieldInsnNode> found = Util.newIdentitySet();
        final Iterator<AbstractInsnNode> iter = insns.iterator();
        int cur = 0;
        while (iter.hasNext()) {
            final AbstractInsnNode insn = iter.next();
            if (insn instanceof FieldInsnNode) {
                final FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (matcher.matches(fieldInsn) && (opcode == -1 || opcode == fieldInsn.getOpcode()) && (ordinal == -1 || ordinal == cur++))
                    found.add(fieldInsn);
            }
        }
        return Collections.unmodifiableSet(found);
    }
}
