package com.pipeforge.pipeline.dag;

import com.pipeforge.exception.PipelineCycleException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Core DAG algorithm tests (Phase 4.6 — "invalid DAG rejected").
 */
class DagAlgorithmsTest {

    @Test
    void topologicalSortOrdersParentsBeforeChildren() {
        // A -> B -> C and A -> C, plus isolated D
        List<String> nodes = List.of("A", "B", "C", "D");
        Map<String, List<String>> adjacency = Map.of(
                "A", List.of("B", "C"),
                "B", List.of("C"),
                "C", List.of(),
                "D", List.of());

        List<String> order = DagAlgorithms.topologicalSort(nodes, adjacency);

        assertThat(order).containsExactlyInAnyOrder("A", "B", "C", "D");
        assertThat(order.indexOf("A")).isLessThan(order.indexOf("B"));
        assertThat(order.indexOf("B")).isLessThan(order.indexOf("C"));
        assertThat(order.indexOf("A")).isLessThan(order.indexOf("C"));
    }

    @Test
    void acyclicGraphPassesValidation() {
        assertThatCode(() -> DagAlgorithms.assertAcyclic(
                List.of("A", "B"), Map.of("A", List.of("B"), "B", List.of())))
                .doesNotThrowAnyException();
    }

    @Test
    void detectsThreeNodeCycle() {
        Map<String, List<String>> adjacency = Map.of(
                "A", List.of("B"),
                "B", List.of("C"),
                "C", List.of("A"));

        assertThatThrownBy(() -> DagAlgorithms.assertAcyclic(List.of("A", "B", "C"), adjacency))
                .isInstanceOf(PipelineCycleException.class);
        assertThatThrownBy(() -> DagAlgorithms.topologicalSort(List.of("A", "B", "C"), adjacency))
                .isInstanceOf(PipelineCycleException.class);
    }

    @Test
    void detectsTwoNodeCycle() {
        Map<String, List<String>> adjacency = Map.of(
                "A", List.of("B"),
                "B", List.of("A"));

        assertThatThrownBy(() -> DagAlgorithms.assertAcyclic(List.of("A", "B"), adjacency))
                .isInstanceOf(PipelineCycleException.class);
    }
}
