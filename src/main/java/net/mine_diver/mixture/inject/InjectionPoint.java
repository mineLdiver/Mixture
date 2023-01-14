package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.transform.AnnotationInfo;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.Set;

public interface InjectionPoint<T extends AbstractInsnNode> {
	Set<T> find(InsnList insns, AnnotationInfo at);
}
