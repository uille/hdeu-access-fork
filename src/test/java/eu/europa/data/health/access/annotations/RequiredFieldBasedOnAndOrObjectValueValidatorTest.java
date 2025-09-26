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


import org.apache.commons.beanutils.PropertyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import eu.europa.data.health.access.dto.AttachmentDTO;
import eu.europa.data.health.access.dto.PhoneNumberDTO;
import eu.europa.data.health.access.dto.SelectObjectDTO;

class RequiredFieldBasedOnAndOrObjectValueValidatorTest {

  private RequiredFieldBasedOnAndOrObjectValueValidator validator;
  private ConstraintValidatorContext ctx;

  @BeforeEach
  void setup() {
    validator = new RequiredFieldBasedOnAndOrObjectValueValidator();
    ctx = mock(ConstraintValidatorContext.class);

    // Configure internal fields (normally set by @initialize via annotation)
    // Base condition: (a.type == "X" OR b.type == "Y") AND c.type == "Z"
    set(validator, "objectName", "a");
    set(validator, "objectNameWithOr", "b");
    set(validator, "objectNameWithAnd", "c");

    set(validator, "baseField", "type");
    set(validator, "baseFieldWithOr", "type");
    set(validator, "baseFieldWithAnd", "type");

    set(validator, "baseFieldValue", "X");
    set(validator, "baseFieldValueWithOr", "Y");
    set(validator, "baseFieldValueWithAnd", "Z");

    // Defaults for tests that use the "string" mandatory branch
    set(validator, "mandatoryField", "mandatoryStr");
    set(validator, "mandatoryFieldType", "string");
  }
  @Test
  void initialize_setsFieldsFromAnnotation_andAffectsValidation() throws Exception {
    // Mock the annotation and return concrete values for every attribute
    RequiredFieldBasedOnAndOrObjectValue ann = mock(RequiredFieldBasedOnAndOrObjectValue.class);
    when(ann.objectName()).thenReturn("a");
    when(ann.objectNameWithOr()).thenReturn("b");
    when(ann.objectNameWithAnd()).thenReturn("c");

    when(ann.baseField()).thenReturn("type");
    when(ann.baseFieldWithOr()).thenReturn("type");
    when(ann.baseFieldWithAnd()).thenReturn("type");

    when(ann.baseFieldValue()).thenReturn("X");
    when(ann.baseFieldValueWithOr()).thenReturn("Y");
    when(ann.baseFieldValueWithAnd()).thenReturn("Z");

    // Use boolean branch so we avoid DTOs here
    when(ann.mandatoryField()).thenReturn("mandatoryBool");
    when(ann.mandatoryFieldType()).thenReturn("boolean");

    var validator = new RequiredFieldBasedOnAndOrObjectValueValidator();

    // Act
    validator.initialize(ann);

    // Assert: private fields were populated from the annotation
    assertEquals("a",  getPrivate(validator, "objectName"));
    assertEquals("b",  getPrivate(validator, "objectNameWithOr"));
    assertEquals("c",  getPrivate(validator, "objectNameWithAnd"));
    assertEquals("type", getPrivate(validator, "baseField"));
    assertEquals("type", getPrivate(validator, "baseFieldWithOr"));
    assertEquals("type", getPrivate(validator, "baseFieldWithAnd"));
    assertEquals("X",  getPrivate(validator, "baseFieldValue"));
    assertEquals("Y",  getPrivate(validator, "baseFieldValueWithOr"));
    assertEquals("Z",  getPrivate(validator, "baseFieldValueWithAnd"));
    assertEquals("mandatoryBool", getPrivate(validator, "mandatoryField"));
    assertEquals("boolean",       getPrivate(validator, "mandatoryFieldType"));

    // And: behavior matches initialized config
    // Base condition true: (a.type == X || b.type == Y) && c.type == Z
    var bean = new ValueBean(new Nested("X"), new Nested("ignored"), new Nested("Z"));
    var ctx = mock(ConstraintValidatorContext.class);

    bean.setMandatoryBool(true);
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryBool(false);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Early returns ----------

  @Test
  void returnsTrue_whenObjAndOrBothNull() {
    // (a == null && b == null)  -> true (regardless of c)
    ValueBean bean = new ValueBean(null, null, null);
    assertTrue(validator.isValid(bean, ctx));
  }

  @Test
  void returnsTrue_whenObjWithAndNull() {
    // c == null -> true
    ValueBean bean = new ValueBean(new Nested("X"), new Nested("Y"), null);
    assertTrue(validator.isValid(bean, ctx));
  }

  // ---------- Condition NOT met -> no enforcement ----------

  @Test
  void conditionNotMatched_returnsTrue_evenIfMandatoryMissing() {
    // a.type != X and b.type != Y
    ValueBean bean = new ValueBean(new Nested("A"), new Nested("B"), new Nested("Z"));
    bean.setMandatoryStr(null); // would fail if enforced
    assertTrue(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: boolean ----------

  @Test
  void mandatoryBoolean_truePasses_falseFails() {
    ValueBean bean = conditionMatchBean(); // a=X, b=Y, c=Z
    set(validator, "mandatoryFieldType", "boolean");
    set(validator, "mandatoryField", "mandatoryBool");

    bean.setMandatoryBool(true);
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryBool(false);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: string (default branch) ----------

  @Test
  void mandatoryString_nonEmptyPasses_emptyOrNullFails() {
    ValueBean bean = conditionMatchBean();
    set(validator, "mandatoryFieldType", "string");
    set(validator, "mandatoryField", "mandatoryStr");

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
    ValueBean bean = conditionMatchBean();
    set(validator, "mandatoryFieldType", "object");
    set(validator, "mandatoryField", "mandatoryObj");

    // Non-null + non-empty key -> true
    SelectObjectDTO obj = new SelectObjectDTO();
    obj.setKey("k1");
    bean.setMandatoryObj(obj);
    assertTrue(validator.isValid(bean, ctx));

    // Empty key -> false
    obj.setKey("");
    bean.setMandatoryObj(obj);
    assertFalse(validator.isValid(bean, ctx));

    // Null -> false
    bean.setMandatoryObj(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: objectList ----------

  @Test
  void mandatoryObjectList_nonEmptyListPasses_emptyOrNullFails() {
    ValueBean bean = conditionMatchBean();
    set(validator, "mandatoryFieldType", "objectList");
    set(validator, "mandatoryField", "mandatoryObjList");

    // Empty -> false
    bean.setMandatoryObjList(new ArrayList<>());
    assertFalse(validator.isValid(bean, ctx));

    // One element -> true (implementation only checks non-empty list of keys, not key content)
    SelectObjectDTO item = new SelectObjectDTO();
    item.setKey(""); // still counts
    bean.setMandatoryObjList(List.of(item));
    assertTrue(validator.isValid(bean, ctx));

    // Null -> false
    bean.setMandatoryObjList(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: attachment ----------

  @Test
  void mandatoryAttachment_nonNullPasses_nullFails() {
    ValueBean bean = conditionMatchBean();
    set(validator, "mandatoryFieldType", "attachment");
    set(validator, "mandatoryField", "mandatoryAttachment");

    bean.setMandatoryAttachment(new AttachmentDTO());
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryAttachment(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: listOfAttachment ----------

  @Test
  void mandatoryListOfAttachment_nonEmptyPasses_emptyOrNullFails() {
    ValueBean bean = conditionMatchBean();
    set(validator, "mandatoryFieldType", "listOfAttachment");
    set(validator, "mandatoryField", "mandatoryAttachmentList");

    bean.setMandatoryAttachmentList(new ArrayList<>());
    assertFalse(validator.isValid(bean, ctx));

    bean.setMandatoryAttachmentList(List.of(new AttachmentDTO()));
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryAttachmentList(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Condition met: phone ----------

  @Test
  void mandatoryPhone_nonNullPasses_nullFails() {
    ValueBean bean = conditionMatchBean();
    set(validator, "mandatoryFieldType", "phone");
    set(validator, "mandatoryField", "mandatoryPhone");

    bean.setMandatoryPhone(new PhoneNumberDTO());
    assertTrue(validator.isValid(bean, ctx));

    bean.setMandatoryPhone(null);
    assertFalse(validator.isValid(bean, ctx));
  }

  // ---------- Exception path ----------

  @Test
  void propertyLookupThrows_returnsFalse() {
    ValueBean bean = conditionMatchBean();
    set(validator, "mandatoryFieldType", "string");
    set(validator, "mandatoryField", "doesNotExist"); // will trigger PropertyUtils exception

    assertFalse(validator.isValid(bean, ctx));
  }


  // ===== helpers =====

  private static void set(Object target, String field, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set '" + field + "': " + e.getMessage(), e);
    }
  }

  private static ValueBean conditionMatchBean() {
    return new ValueBean(new Nested("X"), new Nested("Y"), new Nested("Z"));
  }

  /** Root bean exposing all properties read via PropertyUtils. */
  public static class ValueBean {
    private Nested a;
    private Nested b;
    private Nested c;

    private Boolean mandatoryBool;
    private String mandatoryStr;

    private SelectObjectDTO mandatoryObj;
    private List<SelectObjectDTO> mandatoryObjList;

    private AttachmentDTO mandatoryAttachment;
    private List<AttachmentDTO> mandatoryAttachmentList;

    private PhoneNumberDTO mandatoryPhone;

    public ValueBean() { }

    public ValueBean(Nested a, Nested b, Nested c) {
      this.a = a; this.b = b; this.c = c;
    }

    // ---- helpers: reflectively read private fields and simple beans used by PropertyUtils ----
    private static Object getPrivate(Object target, String field) throws Exception {
      var f = target.getClass().getDeclaredField(field);
      f.setAccessible(true);
      return f.get(target);
    }


    public Nested getA() { return a; }
    public void setA(Nested a) { this.a = a; }
    public Nested getB() { return b; }
    public void setB(Nested b) { this.b = b; }
    public Nested getC() { return c; }
    public void setC(Nested c) { this.c = c; }

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

    public List<AttachmentDTO> getMandatoryAttachmentList() { return mandatoryAttachmentList; }
    public void setMandatoryAttachmentList(List<AttachmentDTO> mandatoryAttachmentList) { this.mandatoryAttachmentList = mandatoryAttachmentList; }

    public PhoneNumberDTO getMandatoryPhone() { return mandatoryPhone; }
    public void setMandatoryPhone(PhoneNumberDTO mandatoryPhone) { this.mandatoryPhone = mandatoryPhone; }
  }

  // ---- helpers: reflectively read private fields and simple beans used by PropertyUtils ----
  private static Object getPrivate(Object target, String field) throws Exception {
    var f = target.getClass().getDeclaredField(field);
    f.setAccessible(true);
    return f.get(target);
  }
  /** Minimal nested bean for baseField lookups. */
  public static class Nested {
    private String type;
    public Nested() {}
    public Nested(String type) { this.type = type; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
  }
}
