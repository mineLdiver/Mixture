package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.Matcher;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public final class InvokeInjectionPoint implements InjectionPoint<MethodInsnNode> {

    @Override
    public Set<MethodInsnNode> find(InsnList insns, At at) {
        Matcher matcher = Matcher.of(at);
        int ordinal = at.ordinal();
        Set<MethodInsnNode> found = Util.newIdentitySet();
        Iterator<AbstractInsnNode> iter = insns.iterator();
        int cur = 0;
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();
            final MethodInsnNode methodInsn;
            if (insn instanceof MethodInsnNode && matcher.matches(methodInsn = (MethodInsnNode) insn) && (ordinal == -1 || ordinal == cur++))
                found.add(methodInsn);
        }
        return Collections.unmodifiableSet(found);
    }
}
