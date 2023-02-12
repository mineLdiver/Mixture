package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.transform.AnnotationInfo;
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
    public Set<FieldInsnNode> find(InsnList insns, AnnotationInfo at) {
        String target = at.getReference("target");
        int ordinal = at.get("ordinal", -1);
        int opcode = at.get("opcode", -1);
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
