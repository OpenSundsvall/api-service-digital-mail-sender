package se.sundsvall.digitalmail.api.model.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InConstraintValidator.class)
public @interface In {
    
    String message() default "must be one of: {allowedValues}";
    
    Class<?>[] groups() default { };
    
    Class<? extends Payload>[] payload() default {};
    
    String[] value();
}