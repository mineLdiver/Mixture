package net.mine_diver.mixture;

import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public interface Injector {
	void inject(String mixedClassName, MethodNode mixedMethod, Map<MixtureInfo.HandlerInfo, Set<AbstractInsnNode>> handlerInfo);
}