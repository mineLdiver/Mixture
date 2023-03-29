package net.mine_diver.mixture.handler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public interface Matcher {

    Matcher NONE = matchable -> false;

    static <A extends Annotation & CommonInjector> Matcher of(final A injector) {
        return Stream
                .concat(
                        Arrays
                                .stream(injector.method())
                                .map(Matcher::of),
                        Arrays
                                .stream(injector.target())
                                .map(Matcher::of)
                )
                .reduce((matcher, matcher2) -> matchable -> matcher.matches(matchable) || matcher2.matches(matchable))
                .orElse(NONE);
    }

    static Matcher of(final At at) {
        final Matcher target = of(at.target());
        final Matcher desc = of(at.desc());
        return matchable -> target.matches(matchable) || desc.matches(matchable);
    }

    static Matcher of(final Reference reference) {
        final String desc = Reference.Parser.get(reference);
        if (desc.isEmpty()) return NONE;
        final int firstSemicolon = desc.indexOf(';');
        final int argsStart = desc.indexOf('(');
        final int typeStart = desc.indexOf(':');
        final boolean skipOwner = !desc.startsWith("L");
        final boolean skipRetAndArgs = argsStart < 0;
        final boolean skipType = typeStart < 0;
        if (!skipRetAndArgs && !skipType) throw new IllegalArgumentException(String.format("Invalid reference! %s", desc));
        final String owner = skipOwner ? null : Type.getType(desc.substring(0, firstSemicolon + 1)).getInternalName();
        final String value = desc.substring(skipOwner ? 0 : firstSemicolon + 1, skipRetAndArgs ? skipType ? desc.length() : typeStart : argsStart);
        final String ret;
        final String[] args;
        if (skipRetAndArgs) {
            ret = skipType ? null : desc.substring(typeStart + 1);
            args = null;
        } else {
            final String methodDesc = desc.substring(argsStart);
            ret = Type.getReturnType(methodDesc).getDescriptor();
            args = Arrays.stream(Type.getArgumentTypes(methodDesc)).map(Type::getDescriptor).toArray(String[]::new);
        }
        final Matchable matchable = new Matchable.MatchableImpl(
                skipOwner ? Optional.empty() : Optional.of(owner),
                value,
                skipRetAndArgs ? Optional.empty() : Optional.of(ret),
                skipRetAndArgs ? Optional.empty() : Optional.of(args)
        );
        return (DelegateMatcher) () -> matchable;
    }

    static Matcher of(final Desc desc) {
        final String value = Reference.Parser.get(desc.value());
        if (value.isEmpty()) return NONE;
        final Type owner = Type.getType(desc.owner());
        final Type ret = Type.getType(desc.ret());
        final Type[] args = Arrays.stream(desc.args()).map(Type::getType).toArray(Type[]::new);
        final Matchable matchable = new Matchable.MatchableImpl(
                owner == Type.VOID_TYPE ? Optional.empty() : Optional.of(owner.getInternalName()),
                value,
                ret == Type.VOID_TYPE ? Optional.empty() : Optional.of(ret.getDescriptor()),
                args.length == 1 && args[0] == Type.VOID_TYPE ? Optional.empty() : Optional.of(
                        Arrays
                                .stream(args)
                                .map(Type::getDescriptor)
                                .toArray(String[]::new)
                )
        );
        return (DelegateMatcher) () -> matchable;
    }

    boolean matches(Matchable matchable);

    default boolean matches(MethodNode methodNode) {
        return matches(Matchable.of(methodNode));
    }

    default boolean matches(FieldInsnNode fieldInsnNode) {
        return matches(Matchable.of(fieldInsnNode));
    }

    default boolean matches(MethodInsnNode methodInsnNode) {
        return matches(Matchable.of(methodInsnNode));
    }

    interface Matchable {

        static Matchable of(MethodNode methodNode) {
            final String name = methodNode.name;
            final Optional<String> ret = Optional.of(Type.getReturnType(methodNode.desc).getDescriptor());
            final Optional<String[]> args = Optional.of(
                    Arrays
                            .stream(Type.getArgumentTypes(methodNode.desc))
                            .map(Type::getDescriptor)
                            .toArray(String[]::new)
            );
            return new MatchableImpl(Optional.empty(), name, ret, args);
        }

        static Matchable of(MethodInsnNode methodInsnNode) {
            final Optional<String> owner = Optional.of(methodInsnNode.owner);
            final String name = methodInsnNode.name;
            final Optional<String> ret = Optional.of(Type.getReturnType(methodInsnNode.desc).getDescriptor());
            final Optional<String[]> args = Optional.of(
                    Arrays
                            .stream(Type.getArgumentTypes(methodInsnNode.desc))
                            .map(Type::getDescriptor)
                            .toArray(String[]::new)
            );
            return new MatchableImpl(owner, name, ret, args);
        }

        static Matchable of(FieldInsnNode fieldInsnNode) {
            final Optional<String> owner = Optional.of(fieldInsnNode.owner);
            final String name = fieldInsnNode.name;
            final Optional<String> ret = Optional.of(fieldInsnNode.desc);
            return new MatchableImpl(owner, name, ret, Optional.empty());
        }

        Optional<String> getOwnerType();
        String getName();
        Optional<String> getReturnType();
        Optional<String[]> getArgumentTypes();

        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        @Value class MatchableImpl implements Matchable {
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
            Optional<String> ownerType;
            String name;
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
            Optional<String> returnType;
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
            Optional<String[]> argumentTypes;
        }
    }

    interface DelegateMatcher extends Matcher {

        Matchable delegate();

        @Override
        default boolean matches(Matchable matchable) {
            return delegate().getOwnerType().flatMap(type -> matchable.getOwnerType().map(type::equals)).orElse(true) &&
                    delegate().getName().equals(matchable.getName()) &&
                    delegate().getReturnType().flatMap(type -> matchable.getReturnType().map(type::equals)).orElse(true) &&
                    delegate().getArgumentTypes().flatMap(types -> matchable.getArgumentTypes().map(types1 -> Arrays.deepEquals(types, types1))).orElse(true);
        }
    }
}
