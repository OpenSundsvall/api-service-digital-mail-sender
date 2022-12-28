package se.sundsvall.digitalmail.api.model.validation;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InConstraintValidatorTest {
    
    @Mock
    private In mockIn;
    
    @Mock
    private ConstraintValidatorContext mockContext;
    
    @Mock
    private ConstraintValidatorContextImpl mockContextImpl;
    
    @Mock
    private HibernateConstraintValidatorContext mockHibernateContext;
    
    private final String[] validValues = {"value1", "value2"};
    
    private final InConstraintValidator validator = new InConstraintValidator();
    
    @BeforeEach
    public void setup() {
        when(mockIn.value()).thenReturn(validValues);
        validator.initialize(mockIn);
    }
    
    @Test
    void test_isValid() {
        assertThat(validator.isValid("value1", mockContext)).isTrue();
        assertThat(validator.isValid("value2", mockContext)).isTrue();
    }
    
    @Test
    void test_isNotValid() {
        assertThat(validator.isValid("nope", mockContextImpl)).isFalse();
        verify(mockContextImpl, times(1)).addMessageParameter(eq("allowedValues"), eq(List.of("value1", "value2")));
    }
    
    @Test
    void test_nullIsNotValid() {
        assertThat(validator.isValid(null, mockContextImpl)).isFalse();
    }
}