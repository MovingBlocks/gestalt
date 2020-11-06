package org.terasology.context;

import java.util.Optional;

public interface Argument<T> {

    /**
     * Constant for string argument.
     */
    Argument<String> STRING = Argument.of(String.class);

    /**
     * Constant for int argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Integer> INT = Argument.of(int.class);

    /**
     * Constant for long argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Long> LONG = Argument.of(long.class);

    /**
     * Constant for float argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Float> FLOAT = Argument.of(float.class);

    /**
     * Constant for double argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Double> DOUBLE = Argument.of(double.class);

    /**
     * Constant for void argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Void> VOID = Argument.of(void.class);

    /**
     * Constant for byte argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Byte> BYTE = Argument.of(byte.class);

    /**
     * Constant for boolean argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Boolean> BOOLEAN = Argument.of(boolean.class);

    /**
     * Constant char argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Character> CHAR = Argument.of(char.class);

    /**
     * Constant short argument. Used by generated code, do not remove.
     */
    @SuppressWarnings("unused")
    Argument<Short> SHORT = Argument.of(short.class);


    Argument[] ZERO_ARGUMENTS = new Argument[0];


    static <T> Argument<T> of(Class<T> clazz) {
        return new DefaultArgument<>(clazz, EmptyAnnotationMetadata.EMPTY_ARGUMENT);
    }

    static <T> Argument<T> of(Class<T> clazz, AnnotationMetadata metadata) {
        return new DefaultArgument<>(clazz, metadata);
    }

    /**
     * @return the type of the argument
     */
    Class<T> getType();

    AnnotationMetadata getAnnotation();

    String getName();

    default boolean isOptional() {
        return getType() == Optional.class;
    }
}
