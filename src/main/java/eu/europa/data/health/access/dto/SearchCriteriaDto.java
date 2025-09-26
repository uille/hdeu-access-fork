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
package eu.europa.data.health.access.dto;

import lombok.Getter;

@Getter
public class SearchCriteriaDto {


  private final Integer pageCount;
  private final Integer pageCountFilter;
  private final Integer pageNumber;
  private final Integer pageLimit;
  private final String sortBy;
  private final String sortOrder;
  private final String status;

  public SearchCriteriaDto(Integer pageCount, Integer pageCountFilter, Integer pageNumber,
    Integer pageLimit,String sortBy, String sortOrder, String status) {
    this.pageCount = pageCount;
    this.pageCountFilter = pageCountFilter;
    this.pageNumber = pageNumber;
    this.pageLimit = pageLimit;
    this.sortBy = sortBy;
    this.sortOrder = sortOrder;
    this.status = status;
  }

}
