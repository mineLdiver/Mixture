package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.transform.AnnotationInfo;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.Collections;
import java.util.Set;

public class HeadInjectionPoint implements InjectionPoint<AbstractInsnNode> {

    @Override
    public Set<AbstractInsnNode> find(InsnList insns, AnnotationInfo at) {
        AbstractInsnNode node = insns.getFirst();
        if (node.getType() == AbstractInsnNode.LABEL)
            node = node.getNext();
        return Collections.singleton(node);
    }
}
