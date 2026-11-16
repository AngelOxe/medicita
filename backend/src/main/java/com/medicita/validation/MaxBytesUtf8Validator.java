package com.medicita.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class MaxBytesUtf8Validator
        implements ConstraintValidator<MaxBytesUtf8, String> {

    private int max;

    @Override
    public void initialize(MaxBytesUtf8 annotation) {
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context) {

        // Los valores obligatorios se comprueban con @NotBlank.
        return value == null
                || value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}