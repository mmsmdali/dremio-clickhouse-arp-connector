/*
 * Copyright (C) 2017-2018 Dremio Corporation
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
package com.dremio.exec.store.jdbc.conf;

import static com.google.common.base.Preconditions.checkNotNull;

// import org.hibernate.validator.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.Secret;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcPluginConfig;
// import com.dremio.exec.store.jdbc.JdbcStoragePlugin;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.dremio.options.OptionManager;
import com.dremio.security.CredentialsService;
// import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;

import io.protostuff.Tag;

/**
 * Configuration for ClickHouse sources.
 */
@SourceType(value = "CLICKHOUSE", label = "ClickHouse", uiConfig = "clickhouse-layout.json", externalQuerySupported = true)
public class ClickHouseConf extends AbstractArpConf<ClickHouseConf> {
  private static final String ARP_FILENAME = "arp/implementation/clickhouse-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
  private static final String DRIVER = "com.clickhouse.jdbc.ClickHouseDriver"; // version 0.4.0 onwards
  // private static final String DRIVER = "ru.yandex.clickhouse.ClickHouseDriver"; // deprecated since 0.3.2+, removed from 0.4.0

  private static final String PREFIX = "ch"; // ch, clickhouse (legacy)
  private static final String PROTOCOL = "http"; // https, http, grpc
  private static final String HOST = "localhost";
  private static final String PORT = "8123";
  private static final String DATABASE = "default";
  private static final String defaultJdbcConnectionString = String.format("jdbc:%s:%s://%s:%s/%s", PREFIX, PROTOCOL, HOST, PORT, DATABASE);

  private static final String defaultUsername = "default";
  
  @NotEmpty
  @Tag(1)
  @DisplayMetadata(label = "JDBC Connection String (jdbc:<prefix>[:<protocol>]://<host>:[<port>][/<database>[?param1=value1&param2=value2]])")
  public String jdbcConnectionString = defaultJdbcConnectionString;
  
  @NotEmpty
  @Tag(2)
  @DisplayMetadata(label = "Username [default]")
  public String username = defaultUsername;

  @NotEmpty
  @Secret
  @Tag(3)
  @DisplayMetadata(label = "Password")
  public String password;    
  /*
  @Tag(2)
  @DisplayMetadata(label = "Record fetch size")
  @NotMetadataImpacting
  public int fetchSize = 200;
  */

//  If you've written your source prior to Dremio 16, and it allows for external query via a flag like below, you should
//  mark the flag as @JsonIgnore and remove use of the flag since external query support is now managed by the SourceType
//  annotation and if the user has been granted the EXTERNAL QUERY permission (enterprise only). Marking the flag as @JsonIgnore
//  will hide the external query tickbox field, but allow your users to upgrade Dremio without breaking existing source
//  configurations. An example of how to dummy this out is commented out below.
//  @Tag(3)
//  @NotMetadataImpacting
//  @JsonIgnore
//  public boolean enableExternalQuery = false;
  
  @Tag(4)
  @DisplayMetadata(label = "Maximum idle connections")
  @NotMetadataImpacting
  public int maxIdleConns = 8;

  @Tag(5)
  @DisplayMetadata(label = "Connection idle time (s)")
  @NotMetadataImpacting
  public int idleTimeSec = 60;
  

  @VisibleForTesting
  public String toJdbcConnectionString() {	  
    final String jdbcConnectionString = this.jdbcConnectionString == null ? defaultJdbcConnectionString : this.jdbcConnectionString;
    final String username = this.username == null ? defaultUsername : this.username;
    final String password = checkNotNull(this.password, "Missing Password.");

    return String.format("%s?user=%s&password=%s", jdbcConnectionString, username, password);
  }

  @Override
  @VisibleForTesting
  public JdbcPluginConfig buildPluginConfig(
          JdbcPluginConfig.Builder configBuilder,
          CredentialsService credentialsService,
          OptionManager optionManager
  ) {
    return configBuilder.withDialect(getDialect())
            .withDialect(getDialect())
            // .withFetchSize(fetchSize)
            .withDatasourceFactory(this::newDataSource)
            .clearHiddenSchemas()
            // .addHiddenSchema("SYSTEM")
            .build();
  }

  private CloseableDataSource newDataSource() {
    return DataSources.newGenericConnectionPoolDataSource(DRIVER,
      toJdbcConnectionString(), /* username */ null, /* password */ null, null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE,
            maxIdleConns, idleTimeSec);
  }

  @Override
  public ArpDialect getDialect() {
    return ARP_DIALECT;
  }

  @VisibleForTesting
  public static ArpDialect getDialectSingleton() {
    return ARP_DIALECT;
  }
}