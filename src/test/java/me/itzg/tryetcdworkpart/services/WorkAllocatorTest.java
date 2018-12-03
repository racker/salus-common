package me.itzg.tryetcdworkpart.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.TxnResponse;
import com.coreos.jetcd.op.Cmp;
import com.coreos.jetcd.op.CmpTarget;
import com.coreos.jetcd.op.Op;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import io.etcd.jetcd.launcher.junit.EtcdClusterResource;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import me.itzg.tryetcdworkpart.Bits;
import me.itzg.tryetcdworkpart.Work;
import me.itzg.tryetcdworkpart.WorkProcessor;
import me.itzg.tryetcdworkpart.config.WorkerProperties;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
public class WorkAllocatorTest {

  private static final int TIMEOUT = 5000;

  @Rule
  public final EtcdClusterResource etcd = new EtcdClusterResource("test-etcd", 1);

  @Rule
  public TestName testName = new TestName();

  private ThreadPoolTaskScheduler taskExecutor;
  private WorkerProperties workerProperties;
  private Client client;

  private List<WorkAllocator> workAllocatorsInTest;

  @Before
  public void setUp() throws Exception {
    taskExecutor = new ThreadPoolTaskScheduler();
    taskExecutor.setPoolSize(Integer.MAX_VALUE);
    taskExecutor.setThreadNamePrefix("watchers-");
    taskExecutor.initialize();

    workerProperties = new WorkerProperties();
    workerProperties.setPrefix("/"+testName.getMethodName()+"/");

    final List<String> endpoints = etcd.cluster().getClientEndpoints().stream()
        .map(URI::toString)
        .collect(Collectors.toList());
    client = Client.builder().endpoints(endpoints).build();

    workAllocatorsInTest = new ArrayList<>();
  }

  @After
  public void tearDown() throws Exception {
    log.info("Stopping work allocators");
    final Semaphore stopSem = new Semaphore(0);

    workAllocatorsInTest.forEach(workAllocator -> workAllocator.stop(() -> stopSem.release()));
    try {
      stopSem.tryAcquire(workAllocatorsInTest.size(), 5, TimeUnit.SECONDS);
    } finally {
      taskExecutor.shutdown();
      client.close();
    }
  }

  private WorkAllocator register(WorkAllocator workAllocator) {
    workAllocatorsInTest.add(workAllocator);
    return workAllocator;
  }

  @Test
  public void testSingleWorkItemSingleWorker() throws InterruptedException {
    final BulkWorkProcessor workProcessor = new BulkWorkProcessor(1);
    final WorkAllocator workAllocator = register(
        new WorkAllocator(
        workerProperties, client, workProcessor, taskExecutor)
    );
    workAllocator.start();

    workAllocator.createWork("1")
    .join();

    workProcessor.hasActiveWorkItems(1, TIMEOUT);
  }

  @Test
  public void testOneWorkerExistingItems() throws InterruptedException, ExecutionException {
    final int totalWorkItems = 5;

    final BulkWorkProcessor bulkWorkProcessor = new BulkWorkProcessor(totalWorkItems);

    final WorkAllocator workAllocator = register(
        new WorkAllocator(
        workerProperties, client, bulkWorkProcessor, taskExecutor)
    );

    for (int i = 0; i < totalWorkItems; i++) {
      workAllocator.createWork(String.format("%d", i));
    }

    workAllocator.start();

    bulkWorkProcessor.hasActiveWorkItems(totalWorkItems, 2000);

    assertWorkLoad(totalWorkItems, workAllocator.getId());
  }

  @Test
  public void testOneWorkerWithItemDeletion() throws InterruptedException, ExecutionException {
    final int totalWorkItems = 5;

    final BulkWorkProcessor bulkWorkProcessor = new BulkWorkProcessor(totalWorkItems);

    final WorkAllocator workAllocator = register(
        new WorkAllocator(
        workerProperties, client, bulkWorkProcessor, taskExecutor)
    );

    final List<Work> createdWork = new ArrayList<>();
    for (int i = 0; i < totalWorkItems; i++) {
      createdWork.add(
        workAllocator.createWork(String.format("%d", i)).get()
      );
    }

    workAllocator.start();

    bulkWorkProcessor.hasActiveWorkItems(totalWorkItems, 2000);
    assertWorkLoad(totalWorkItems, workAllocator.getId());

    workAllocator.deleteWork(createdWork.get(0).getId());
    bulkWorkProcessor.hasActiveWorkItems(totalWorkItems-1, 2000);
    assertWorkLoad(totalWorkItems-1, workAllocator.getId());

  }

  @Test
  public void testOneWorkerJoinedByAnother() throws ExecutionException, InterruptedException {
    final int totalWorkItems = 6;

    // rebalance delay needs to be comfortably within the hasActiveWorkItems timeouts used below
    workerProperties.setRebalanceDelay(Duration.ofMillis(500));

    final BulkWorkProcessor workProcessor1 = new BulkWorkProcessor(totalWorkItems);
    final WorkAllocator workAllocator1 = register(
        new WorkAllocator(
        workerProperties, client, workProcessor1, taskExecutor)
    );

    final BulkWorkProcessor workProcessor2 = new BulkWorkProcessor(totalWorkItems);
    final WorkAllocator workAllocator2 = register(
        new WorkAllocator(
        workerProperties, client, workProcessor2, taskExecutor)
    );

    final List<Work> createdWork = new ArrayList<>();
    for (int i = 0; i < totalWorkItems; i++) {
      createdWork.add(
          workAllocator1.createWork(String.format("%d", i)).get()
      );
    }

    workAllocator1.start();
    workProcessor1.hasActiveWorkItems(totalWorkItems, TIMEOUT);

    log.info("Starting second worker");
    workAllocator2.start();
    workProcessor1.hasActiveWorkItems(totalWorkItems/2, TIMEOUT);
    workProcessor2.hasActiveWorkItems(totalWorkItems/2, TIMEOUT);

  }

  @Test
  public void testRebalanceRounding() throws ExecutionException, InterruptedException {
    final int totalWorkItems = 4;

    // rebalance delay needs to be comfortably within the hasActiveWorkItems timeouts used below
    workerProperties.setRebalanceDelay(Duration.ofMillis(500));

    final BulkWorkProcessor workProcessor1 = new BulkWorkProcessor(totalWorkItems);
    final WorkAllocator workAllocator1 = register(
        new WorkAllocator(
        workerProperties, client, workProcessor1, taskExecutor)
    );

    final BulkWorkProcessor workProcessor2 = new BulkWorkProcessor(totalWorkItems);
    final WorkAllocator workAllocator2 = register(
        new WorkAllocator(
        workerProperties, client, workProcessor2, taskExecutor)
    );

    final BulkWorkProcessor workProcessor3 = new BulkWorkProcessor(totalWorkItems);
    final WorkAllocator workAllocator3 = register(
        new WorkAllocator(
        workerProperties, client, workProcessor3, taskExecutor)
    );

    final BulkWorkProcessor workProcessor4 = new BulkWorkProcessor(totalWorkItems);
    final WorkAllocator workAllocator4 = register(
        new WorkAllocator(
        workerProperties, client, workProcessor4, taskExecutor)
    );

    final List<Work> createdWork = new ArrayList<>();
    for (int i = 0; i < totalWorkItems; i++) {
      createdWork.add(
          workAllocator1.createWork(String.format("%d", i)).get()
      );
    }

    workAllocator1.start();
    workProcessor1.hasActiveWorkItems(totalWorkItems, TIMEOUT);

    log.info("starting second allocator");
    workAllocator2.start();
    workProcessor1.hasActiveWorkItems(totalWorkItems/2, TIMEOUT);
    workProcessor2.hasActiveWorkItems(totalWorkItems/2, TIMEOUT);

    log.info("starting third allocator");
    workAllocator3.start();
    // wait past rebalance to ensure load balance of 1.3333 is rounded to 2
    Thread.sleep(600);
    // The expected balancing looks uneven here, but it's because the work load has a rounding-up
    // tolerance to avoid the one work item, in this case, from flip-flopping between the allocators
    workProcessor1.hasActiveWorkItems(2, TIMEOUT);
    workProcessor2.hasActiveWorkItems(2, TIMEOUT);
    workProcessor3.hasActiveWorkItems(0, TIMEOUT);

    log.info("adding new work for third to pickup");
    createdWork.add(
        workAllocator1.createWork(String.format("%d", totalWorkItems+1)).get()
    );
    workProcessor3.hasActiveWorkItems(1, TIMEOUT);
    workProcessor1.hasActiveWorkItems(2, TIMEOUT);
    workProcessor2.hasActiveWorkItems(2, TIMEOUT);
  }

  @Test
  public void testTwoWorkersReleasedToOne() throws InterruptedException, ExecutionException {
    final int totalWorkItems = 6;

    workerProperties.setRebalanceDelay(Duration.ofMillis(500));

    final BulkWorkProcessor workProcessor1 = new BulkWorkProcessor(totalWorkItems);
    final WorkAllocator workAllocator1 = register(
        new WorkAllocator(
        workerProperties, client, workProcessor1, taskExecutor)
    );
    workAllocator1.start();

    final BulkWorkProcessor workProcessor2 = new BulkWorkProcessor(totalWorkItems);
    final WorkAllocator workAllocator2 = register(
        new WorkAllocator(
        workerProperties, client, workProcessor2, taskExecutor)
    );
    workAllocator2.start();

    // both are started and ready to accept newly created worked items

    final List<Work> createdWork = new ArrayList<>();
    for (int i = 0; i < totalWorkItems; i++) {
      createdWork.add(
          workAllocator1.createWork(String.format("%d", i)).get()
      );
    }

    // first verify even load across the two
    workProcessor1.hasActiveWorkItems(totalWorkItems/2, TIMEOUT);
    workProcessor2.hasActiveWorkItems(totalWorkItems/2, TIMEOUT);

    log.info("stopping allocator 2");
    final Semaphore stopped = new Semaphore(0);
    workAllocator2.stop(() -> {
      stopped.release();
    });

    stopped.acquire();
    workProcessor1.hasActiveWorkItems(totalWorkItems, TIMEOUT);
    workProcessor2.hasActiveWorkItems(0, TIMEOUT);
  }

  private void assertWorkLoad(int expected, String workAllocatorId)
      throws ExecutionException, InterruptedException {
    final int actualWorkLoad = client.getKVClient()
        .get(
            ByteSequence
                .fromString(workerProperties.getPrefix() + Bits.WORKERS_SET + workAllocatorId)
        )
        .thenApply(getResponse -> {
          final int actual = Integer
              .parseInt(getResponse.getKvs().get(0).getValue().toStringUtf8(), 10);
          return actual;
        })
        .get();

    assertEquals(expected, actualWorkLoad);
  }

  @Test
  public void testManyWorkersMoreWorkItems() throws InterruptedException, ExecutionException {
    final int totalWorkers = 5;
    final int totalWorkItems = 40;

    final BulkWorkProcessor bulkWorkProcessor = new BulkWorkProcessor(totalWorkItems);

    final List<WorkAllocator> workAllocators = IntStream.range(0, totalWorkers)
        .mapToObj(index -> register(new WorkAllocator(workerProperties, client, bulkWorkProcessor, taskExecutor)))
        .collect(Collectors.toList());

    for (WorkAllocator workAllocator : workAllocators) {
      workAllocator.start();
    }
    // allow them to start and settle
    Thread.sleep(1000);

    final List<Work> createdWork = new ArrayList<>();
    for (int i = 0; i < totalWorkItems; i++) {
      createdWork.add(
          workAllocators.get(0).createWork(String.format("%d", i)).get()
      );
    }

    bulkWorkProcessor.hasEnoughStarts(totalWorkItems, 10000);

    bulkWorkProcessor.hasActiveWorkItems(totalWorkItems, 10000);

    final ByteSequence activePrefix = ByteSequence
        .fromString(workerProperties.getPrefix() + Bits.ACTIVE_SET);
    final WorkItemSummary workItemSummary = client.getKVClient()
        .get(
            activePrefix,
            GetOption.newBuilder()
                .withPrefix(activePrefix)
                .build()
        )
        .thenApply(getResponse -> {
          final WorkItemSummary result = new WorkItemSummary();
          for (KeyValue kv : getResponse.getKvs()) {
            result.activeWorkIds.add(Bits.extractIdFromKey(kv));
            final String workerId = kv.getValue().toStringUtf8();
            int load = result.workerLoad.getOrDefault(workerId, 0);
            result.workerLoad.put(workerId, load + 1);
          }
          return result;
        })
        .get();

    assertThat(workItemSummary.activeWorkIds, Matchers.hasSize(totalWorkItems));
    //
    workItemSummary.workerLoad.forEach((workerId, load) -> {
      log.info("Worker={} has load={}", workerId, load);
    });
  }

  @Test
  public void testVersionZeroAssumption() throws ExecutionException, InterruptedException {
    final ByteSequence resultKey = ByteSequence.fromString("/result");
    final TxnResponse resp = client.getKVClient().txn()
        .If(new Cmp(ByteSequence.fromString("/doesnotexist"), Cmp.Op.EQUAL, CmpTarget.version(0)))
        .Then(Op.put(resultKey, ByteSequence.fromString("success"),
            PutOption.DEFAULT
        ))
        .commit()
        .get();

    assertTrue(resp.isSucceeded());

    final GetResponse getResponse = client.getKVClient().get(resultKey)
        .get();

    assertEquals(
        "success",
        getResponse.getKvs().get(0).getValue().toStringUtf8()
    );
  }

  @Test
  public void testAlwaysTrueTxnAssumption() throws ExecutionException, InterruptedException {
    final ByteSequence resultKey = ByteSequence.fromString("/alwaysTrue");
    final TxnResponse resp = client.getKVClient().txn()
        .Then(Op.put(resultKey, ByteSequence.fromString("true"),
            PutOption.DEFAULT
        ))
        .commit()
        .get();

    assertTrue(resp.isSucceeded());

    final GetResponse getResponse = client.getKVClient().get(resultKey)
        .get();

    assertEquals(
        "true",
        getResponse.getKvs().get(0).getValue().toStringUtf8()
    );
  }

  static class WorkItemSummary {
    HashSet<String> activeWorkIds = new HashSet<>();
    Map<String, Integer> workerLoad = new HashMap<>();
  }

  private static class BulkWorkProcessor implements WorkProcessor {

    final Lock lock = new ReentrantLock();
    final Condition hasEnoughStarts = lock.newCondition();
    final Set<Integer> observedStarts;
    int activeWorkItems = 0;
    final Condition activeItemsCondition = lock.newCondition();

    public BulkWorkProcessor(int expectedWorkItems) {
      observedStarts = new HashSet<>(expectedWorkItems);
    }

    @Override
    public void start(String id, String content) {
      final int workItemIndex = Integer.parseInt(content);

      lock.lock();
      ++activeWorkItems;
      activeItemsCondition.signal();
      try {
        if (observedStarts.add(workItemIndex)) {
          hasEnoughStarts.signal();
        }
      } finally {
        lock.unlock();
      }
    }

    public void hasEnoughStarts(int expectedWorkItems, long timeout) throws InterruptedException {
      final long start = System.currentTimeMillis();

      lock.lock();
      try {
        while (observedStarts.size() < expectedWorkItems) {
          if (System.currentTimeMillis() - start > timeout) {
            Assert.fail(String.format("failed to see %d in time, had %d", expectedWorkItems, observedStarts.size()));
          }
          hasEnoughStarts.await(timeout, TimeUnit.MILLISECONDS);
        }
      } finally {
        lock.unlock();
      }
    }

    public void hasActiveWorkItems(int expected, long timeout) throws InterruptedException {
      final long start = System.currentTimeMillis();
      lock.lock();
      try {
        while (activeWorkItems != expected) {
          if (System.currentTimeMillis() - start > timeout) {
            Assert.fail(String.format("hasActiveWorkItems failed to see in time %d, had %d", expected, activeWorkItems));
          }
          activeItemsCondition.await(timeout, TimeUnit.MILLISECONDS);
        }
      } finally {
        lock.unlock();
      }
    }

    public void hasAtLeastWorkItems(int expected, long timeout) throws InterruptedException {
      final long start = System.currentTimeMillis();
      lock.lock();
      try {
        while (activeWorkItems < expected) {
          if (System.currentTimeMillis() - start > timeout) {
            Assert.fail(String.format("failed to see %d in time, had %d", expected, activeWorkItems));
          }
          activeItemsCondition.await(timeout, TimeUnit.MILLISECONDS);
        }
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void update(String id, String content) {
      // ignore for now
    }

    @Override
    public void stop(String id, String content) {
      lock.lock();
      try {
        --activeWorkItems;
        activeItemsCondition.signal();
      } finally {
        lock.unlock();
      }
    }
  }
}