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
 * Configuration for Clickhouse sources.
 */
@SourceType(value = "CLICKHOUSE", label = "Clickhouse")
public class ClickhouseConf extends AbstractArpConf<ClickhouseConf> {
  private static final String ARP_FILENAME = "arp/implementation/clickhouse-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
//  private static final String DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";

  @NotBlank
  @Tag(1)
  @DisplayMetadata(label = "Host")
  public String host;

  @NotBlank
  @Tag(2)
  @DisplayMetadata(label = "Port")
  public String port;

  @Tag(3)
  @DisplayMetadata(label = "Database")
  public String database;
  
  @NotBlank
  @Tag(4)
  @DisplayMetadata(label = "User")
  public String user;

  @NotBlank
  @Secret
  @Tag(5)
  @DisplayMetadata(label = "Password")
  public String password;

@Tag(6)
@DisplayMetadata(label = "Query String [E.g. receive_timeout=6000&connect_timeout=567890]")
public String qs;

@NotBlank
@Tag(7)
@DisplayMetadata(label = "JDBC Driver [E.g. ru.yandex.clickhouse , cc.blynk.clickhouse , com.github.housepower.jdbc]")
public String driver="ru.yandex.clickhouse";


  @VisibleForTesting
  public String toJdbcConnectionString() {
final String host = checkNotNull(this.host, "Missing Host.");
final String port = checkNotNull(this.port, "Missing Port.");
final String user = checkNotNull(this.user, "Missing Username.");
final String password = checkNotNull(this.password, "Missing Password.");

driver=(driver==null?"ru.yandex.clickhouse":driver)+".ClickHouseDriver";

    return String.format("jdbc:clickhouse://%s:%s/%s?user=%s&password=%s&%s", host, port, database, user, password,qs);
  }

  @Override
  @VisibleForTesting
  public Config toPluginConfig(SabotContext context) {
    return JdbcStoragePlugin.Config.newBuilder()
        .withDialect(getDialect())
        .withDatasourceFactory(this::newDataSource)
        .clearHiddenSchemas()
        //.addHiddenSchema("SYSTEM")
        .build();
  }

  private CloseableDataSource newDataSource() {

final String jdbcConnectionString=toJdbcConnectionString();
    return DataSources.newGenericConnectionPoolDataSource(driver,     jdbcConnectionString, null,null,null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE);
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
