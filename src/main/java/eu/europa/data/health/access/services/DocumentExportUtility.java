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

import static eu.europa.data.health.access.Constants.EXPORT_DATASET_EXTERNAL_URL;
import static eu.europa.data.health.access.Constants.EXTERNAL_DATASET_ID;
import static eu.europa.data.health.access.Constants.LABEL;
import static eu.europa.data.health.access.Constants.PROVENANCE;
import static eu.europa.data.health.access.Constants.RESULT;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.CTBorder;
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


public  abstract class DocumentExportUtility {

  private final ObjectFactory factory = new ObjectFactory();
  private final JsonObject config;

  protected DocumentExportUtility(JsonObject config) {
    this.config = config;
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
    if (Objects.nonNull(jsonArray) && !jsonArray.isEmpty()) {
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
          if (Objects.nonNull(text)) {
            P paragraph = createStyledParagraph(objectFactory, text, false, false);
            tableCell.getContent().add(paragraph);
            setTableCellBorders(tableCell); // Optional: Add borders to each cell
          }

          // Add the cell to the row
          tableRow.getContent().add(tableCell);

          // Add the row to the table
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
      } else {
        String value = Objects.equals(property1, PROVENANCE) ?
          datasets.getJsonObject(i).getJsonObject(RESULT).getJsonArray(property1)
            .getJsonObject(0)
            .getString(LABEL) :
          datasets.getJsonObject(i).getJsonObject(RESULT).getJsonObject(property1)
            .getString(property2);
        addTextToNewParagraphWithTable(document, value, false, false);
      }
    }
  }

}
