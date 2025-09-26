/*
 * Copyright (C) 2024 European Union
 *  __
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the “Licence”);
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an “AS IS” basis, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */
package eu.europa.data.health.access.annotations;

import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RequiredFieldBasedOnTwoObjectValueValidator
 */
class RequiredFieldBasedOnTwoObjectValueValidatorTest {

  private RequiredFieldBasedOnTwoObjectValueValidator validator;
  private ConstraintValidatorContext ctx;

  @BeforeEach
  void setUp() {
    validator = new RequiredFieldBasedOnTwoObjectValueValidator();
    ctx = mock(ConstraintValidatorContext.class);

    // Mock the annotation so initialize(...) is covered
    RequiredFieldBasedOnTwoObjectValue ann = mock(RequiredFieldBasedOnTwoObjectValue.class);
    // Condition: (left.type == "X") && (right.type == "Y")
    when(ann.objectName()).thenReturn("left");
    when(ann.objectName2()).thenReturn("right");
    when(ann.baseField()).thenReturn("type");
    when(ann.baseField2()).thenReturn("type");
    when(ann.baseFieldValue()).thenReturn("X");
    when(ann.baseFieldValue2()).thenReturn("Y");
    // We’ll override these per test
    when(ann.mandatoryField()).thenReturn("mandatoryStr");
    when(ann.mandatoryFieldType()).thenReturn("string");

    validator.initialize(ann);

    // (Optional sanity) prove initialize wired fields
    assertEquals("left", getPrivate(validator, "objectName"));
    assertEquals("right", getPrivate(validator, "objectName2"));
    assertEquals("type", getPrivate(validator, "baseField"));
    assertEquals("type", getPrivate(validator, "baseField2"));
    assertEquals("X", getPrivate(validator, "baseFieldValue"));
    assertEquals("Y", getPrivate(validator, "baseFieldValue2"));
  }
  @Test
  void missingProperty_returnsFalse() {
      RootBean bean = conditionMatchBean();
      setPrivate(validator, "mandatoryFieldType", "string");
      setPrivate(validator, "mandatoryField", "nonExistentField");

      assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Early returns ----------

  @Test
  void returnsTrue_whenFirstObjectNull() {
    RootBean bean = new RootBean(null, new Side("Y"));
    assertTrue(validator.isValid(bean, ctx));
  }

  @Test
  void returnsTrue_whenSecondObjectNull() {
    RootBean bean = new RootBean(new Side("X"), null);
    assertTrue(validator.isValid(bean, ctx));
  }

  // ---------- Condition NOT met -> returns true (no enforcement) ----------

  @Test
  void conditionNotMatched_returnsTrue_evenIfMandatoryMissing() {
    RootBean bean = new RootBean(new Side("notX"), new Side("Y"));
    // Would fail if enforced
    bean.setMandatoryStr(null);
    setPrivate(validator, "mandatoryFieldType", "string");
    setPrivate(validator, "mandatoryField", "mandatoryStr");
    assertTrue(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: boolean ----------

  @Test
  void mandatoryBoolean_truePasses_falseFails() {
    RootBean bean = conditionMatchBean();
    setPrivate(validator, "mandatoryFieldType", "boolean");
    setPrivate(validator, "mandatoryField", "mandatoryBool");

    bean.setMandatoryBool(true);
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryBool(false);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: string (default branch) ----------

  @Test
  void mandatoryString_nonEmptyPasses_emptyOrNullFails() {
    RootBean bean = conditionMatchBean();
    setPrivate(validator, "mandatoryFieldType", "string");
    setPrivate(validator, "mandatoryField", "mandatoryStr");

    bean.setMandatoryStr("  ok  ");
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryStr("   ");
    assertFalse(validator.isValid(bean, ctx));

    bean.setMandatoryStr(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: object ----------

  @Test
  void mandatoryObject_nonNullWithKeyPasses_emptyKeyOrNullFails() {
    RootBean bean = conditionMatchBean();
    setPrivate(validator, "mandatoryFieldType", "object");
    setPrivate(validator, "mandatoryField", "mandatoryObj");

    // non-null, non-empty key -> true
    SelectObjectDTO obj = new SelectObjectDTO();
    obj.setKey("k1");
    bean.setMandatoryObj(obj);
    assertTrue(validator.isValid(bean, ctx));

    // empty key -> false
    obj.setKey("");
    bean.setMandatoryObj(obj);
    assertFalse(validator.isValid(bean, ctx));

    // null -> false
    bean.setMandatoryObj(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: objectList ----------

  @Test
  void mandatoryObjectList_nonEmptyListPasses_emptyOrNullFails() {
    RootBean bean = conditionMatchBean();
    setPrivate(validator, "mandatoryFieldType", "objectList");
    setPrivate(validator, "mandatoryField", "mandatoryObjList");

    // empty -> false
    bean.setMandatoryObjList(new ArrayList<>());
    assertFalse(validator.isValid(bean, ctx));

    // one element -> true (implementation only checks non-empty list of keys, not key content)
    SelectObjectDTO item = new SelectObjectDTO();
    item.setKey(""); // still counts
    bean.setMandatoryObjList(List.of(item));
    assertTrue(validator.isValid(bean, ctx));

    // null -> false (will throw inside and be caught -> false)
    bean.setMandatoryObjList(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: attachment ----------

  @Test
  void mandatoryAttachment_nonNullPasses_nullFails() {
    RootBean bean = conditionMatchBean();
    setPrivate(validator, "mandatoryFieldType", "attachment");
    setPrivate(validator, "mandatoryField", "mandatoryAttachment");

    bean.setMandatoryAttachment(new AttachmentDTO());
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryAttachment(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: phone ----------

  @Test
  void mandatoryPhone_nonNullPasses_nullFails() {
    RootBean bean = conditionMatchBean();
    setPrivate(validator, "mandatoryFieldType", "phone");
    setPrivate(validator, "mandatoryField", "mandatoryPhone");

    bean.setMandatoryPhone(new PhoneNumberDTO());
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryPhone(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Exception path ----------

  @Test
  void propertyLookupThrows_returnsFalse() {
    RootBean bean = conditionMatchBean();
    setPrivate(validator, "mandatoryFieldType", "string");
    setPrivate(validator, "mandatoryField", "doesNotExist"); // PropertyUtils will throw

    assertFalse(validator.isValid(bean, ctx));
  }

  // ===== helpers =====

  private static RootBean conditionMatchBean() {
    // left.type == "X" && right.type == "Y"
    return new RootBean(new Side("X"), new Side("Y"));
  }

  private static Object getPrivate(Object target, String field) {
    try {
      Field f = target.getClass().getDeclaredField(field);
      f.setAccessible(true);
      return f.get(target);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void setPrivate(Object target, String field, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Root bean whose properties are read via PropertyUtils in the validator. */
  public static class RootBean {
    private Side left;
    private Side right;

    private Boolean mandatoryBool;
    private String mandatoryStr;

    private SelectObjectDTO mandatoryObj;
    private List<SelectObjectDTO> mandatoryObjList;

    private AttachmentDTO mandatoryAttachment;
    private PhoneNumberDTO mandatoryPhone;

    public RootBean() {}
    public RootBean(Side left, Side right) { this.left = left; this.right = right; }

    public Side getLeft() { return left; }
    public void setLeft(Side left) { this.left = left; }
    public Side getRight() { return right; }
    public void setRight(Side right) { this.right = right; }

    public Boolean getMandatoryBool() { return mandatoryBool; }
    public void setMandatoryBool(Boolean mandatoryBool) { this.mandatoryBool = mandatoryBool; }

    public String getMandatoryStr() { return mandatoryStr; }
    public void setMandatoryStr(String mandatoryStr) { this.mandatoryStr = mandatoryStr; }

    public SelectObjectDTO getMandatoryObj() { return mandatoryObj; }
    public void setMandatoryObj(SelectObjectDTO mandatoryObj) { this.mandatoryObj = mandatoryObj; }

    public List<SelectObjectDTO> getMandatoryObjList() { return mandatoryObjList; }
    public void setMandatoryObjList(List<SelectObjectDTO> mandatoryObjList) { this.mandatoryObjList = mandatoryObjList; }

    public AttachmentDTO getMandatoryAttachment() { return mandatoryAttachment; }
    public void setMandatoryAttachment(AttachmentDTO mandatoryAttachment) { this.mandatoryAttachment = mandatoryAttachment; }

    public PhoneNumberDTO getMandatoryPhone() { return mandatoryPhone; }
    public void setMandatoryPhone(PhoneNumberDTO mandatoryPhone) { this.mandatoryPhone = mandatoryPhone; }
  }

  /** Minimal nested bean with a "type" property for baseField lookups. */
  public static class Side {
    private String type;
    public Side() {}
    public Side(String type) { this.type = type; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
  }
}
