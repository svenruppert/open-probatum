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

package com.svenruppert.openprobatum.catalog;

/**
 * The kind of {@link LearningResource} inside a module (concept §8.3).
 *
 * @since V00.20.00
 */
public enum ResourceType {

  /** Inline article text. */
  ARTICLE(false),

  /** A reference (URL) to an external video. */
  VIDEO_REFERENCE(true),

  /** A downloadable file (URL). */
  DOWNLOAD(true),

  /** A link to external material (URL). */
  EXTERNAL_LINK(true),

  /** An inline checklist (one item per line). */
  CHECKLIST(false);

  private final boolean urlPayload;

  ResourceType(boolean urlPayload) {
    this.urlPayload = urlPayload;
  }

  /** @return {@code true} when this type's payload is a URL rather than inline text. */
  public boolean isUrlPayload() {
    return urlPayload;
  }
}
