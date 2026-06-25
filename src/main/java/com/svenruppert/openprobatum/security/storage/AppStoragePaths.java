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

package com.svenruppert.openprobatum.security.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * Single source of truth for the application's on-disk storage paths.
 *
 * <p>Production runs use {@code ./data/} as the base. Tests, CI and
 * any deployment that wants to redirect persistence elsewhere
 * override the base via the JVM system property
 * {@code -Dapp.storage.dir=/some/path}.
 *
 * <p>Derived locations:
 * <ul>
 *   <li>{@link #frameworkStorageDir()} — the {@code jsentinel} subtree under the
 *       base, securing the one-time bootstrap token. Since jSentinel 00.75.20 the
 *       framework + application Eclipse-Stores are one {@code JSentinelStoragePair}
 *       opened at {@link #baseDir()} (see {@code AppStorage}); no per-store
 *       directory policy lives here any more.</li>
 *   <li>{@link #bootstrapTokenFile()} — the one-time bootstrap token
 *       written by {@code BootstrapStartup} on first start.</li>
 * </ul>
 *
 * <p>This file is deliberately tiny — keeping path policy in one
 * place avoids drift when redirecting storage at runtime. Surefire
 * sets {@code app.storage.dir = ${project.build.directory}/test-data}
 * via {@code <systemPropertyVariables>}, so test runs never touch
 * the repository-rooted {@code ./data/} directory.
 *
 * <p>The base is resolved to an <em>absolute, normalised</em> path so
 * {@code ..} segments and the process working directory can no longer
 * move the credential store to an unexpected location. The
 * {@link #ensureSecureDir(Path)} / {@link #secureFile(Path)} helpers
 * restrict the credential and token stores to owner-only access on
 * POSIX file systems.
 */
public final class AppStoragePaths {

  /** System-property name for the storage base directory. */
  public static final String PROPERTY = "app.storage.dir";

  /** Built-in default when nothing was configured. */
  public static final String DEFAULT = "./data";

  private static final boolean POSIX =
      FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

  /** {@code rwx------} (0700) — owner-only directory. */
  private static final Set<PosixFilePermission> DIR_0700 = EnumSet.of(
      PosixFilePermission.OWNER_READ,
      PosixFilePermission.OWNER_WRITE,
      PosixFilePermission.OWNER_EXECUTE);

  /** {@code rw-------} (0600) — owner-only file. */
  private static final Set<PosixFilePermission> FILE_0600 = EnumSet.of(
      PosixFilePermission.OWNER_READ,
      PosixFilePermission.OWNER_WRITE);

  private AppStoragePaths() {
  }

  /**
   * Base directory for all app-owned storage, resolved to an absolute,
   * normalised path.
   */
  public static Path baseDir() {
    return Path.of(System.getProperty(PROPERTY, DEFAULT))
        .toAbsolutePath()
        .normalize();
  }

  /** jSentinel framework storage — audit + session stores. */
  public static Path frameworkStorageDir() {
    return baseDir().resolve("jsentinel");
  }

  /** Bootstrap token file written on first start. */
  public static Path bootstrapTokenFile() {
    return frameworkStorageDir().resolve("bootstrap.token");
  }

  /**
   * Creates {@code dir} (and any missing parents) and restricts it to
   * owner-only access ({@code 0700}) on POSIX file systems, so the
   * credential and token stores beneath it are not world-readable. The
   * permission step is a no-op on non-POSIX systems (e.g. Windows),
   * where ACLs govern access instead. Returns {@code dir} for chaining.
   */
  public static Path ensureSecureDir(Path dir) {
    try {
      Files.createDirectories(dir);
      if (POSIX) {
        Files.setPosixFilePermissions(dir, DIR_0700);
      }
      return dir;
    } catch (IOException e) {
      throw new UncheckedIOException("Could not create secure directory " + dir, e);
    }
  }

  /**
   * Restricts an existing file to owner read/write ({@code 0600}) on
   * POSIX file systems. No-op if the file does not exist yet or on
   * non-POSIX systems.
   */
  public static void secureFile(Path file) {
    try {
      if (POSIX && Files.exists(file)) {
        Files.setPosixFilePermissions(file, FILE_0600);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not secure file " + file, e);
    }
  }
}
