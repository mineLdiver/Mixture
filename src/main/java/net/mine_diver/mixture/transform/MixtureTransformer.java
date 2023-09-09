package net.mine_diver.mixture.transform;

import lombok.Value;
import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.handler.CommonInjector;
import net.mine_diver.mixture.handler.Matcher;
import net.mine_diver.mixture.handler.Reference;
import net.mine_diver.mixture.handler.Shadow;
import net.mine_diver.mixture.inject.InjectionPoint;
import net.mine_diver.mixture.inject.Injector;
import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.sarcasm.transformer.ProxyTransformer;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

public final class MixtureTransformer<T> implements ProxyTransformer {

    private final ClassNode targetNode;
    private final Set<MixtureInfo> info = Util.newIdentitySet();
    private final Set<String> methods = new HashSet<>();
    private final Set<DetachedHandlerContext<?, ?>> handlers = Util.newIdentitySet();
    private final Set<String> interfaces = new HashSet<>();
    private final Map<MixtureInfo, Set<MethodNode>> mixtureMethods = new IdentityHashMap<>();

    public MixtureTransformer(Class<T> targetClass) {
        targetNode = ASMHelper.readClassNode(targetClass);
    }

    private static @Value class AttachedHandlerContext<I extends Annotation & CommonInjector> {
        MixtureInfo.HandlerInfo<I> handler;
        Injector<I> injector;
        MethodNode mixedMethod;
        AbstractInsnNode injectionPoint;
    }

    private static @Value class DetachedHandlerContext<I extends Annotation & CommonInjector, IP extends AbstractInsnNode> {
        MixtureInfo.HandlerInfo<I> handler;
        Injector<I> injector;
        Predicate<MethodNode> methodMatcher;
        InjectionPoint<IP> injectionPoint;

        private Stream<AttachedHandlerContext<I>> streamAttached(ClassNode mixedClass) {
            return mixedClass.methods
                    .stream()
                    .filter(methodMatcher)
                    .flatMap(methodNode ->
                            injectionPoint.find(methodNode.instructions, handler.annotation.at())
                                    .stream()
                                    .map(ip ->
                                            new AttachedHandlerContext<>(
                                                    handler,
                                                    injector,
                                                    methodNode,
                                                    ip
                                            )
                                    )
                    );
        }
    }

    public <I extends Annotation & CommonInjector, IP extends AbstractInsnNode> void add(MixtureInfo info) {
        this.info.add(info);
        //noinspection unchecked
        Set<DetachedHandlerContext<?, ?>> bHandlers = info.handlers
                .stream()
                .map(handlerInfo ->
                        new DetachedHandlerContext<>(
                                (MixtureInfo.HandlerInfo<I>) handlerInfo,
                                (Injector<I>) Mixtures.INJECTORS.get(Type.getDescriptor(handlerInfo.annotation.annotationType())),
                                Matcher.of(handlerInfo.annotation)::matches,
                                (InjectionPoint<IP>) Mixtures.INJECTION_POINTS.get(Identifier.of(handlerInfo.annotation.at().value()))
                        )
                )
                .collect(Collectors.toCollection(Util::newIdentitySet));
        handlers.addAll(bHandlers);
        targetNode.methods
                .stream()
                .filter(
                        bHandlers
                                .stream()
                                .map(DetachedHandlerContext::getMethodMatcher)
                                .reduce(Predicate::or)
                                .orElseGet(() -> methodNode -> false)
                )
                .map(ASMHelper::toTarget)
                .forEach(methods::add);
        interfaces.addAll(info.classNode.interfaces);
        mixtureMethods.put(info, Collections.unmodifiableSet(
                info.methods
                        .stream()
                        .filter(methodNode -> !methodNode.name.startsWith("<"))
                        .collect(Collectors.<MethodNode, Set<MethodNode>>toCollection(Util::newIdentitySet))
        ));
    }

    @Override
    public String[] getRequestedMethods() {
        return methods.toArray(new String[0]);
    }

    @Override
    public void transform(ClassNode mixedClass) {
        // adding interfaces
        mixedClass.interfaces.addAll(interfaces);

        // adding methods and fixing instruction owners
        mixtureMethods
                .forEach((mixtureInfo, methodNodes) -> methodNodes
                        .forEach(methodNode -> {
                            MethodNode fixedNode = ASMHelper.clone(methodNode);
                            fixedNode.instructions
                                    .forEach(abstractInsnNode -> fixAccessInstruction(mixedClass, mixtureInfo, abstractInsnNode));
                            mixedClass.methods.add(fixedNode);
                        }));

        // adding fields
        info
                .stream()
                .flatMap(mixtureInfo -> mixtureInfo.fields.stream())
                .forEach(mixedClass.fields::add);

        // handling injections
        handlers
                .stream()
                .flatMap(bakedHandler -> bakedHandler.streamAttached(mixedClass))
                .collect(
                        Collectors.groupingBy(
                                AttachedHandlerContext::getMixedMethod,
                                Collectors.groupingBy(
                                        AttachedHandlerContext::getInjector,
                                        Collectors.groupingBy(
                                                AttachedHandlerContext::getInjectionPoint,
                                                Collectors.mapping(
                                                        AttachedHandlerContext::getHandler,
                                                        Collectors.toCollection(Util::newIdentitySet)
                                                )
                                        )
                                )
                        )
                )
                .forEach((method, IIPHandlers) -> IIPHandlers.forEach((injector, IPHandlers) -> IPHandlers.forEach((injectionPoint, handlers) -> injector.inject(mixedClass, method, injectionPoint, handlers))));
    }

    private static void fixAccessInstruction(ClassNode node, MixtureInfo mixtureInfo, AbstractInsnNode abstractInsnNode) {
        switch (abstractInsnNode.getType()) {
            case METHOD_INSN:
                MethodInsnNode methodInsn = (MethodInsnNode) abstractInsnNode;
                if (methodInsn.owner.equals(mixtureInfo.classNode.name)) {
                    Shadow shadow = mixtureInfo.shadows.get(ASMHelper.toTarget(methodInsn));
                    if (shadow != null) {
                        methodInsn.owner = node.superName;
                        methodInsn.name = Reference.Parser.get(methodInsn.name, shadow.overrides());
                    } else
                        methodInsn.owner = node.name;
                }
                break;
            case FIELD_INSN:
                FieldInsnNode fieldInsn = (FieldInsnNode) abstractInsnNode;
                if (fieldInsn.owner.equals(mixtureInfo.classNode.name)) {
                    Shadow shadow = mixtureInfo.shadows.get(ASMHelper.toTarget(fieldInsn));
                    if (shadow != null) {
                        fieldInsn.owner = node.superName;
                        fieldInsn.name = Reference.Parser.get(fieldInsn.name, shadow.overrides());
                    } else
                        fieldInsn.owner = node.name;
                }
                break;
        }
    }
}
