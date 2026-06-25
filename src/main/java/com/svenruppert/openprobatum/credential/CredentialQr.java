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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Generates the QR code embedded in the certificate PDF (concept §10.7, §11.2)
 * — it encodes the public validation link so a verifier can scan straight to
 * the validation page. The QR is a pointer, never a proof (§10.8).
 *
 * @since V00.10.00
 */
public final class CredentialQr {

  /** Default QR edge length in pixels. */
  public static final int DEFAULT_SIZE = 240;

  private CredentialQr() {
  }

  /** PNG QR of {@code content} at {@link #DEFAULT_SIZE}. */
  public static byte[] pngFor(String content) {
    return pngFor(content, DEFAULT_SIZE);
  }

  /** PNG QR of {@code content} at {@code size} x {@code size} pixels. */
  public static byte[] pngFor(String content, int size) {
    Objects.requireNonNull(content, "content");
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    hints.put(EncodeHintType.MARGIN, 1);
    try {
      BitMatrix matrix = new QRCodeWriter()
          .encode(content, BarcodeFormat.QR_CODE, size, size, hints);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return out.toByteArray();
    } catch (WriterException e) {
      throw new IllegalStateException("Could not encode QR for: " + content, e);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write QR PNG", e);
    }
  }
}
