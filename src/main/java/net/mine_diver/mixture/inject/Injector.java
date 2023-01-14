package net.mine_diver.mixture.inject;

import java.util.Map;
import java.util.Set;

import net.mine_diver.mixture.transform.MixtureInfo;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface Injector {
	void inject(ClassNode mixedClass, MethodNode mixedMethod, Map<MixtureInfo.HandlerInfo, Set<AbstractInsnNode>> handlerInfos);
}