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

package junit.com.svenruppert.openprobatum.catalog;

import com.svenruppert.openprobatum.catalog.LearningResource;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("LearningResource — typed module content (P007)")
class LearningResourceTest {

  @Test
  @DisplayName("inline types carry text; link types carry a URL")
  void factoriesSetType() {
    assertEquals(ResourceType.ARTICLE, LearningResource.article("A", "body text").type());
    assertEquals(ResourceType.CHECKLIST, LearningResource.checklist("C", "one\ntwo").type());
    assertEquals(ResourceType.VIDEO_REFERENCE,
        LearningResource.video("V", "https://youtu.be/x").type());
    assertEquals(ResourceType.DOWNLOAD,
        LearningResource.download("D", "https://files/x.pdf").type());
    assertEquals(ResourceType.EXTERNAL_LINK,
        LearningResource.externalLink("L", "http://example.org").type());
  }

  @Test
  @DisplayName("a URL-payload type rejects a non-URL payload")
  void urlTypesValidatePayload() {
    assertThrows(IllegalArgumentException.class,
        () -> LearningResource.video("V", "not-a-url"));
    assertThrows(IllegalArgumentException.class,
        () -> LearningResource.download("D", "ftp://nope"));
  }

  @Test
  @DisplayName("a blank payload and nulls are rejected")
  void blankAndNullRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> LearningResource.article("A", "   "));
    assertThrows(NullPointerException.class,
        () -> LearningResource.article(null, "x"));
  }

  @Test
  @DisplayName("a module carries its resources immutably")
  void moduleResourcesAreImmutable() {
    Module m = Module.mandatory("Routing", "intro",
        List.of(LearningResource.article("Read", "text"),
            LearningResource.video("Watch", "https://youtu.be/x")));
    assertEquals(2, m.resources().size());
    assertThrows(UnsupportedOperationException.class,
        () -> m.resources().add(LearningResource.article("X", "y")));
    assertEquals(List.of(), Module.mandatory("Bare", "c").resources());
  }
}
