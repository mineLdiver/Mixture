package net.mine_diver.mixture.transform;

import lombok.RequiredArgsConstructor;
import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.sarcasm.transformer.ProxyTransformer;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class MixtureTransformer implements ProxyTransformer {

	public final MixtureInfo info;

	@Override
	public String[] getRequestedMethods() {
		return info.handlers.stream().map(handlerInfo -> handlerInfo.annotation.getReference("method")).toArray(String[]::new);
	}

	@Override
	public void transform(ClassNode node) {
		info.handlers.stream().collect(Collectors.groupingBy(handlerInfo -> handlerInfo.annotation.getReference("method"), Collectors.toCollection(Util::newIdentitySet))).forEach((s, handlerInfos) -> node.methods.stream().filter(methodNode -> s.equals(ASMHelper.toTarget(methodNode))).forEach(methodNode -> {
			Map<String, Map<MixtureInfo.HandlerInfo, Set<AbstractInsnNode>>> injectorToHandlers = new HashMap<>();
			handlerInfos.forEach(handlerInfo -> {
				AnnotationInfo at = handlerInfo.annotation.get("at");
				Identifier point = Identifier.of(at.get("value"));
				if (Mixtures.INJECTION_POINTS.containsKey(point))
					//noinspection unchecked
					injectorToHandlers.computeIfAbsent(handlerInfo.annotation.node.desc, s1 -> new IdentityHashMap<>()).put(handlerInfo, (Set<AbstractInsnNode>) Mixtures.INJECTION_POINTS.get(point).find(methodNode.instructions, at));
				else
					throw new IllegalStateException("Unknown injection point \"" + point + "\" in mixture handler \"" + ASMHelper.toTarget(handlerInfo.getMixtureInfo().classNode, handlerInfo.methodNode) + "\"!");
			});
			injectorToHandlers.forEach((injector, handlers) -> Mixtures.INJECTORS.get(injector).inject(node, methodNode, handlers));
		}));
		info.handlers.forEach(mixtureHandlerInfo -> {
			MethodNode mixtureNode = mixtureHandlerInfo.methodNode;
			mixtureNode.invisibleAnnotations.remove(mixtureHandlerInfo.annotation.node);
			node.methods.add(mixtureNode);
		});
		if (Mixtures.DEBUG_EXPORT) {
			File exportLoc = new File(".mixture.out/class/" + node.name + ".class");
			//noinspection ResultOfMethodCallIgnored
			exportLoc.getParentFile().mkdirs();
			FileOutputStream file;
			try {
				file = new FileOutputStream(exportLoc);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			node.accept(writer);
			try {
				file.write(writer.toByteArray());
				file.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
