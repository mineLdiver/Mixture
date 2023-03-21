package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

public class ReturnInjectionPoint implements InjectionPoint<InsnNode> {

    @Override
    public Set<InsnNode> find(InsnList insns, At at) {
        int ordinal = at.ordinal();
        Set<InsnNode> found = Util.newIdentitySet();
        Iterator<AbstractInsnNode> iter = insns.iterator();
        int cur = 0;
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof InsnNode) {
                InsnNode insnNode = (InsnNode) insn;
                int opcode = insnNode.getOpcode();
                if (opcode >= IRETURN && opcode <= RETURN && (ordinal == -1 || ordinal == cur++))
                    found.add(insnNode);
            }
        }
        return Collections.unmodifiableSet(found);
    }
}
