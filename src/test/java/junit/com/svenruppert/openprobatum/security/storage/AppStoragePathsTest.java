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

package junit.com.svenruppert.openprobatum.security.storage;

import com.svenruppert.openprobatum.security.storage.AppStoragePaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("AppStoragePaths — normalised base + owner-only permissions")
class AppStoragePathsTest {

  private static final boolean POSIX =
      FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

  private String previous;

  @AfterEach
  void restore() {
    if (previous == null) {
      System.clearProperty(AppStoragePaths.PROPERTY);
    } else {
      System.setProperty(AppStoragePaths.PROPERTY, previous);
    }
  }

  @Test
  @DisplayName("baseDir() is absolute and normalised (.. segments collapsed)")
  void baseDirIsNormalised(@TempDir Path tempDir) {
    previous = System.getProperty(AppStoragePaths.PROPERTY);
    Path messy = tempDir.resolve("a").resolve("..").resolve("data");
    System.setProperty(AppStoragePaths.PROPERTY, messy.toString());

    Path base = AppStoragePaths.baseDir();

    assertTrue(base.isAbsolute(), "base must be absolute");
    assertEquals(tempDir.resolve("data").toAbsolutePath().normalize(), base);
    assertFalse(base.toString().contains(".."), "no .. segments must remain");
  }

  @Test
  @DisplayName("ensureSecureDir creates the directory with 0700 on POSIX")
  void secureDirIsOwnerOnly(@TempDir Path tempDir) throws IOException {
    Path created = AppStoragePaths.ensureSecureDir(tempDir.resolve("secure-dir"));

    assertTrue(Files.isDirectory(created));
    assumeTrue(POSIX, "POSIX permission assertions only apply to POSIX file systems");
    assertEquals(
        Set.of(PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE),
        Files.getPosixFilePermissions(created));
  }

  @Test
  @DisplayName("secureFile restricts an existing file to 0600 on POSIX")
  void secureFileIsOwnerOnly(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("secret.token");
    Files.writeString(file, "s3cr3t");
    AppStoragePaths.secureFile(file);

    assumeTrue(POSIX, "POSIX permission assertions only apply to POSIX file systems");
    assertEquals(
        Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
        Files.getPosixFilePermissions(file));
  }

  @Test
  @DisplayName("secureFile is a no-op when the file does not exist")
  void secureFileMissingIsNoop(@TempDir Path tempDir) {
    AppStoragePaths.secureFile(tempDir.resolve("nope.token")); // must not throw
  }
}
