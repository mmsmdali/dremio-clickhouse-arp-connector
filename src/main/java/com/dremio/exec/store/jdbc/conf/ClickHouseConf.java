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

import org.hibernate.validator.constraints.NotBlank;

import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.exec.server.SabotContext;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcStoragePlugin;
import com.dremio.exec.store.jdbc.JdbcStoragePlugin.Config;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.google.common.annotations.VisibleForTesting;
import com.dremio.exec.catalog.conf.Secret;

import io.protostuff.Tag;

/**
 * Configuration for ClickHouse sources.
 */
@SourceType(value = "CLICKHOUSE", label = "ClickHouse")
public class ClickHouseConf extends AbstractArpConf<ClickHouseConf> {
  private static final String ARP_FILENAME = "arp/implementation/clickhouse-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
//  private static final String DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";

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
  @Tag(6)
  @DisplayMetadata(label = "Options [E.g. receive_timeout=6000&connect_timeout=567890]")
  public String options;
  */
  @NotBlank
  @Tag(6)
  @DisplayMetadata(label = "JDBC Driver [E.g. ru.yandex.clickhouse.ClickHouseDriver , cc.blynk.clickhouse.ClickHouseDriver , com.github.housepower.jdbc.ClickHouseDriver]")
  public String driver="ru.yandex.clickhouse.ClickHouseDriver";
  /*
  @Tag(2)
  @DisplayMetadata(label = "Record fetch size")
  @NotMetadataImpacting
  public int fetchSize = 200;
  */
  @VisibleForTesting
  public String toJdbcConnectionString() {
    host = host == null ? "localhost" : host;
    port = port == null ? "8123" : port;
    database = database == null ? "default" : database;
    user = user == null ? "default" : user;
    final String password = checkNotNull(this.password, "Missing Password.");
    driver = driver == null ? "ru.yandex.clickhouse.ClickHouseDriver" : driver;

    return String.format("jdbc:clickhouse://%s:%s/%s?user=%s&password=%s" /* &%s */ , host, port, database, user, password /*, options*/ );
  }

  @Override
  @VisibleForTesting
  public Config toPluginConfig(SabotContext context) {
    return JdbcStoragePlugin.Config.newBuilder()
        .withDialect(getDialect())
        // .withFetchSize(fetchSize)
        .withDatasourceFactory(this::newDataSource)
        .clearHiddenSchemas()
        // .addHiddenSchema("SYSTEM")
        .build();
  }

  private CloseableDataSource newDataSource() {
    final String jdbcConnectionString=toJdbcConnectionString();
    return DataSources.newGenericConnectionPoolDataSource(driver,     jdbcConnectionString, /* user */ null, /* password */ null, null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE);
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
