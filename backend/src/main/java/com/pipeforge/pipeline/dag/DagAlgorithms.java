package com.pipeforge.pipeline.dag;

import com.pipeforge.exception.PipelineCycleException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure graph algorithms over a directed graph of nodes and an adjacency map
 * (edge {@code parent -> child} means "child depends on parent").
 *
 * <p>Stateless and free of persistence concerns so the core logic
 * (PRD §5.3 "validate DAG structure / prevent cycles") is directly unit-tested.
 */
public final class DagAlgorithms {

    private static final int WHITE = 0; // unvisited
    private static final int GRAY = 1;  // on the current DFS path
    private static final int BLACK = 2; // fully explored

    private DagAlgorithms() {
    }

    /**
     * Throws {@link PipelineCycleException} if the graph contains a cycle,
     * using a three-colour depth-first search.
     */
    public static <T> void assertAcyclic(Collection<T> nodes, Map<T, List<T>> adjacency) {
        Map<T, Integer> colour = new HashMap<>();
        for (T node : nodes) {
            colour.put(node, WHITE);
        }
        for (T node : nodes) {
            if (colour.get(node) == WHITE) {
                dfsDetectCycle(node, adjacency, colour);
            }
        }
    }

    private static <T> void dfsDetectCycle(T node, Map<T, List<T>> adjacency, Map<T, Integer> colour) {
        colour.put(node, GRAY);
        for (T next : adjacency.getOrDefault(node, List.of())) {
            int c = colour.getOrDefault(next, WHITE);
            if (c == GRAY) {
                throw new PipelineCycleException("Dependency cycle detected at task " + next);
            }
            if (c == WHITE) {
                dfsDetectCycle(next, adjacency, colour);
            }
        }
        colour.put(node, BLACK);
    }

    /**
     * Returns a topological ordering of the nodes (parents before children).
     * Throws {@link PipelineCycleException} if the graph is cyclic.
     */
    public static <T> List<T> topologicalSort(Collection<T> nodes, Map<T, List<T>> adjacency) {
        assertAcyclic(nodes, adjacency);

        Map<T, Boolean> visited = new HashMap<>();
        List<T> postOrder = new ArrayList<>(nodes.size());
        for (T node : nodes) {
            if (!Boolean.TRUE.equals(visited.get(node))) {
                dfsPostOrder(node, adjacency, visited, postOrder);
            }
        }
        Collections.reverse(postOrder);
        return postOrder;
    }

    private static <T> void dfsPostOrder(T node, Map<T, List<T>> adjacency,
                                         Map<T, Boolean> visited, List<T> postOrder) {
        visited.put(node, true);
        for (T next : adjacency.getOrDefault(node, List.of())) {
            if (!Boolean.TRUE.equals(visited.get(next))) {
                dfsPostOrder(next, adjacency, visited, postOrder);
            }
        }
        postOrder.add(node);
    }
}
