package se.sundsvall.digitalmail.integration.w3c;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class W3CValidatorDtoTest {
    
    @Test
    void dummyTest() {
        W3CValidatorDto dto = new W3CValidatorDto();
        assertThat(dto.getMessages()).isNotNull();
    }
}