/*
/* Copyright 2018-2025 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/

package io.openlineage.spark3.agent.lifecycle.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.openlineage.client.OpenLineage;
import io.openlineage.client.dataset.DatasetCompositeFacetsBuilder;
import io.openlineage.spark.agent.lifecycle.SparkOpenLineageExtensionVisitorWrapper;
import io.openlineage.spark.agent.util.DatasetVersionUtils;
import io.openlineage.spark.api.DatasetFactory;
import io.openlineage.spark.api.OpenLineageContext;
import io.openlineage.spark3.agent.utils.DataSourceV2RelationDatasetExtractor;
import io.openlineage.spark3.agent.utils.DatasetVersionDatasetFacetUtils;
import java.util.List;
import java.util.Optional;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation;
import org.apache.spark.sql.execution.ui.SparkListenerSQLExecutionEnd;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DataSourceV2RelationOutputDatasetBuilderTest {

  OpenLineageContext context = mock(OpenLineageContext.class);
  DatasetFactory factory = mock(DatasetFactory.class);
  DataSourceV2RelationOutputDatasetBuilder builder =
      new DataSourceV2RelationOutputDatasetBuilder(context, factory);

  @Test
  void testDataSourceV2RelationInputDatasetBuilderIsDefinedAtLogicalPlan() {
    assertFalse(builder.isDefinedAtLogicalPlan(mock(LogicalPlan.class)));
    assertTrue(builder.isDefinedAtLogicalPlan(mock(DataSourceV2Relation.class)));
  }

  @Test
  void testDataSourceV2RelationOutputDatasetBuilderIsDefinedAtLogicalPlan() {
    assertFalse(builder.isDefinedAtLogicalPlan(mock(LogicalPlan.class)));
    assertTrue(builder.isDefinedAtLogicalPlan(mock(DataSourceV2Relation.class)));
  }

  @Test
  void testIsApplied() {
    DatasetCompositeFacetsBuilder datasetFacetsBuilder = mock(DatasetCompositeFacetsBuilder.class);
    List<OpenLineage.InputDataset> datasets = mock(List.class);
    DataSourceV2Relation relation = mock(DataSourceV2Relation.class);

    when(context.getSparkExtensionVisitorWrapper())
        .thenReturn(mock(SparkOpenLineageExtensionVisitorWrapper.class));
    when(factory.createCompositeFacetBuilder()).thenReturn(datasetFacetsBuilder);

    try (MockedStatic planUtils3MockedStatic =
        mockStatic(DataSourceV2RelationDatasetExtractor.class)) {
      try (MockedStatic facetUtilsMockedStatic =
          mockStatic(DatasetVersionDatasetFacetUtils.class)) {
        try (MockedStatic versionUtilsMockedStatic = mockStatic(DatasetVersionUtils.class)) {
          when(DataSourceV2RelationDatasetExtractor.extract(
                  factory, context, relation, datasetFacetsBuilder))
              .thenReturn(datasets);

          when(DatasetVersionDatasetFacetUtils.extractVersionFromDataSourceV2Relation(
                  context, relation))
              .thenReturn(Optional.of("v2"));

          Assertions.assertEquals(
              datasets, builder.apply(new SparkListenerSQLExecutionEnd(1L, 1L), relation));

          versionUtilsMockedStatic.verify(
              () ->
                  DatasetVersionUtils.buildVersionOutputFacets(context, datasetFacetsBuilder, "v2"),
              times(1));
        }
      }
    }
  }
}
