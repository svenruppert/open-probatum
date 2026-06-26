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

package com.svenruppert.openprobatum.content;

import java.util.Objects;

/**
 * Lazy holder for the application's {@link ContentAuthorship} registry, mirroring
 * the other repository providers. The Initialization-on-Demand Holder idiom keeps
 * Eclipse-Store untouched at class-load until {@link #registry()} is first reached
 * and no test override was installed via {@link #setRegistry(ContentAuthorship)}.
 *
 * @since V00.30.00
 */
public final class ContentAuthorshipProvider {

  private static volatile ContentAuthorship override;

  private ContentAuthorshipProvider() {
  }

  private static final class Holder {
    static final ContentAuthorship INSTANCE = new EclipseStoreContentAuthorship();
  }

  /** The active registry — a test override if installed, else the file-backed singleton. */
  public static ContentAuthorship registry() {
    ContentAuthorship swap = override;
    return swap != null ? swap : Holder.INSTANCE;
  }

  public static void setRegistry(ContentAuthorship replacement) {
    override = Objects.requireNonNull(replacement, "replacement");
  }

  /** Clears any test override. */
  public static void reset() {
    override = null;
  }
}
