package me.itzg.tryetcdworkpart.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;

@ConfigurationProperties("worker")
@Component
@Data
public class WorkerProperties {

  @DurationUnit(ChronoUnit.SECONDS)
  Duration leaseDuration = Duration.ofSeconds(30);

  @DurationUnit(ChronoUnit.SECONDS)
  Duration rebalanceDelay = Duration.ofSeconds(1);

  @NotEmpty
  String prefix = "/work/";
}
