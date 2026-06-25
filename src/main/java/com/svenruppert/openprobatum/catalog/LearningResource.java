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

import java.util.Objects;

/**
 * A single piece of learning material inside a module (concept §8.3). The
 * {@code payload} is inline text for {@link ResourceType#ARTICLE}/
 * {@link ResourceType#CHECKLIST} and an {@code http(s)} URL for the link types.
 *
 * @param type    the resource kind
 * @param title   the resource heading
 * @param payload inline text or a URL, per {@link ResourceType#isUrlPayload()}
 * @since V00.20.00
 */
public record LearningResource(ResourceType type, String title, String payload) {

  public LearningResource {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(payload, "payload");
    if (payload.isBlank()) {
      throw new IllegalArgumentException("a learning resource needs a non-blank payload");
    }
    if (type.isUrlPayload() && !(payload.startsWith("http://") || payload.startsWith("https://"))) {
      throw new IllegalArgumentException(type + " payload must be an http(s) URL");
    }
  }

  public static LearningResource article(String title, String text) {
    return new LearningResource(ResourceType.ARTICLE, title, text);
  }

  public static LearningResource checklist(String title, String items) {
    return new LearningResource(ResourceType.CHECKLIST, title, items);
  }

  public static LearningResource video(String title, String url) {
    return new LearningResource(ResourceType.VIDEO_REFERENCE, title, url);
  }

  public static LearningResource download(String title, String url) {
    return new LearningResource(ResourceType.DOWNLOAD, title, url);
  }

  public static LearningResource externalLink(String title, String url) {
    return new LearningResource(ResourceType.EXTERNAL_LINK, title, url);
  }
}
