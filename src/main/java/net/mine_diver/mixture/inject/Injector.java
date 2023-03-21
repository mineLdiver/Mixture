package net.mine_diver.mixture.inject;

import net.mine_diver.mixture.handler.CommonInjector;
import net.mine_diver.mixture.transform.MixtureInfo;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;

public interface Injector<T extends Annotation & CommonInjector> {

	void inject(ClassNode mixedClass, MethodNode mixedMethod, MixtureInfo.HandlerInfo<T> handlerInfo, AbstractInsnNode injectionPoint);
}