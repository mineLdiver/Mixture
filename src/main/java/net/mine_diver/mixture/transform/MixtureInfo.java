package net.mine_diver.mixture.transform;

import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.handler.CommonInjector;
import net.mine_diver.mixture.handler.Mixture;
import net.mine_diver.mixture.handler.Shadow;
import net.mine_diver.mixture.util.MixtureUtil;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Identifier;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MixtureInfo {
    public final ClassNode classNode;
    public final Mixture annotation;
    public final Set<HandlerInfo<?>> handlers;
    public final Map<String, Shadow> shadows;
    public final Set<FieldNode> fields;
    public final Set<MethodNode> methods;

    public MixtureInfo(Class<?> mixtureClass) {
        classNode = ASMHelper.readClassNode(mixtureClass);
        annotation = MixtureUtil.createAnnotationInstance(classNode.invisibleAnnotations.stream().filter(annotationNode -> Type.getDescriptor(Mixture.class).equals(annotationNode.desc)).findFirst().orElseThrow(NullPointerException::new));
        handlers = Collections.unmodifiableSet((Set<? extends HandlerInfo<?>>) classNode.methods.stream().filter(method -> method.invisibleAnnotations != null && method.invisibleAnnotations.stream().anyMatch(ann -> {
            if (Mixtures.INJECTORS.containsKey(ann.desc)) {
                String rawPredicate = CommonInjector.of(MixtureUtil.createAnnotationInstance(ann)).predicate();
                return MixtureUtil.isNullOrEmpty(rawPredicate) || Mixtures.PREDICATES.contains(Identifier.of(rawPredicate));
            }
            return false;
        })).map(HandlerInfo::new).collect(Collectors.toCollection(Util::newIdentitySet)));
        {
            Map<String, Shadow> shadows = new HashMap<>();
            classNode.fields
                    .stream()
                    .filter(fieldNode -> fieldNode.invisibleAnnotations != null && fieldNode.invisibleAnnotations
                            .stream()
                            .anyMatch(node -> Type.getDescriptor(Shadow.class).equals(node.desc))
                    )
                    .forEach(fieldNode -> shadows.put(
                            ASMHelper.toTarget(classNode, fieldNode),
                            MixtureUtil.createAnnotationInstance(
                                    fieldNode.invisibleAnnotations
                                            .stream()
                                            .filter(node -> Type.getDescriptor(Shadow.class).equals(node.desc))
                                            .findFirst()
                                            .orElseThrow(NullPointerException::new)
                            )
                    ));
            classNode.methods
                    .stream()
                    .filter(methodNode -> methodNode.invisibleAnnotations != null && methodNode.invisibleAnnotations
                            .stream()
                            .anyMatch(node -> Type.getDescriptor(Shadow.class).equals(node.desc))
                    )
                    .forEach(methodNode -> shadows.put(
                            ASMHelper.toTarget(classNode, methodNode),
                            MixtureUtil.createAnnotationInstance(
                                    methodNode.invisibleAnnotations
                                            .stream()
                                            .filter(node -> Type.getDescriptor(Shadow.class).equals(node.desc))
                                            .findFirst()
                                            .orElseThrow(NullPointerException::new)
                            )
                    ));
            this.shadows = Collections.unmodifiableMap(shadows);
        }
        fields = Collections.unmodifiableSet((Set<? extends FieldNode>) classNode.fields
                .stream()
                .filter(fieldNode -> !shadows.containsKey(ASMHelper.toTarget(classNode, fieldNode)))
                .collect(Collectors.toCollection(Util::newIdentitySet))
        );
        methods = Collections.unmodifiableSet((Set<? extends MethodNode>) classNode.methods
                .stream()
                .filter(methodNode -> !shadows.containsKey(ASMHelper.toTarget(classNode, methodNode)))
                .collect(Collectors.toCollection(Util::newIdentitySet))
        );
    }

    public final class HandlerInfo<A extends Annotation & CommonInjector> {
        public final MethodNode methodNode;
        public final A annotation;

        private HandlerInfo(MethodNode methodNode) {
            this.methodNode = methodNode;
            Set<AnnotationNode> anns = methodNode.invisibleAnnotations.stream().filter(ann -> Mixtures.INJECTORS.containsKey(ann.desc)).collect(Collectors.toCollection(Util::newIdentitySet));
            if (anns.size() > 1)
                throw new IllegalStateException("Multiple injector annotations on Mixture method \"L" + classNode.name + ";" + methodNode.name + methodNode.desc + "\"!");
            AnnotationNode node = anns.iterator().next();
            annotation = CommonInjector.of(MixtureUtil.createAnnotationInstance(node));
            methodNode.invisibleAnnotations.remove(node);
        }

        public MixtureInfo getMixtureInfo() {
            return MixtureInfo.this;
        }
    }
}