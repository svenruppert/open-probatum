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

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.openprobatum.security.storage.AppStoragePaths;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production {@link CredentialRepository}. Holds the credential map in its own
 * Eclipse-Store instance under {@code ./data/app/credentials}, separate from
 * the user directory and the jSentinel framework storage so each can be backed
 * up / reset independently. Only the credential <em>record</em> is stored — a
 * PDF is never persisted (concept §3.2, §10.7).
 *
 * <p>Mirrors the user-directory persistence: the storage directory is created
 * owner-only before opening; the JVM shutdown hook is registered once; and
 * {@link #close()} nulls the manager so the same instance can reopen.
 *
 * @since V00.10.00
 */
public final class EclipseStoreCredentialRepository
    implements CredentialRepository, HasLogger {

  /** Default credential storage, honouring {@link AppStoragePaths#PROPERTY}. */
  public static final Path DEFAULT_STORAGE_DIR =
      AppStoragePaths.credentialDirectoryDir();

  private final Path storageDir;
  private final AtomicBoolean hookRegistered = new AtomicBoolean();
  private volatile EmbeddedStorageManager manager;
  private volatile CredentialsRoot root;

  public EclipseStoreCredentialRepository() {
    this(DEFAULT_STORAGE_DIR);
  }

  public EclipseStoreCredentialRepository(Path storageDir) {
    this.storageDir = Objects.requireNonNull(storageDir, "storageDir");
  }

  @Override
  public synchronized void save(Credential credential) {
    Objects.requireNonNull(credential, "credential");
    ensureOpen();
    root.credentials.put(credential.id(), credential);
    manager.store(root.credentials);
    logger().debug("EclipseStoreCredentialRepository: persisted credential {}", credential.id());
  }

  @Override
  public synchronized Optional<Credential> findById(UUID id) {
    ensureOpen();
    return Optional.ofNullable(root.credentials.get(id));
  }

  @Override
  public synchronized Collection<Credential> all() {
    ensureOpen();
    return new ArrayList<>(root.credentials.values());
  }

  @Override
  public synchronized void close() {
    EmbeddedStorageManager local = manager;
    if (local == null) {
      return; // not open or already closed — idempotent
    }
    manager = null;
    root = null;
    try {
      local.shutdown();
      logger().info("EclipseStoreCredentialRepository: storage at {} closed", storageDir);
    } catch (RuntimeException e) {
      logger().warn("EclipseStoreCredentialRepository: shutdown failed for {}", storageDir, e);
    }
  }

  private void ensureOpen() {
    if (manager != null) {
      return;
    }
    AppStoragePaths.ensureSecureDir(storageDir);
    CredentialsRoot initial = new CredentialsRoot();
    manager = EmbeddedStorage.start(initial, storageDir);
    Object loaded = manager.root();
    if (loaded instanceof CredentialsRoot existing) {
      root = existing;
      logger().info("EclipseStoreCredentialRepository: opened {} ({} credentials present)",
          storageDir, root.credentials.size());
    } else if (loaded == null) {
      root = initial;
      manager.setRoot(root);
      manager.storeRoot();
      logger().info("EclipseStoreCredentialRepository: bootstrapped fresh storage at {}", storageDir);
    } else {
      EmbeddedStorageManager bad = manager;
      manager = null;
      bad.shutdown();
      throw new IllegalStateException(
          "Unexpected root type in " + storageDir + ": " + loaded.getClass().getName()
              + " — expected " + CredentialsRoot.class.getName());
    }
    if (hookRegistered.compareAndSet(false, true)) {
      Runtime.getRuntime().addShutdownHook(new Thread(this::close,
          "app-credentials-storage-shutdown"));
    }
  }

  /** Eclipse-Store root container. Public type so EclipseStore can map it. */
  public static final class CredentialsRoot {
    public final Map<UUID, Credential> credentials = new ConcurrentHashMap<>();
  }
}
