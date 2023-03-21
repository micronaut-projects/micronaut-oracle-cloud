package io.micronaut.oraclecloud.serde.model;

import io.micronaut.serde.annotation.SerdeImport;
import com.oracle.bmc.monitoring.model.Metric;

@SerdeImport(Metric.class)
interface SerdeImportMetric {}
