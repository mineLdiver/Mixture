package net.mine_diver.mixture.transform;

import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.inject.Injector;
import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.mixture.util.MixtureUtils;
import net.mine_diver.sarcasm.transformer.ProxyTransformer;
import net.mine_diver.sarcasm.util.ASMHelper;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

public final class MixtureTransformer implements ProxyTransformer {

	public final Set<MixtureInfo> info = net.mine_diver.sarcasm.util.Util.newIdentitySet();

	@Override
	public String[] getRequestedMethods() {
		return info.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).filter(handlerInfo -> {
			String rawPredicate = handlerInfo.annotation.get("predicate", "");
			return MixtureUtils.isNullOrEmpty(rawPredicate) || Mixtures.PREDICATES.contains(Identifier.of(rawPredicate));
		}).map(handlerInfo -> handlerInfo.annotation.getReference("method")).toArray(String[]::new);
	}

	@Override
	public void transform(ClassNode node) {
		// adding interfaces
		info.stream().flatMap(mixtureInfo -> mixtureInfo.classNode.interfaces.stream()).distinct().forEach(node.interfaces::add);

		// fixing instruction owners
		info.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).forEach(mixtureHandlerInfo -> {
			MethodNode mixtureNode = mixtureHandlerInfo.methodNode;
			mixtureNode.invisibleAnnotations.remove(mixtureHandlerInfo.annotation.node);
			mixtureNode.instructions.forEach(abstractInsnNode -> {
				switch (abstractInsnNode.getType()) {
					case METHOD_INSN:
						MethodInsnNode methodInsn = (MethodInsnNode) abstractInsnNode;
						if (methodInsn.owner.equals(mixtureHandlerInfo.getMixtureInfo().classNode.name))
							methodInsn.owner = node.name;
						break;
					case FIELD_INSN:
						FieldInsnNode fieldInsn = (FieldInsnNode) abstractInsnNode;
						if (fieldInsn.owner.equals(mixtureHandlerInfo.getMixtureInfo().classNode.name))
							fieldInsn.owner = node.name;
						break;
				}
			});
		});

		// adding methods
		info.stream().flatMap(mixtureInfo -> mixtureInfo.classNode.methods.stream()).filter(methodNode -> !methodNode.name.startsWith("<")).forEach(node.methods::add);

		// adding fields
		info.stream().flatMap(mixtureInfo -> mixtureInfo.classNode.fields.stream()).forEach(node.fields::add);

		// handling injections
		info.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).filter(handlerInfo -> {
			String rawPredicate = handlerInfo.annotation.get("predicate", "");
			return MixtureUtils.isNullOrEmpty(rawPredicate) || Mixtures.PREDICATES.contains(Identifier.of(rawPredicate));
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
	}
}
