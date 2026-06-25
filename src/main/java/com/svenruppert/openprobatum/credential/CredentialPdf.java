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

package com.svenruppert.openprobatum.credential;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Renders a credential as a human-readable PDF (concept §10.7). The PDF is only
 * a <em>rendering</em> of the record — generating it never changes the
 * credential. It separates the business issue date ("Issued on") from the PDF
 * generation date ("PDF generated on"), shows the match-rule and validation
 * link, and deliberately prints <strong>no status</strong>: a printed "valid"
 * would go stale the moment the credential is revoked while the PDF circulates.
 *
 * @since V00.10.00
 */
public final class CredentialPdf {

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  private CredentialPdf() {
  }

  /**
   * Renders {@code credential} to PDF bytes.
   *
   * @param credential    the record to render (never mutated)
   * @param validationUrl the public validation link shown on the document
   * @param qrPng         optional QR-code PNG (validation link); may be {@code null}
   * @param generatedAt   the PDF generation timestamp
   */
  public static byte[] render(Credential credential, String validationUrl,
                              byte[] qrPng, Instant generatedAt) {
    Objects.requireNonNull(credential, "credential");
    Objects.requireNonNull(validationUrl, "validationUrl");
    Objects.requireNonNull(generatedAt, "generatedAt");

    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);
      PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      PDType1Font body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

      try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
        float x = 60;
        float y = 760;
        line(cs, bold, 22, x, y, "Certificate");
        y -= 46;
        line(cs, body, 14, x, y, "Awarded to: " + credential.recipientName());
        y -= 26;
        line(cs, bold, 16, x, y, credential.title());
        y -= 26;
        line(cs, body, 12, x, y, "Type: " + credential.type().name());
        y -= 20;
        line(cs, body, 12, x, y, "Issuer: " + credential.issuer());
        y -= 20;
        line(cs, body, 12, x, y, "Issued on: " + DATE.format(credential.issuedAt()));
        y -= 20;
        line(cs, body, 12, x, y, "PDF generated on: " + DATE.format(generatedAt));
        y -= 20;
        if (credential.expiry().isPresent()) {
          line(cs, body, 12, x, y, "Expires on: " + DATE.format(credential.expiry().get()));
          y -= 20;
        }
        line(cs, body, 12, x, y, "Credential ID: " + credential.id());
        y -= 34;
        line(cs, body, 10, x, y, "Genuine only if these details match the official record at:");
        y -= 14;
        line(cs, body, 10, x, y, validationUrl);
        y -= 24;

        if (qrPng != null && qrPng.length > 0) {
          PDImageXObject qr = PDImageXObject.createFromByteArray(doc, qrPng, "qr");
          cs.drawImage(qr, x, y - 120, 120, 120);
        }
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to render credential PDF", e);
    }
  }

  private static void line(PDPageContentStream cs, PDType1Font font, float size,
                           float x, float y, String text) throws IOException {
    cs.beginText();
    cs.setFont(font, size);
    cs.newLineAtOffset(x, y);
    cs.showText(text == null ? "" : text);
    cs.endText();
  }
}
