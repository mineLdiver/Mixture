package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.transform.AnnotationInfo;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public final class InvokeInjectionPoint implements InjectionPoint<MethodInsnNode> {

    @Override
    public Set<MethodInsnNode> find(InsnList insns, AnnotationInfo at) {
        String target = at.getReference("target");
        int ordinal = at.get("ordinal", -1);
        Set<MethodInsnNode> found = Util.newIdentitySet();
        Iterator<AbstractInsnNode> iter = insns.iterator();
        int cur = 0;
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof MethodInsnNode && target.equals(ASMHelper.toTarget(((MethodInsnNode) insn))) && (ordinal == -1 || ordinal == cur++))
                found.add((MethodInsnNode) insn);
        }
        return Collections.unmodifiableSet(found);
    }
}
