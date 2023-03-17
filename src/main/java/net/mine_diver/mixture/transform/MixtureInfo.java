package net.mine_diver.mixture.transform;

import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.handler.Mixture;
import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.mixture.util.MixtureUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public final class MixtureInfo {
	
	public final ClassNode classNode;
	public final AnnotationInfo annotation;
	public final Set<HandlerInfo> handlers;
	
	public MixtureInfo(ClassNode classNode) {
		this.classNode = classNode;
		annotation = AnnotationInfo.of(classNode.invisibleAnnotations.stream().filter(annotationNode -> Type.getDescriptor(Mixture.class).equals(annotationNode.desc)).findFirst().orElseThrow(NullPointerException::new));
		handlers = Collections.unmodifiableSet((Set<HandlerInfo>) classNode.methods.stream().filter(method -> method.invisibleAnnotations != null && method.invisibleAnnotations.stream().anyMatch(ann -> {
			if (Mixtures.INJECTORS.containsKey(ann.desc)) {
				String rawPredicate = AnnotationInfo.of(ann).get("predicate", "");
				return MixtureUtils.isNullOrEmpty(rawPredicate) || Mixtures.PREDICATES.contains(Identifier.of(rawPredicate));
			}
			return false;
		})).map(HandlerInfo::new).collect(Collectors.toCollection(net.mine_diver.sarcasm.util.Util::newIdentitySet)));
	}
	
	public final class HandlerInfo {
		
		public final MethodNode methodNode;
		public final AnnotationInfo annotation;
		
		private HandlerInfo(MethodNode methodNode) {
			this.methodNode = methodNode;
			Set<AnnotationNode> anns = methodNode.invisibleAnnotations.stream().filter(ann -> Mixtures.INJECTORS.containsKey(ann.desc)).collect(Collectors.toCollection(net.mine_diver.sarcasm.util.Util::newIdentitySet));
			if (anns.size() > 1)
				throw new IllegalStateException("Multiple injector annotations on Mixture method \"L" + classNode.name + ";" + methodNode.name + methodNode.desc + "\"!");
			annotation = AnnotationInfo.of(anns.iterator().next());
			methodNode.invisibleAnnotations.remove(annotation.node);
		}
		
		public MixtureInfo getMixtureInfo() {
			return MixtureInfo.this;
		}
	}
}