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

package com.svenruppert.openprobatum.security.model;

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.openprobatum.security.storage.AppStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * Production-default {@link UserDirectoryPersistence}. The user map lives in the
 * single shared application Eclipse-Store ({@link AppStorage#app()}), rooted at
 * {@link AppStorage.AppRoot#users} — no second store, no own manager and no own
 * shutdown hook (the {@link AppStorage} pair owns the lifecycle, jSentinel
 * 00.75.20). Only the record is stored; Eclipse-Store maps {@link AppUser} via
 * reflection, so no {@code Serializable} and no JDK serialisation is involved.
 */
public final class EclipseStoreUserDirectoryPersistence
    implements UserDirectoryPersistence, HasLogger {

  @Override
  public synchronized Map<String, StoredUser> load() {
    return new HashMap<>(AppStorage.appRoot().users);
  }

  @Override
  public synchronized void save(Map<String, StoredUser> snapshot) {
    Map<String, StoredUser> users = AppStorage.appRoot().users;
    users.clear();
    users.putAll(snapshot);
    AppStorage.app().store(users);
    logger().debug("EclipseStoreUserDirectoryPersistence: persisted {} users", users.size());
  }
}
