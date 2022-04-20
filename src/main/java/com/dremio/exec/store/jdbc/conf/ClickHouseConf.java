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

import com.dremio.options.OptionManager;
import com.dremio.security.CredentialsService;
import org.hibernate.validator.constraints.NotBlank;

import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcPluginConfig;
// import com.dremio.exec.store.jdbc.JdbcStoragePlugin;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
// import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.dremio.exec.catalog.conf.Secret;

import io.protostuff.Tag;

/**
 * Configuration for ClickHouse sources.
 */
@SourceType(value = "CLICKHOUSE", label = "ClickHouse", uiConfig = "clickhouse-layout.json", externalQuerySupported = true)
public class ClickHouseConf extends AbstractArpConf<ClickHouseConf> {
  private static final String ARP_FILENAME = "arp/implementation/clickhouse-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
  private static final String DRIVER = "com.clickhouse.jdbc.ClickHouseDriver";
  private static final String PREFIX = "clickhouse";
  private static final String PROTOCOL = "http";

  @NotBlank
  @Tag(1)
  @DisplayMetadata(label = "Host [localhost, 127.0.0.1, 127.1.1.0]")
  public String host="localhost";

  @NotBlank
  @Tag(2)
  @DisplayMetadata(label = "Port [8123]")
  public String port="8123";
  
  @Tag(3)
  @DisplayMetadata(label = "Database [default]")
  public String database="default";
  
  @NotBlank
  @Tag(4)
  @DisplayMetadata(label = "User [default]")
  public String user="default";

  @NotBlank
  @Secret
  @Tag(5)
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

  @Tag(6)
  @DisplayMetadata(label = "Maximum idle connections")
  @NotMetadataImpacting
  public int maxIdleConns = 8;

  @Tag(7)
  @DisplayMetadata(label = "Connection idle time (s)")
  @NotMetadataImpacting
  public int idleTimeSec = 60;

  @VisibleForTesting
  public String toJdbcConnectionString() {
	  final String host = this.host == null ? "localhost" : this.host;
    final String port = this.port == null ? "8123" : this.port;
    final String database = this.database == null ? "default" : this.database;
    final String user = this.user == null ? "default" : this.user;
    final String password = checkNotNull(this.password, "Missing Password.");

    return String.format("jdbc:%s:%s://%s:%s/%s?user=%s&password=%s", PREFIX, PROTOCOL, host, port, database, user, password);
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
      toJdbcConnectionString(), /* user */ null, /* password */ null, null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE,
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