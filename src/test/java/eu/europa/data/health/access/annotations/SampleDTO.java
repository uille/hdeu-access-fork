package eu.europa.data.health.access.annotations;

import java.util.List;

public class SampleDTO {
  private List<SampleItem> items;
  private String mandatoryField;

  // getters and setters
  public List<SampleItem> getItems() {
    return items;
  }

  public void setItems(List<SampleItem> items) {
    this.items = items;
  }

  public String getMandatoryField() {
    return mandatoryField;
  }

  public void setMandatoryField(String mandatoryField) {
    this.mandatoryField = mandatoryField;
  }
}
