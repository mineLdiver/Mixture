package net.mine_diver.mixture;

import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public interface InjectionPoint<T extends AbstractInsnNode> {
	Set<T> find(InsnList insns, AnnotationInfo at);
}
