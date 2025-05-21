package eu.europa.data.health.access.annotations;

import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ListContainsOneMoreValueExpectASpecificValueValidatorTest {

  private ListContainsOneMoreValueExpectASpecificValueValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ListContainsOneMoreValueExpectASpecificValueValidator();
    validator.initialize(new ListContainsOneMoreValueExpectASpecificValue() {
      public String listField() { return "items"; }
      public String baseField() { return "baseField"; }
      public String expectFieldValue() { return "e"; }

      @Override
      public String[] baseFieldValues() {
        return new String[0];
      }

      @Override
      public String baseFieldValue() {
        return "";
      }

      public String mandatoryField() { return "mandatoryField"; }
      public String message() { return "Invalid"; }
      public Class<?>[] groups() { return new Class[0]; }

      @Override
      public Class<? extends Payload>[] payload() {
        return new Class[0];
      }

      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return ListContainsOneMoreValueExpectASpecificValue.class;
      }
    });
  }

  @Test
  void returnsTrueWhenListIsEmpty() {
    SampleDTO dto = new SampleDTO();
    dto.setItems(List.of());
    assertTrue(validator.isValid(dto, null));
  }

  @Test
  void returnsTrueWhenListHasOneElementEqualToExpected() {
    SampleItem item = new SampleItem();
    item.setBaseField("e");

    SampleDTO dto = new SampleDTO();
    dto.setItems(List.of(item));

    assertTrue(validator.isValid(dto, null));
  }

  @Test
  void returnsFalseWhenListHasNonExpectedAndMandatoryFieldIsBlank() {
    SampleItem item = new SampleItem();
    item.setBaseField("x");

    SampleDTO dto = new SampleDTO();
    dto.setItems(List.of(item));
    dto.setMandatoryField("  "); // blank

    assertFalse(validator.isValid(dto, null));
  }

  @Test
  void returnsTrueWhenListHasNonExpectedAndMandatoryFieldIsPresent() {
    SampleItem item = new SampleItem();
    item.setBaseField("x");

    SampleDTO dto = new SampleDTO();
    dto.setItems(List.of(item));
    dto.setMandatoryField("required");

    assertTrue(validator.isValid(dto, null));
  }

  @Test
  void returnsFalseOnException() {
    assertFalse(validator.isValid("not a valid DTO", null));
  }
}

