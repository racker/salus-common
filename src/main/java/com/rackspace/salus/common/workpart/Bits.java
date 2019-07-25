package com.rackspace.salus.common.workpart;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchEvent.EventType;
import java.nio.charset.StandardCharsets;

public class Bits {
  public static final String REGISTRY_SET = "registry/";
  public static final String ACTIVE_SET = "active/";
  public static final String WORKERS_SET = "workers/";

  /**
   * This format is zero-padded to ensure that the work load values are textually sortable
   * when stored as values in etcd.
   */
  public static final String WORK_LOAD_FORMAT = "%010d";

  public static ByteSequence fromString(String utf8string) {
    return ByteSequence.from(utf8string, StandardCharsets.UTF_8);
  }

  public static ByteSequence fromFormat(String format, Object... args) {
    return fromString(String.format(format, args));
  }

  public static String extractIdFromKey(KeyValue kv) {
    final String key = kv.getKey().toString(StandardCharsets.UTF_8);
    final int pos = key.lastIndexOf('/');
    return key.substring(pos + 1);
  }

  public static boolean isNewKeyEvent(WatchEvent event) {
    return event.getEventType() == EventType.PUT
        && event.getKeyValue().getVersion() == 1;
  }

  public static boolean isUpdateKeyEvent(WatchEvent event) {
    return event.getEventType() == EventType.PUT
        && event.getKeyValue().getVersion() > 1;
  }

  public static boolean isDeleteKeyEvent(WatchEvent event) {
    return event.getEventType() == EventType.DELETE;
  }
}
