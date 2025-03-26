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
package eu.europa.data.health.access;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public abstract class UtilityServiceTest {

  public static Object mapJsonToObject(String jsonPath, Class cls) {
    try {
      String jsonString = getStringJsonObject(jsonPath);
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(jsonString, cls);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String getStringJsonObject(String object) {
    String path = "src/test/resources/jsonModel/" + object + ".json";
    try {
      return new String(Files.readAllBytes(Paths.get(path)));
    } catch (IOException e) {
      try {
        throw new IOException("Error importing json test object '" + path + "'.");
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

}
