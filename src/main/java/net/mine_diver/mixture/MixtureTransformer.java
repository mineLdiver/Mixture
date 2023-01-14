package net.mine_diver.mixture;

import static org.objectweb.asm.Opcodes.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.sarcasm.transformer.ProxyTransformer;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@RequiredArgsConstructor
public final class MixtureTransformer implements ProxyTransformer {

	public final MixtureInfo info;

	@Override
	public String[] getRequestedMethods() {
		return info.handlers.stream().map(handlerInfo -> handlerInfo.annotation.getReference("method")).toArray(String[]::new);
	}

	@Override
	public void transform(ClassNode node) {
		try {
			File fileFile = new File(".", "test.class");
			System.out.printf(fileFile.getAbsolutePath());
			FileOutputStream file = new FileOutputStream(fileFile);
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			node.accept(writer);
			file.write(writer.toByteArray());
			file.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
    
	public static <T, R extends T> Class<R> mix(Class<T> targetClass, Set<Class<? extends T>> mixtures) {
		ClassNode targetNode = new ClassNode();
		new ClassReader(ASMHelper.readClassBytes(targetClass)).accept(targetNode, 0);
		ClassWriter mixed = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		String mixedName = Type.getInternalName(targetClass).concat("$$Mixture$Mixed");
		Set<ClassNode> mixtureNodes = mixtures.stream().map(mixture -> {
			ClassNode classNode = new ClassNode();
			new ClassReader(ASMHelper.readClassBytes(mixture)).accept(classNode, 0);
			return classNode;
		}).collect(Collectors.toSet());
		String[] mixedInterfaces = mixtureNodes.stream().flatMap(mixtureNode -> mixtureNode.interfaces.stream()).distinct().toArray(String[]::new);
		mixed.visit(V1_8, ACC_PUBLIC, mixedName, null, Type.getDescriptor(targetClass), mixedInterfaces);
		Set<MixtureInfo> mixtureInfos = mixtureNodes.stream().map(MixtureInfo::new).collect(Collectors.toCollection(Util::newIdentitySet));
		Set<String> mixedMethods = Collections.unmodifiableSet(mixtureInfos.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).map(mixtureHandlerInfo -> mixtureHandlerInfo.annotation.getReference("method")).collect(Collectors.toSet()));
		mixedMethods.forEach(mixedMethod -> {
			MethodNode methodToMix = targetNode.methods.stream().filter(method -> mixedMethod.equals(ASMHelper.toTarget(method))).findFirst().orElseThrow(() -> new IllegalStateException("Tried to mix method \"" + (Type.getDescriptor(targetClass) + mixedMethod) + "\", which doesn't exist!"));
			if (Modifier.isPrivate(methodToMix.access)) throw new IllegalStateException("Tried to mix method \"" + (Type.getDescriptor(targetClass) + mixedMethod) + "\", which is private!");
			mix(mixedName, methodToMix, Collections.unmodifiableSet((Set<MixtureInfo.HandlerInfo>) mixtureInfos.stream().flatMap(mixtureInfo -> mixtureInfo.handlers.stream()).collect(Collectors.toCollection(Util::newIdentitySet))));
			methodToMix.accept(mixed.visitMethod(methodToMix.access, methodToMix.name, methodToMix.desc, methodToMix.signature, methodToMix.exceptions.toArray(new String[0])));
		});
		mixtureInfos.forEach(mixtureInfo -> mixtureInfo.handlers.forEach(mixtureHandlerInfo -> {
			MethodNode mixtureNode = mixtureHandlerInfo.methodNode;
			mixtureNode.invisibleAnnotations.remove(mixtureHandlerInfo.annotation.node);
			mixtureNode.accept(mixed.visitMethod(mixtureNode.access, mixtureNode.name, mixtureNode.desc, mixtureNode.signature, mixtureNode.exceptions.toArray(new String[0])));
		}));
		mixed.visitEnd();
		try {
			FileOutputStream file = new FileOutputStream(new File(".", "test.class"));
			file.write(mixed.toByteArray());
			file.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static void mix(String mixedName, MethodNode methodToMix, Set<MixtureInfo.HandlerInfo> handlerInfos) {
		Map<String, Map<MixtureInfo.HandlerInfo, Set<AbstractInsnNode>>> injectorToHandlers = new HashMap<>();
		handlerInfos.stream().forEach(handlerInfo -> {
			AnnotationInfo at = handlerInfo.annotation.<AnnotationInfo>get("at");
			Identifier point = Identifier.of(at.get("value"));
			if (Mixtures.INJECTION_POINTS.containsKey(point))
				injectorToHandlers.computeIfAbsent(handlerInfo.annotation.node.desc, s -> new IdentityHashMap<>()).put(handlerInfo, (Set<AbstractInsnNode>) Mixtures.INJECTION_POINTS.get(point).find(methodToMix.instructions, at));
			else
				throw new IllegalStateException("Unknown InjectionPoint \"" + point + "\" in Mixture handler \"" + ASMHelper.toTarget(handlerInfo.getMixtureInfo().classNode, handlerInfo.methodNode) + "\"!");
		});
//		injectorToHandlers.forEach((injector, handlers) -> Mixtures.INJECTORS.get(injector).inject(mixedName, methodToMix, handlers));
	}
}
