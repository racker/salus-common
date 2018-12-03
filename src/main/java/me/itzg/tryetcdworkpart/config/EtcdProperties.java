package me.itzg.tryetcdworkpart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("etcd")
@Component
@Data
public class EtcdProperties {
  String url = "http://localhost:2379";
}
