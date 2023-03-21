package net.mine_diver.mixture.transform;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.CommonInjector;
import net.mine_diver.mixture.handler.Reference;
import net.mine_diver.mixture.inject.Injector;
import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.mixture.util.MixtureUtils;
import net.mine_diver.sarcasm.transformer.ProxyTransformer;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

public final class MixtureTransformer<T> implements ProxyTransformer {

	private final ClassNode targetNode;
	public final Set<MixtureInfo> info = net.mine_diver.sarcasm.util.Util.newIdentitySet();

	public MixtureTransformer(Class<T> targetClass) {
		targetNode = ASMHelper.readClassNode(targetClass);
	}

	@Override
	public String[] getRequestedMethods() {
		return info.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).filter(handlerInfo -> {
			String rawPredicate = handlerInfo.annotation.predicate();
			return MixtureUtils.isNullOrEmpty(rawPredicate) || Mixtures.PREDICATES.contains(Identifier.of(rawPredicate));
		}).flatMap(handlerInfo -> Arrays.stream(handlerInfo.annotation.method()).map(Reference.Parser::get)).distinct().toArray(String[]::new);
	}

	@Override
	public void transform(ClassNode node) {
		// adding interfaces
		info.stream()
				.flatMap(mixtureInfo -> mixtureInfo.classNode.interfaces.stream())
				.distinct()
				.forEach(node.interfaces::add);

		// adding methods and fixing instruction owners
		info.stream()
				.map(mixtureInfo -> mixtureInfo.classNode)
				.forEach(mixtureNode -> mixtureNode.methods.stream()
						.filter(methodNode -> !methodNode.name.startsWith("<"))
						.forEach(methodNode -> {
							MethodNode fixedNode = ASMHelper.clone(methodNode);
							fixedNode.instructions.forEach(abstractInsnNode -> {
								switch (abstractInsnNode.getType()) {
									case METHOD_INSN:
										MethodInsnNode methodInsn = (MethodInsnNode) abstractInsnNode;
										if (methodInsn.owner.equals(mixtureNode.name))
											methodInsn.owner = node.name;
										break;
									case FIELD_INSN:
										FieldInsnNode fieldInsn = (FieldInsnNode) abstractInsnNode;
										if (fieldInsn.owner.equals(mixtureNode.name))
											fieldInsn.owner = node.name;
										break;
								}
							});
							node.methods.add(fixedNode);
						})
				);

		// adding fields
		info.stream()
				.flatMap(mixtureInfo -> mixtureInfo.classNode.fields.stream())
				.forEach(node.fields::add);

		@Data
		@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
		class MethodHandlerEntry<A extends Annotation & CommonInjector> { String method; MixtureInfo.HandlerInfo<A> handler; }

		// handling injections
		info.stream()
				.flatMap(mixtureInfo -> mixtureInfo.handlers.stream())
				.filter(handlerInfo -> {
					String rawPredicate = handlerInfo.annotation.predicate();
					return MixtureUtils.isNullOrEmpty(rawPredicate) || Mixtures.PREDICATES.contains(Identifier.of(rawPredicate));
				})
				.flatMap(handlerInfo -> Arrays.stream(handlerInfo.annotation.method()).map(reference -> new MethodHandlerEntry<>(Reference.Parser.get(reference), handlerInfo)))
				.collect(Collectors.groupingBy(MethodHandlerEntry::getMethod, Collectors.mapping(MethodHandlerEntry::getHandler, Collectors.toCollection(Util::newIdentitySet))))
				.forEach((s, handlerInfos) -> node.methods.stream()
						.filter(methodNode -> s.equals(ASMHelper.toTarget(methodNode)))
						.forEach(methodNode -> {
							Map<Injector<?>, Map<MixtureInfo.HandlerInfo<?>, Set<AbstractInsnNode>>> injectorToHandlers = new HashMap<>();
							handlerInfos.forEach(handlerInfo -> {
								At at = handlerInfo.annotation.at();
								Identifier point = Identifier.of(at.value());
								if (Mixtures.INJECTION_POINTS.containsKey(point))
									//noinspection unchecked
									injectorToHandlers.computeIfAbsent(Mixtures.INJECTORS.get(Type.getDescriptor(handlerInfo.annotation.annotationType())), s1 -> new IdentityHashMap<>()).put(handlerInfo, (Set<AbstractInsnNode>) Mixtures.INJECTION_POINTS.get(point).find(methodNode.instructions, at));
								else
									throw new IllegalStateException("Unknown injection point \"" + point + "\" in mixture handler \"" + ASMHelper.toTarget(handlerInfo.getMixtureInfo().classNode, handlerInfo.methodNode) + "\"!");
							});
							injectorToHandlers.forEach((injector, handlers) -> applyHandlers(node, methodNode, injector, handlers));
						})
				);
	}

	private static <T extends Annotation & CommonInjector> void applyHandlers(ClassNode node, MethodNode methodNode, Injector<?> injector, Map<? extends MixtureInfo.HandlerInfo<?>, Set<AbstractInsnNode>> handlers) {
		//noinspection unchecked
		final Injector<T> tInjector = (Injector<T>) injector;
		//noinspection unchecked
		final Map<? extends MixtureInfo.HandlerInfo<T>, Set<AbstractInsnNode>> tHandlers = (Map<? extends MixtureInfo.HandlerInfo<T>, Set<AbstractInsnNode>>) handlers;
		tHandlers.forEach((handlerInfo, injectionPoints) -> injectionPoints.forEach(injectionPoint -> tInjector.inject(node, methodNode, handlerInfo, injectionPoint)));
	}
}
