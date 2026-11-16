package com.medicita.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = MaxBytesUtf8Validator.class)
@Target({
        ElementType.FIELD,
        ElementType.PARAMETER,
        ElementType.METHOD,
        ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytesUtf8 {

    String message() default "No debe superar {max} bytes en UTF-8";

    int max() default 72;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}