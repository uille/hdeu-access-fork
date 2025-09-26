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
package eu.europa.data.health.access.services;

import static eu.europa.data.health.access.Constants.ADDRESS;
import static eu.europa.data.health.access.Constants.COUNTRY_CODE;
import static eu.europa.data.health.access.Constants.EMAIL;
import static eu.europa.data.health.access.Constants.EXPORT_DATASET_EXTERNAL_URL;
import static eu.europa.data.health.access.Constants.EXTERNAL_DATASET_ID;
import static eu.europa.data.health.access.Constants.FULL_NAME;
import static eu.europa.data.health.access.Constants.LABEL;
import static eu.europa.data.health.access.Constants.LEGAL_OR_NATURAL_PERSON;
import static eu.europa.data.health.access.Constants.NUMBER;
import static eu.europa.data.health.access.Constants.PHONE;
import static eu.europa.data.health.access.Constants.RESULT;
import static eu.europa.data.health.access.Constants.SAME_AS_CONTACT_PERSON;
import static eu.europa.data.health.access.Constants.TITLE;
import static eu.europa.data.health.access.Constants.VALUE;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.docx4j.model.fields.FieldUpdater;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.DocumentSettingsPart;
import org.docx4j.toc.TocGenerator;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.CTSettings;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.Ind;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblGrid;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.TblPr;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.TcPrInner.TcBorders;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public  abstract class DocumentExportUtility {

  private final ObjectFactory factory = new ObjectFactory();
  private final JsonObject config;
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final WebClient client;
  private final TranslationManager translationManager;

  protected DocumentExportUtility(JsonObject config, WebClient client, TranslationManager translationManager) {
    this.config = config;
    this.client = client;
    this.translationManager = translationManager;
  }


  void addTextToNewParagraph(WordprocessingMLPackage document, String text, boolean isBold,
    boolean isItalic) {
    if (Objects.nonNull(text)) {
      ObjectFactory objectFactory = new ObjectFactory();

      // Create a new paragraph
      P paragraph = objectFactory.createP();
      R run = objectFactory.createR(); // Create a new run
      Text textElement = objectFactory.createText();
      textElement.setValue(text); // Set the text content

      // Check if RPr (Run Properties) is null, if so create it
      if (run.getRPr() == null) {
        run.setRPr(objectFactory.createRPr());
      }

      // Set bold or italic properties
      if (isBold) {
        run.getRPr().setB(objectFactory.createBooleanDefaultTrue()); // Set bold
      }
      if (isItalic) {
        run.getRPr().setI(objectFactory.createBooleanDefaultTrue()); // Set italic
      }

      run.getContent().add(textElement); // Add the text to the run
      paragraph.getContent().add(run);    // Add the run to the paragraph

      // Add the paragraph to the document
      document.getMainDocumentPart().addObject(paragraph);
    }
  }



  /**
   * Adds text to the document inside a table cell with optional bold/italic styling and borders.
   */
  public void addTextToNewParagraphWithTable(WordprocessingMLPackage document, String text, boolean isBold, boolean isItalic) {
    if (Objects.nonNull(text)) {
      ObjectFactory objectFactory = new ObjectFactory();

      // Create a table
      Tbl table = objectFactory.createTbl();

      // Define table properties and set fixed width
      setFixedTableWidth(table, document);

      // Create a row
      Tr tableRow = objectFactory.createTr();

      // Create a cell and add content
      Tc tableCell = objectFactory.createTc();
      P paragraph = createStyledParagraph(objectFactory, text, isBold, isItalic);
      tableCell.getContent().add(paragraph);

      // Add borders to the cell
      setTableCellBorders(tableCell);

      // Add the cell to the row
      tableRow.getContent().add(tableCell);

      // Add the row to the table
      table.getContent().add(tableRow);

      // Add the table to the document
      document.getMainDocumentPart().addObject(table);
    }
  }

  private void addVariablesTableWithCheckbox(WordprocessingMLPackage document,
    JsonArray distributionsSample,
    String lang, String datasetId, JsonObject section1, JsonObject languageMap) {
    if (distributionsSample == null || distributionsSample.isEmpty()) {
      return;
    }

    ObjectFactory factory = new ObjectFactory();

    // Distinct variables based on the "name"
    Map<String, JsonObject> distinctByName = new LinkedHashMap<>();

    for (int j = 0; j < distributionsSample.size(); j++) {
      JsonObject dist = distributionsSample.getJsonObject(j);
      if (dist == null || !dist.containsKey("variables")) continue;

      JsonArray vars = dist.getJsonArray("variables");
      if (vars == null) continue;

      for (int k = 0; k < vars.size(); k++) {
        JsonObject v = vars.getJsonObject(k);
        if (v == null) continue;
        String name = v.getString("name", "");
        if (!name.isEmpty() && !distinctByName.containsKey(name)) {
          distinctByName.put(name, v);
        }
      }
    }

    if (distinctByName.isEmpty()) return;

    // Create table
    Tbl table = factory.createTbl();
    setFixedTableWidth(table, document);

    table.getContent().add(
      createRow(factory,
        Arrays.asList("", languageMap.getJsonObject("variables").getString(LABEL), languageMap.getJsonObject("datatype").getString(LABEL), languageMap.getJsonObject("description").getString(LABEL)),
        Arrays.asList(true, true, true, true))
    );

    // data rows
    for (JsonObject v : distinctByName.values()) {
      String name = v.getString("name", "");

      String datasetType = v.getString("datatype", "");

      // description in the correct lang
      String description = extractLocalizedDescription(v, lang);

      boolean selected = isVariableSelectedInSection(section1, datasetId, name);
      String checkbox = selected ? "\u2612" /* ☒ */ : "\u2610" /* ☐ */;

      Tr row = factory.createTr();
      row.getContent().add(createCell(factory, checkbox, false, false));
      row.getContent().add(createCell(factory, name, false, false));
      row.getContent().add(createCell(factory, datasetType, false, false));
      row.getContent().add(createCell(factory, description, false, false));

      table.getContent().add(row);
    }

    document.getMainDocumentPart().addObject(table);
  }

  private boolean isVariableSelectedInSection(JsonObject section1, String datasetId, String varName) {
    if (section1 == null) return false;

    JsonArray arr = section1.getJsonArray("datasetVariables");
    if (arr == null || arr.isEmpty()) return false;

    for (int i = 0; i < arr.size(); i++) {
      JsonObject item = arr.getJsonObject(i);
      if (item == null) continue;

      String name = item.getString("name", "");
      String dsId = item.getString("datasetId");

      if (name.equals(varName) && datasetId.equals(dsId)) {
        return true;
      }
    }

    return false;
  }

  private Tr createRow(ObjectFactory factory, List<String> texts, List<Boolean> boldFlags) {
    Tr tr = factory.createTr();
    for (int i = 0; i < texts.size(); i++) {
      String t = texts.get(i);
      boolean isBold = (boldFlags != null && i < boldFlags.size()) ? boldFlags.get(i) : false;
      tr.getContent().add(createCell(factory, t, isBold, false));
    }
    return tr;
  }

  private Tc createCell(ObjectFactory factory, String text, boolean isBold, boolean isItalic) {
    Tc tc = factory.createTc();
    P p = createStyledParagraph(factory, text, isBold, isItalic);
    tc.getContent().add(p);
    setTableCellBorders(tc);
    return tc;
  }

  private String extractLocalizedDescription(JsonObject variable, String lang) {
    if (variable == null) return "";

    Object descAny = variable.getValue("description");
    if (descAny == null) return "";

    JsonObject desc;
    if (descAny instanceof JsonObject) {
      desc = (JsonObject) descAny;
    } else if (descAny instanceof Map) {
      desc = new JsonObject((Map<String, Object>) descAny);
    } else {
      return "";
    }

    String txt = desc.getString(lang);
    if (txt != null && !txt.isEmpty()) return txt;

    txt = desc.getString("en");
    if (txt != null && !txt.isEmpty()) return txt;

    for (String key : desc.fieldNames()) {
      String v = desc.getString(key);
      if (v != null && !v.isEmpty()) return v;
    }

    return "";
  }


  /**
   * Sets the table width to fit the entire Word page (ignoring dynamic sizing).
   */
  private void setFixedTableWidth(Tbl table, WordprocessingMLPackage document) {
    ObjectFactory objectFactory = new ObjectFactory();

    // Get the full page width in twips (20ths of a point)
    int pageWidthTwips = getPageWidthInTwips(document);

    // Create table properties
    TblPr tableProperties = table.getTblPr();
    if (tableProperties == null) {
      tableProperties = objectFactory.createTblPr();
      table.setTblPr(tableProperties);
    }

    // Set the table width to the full page width
    TblWidth tableWidth = objectFactory.createTblWidth();
    tableWidth.setType("dxa"); // Twips (20ths of a point)
    tableWidth.setW(BigInteger.valueOf(pageWidthTwips));
    tableProperties.setTblW(tableWidth);

    // Create table grid to define fixed column sizes
    TblGrid tblGrid = objectFactory.createTblGrid();
    TblGridCol gridCol = objectFactory.createTblGridCol();
    gridCol.setW(BigInteger.valueOf(pageWidthTwips)); // Single column spans entire page
    tblGrid.getGridCol().add(gridCol);
    table.setTblGrid(tblGrid);
  }

  /**
   * Gets the total width of the Word page in twips (20ths of a point).
   */
  private int getPageWidthInTwips(WordprocessingMLPackage document) {
    return document.getDocumentModel().getSections().get(0).getPageDimensions().getWritableWidthTwips();
  }


  /**
   * Creates a styled paragraph with the given text, bold, and italic properties.
   */
  private P createStyledParagraph(ObjectFactory objectFactory, String text, boolean isBold,
    boolean isItalic) {
    P paragraph = objectFactory.createP();
    R run = objectFactory.createR();
    Text textElement = objectFactory.createText();
    textElement.setValue(text);

    //Ensure the text is treated as literal text (e.g., without line breaks)
    textElement.setSpace("preserve");

    RPr runProperties = objectFactory.createRPr();
    if (isBold) {
      runProperties.setB(objectFactory.createBooleanDefaultTrue());
    }
    if (isItalic) {
      runProperties.setI(objectFactory.createBooleanDefaultTrue());
    }
    run.setRPr(runProperties);
    run.getContent().add(textElement);
    paragraph.getContent().add(run);

    return paragraph;
  }

  /**
   * Sets borders for a table cell (top, bottom, left, and right).
   */
  private void setTableCellBorders(Tc tableCell) {
    TcPr cellProperties = tableCell.getTcPr();
    if (cellProperties == null) {
      cellProperties = new TcPr();
      tableCell.setTcPr(cellProperties);
    }

    TcBorders cellBorders = new TcBorders();

    //Create and configure each border
    CTBorder border = new CTBorder();
    border.setVal(STBorder.SINGLE);         //Border style
    border.setSz(BigInteger.valueOf(12));   //Border thickness
    border.setSpace(BigInteger.ZERO);       //No spacing
    border.setColor("000000");              //Black color

    //Set borders for all sides
    cellBorders.setTop(border);
    cellBorders.setBottom(border);
    cellBorders.setLeft(border);
    cellBorders.setRight(border);

    //Apply borders to the cell properties
    cellProperties.setTcBorders(cellBorders);
  }

  void addHeaderToTheText(WordprocessingMLPackage document, String text,
    String headerStyle) {
    P headerParagraph = factory.createP();
    R run = factory.createR();
    Text textElement = factory.createText();
    textElement.setValue(text);
    run.getContent().add(textElement);

    // Set style (Heading1, Heading2, etc.)
    headerParagraph.setPPr(factory.createPPr());
    headerParagraph.getPPr().setPStyle(factory.createPPrBasePStyle());
    headerParagraph.getPPr().getPStyle().setVal(headerStyle);  // Apply the specified header style

    headerParagraph.getContent().add(run);
    document.getMainDocumentPart().addObject(headerParagraph);
  }

  void addTextWithCheckboxToNewParagraph(WordprocessingMLPackage document, String labelText,
    boolean isBold, boolean isChecked) {
    ObjectFactory objectFactory = new ObjectFactory();
    P paragraph = objectFactory.createP();
    // Create a run for the checkbox
    R runCheckbox = objectFactory.createR();
    Text checkboxText = objectFactory.createText();

    // Unicode characters for checked (☒) and unchecked (☐)
    String checkbox = isChecked ? "\u2612" : "\u2610";
    checkboxText.setValue(checkbox + " ");

    runCheckbox.getContent().add(checkboxText);
    paragraph.getContent().add(runCheckbox);

    R runLabel = objectFactory.createR();
    Text label = objectFactory.createText();
    label.setValue(labelText);
    runLabel.getContent().add(label);

    if (isBold) {
      runLabel.setRPr(objectFactory.createRPr());
      runLabel.getRPr().setB(objectFactory.createBooleanDefaultTrue());
    }
    paragraph.getContent().add(runLabel);
    document.getMainDocumentPart().addObject(paragraph);
  }


  String getTranslateValueForKey(JsonObject jsonObject, String keyToFind, String property) {
    JsonArray itemsArray = jsonObject.getJsonArray("items");
    for (int i = 0; i < itemsArray.size(); i++) {
      JsonObject item = itemsArray.getJsonObject(i);
      String key = item.getString("key");
      if (key.equals(keyToFind)) {
        return item.getString(property);
      }
    }
    return null;
  }

  void generatedJsonArrayValues(WordprocessingMLPackage document, JsonArray jsonArray, String type) {
    if (jsonArray == null || jsonArray.isEmpty()) {
      // Create a table with one empty cell
      ObjectFactory objectFactory = new ObjectFactory();

      Tbl table = objectFactory.createTbl();
      setFixedTableWidth(table, document);

      Tr tableRow = objectFactory.createTr();
      Tc tableCell = objectFactory.createTc();

      // Add an empty paragraph
      P emptyParagraph = createStyledParagraph(objectFactory, "", false, false);
      tableCell.getContent().add(emptyParagraph);
      setTableCellBorders(tableCell);

      tableRow.getContent().add(tableCell);
      table.getContent().add(tableRow);

      document.getMainDocumentPart().addObject(table);
    } else {
      ObjectFactory objectFactory = new ObjectFactory();

      // Create a single table
      Tbl table = objectFactory.createTbl();
      setFixedTableWidth(table, document); // Ensure the table spans the page width

      for (Object item : jsonArray) {
        if (item instanceof JsonObject jsonItem) {
          // Create a row for each value
          Tr tableRow = objectFactory.createTr();

          // Create a cell with the value
          Tc tableCell = objectFactory.createTc();
          String text = jsonItem.getString(type);
          if (text != null) {
            P paragraph = createStyledParagraph(objectFactory, text, false, false);
            tableCell.getContent().add(paragraph);
          } else {
            P emptyParagraph = createStyledParagraph(objectFactory, "", false, false);
            tableCell.getContent().add(emptyParagraph);
          }

          setTableCellBorders(tableCell);

          tableRow.getContent().add(tableCell);
          table.getContent().add(tableRow);
        }
      }

      // Add the table to the document after all rows are added
      document.getMainDocumentPart().addObject(table);
    }
  }

  //Method to check if the JsonArray contains an object with a specific key value
  boolean containsKey(JsonArray jsonArray, String keyToCheck) {
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject obj = jsonArray.getJsonObject(i);
      String key = obj.getString("key");
      if (keyToCheck.equals(key)) {
        return true;
      }
    }
    return false;
  }

  boolean containsSelectedValueWithoutSpecificValue(JsonArray jsonArray,
    String excludedKey) {
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject obj = jsonArray.getJsonObject(i);
      String key = obj.getString("key");

      if (!key.equals(excludedKey)) {
        return true;
      }
    }
    return false;
  }

  JsonArray filterDatasetsByCatalogIds(JsonArray datasets, JsonArray catalogIds) {
    JsonArray filteredDatasets = new JsonArray();
    for (int i = 0; i < datasets.size(); i++) {
      JsonObject datasetObject = datasets.getJsonObject(i);
      if (datasetObject.containsKey(EXTERNAL_DATASET_ID)) {
        filteredDatasets.add(datasetObject);
      } else {
        JsonObject catalogObject = datasetObject.getJsonObject(RESULT).getJsonObject("catalog");
        if (catalogObject != null) {
          String catalogId = catalogObject.getString("id");
          for (int j = 0; j < catalogIds.size(); j++) {
            String catalogIdFromSection = catalogIds.getString(j);
            if (catalogId.equals(catalogIdFromSection)) {
              filteredDatasets.add(datasetObject);
              break;
            }
          }
        }
      }
    }
    return filteredDatasets;
  }

  /**
   * Method where put specific values on common 1 row cell with a background color
   */
  private void addTextsInCommonCell(WordprocessingMLPackage document,
    Map<String, Boolean> textValues, String backgroundColor) {
    Tbl table = factory.createTbl();
    Tr row = factory.createTr();
    Tc cell = factory.createTc();

    CTShd cellShading = factory.createCTShd();
    cellShading.setFill(backgroundColor);
    TcPr cellProperties = factory.createTcPr();
    cellProperties.setShd(cellShading);

    TcBorders borders = new TcBorders();
    CTBorder border = new CTBorder();
    border.setColor("000000");  //Border color (black)
    border.setSz(BigInteger.valueOf(8));  //Border thickness
    borders.setTop(border);
    borders.setBottom(border);
    borders.setLeft(border);
    borders.setRight(border);
    cellProperties.setTcBorders(borders);
    cell.setTcPr(cellProperties);

    for (Map.Entry<String, Boolean> entry : textValues.entrySet()) {
      P paragraph = factory.createP();
      R run = factory.createR();
      Text textElement = factory.createText();
      textElement.setValue(entry.getKey());  // Get text
      //Set bold if required
      if (Boolean.TRUE.equals(entry.getValue())) {
        RPr runProperties = factory.createRPr();
        runProperties.setB(factory.createBooleanDefaultTrue());
        run.setRPr(runProperties);
      }
      run.getContent().add(textElement);
      paragraph.getContent().add(run);

      //Add left indentation to create padding from the border
      PPr paragraphProperties = factory.createPPr();
      Ind indentation = new Ind();
      indentation.setLeft(BigInteger.valueOf(200));
      paragraphProperties.setInd(indentation);
      paragraph.setPPr(paragraphProperties);

      cell.getContent().add(paragraph);
    }
    row.getContent().add(cell);
    table.getContent().add(row);
    document.getMainDocumentPart().addObject(table);
  }

  void addSectionWithHeader(WordprocessingMLPackage document, JsonObject languageMap,
    String sectionHeader, String sectionDescription, String aboutThisSection) {
    addHeaderToTheText(document, languageMap.getString(sectionHeader), "Heading1");
    Map<String, Boolean> textValues = new LinkedHashMap<>();
    textValues.put(aboutThisSection, true);
    textValues.put(languageMap.getString(sectionDescription), false);
    addTextsInCommonCell(document, textValues, "F3F6FC");
  }

  void writeDatasetsValues(WordprocessingMLPackage document,
    JsonArray datasets, String property1, String property2) {
    for (int i = 0; i < datasets.size(); i++) {
      if (datasets.getJsonObject(i).containsKey(EXTERNAL_DATASET_ID)) {
        addTextToNewParagraphWithTable(document,
          config.getString(EXPORT_DATASET_EXTERNAL_URL) + datasets.getJsonObject(i).getString(
            EXTERNAL_DATASET_ID), false, false);
      } else if(Objects.nonNull(datasets.getJsonObject(i).getJsonObject(RESULT).getJsonObject(property1))){
        String value = datasets.getJsonObject(i).getJsonObject(RESULT).getJsonObject(property1)
          .getString(property2);

        addTextToNewParagraphWithTable(document, value, false, false);
      }
    }
  }

  void writeDatasetsValuesWithVariables(WordprocessingMLPackage document,
    JsonArray datasets, String property1, String property2, JsonObject section1, JsonObject languageMap) {
    for (int i = 0; i < datasets.size(); i++) {
      if (datasets.getJsonObject(i).containsKey(EXTERNAL_DATASET_ID)) {
        addTextToNewParagraphWithTable(document,
          config.getString(EXPORT_DATASET_EXTERNAL_URL) + datasets.getJsonObject(i).getString(
            EXTERNAL_DATASET_ID), false, false);
      } else if (Objects.nonNull(datasets.getJsonObject(i).getJsonObject(RESULT).getJsonObject(property1))){
        String value = datasets.getJsonObject(i).getJsonObject(RESULT).getJsonObject(property1)
          .getString(property2);

        addTextToNewParagraphWithTable(document, value, false, false);

        document.getMainDocumentPart().addParagraphOfText(languageMap.getJsonObject("variables").getString(LABEL));

        // Table with the distinct variables from distributions_sample
        String datasetId = datasets.getJsonObject(i).getJsonObject(RESULT).getString("id");
        JsonArray distributionsSample = datasets.getJsonObject(i)
          .getJsonObject(RESULT)
          .getJsonArray("distributions_sample");

        addVariablesTableWithCheckbox(document, distributionsSample, property2, datasetId, section1, languageMap);

        document.getMainDocumentPart().addParagraphOfText("");
      }
    }
  }

  // Helper for safe field extraction
  public String getField(JsonObject section, String key) {
    return (section != null && section.containsKey(key)) ? section.getString(key, "") : "";
  }

  public JsonArray getArrayField(JsonObject section, String key) {
    return (section != null && section.containsKey(key)) ? section.getJsonArray(key) : null;
  }

  public boolean isChecked(JsonObject section, String key) {
    return section != null && section.getBoolean(key, false);
  }

  public JsonObject getObject(JsonObject sec, String key) {
    return (sec != null && sec.containsKey(key)) ? sec.getJsonObject(key) : null;
  }

  public String getObjectValue(JsonObject sec, String key) {
    JsonObject obj = getObject(sec, key);
    return obj != null ? obj.getString(VALUE, "") : "";
  }

  public boolean isKey(JsonObject obj, String expectedKey) {
    return obj != null && expectedKey.equals(obj.getString("key", ""));
  }

  public String buildPhone(JsonObject phoneObj) {
    if (phoneObj == null) {
      return "";
    }
    return "+" + phoneObj.getString(COUNTRY_CODE, "") + " " + phoneObj.getString(NUMBER, "");
  }

  public static List<String> extractCatalogIds(JsonArray hubDatasets) {
    if (hubDatasets != null && !hubDatasets.isEmpty()) {
      return hubDatasets.stream()
        .map(jsonValue -> (JsonObject) jsonValue)
        .map(item -> {
          JsonObject result = item.getJsonObject("result");
          if (result != null) {
            JsonObject catalog = result.getJsonObject("catalog");
            if (catalog != null) {
              return catalog.getString("id", null);
            }
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  public static JsonObject findSection6ObjectByCatalogId(JsonArray section6Array,
    String catalogId) {
    if (section6Array == null || section6Array.isEmpty() || catalogId == null) {
      return null;
    }

    for (Object value : section6Array) {
      JsonObject section6Object = (JsonObject) value;
      JsonArray catalogIdsArray = section6Object.getJsonArray("catalog_ids");

      if (catalogIdsArray != null) {
        for (Object idValue : catalogIdsArray) {
          String id = idValue.toString().replace("\"", ""); // remove quotes
          if (catalogId.equals(id)) {
            return section6Object; // match found
          }
        }
      }
    }

    return null; // no match found
  }

  protected void generateTableOfContext(WordprocessingMLPackage document) {
    try {
      TocGenerator tocGenerator = new TocGenerator(document);
      tocGenerator.updateToc(false, true);
    } catch (Exception e) {
      log.debug(e.getMessage());
    }
  }

  protected void section4Mappings(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonObject section3, String aboutThisSection) {
    addSectionWithHeader(document, languageMap, "section4h5", "section4Description",
      aboutThisSection);

    boolean isDescriptionChecked = isChecked(section, SAME_AS_CONTACT_PERSON);
    addTextWithCheckboxToNewParagraph(document,
      languageMap.getJsonObject(SAME_AS_CONTACT_PERSON).getString(LABEL), true,
      isDescriptionChecked);

    JsonObject legalPerson = this.getObject(section3, LEGAL_OR_NATURAL_PERSON);

    if (Objects.nonNull(legalPerson) && Objects.equals(legalPerson.getString("key"), "a")) {
      addTextToNewParagraph(document, languageMap.getJsonObject(FULL_NAME).getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, getField(section, FULL_NAME), false,
        false);

      addTextToNewParagraph(document, languageMap.getJsonObject(EMAIL).getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, getField(section, EMAIL),
        false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject(PHONE).getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document,
        buildPhone(getObject(section, PHONE)), false, false);

      // --- ORGANIZATION INFORMATION ---
      addTextToNewParagraph(document,
        languageMap.getJsonObject("nameOfTheOrganisation").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "nameOfTheOrganisation"), false,
        false);

      addTextToNewParagraph(document, languageMap.getJsonObject(ADDRESS).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, ADDRESS), false, false);

      addTextToNewParagraph(document,
        languageMap.getJsonObject("businessIdentifierOrganization").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "businessIdentifierOrganization"),
        false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject("vatNumber").getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, "vatNumber"), false, false);
    } else {
      // --- CONTACT INFORMATION ---
      addTextToNewParagraph(document, languageMap.getJsonObject(FULL_NAME).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, FULL_NAME), false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject(EMAIL).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, EMAIL), false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject(PHONE).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, buildPhone(getObject(section, PHONE)), false, false);

      addTextToNewParagraph(document, languageMap.getJsonObject(ADDRESS).getString(LABEL), true,
        false);
      addTextToNewParagraphWithTable(document, getField(section, ADDRESS), false, false);
    }

    // --- INVOICE INFORMATION ---
    addTextToNewParagraph(document, languageMap.getJsonObject("invoiceType").getString(LABEL), true,
      false);
    JsonObject invoiceType = getObject(section, "invoiceType");
    addTextToNewParagraphWithTable(document, getObjectValue(section, "invoiceType"), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("invoiceReferenceNumber").getString(LABEL), true, false);
    addTextToNewParagraphWithTable(document, getField(section, "invoiceReferenceNumber"), false,
      false);

    if (isKey(invoiceType, "b")) {
      addTextToNewParagraph(document, languageMap.getJsonObject("invoiceAddress").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, getField(section, "invoiceAddress"), false, false);
    }

    // --- OPERATOR IDENTIFIER (ONLY FOR LEGAL PERSONS) ---
    if (isKey(legalPerson, "a")) {
      addTextToNewParagraph(document,
        languageMap.getJsonObject("operatorIdentifier").getString(LABEL), true, false);
      addTextToNewParagraphWithTable(document, getField(section, "operatorIdentifier"), false,
        false);

      addTextToNewParagraph(document, languageMap.getJsonObject("peppolCode").getString(LABEL),
        true, false);
      addTextToNewParagraphWithTable(document, getField(section, "peppolCode"), false, false);
    }

    // --- FINANCIAL COVERAGE ---
    addTextToNewParagraph(document,
      languageMap.getJsonObject("isTheProjectFinanciallyCovered").getString(LABEL), true, false);
    String financiallyCoveredValue = getObjectValue(section, "isTheProjectFinanciallyCovered");
    addTextToNewParagraphWithTable(document, financiallyCoveredValue, false, false);

    if (financiallyCoveredValue != null) {
      JsonObject financiallyCovered = getObject(section, "isTheProjectFinanciallyCovered");
      if (isKey(financiallyCovered, "a")) {
        addTextToNewParagraph(document,
          languageMap.getJsonObject("rangeOfAmountOfFinancing").getString(LABEL), true, false);
        addTextToNewParagraphWithTable(document,
          getObjectValue(section, "rangeOfAmountOfFinancing"), false, false);
      }
    }
  }

  protected void section60(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject section, JsonArray datasets) {
    addTextToNewParagraph(document,
      datasets.getJsonObject(0).getJsonObject("result").getJsonObject("country")
        .getString("label", null), true, false);
    addHeaderToTheText(document,
      languageMap.getString("descriptionOfTheDataNeeded") + " " + datasets.getJsonObject(0)
        .getJsonObject("result").getJsonObject("country")
        .getString("label", null),
      "Heading2");
    addTextToNewParagraph(document, languageMap.getString("applicationHDABsForSelectedCountry"),
      true, false);
    writeDatasetsValues(document, datasets, "hdab", "name");
    addTextToNewParagraph(document, languageMap.getString("applicationDatasetsPerCountry"), true,
      false);
    writeDatasetsValues(document, datasets, TITLE, "en");

    // --- HDAB CONTACTS AND DATA LINKAGE ---
    addTextToNewParagraph(document, languageMap.getJsonObject("hdabContacts").getString(LABEL),
      true, false);
    addTextToNewParagraphWithTable(document, getField(section, "hdabContacts"), false, false);

    addTextToNewParagraph(document,
      languageMap.getJsonObject("howWillTheDataFromDifferentSourcesBeLinked").getString(LABEL),
      true, false);
    addTextToNewParagraphWithTable(document,
      getField(section, "howWillTheDataFromDifferentSourcesBeLinked"), false, false);
  }

  protected void getDatasetsValues(RoutingContext context, JsonArray datasets, String lg,
    JsonObject form, String type, String filename) {
    HubSearchClient hubSearchClient = new HubSearchClient(client, config);
    JsonArray hubSearchDataSetValues = new JsonArray();
    List<Future<JsonObject>> futures = new ArrayList<>();
    //Collect all futures for the dataset retrievals
    for (int i = 0; i < datasets.size(); i++) {
      String datasetId = datasets.getJsonObject(i).getString("dataset_id");
      Future<JsonObject> future = hubSearchClient.getDataset(datasetId)
        .onSuccess(hubSearchDataSetValues::add)
        .onFailure(cause -> log.info(cause.getMessage()));
      futures.add(future);
    }
    //Wait until all dataset retrievals are completed
    Future.all(futures).onSuccess(result -> {
      if (result.succeeded()) {
        export(context, lg, form, hubSearchDataSetValues, type, filename);
      } else {
        log.error("One or more dataset retrievals failed.");
        context.response().setStatusCode(500).end();
      }
    });
  }

  private void export(RoutingContext context, String lg, JsonObject form, JsonArray datasetsValues,
    String type, String filename) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      InputStream templateInputStream = getClass().getResourceAsStream(
        "/templates/" + filename)) {
      if (templateInputStream == null) {
        throw new IllegalArgumentException("Template file not found!");
      }
      WordprocessingMLPackage document = WordprocessingMLPackage.load(templateInputStream);
      JsonObject languageMap = translationManager.getTranslationByLanguish(lg);
      //add values
      addFormData(document, languageMap, form, datasetsValues);

      //generate table of context
      generateTableOfContext(document);

      document.getMainDocumentPart()
        .getDocumentSettingsPart().getJaxbElement()
        .setUpdateFields(new BooleanDefaultTrue());

      FieldUpdater fieldUpdater = new FieldUpdater(document);
      fieldUpdater.update(true);

      // update document settings to avoid field updates MS Word prompt
      // 1) Get the settings part (creates one if absent)
      DocumentSettingsPart dsp = document.getMainDocumentPart().getDocumentSettingsPart();
      // 2) Grab or build its JAXB object
      CTSettings settings = dsp.getJaxbElement();
      if (settings == null) {
        settings = new CTSettings();
      }
      // 3) Insert <w:updateFields w:val="false"/>
      BooleanDefaultTrue b = new BooleanDefaultTrue();
      b.setVal(false);
      settings.setUpdateFields(b);
      // 4) Write it back to document settings
      dsp.setJaxbElement(settings);

      document.save(outputStream);
      if ("pdf".equalsIgnoreCase(type)) {
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        // Convert DOCX to PDF logic here
        context.response().putHeader("Content-Disposition", "attachment; filename=Form.pdf")
          .putHeader("Content-Type", "application/pdf")
          .end(Buffer.buffer(pdfOutputStream.toByteArray()));
      } else if ("docx".equalsIgnoreCase(type)) {
        context.response().putHeader("Content-Length", String.valueOf(outputStream.size()))
          .putHeader("Content-Disposition", "attachment; filename=DataAccessForm.docx")
          .putHeader("Content-Type",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
          .end(Buffer.buffer(outputStream.toByteArray()));
      } else {
        context.response().setStatusCode(400).end("Unsupported export type. Use 'pdf' or 'docx'.");
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      context.response().setStatusCode(500).end();
    }
  }

  protected abstract void addFormData(WordprocessingMLPackage document, JsonObject languageMap,
    JsonObject form, JsonArray datasetsValues);

}
