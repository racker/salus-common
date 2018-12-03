package me.itzg.tryetcdworkpart;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchEvent.EventType;

public class Bits {
  public static final String REGISTRY_SET = "registry/";
  public static final String ACTIVE_SET = "active/";
  public static final String WORKERS_SET = "workers/";

  /**
   * This format is zero-padded to ensure that the work load values are textually sortable
   * when stored as values in etcd.
   */
  public static final String WORK_LOAD_FORMAT = "%010d";

  public static ByteSequence fromFormat(String format, Object... args) {
    return ByteSequence.fromString(String.format(format, args));
  }

  public static String extractIdFromKey(KeyValue kv) {
    final String key = kv.getKey().toStringUtf8();
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
