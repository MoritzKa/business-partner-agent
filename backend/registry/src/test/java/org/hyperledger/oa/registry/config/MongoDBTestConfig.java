/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.registry.config;

import javax.inject.Singleton;

import org.hyperledger.oa.registry.config.AbstractMongoDBConfig;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Factory
public class MongoDBTestConfig extends AbstractMongoDBConfig {

    @Singleton
    @Bean(preDestroy = "close")
    @Requires(beans = { MongoDBTestContainer.class }, env = { Environment.TEST })
    public MongoClient mongoClient(MongoDBTestContainer tc) {
        if (tc == null || tc.getMappedPort() == null) {
            throw new RuntimeException("Mapped port for mongodb is not set.");
        }
        log.debug("Starting mongodb test container on port: {}", tc.getMappedPort());
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .writeConcern(WriteConcern.ACKNOWLEDGED)
                .applyConnectionString(new ConnectionString("mongodb://localhost:" + tc.getMappedPort()))
                .codecRegistry(createCodecRegistry());
        return MongoClients.create(settings.build());
    }
}
