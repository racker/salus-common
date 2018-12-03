package me.itzg.tryetcdworkpart.web;

import java.util.concurrent.CompletableFuture;
import me.itzg.tryetcdworkpart.Work;
import me.itzg.tryetcdworkpart.services.WorkAllocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/work")
public class WorkController {

  private final WorkAllocator workAllocator;

  @Autowired
  public WorkController(WorkAllocator workAllocator) {
    this.workAllocator = workAllocator;
  }

  @PostMapping
  public CompletableFuture<Work> create(@RequestBody String content) {
    return workAllocator.createWork(content);
  }

  @PutMapping("{id}")
  public CompletableFuture<Work> update(@PathVariable String id, @RequestBody String content) {
    return workAllocator.updateWork(id, content);
  }

  /**
   *
   * @param id the work item to delete
   * @return a {@link CompletableFuture} of the number of work items successfully deleted, usually 1
   */
  @DeleteMapping("{id}")
  public CompletableFuture<Long> delete(@PathVariable String id) {
    return workAllocator.deleteWork(id);
  }

}
