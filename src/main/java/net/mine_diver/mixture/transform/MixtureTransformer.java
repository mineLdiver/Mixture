package net.mine_diver.mixture.transform;

import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.inject.Injector;
import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.mixture.util.Util;
import net.mine_diver.sarcasm.transformer.ProxyTransformer;
import net.mine_diver.sarcasm.util.ASMHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MixtureTransformer implements ProxyTransformer {

	public final Set<MixtureInfo> info = net.mine_diver.sarcasm.util.Util.newIdentitySet();

	@Override
	public String[] getRequestedMethods() {
		return info.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).filter(handlerInfo -> {
			String rawPredicate = handlerInfo.annotation.get("predicate", "");
			return Util.isNullOrEmpty(rawPredicate) || Mixtures.PREDICATES.contains(Identifier.of(rawPredicate));
		}).map(handlerInfo -> handlerInfo.annotation.getReference("method")).toArray(String[]::new);
	}

	@Override
	public void transform(ClassNode node) {
		info.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).filter(handlerInfo -> {
			String rawPredicate = handlerInfo.annotation.get("predicate", "");
			return Util.isNullOrEmpty(rawPredicate) || Mixtures.PREDICATES.contains(Identifier.of(rawPredicate));
		}).collect(Collectors.groupingBy(handlerInfo -> handlerInfo.annotation.getReference("method"), Collectors.toCollection(net.mine_diver.sarcasm.util.Util::newIdentitySet))).forEach((s, handlerInfos) -> node.methods.stream().filter(methodNode -> s.equals(ASMHelper.toTarget(methodNode))).forEach(methodNode -> {
			Map<Injector, Map<MixtureInfo.HandlerInfo, Set<AbstractInsnNode>>> injectorToHandlers = new HashMap<>();
			handlerInfos.forEach(handlerInfo -> {
				AnnotationInfo at = handlerInfo.annotation.get("at");
				Identifier point = Identifier.of(at.get("value"));
				if (Mixtures.INJECTION_POINTS.containsKey(point))
					//noinspection unchecked
					injectorToHandlers.computeIfAbsent(Mixtures.INJECTORS.get(handlerInfo.annotation.node.desc), s1 -> new IdentityHashMap<>()).put(handlerInfo, (Set<AbstractInsnNode>) Mixtures.INJECTION_POINTS.get(point).find(methodNode.instructions, at));
				else
					throw new IllegalStateException("Unknown injection point \"" + point + "\" in mixture handler \"" + ASMHelper.toTarget(handlerInfo.getMixtureInfo().classNode, handlerInfo.methodNode) + "\"!");
			});
			injectorToHandlers.forEach((injector, handlers) -> handlers.forEach((handlerInfo, injectionPoints) -> injectionPoints.forEach(injectionPoint -> injector.inject(node, methodNode, handlerInfo, injectionPoint))));
		}));
		info.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).forEach(mixtureHandlerInfo -> {
			MethodNode mixtureNode = mixtureHandlerInfo.methodNode;
			mixtureNode.invisibleAnnotations.remove(mixtureHandlerInfo.annotation.node);
			node.methods.add(mixtureNode);
		});
		info.forEach(mixtureInfo -> node.fields.addAll(mixtureInfo.classNode.fields));
	}

}
