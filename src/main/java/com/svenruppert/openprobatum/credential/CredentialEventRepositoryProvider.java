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

import java.util.Objects;

/**
 * Lazy holder for the application's {@link CredentialEventRepository}, mirroring
 * {@link CredentialRepositoryProvider}. The Initialization-on-Demand Holder idiom
 * keeps Eclipse-Store untouched at class-load: the file-backed repository is
 * built only when {@link #repository()} is first reached <em>and</em> no test
 * override was installed via {@link #setRepository(CredentialEventRepository)}.
 *
 * @since V00.30.00
 */
public final class CredentialEventRepositoryProvider {

  private static volatile CredentialEventRepository override;

  private CredentialEventRepositoryProvider() {
  }

  private static final class Holder {
    static final CredentialEventRepository INSTANCE = new EclipseStoreCredentialEventRepository();
  }

  /** The active repository — a test override if installed, else the file-backed singleton. */
  public static CredentialEventRepository repository() {
    CredentialEventRepository swap = override;
    return swap != null ? swap : Holder.INSTANCE;
  }

  public static void setRepository(CredentialEventRepository replacement) {
    override = Objects.requireNonNull(replacement, "replacement");
  }

  /** Clears any test override. */
  public static void reset() {
    override = null;
  }
}
