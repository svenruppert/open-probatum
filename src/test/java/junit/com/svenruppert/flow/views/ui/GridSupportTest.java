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

package junit.com.svenruppert.flow.views.ui;

import com.svenruppert.flow.views.ui.GridSupport;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GridSupport — shared admin-grid helpers")
class GridSupportTest {

  @Test
  @DisplayName("maskId truncates a long id to first 8 chars + ellipsis (R27)")
  void maskTruncatesLongId() {
    assertEquals("abcdefgh…", GridSupport.maskId("abcdefghijklmnop"));
  }

  @Test
  @DisplayName("maskId keeps an id of 8 chars or fewer verbatim")
  void maskKeepsShortId() {
    assertEquals("short", GridSupport.maskId("short"));
    assertEquals("eightchr", GridSupport.maskId("eightchr"));
  }

  @Test
  @DisplayName("maskId renders a dash for null/blank")
  void maskNullOrBlankIsDash() {
    assertEquals("—", GridSupport.maskId(null));
    assertEquals("—", GridSupport.maskId("   "));
  }

  @Test
  @DisplayName("textValue trims and lower-cases the field value")
  void textValueTrimsAndLowercases() {
    TextField f = new TextField();
    f.setValue("  Hello World  ");
    assertEquals("hello world", GridSupport.textValue(f));
  }

  @Test
  @DisplayName("textValue of an empty field is the empty string")
  void textValueEmptyFieldIsEmpty() {
    assertEquals("", GridSupport.textValue(new TextField()));
  }
}
