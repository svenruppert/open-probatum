/*
 * Copyright © 2013 Sven Ruppert (sven.ruppert@gmail.com)
 *
 * Licensed under the EUPL, Version 1.2 (the "Licence");
 * you may not use this file except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package com.svenruppert.flow.views.ui;

import com.vaadin.flow.component.textfield.TextField;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

/**
 * Small helpers shared by the admin grid views (audit, sessions, roles)
 * so the filter-normalisation, timestamp formatting and id-masking are
 * defined once instead of copy-pasted per view.
 */
public final class GridSupport {

  /** {@code yyyy-MM-dd HH:mm:ss} in the system zone — the admin-grid timestamp format. */
  public static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  private GridSupport() {
  }

  /** Trimmed, lower-cased filter value; empty string for a null/blank field. */
  public static String textValue(TextField field) {
    String v = field.getValue();
    return v == null ? "" : v.trim().toLowerCase();
  }

  /**
   * Masks an identifier for grid display — first 8 characters plus an
   * ellipsis — so a full session id is not exposed to a shoulder-surfer
   * or a screenshot. Returns a dash for a null/blank id.
   */
  public static String maskId(String id) {
    if (id == null || id.isBlank()) {
      return "—";
    }
    return id.length() <= 8 ? id : id.substring(0, 8) + "…";
  }
}
