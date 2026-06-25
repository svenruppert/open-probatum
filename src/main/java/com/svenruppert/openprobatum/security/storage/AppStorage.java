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

import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.security.model.StoredUser;
import com.svenruppert.jsentinel.persistence.eclipsestore.EclipseStoreJSentinelStorage;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStorageFactory;
import com.svenruppert.jsentinel.persistence.eclipsestore.JSentinelStoragePair;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single Eclipse-Store lifecycle for the whole application (jSentinel
 * 00.75.20 {@link JSentinelStoragePair}). One storage, one shutdown hook, one
 * atomic two-phase close — replacing the former three parallel stores (framework
 * + user directory + credential repository).
 *
 * <ul>
 *   <li>{@link #framework()} — the jSentinel framework stores (audit, session,
 *       version, login-attempt, …).</li>
 *   <li>{@link #app()} — the application's own {@link EmbeddedStorageManager},
 *       rooted at the shared {@link AppRoot} (user map + credential map).</li>
 * </ul>
 *
 * <p>The pair opens lazily on first use (so class-loading touches no disk) and
 * its {@code close()} is registered exactly once as a JVM shutdown hook. Tests
 * install an isolated pair via {@link #setPair(JSentinelStoragePair)} (e.g.
 * {@code JSentinelStorageFactory.openAt(tempDir)}) and clear it with
 * {@link #reset()}.
 *
 * @since V00.10.00
 */
public final class AppStorage {

  private static volatile JSentinelStoragePair override;

  private AppStorage() {
  }

  private static final class Holder {
    static final JSentinelStoragePair PAIR = open();
  }

  private static JSentinelStoragePair open() {
    JSentinelStoragePair pair = JSentinelStorageFactory.openAt(AppStoragePaths.baseDir());
    // Restrict the whole storage tree to owner-only on POSIX (R02): a 0700 base
    // directory blocks traversal into the framework + app stores beneath it.
    AppStoragePaths.ensureSecureDir(AppStoragePaths.baseDir());
    // ONE shutdown hook — pair.close() is two-phase (app first, framework
    // always) and idempotent.
    Runtime.getRuntime().addShutdownHook(new Thread(pair::close, "jsentinel-pair-close"));
    return pair;
  }

  /** The active storage pair — a test override if installed, else the singleton. */
  public static JSentinelStoragePair pair() {
    JSentinelStoragePair swap = override;
    return swap != null ? swap : Holder.PAIR;
  }

  /** The jSentinel framework storage (audit / session / version / … stores). */
  public static EclipseStoreJSentinelStorage framework() {
    return pair().framework();
  }

  /** The application's own Eclipse-Store manager. */
  public static EmbeddedStorageManager app() {
    return pair().app();
  }

  /**
   * The shared application root (user + credential maps), get-or-created on the
   * app store. The first call on a fresh store sets and persists a new root.
   */
  public static synchronized AppRoot appRoot() {
    EmbeddedStorageManager app = app();
    Object root = app.root();
    if (root instanceof AppRoot existing) {
      return existing;
    }
    AppRoot fresh = new AppRoot();
    app.setRoot(fresh);
    app.storeRoot();
    return fresh;
  }

  /** Test seam — install an isolated storage pair. */
  public static void setPair(JSentinelStoragePair replacement) {
    override = replacement;
  }

  /** Clears any test override. */
  public static void reset() {
    override = null;
  }

  /**
   * Eclipse-Store root for the application's domain data. Public so Eclipse-Store
   * can map it; holds the user directory and the credential store side by side
   * in the one app store.
   */
  public static final class AppRoot {
    public final Map<String, StoredUser> users = new ConcurrentHashMap<>();
    public final Map<UUID, Credential> credentials = new ConcurrentHashMap<>();
    public final Map<UUID, com.svenruppert.openprobatum.catalog.Offering> offerings =
        new ConcurrentHashMap<>();
  }
}
