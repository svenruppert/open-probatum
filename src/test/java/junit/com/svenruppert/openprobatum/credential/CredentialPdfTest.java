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

package junit.com.svenruppert.openprobatum.credential;

import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialPdf;
import com.svenruppert.openprobatum.credential.CredentialType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CredentialPdf — renders match fields, two dates, no status (P007)")
class CredentialPdfTest {

  private static Credential credential() {
    return Credential.issue("Vaadin Certified", CredentialType.COMPLETION_CERTIFICATE,
        "Alice", "Test Academy", Instant.parse("2026-01-02T00:00:00Z"), null);
  }

  private static String textOf(byte[] pdf) throws IOException {
    try (PDDocument doc = Loader.loadPDF(pdf)) {
      return new PDFTextStripper().getText(doc);
    }
  }

  @Test
  @DisplayName("the PDF carries the match fields, both dates and the validation link")
  void rendersMatchFields() throws IOException {
    Credential c = credential();
    String url = "http://host/validate/" + c.id();
    byte[] pdf = CredentialPdf.render(c, url, null, Instant.parse("2026-06-25T00:00:00Z"));

    assertTrue(pdf.length > 0, "a non-empty PDF must be produced");
    String text = textOf(pdf);
    assertTrue(text.contains("Alice"), "recipient");
    assertTrue(text.contains("Vaadin Certified"), "title");
    assertTrue(text.contains("Test Academy"), "issuer");
    assertTrue(text.contains("2026-01-02"), "issued date");
    assertTrue(text.contains("2026-06-25"), "PDF generated date (separate from issued)");
    assertTrue(text.contains(c.id().toString()), "credential id");
    assertTrue(text.contains(url), "validation link");
  }

  @Test
  @DisplayName("the PDF prints NO status field (concept §10.7)")
  void printsNoStatus() throws IOException {
    byte[] pdf = CredentialPdf.render(credential(), "http://host/validate/x", null, Instant.now());
    assertFalse(textOf(pdf).contains("Status"),
        "a printed status would go stale while the PDF circulates");
  }

  @Test
  @DisplayName("rendering does not mutate the credential record")
  void renderDoesNotMutate() {
    Credential c = credential();
    Credential snapshot = new Credential(c.id(), c.title(), c.type(), c.recipientName(),
        c.issuer(), c.issuedAt(), c.expiresAt(), c.status(), c.supersededBy(),
        c.recipientId(), c.evidence());
    CredentialPdf.render(c, "http://host/validate/x", null, Instant.now());
    CredentialPdf.render(c, "http://host/validate/x", null, Instant.now());
    assertEquals(snapshot, c, "the record is untouched by PDF generation");
  }
}
