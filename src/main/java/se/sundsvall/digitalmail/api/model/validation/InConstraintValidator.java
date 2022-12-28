package se.sundsvall.digitalmail.api.model.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

class InConstraintValidator implements ConstraintValidator<In, String> {
    
    private List<String> value;
    
    @Override
    public void initialize(final In inAnnotation) {
        value = Arrays.asList(inAnnotation.value());
    }
    
    @Override
    public boolean isValid(final String s, final ConstraintValidatorContext context) {
        if (null == s) {
            return false;
        }
        
        boolean valid = value.contains(s);
        if (!valid) {
            ((ConstraintValidatorContextImpl) context).addMessageParameter("allowedValues", value);
        }
        
        return valid;
    }
}