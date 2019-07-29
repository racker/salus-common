/*
 *    Copyright 2019 Rackspace US, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package com.rackspace.salus.common.transactions;

import javax.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration
public class JpaKafkaTransactions {

  @Bean(name = "transactionManager")
  @Primary
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  public JpaTransactionManager transactionManager(EntityManagerFactory em) {
    return new JpaTransactionManager(em);
  }

  @Bean(name = "jpaKafkaTransactionManager")
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  public ChainedTransactionManager jpaKafkaTransactionManager(
      JpaTransactionManager transactionManager,
      KafkaTransactionManager kafkaTransactionManager) {
    return new ChainedTransactionManager(kafkaTransactionManager, transactionManager);
  }
}
