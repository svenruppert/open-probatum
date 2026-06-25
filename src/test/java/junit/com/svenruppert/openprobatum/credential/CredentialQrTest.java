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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.svenruppert.openprobatum.credential.CredentialQr;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CredentialQr — encodes the validation link, decodable back (P008)")
class CredentialQrTest {

  private static String decode(byte[] png) throws Exception {
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
    BinaryBitmap bitmap = new BinaryBitmap(
        new HybridBinarizer(new BufferedImageLuminanceSource(img)));
    Result result = new MultiFormatReader().decode(bitmap);
    return result.getText();
  }

  @Test
  @DisplayName("the PNG decodes back to the exact validation URL")
  void roundTrip() throws Exception {
    String url = "http://host:8080/validate/3f2504e0-4f89-41d3-9a0c-0305e82c3301";
    byte[] png = CredentialQr.pngFor(url);
    assertTrue(png.length > 0);
    assertEquals(url, decode(png));
  }

  @Test
  @DisplayName("the output is a readable PNG of the requested size")
  void isPngOfSize() throws IOException {
    byte[] png = CredentialQr.pngFor("http://x/validate/abc", 180);
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
    assertEquals(180, img.getWidth());
    assertEquals(180, img.getHeight());
  }
}
