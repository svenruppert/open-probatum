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

package com.svenruppert.openprobatum.security.bootstrap;

import com.svenruppert.openprobatum.security.storage.AppStorage;
import com.svenruppert.jsentinel.persistence.eclipsestore.EclipseStoreJSentinelStorage;

/**
 * Exposes the jSentinel framework storage to the bootstrap / session / version
 * wiring. Since jSentinel 00.75.20 the framework store is one half of the single
 * {@link AppStorage} pair (the other half being the application's own store), so
 * this no longer opens a second Eclipse-Store or registers its own shutdown hook
 * — it simply forwards to {@link AppStorage#framework()}.
 *
 * <p>The {@link #setStorage(EclipseStoreJSentinelStorage)} seam still lets a test
 * stub the framework storage independently of the app store.
 */
public final class JSentinelStorageProvider {

  private static volatile EclipseStoreJSentinelStorage override;

  private JSentinelStorageProvider() {
  }

  public static EclipseStoreJSentinelStorage storage() {
    EclipseStoreJSentinelStorage local = override;
    return local != null ? local : AppStorage.framework();
  }

  /** Test seam — install a custom framework storage instance. */
  public static synchronized void setStorage(EclipseStoreJSentinelStorage replacement) {
    override = replacement;
  }
}
