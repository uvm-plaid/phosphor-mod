package edu.columbia.cs.psl.phosphor.struct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * A keyed, dataless implementation of a directed graph
 * @param <KeyType> The type of key to associate with each node in the graph. Any comparable can be used with the same
 *                  runtime complexity.
 */
class Graph<KeyType extends Comparable> {

    /**
     * A class used to represent the edges of the graph.
     */
    protected class Edge {
        /**
         * The key of the node connected to the tail of the edge
         */
        private KeyType tail;

        /**
         * The key of the node connected to the head of the edge
         */
        private KeyType head;

        /**
         * The weight corresponding to the edge
         */
        private Integer weight;

        public Edge(KeyType tail, KeyType head) {
            this.tail = tail;
            this.head = head;
            this.weight = 0;
        }

        public Edge(KeyType tail, KeyType head, Integer weight) {
            this.tail = tail;
            this.head = head;
            this.weight = weight;
        }

        public KeyType getTail() {
            return tail;
        }

        public void setTail(KeyType tail) {
            this.tail = tail;
        }

        public KeyType getHead() {
            return head;
        }

        public void setHead(KeyType head) {
            this.head = head;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }
    }

    /**
     * A class used to represent the nodes of the graph.
     */
    protected class Node {

        /**
         * A unique key corresponding to the node.
         */
        private KeyType key;

        /**
         * A map of connected nodes.
         */
        private HashMap<KeyType, Edge> incidents;  // edge : key of connected node

        /**
         * A flag used to specify if the node has been visited or not.
         */
        private Boolean explored;

        /**
         * Creates a new node.
         * @param key Associates the node with a unique key.
         */
        public Node(KeyType key) {
            this.key = key;
            this.incidents = new HashMap<>();
            explored = Boolean.FALSE;
        }

        public java.util.ArrayList<Edge> getIncidents() {
            return new java.util.ArrayList<>(this.incidents.values());
        }

        public Edge getEdge(KeyType incident) {
            if (this.incidents.containsKey(incident)) {
                return this.incidents.get(incident);
            }
            return null;
        }

        public void removeEdge(KeyType incident) {
            this.incidents.remove(incident);
        }

        public void addIncident(Edge edge) {
            this.incidents.put(edge.head, edge);
        }

        public Boolean isExplored() {
            return explored;
        }

        public void setExplored(Boolean explored) {
            this.explored = explored;
        }

        public KeyType getKey() {
            return this.key;
        }

        @Override
        public String toString() {
            return this.key.toString();
        }
    }

    /**
     * The graph's adjacency list is represented as a map rather than an array to allow for node keys other than
     * integers (while retaining the same runtime complexity).
     */
    protected HashMap<KeyType, Node> adjacencyList;

    /**
     * Creates an empty graph.
     */
    public Graph() {
        adjacencyList = new HashMap<>();
    }

    /**
     * Adds a node to the graph.
     * @param node The node to add to the graph.
     */
    public void addNode(Node node) {
        adjacencyList.put(node.key, node);
    }

    /**
     * Retrieve a node from the graph.
     * @param key The key for the node.
     * @return The node with the provided key.
     */
    public Node getNode(KeyType key) {
        return adjacencyList.get(key);
    }

    /**
     * Connects to nodes together.
     * @param edge The edge describing the connection.
     */
    public void addEdge(Edge edge) {
        this.getNode(edge.tail).addIncident(edge);
    }

    /**
     * Removes and edge from the graph. The nodes connected to the edge remains in the graph.
     * @param edge The edge to remove.
     */
    public void removeEdge(Edge edge) {
        this.getNode(edge.getTail()).removeEdge(edge.head);
    }

    public Edge getEdge(KeyType tail, KeyType head) {
        if (this.adjacencyList.containsKey(tail)) {
            return this.adjacencyList.get(tail).getEdge(head);
        }
        return null;
    }

    /**
     * Depth-first traverses the graph. Calls the private depthFirstTraversal which actually does the traversal.
     * @param start The key of the node the traversal should start from.
     * @return A list of traversed node keys.
     */
    public java.util.ArrayList<KeyType> depthFirstTraversal(KeyType start) {
        Node startNode = getNode(start);
        if (startNode == null) {
            throw new IndexOutOfBoundsException("Invalid node key: " + start.toString());
        }
        java.util.ArrayList<KeyType> traversed = depthFirstTraversal(getNode(start));
        resetExplored();
        return traversed;
    }

    /**
     * Breadth-first traverses the graph.
     * @param start The key of the node to start the traversal from.
     * @return A list of traversed node keys.
     */
    public java.util.ArrayList<KeyType> breadthFirstTraversal(KeyType start) {
        Node startNode = getNode(start);
        if (startNode == null) {
            throw new IndexOutOfBoundsException("Invalid node key: " + start.toString());
        }

        java.util.ArrayList<KeyType> traversed = new java.util.ArrayList<>();
        java.util.LinkedList<Node> toVisit = new java.util.LinkedList<>();
        toVisit.add(startNode);
        while (!toVisit.isEmpty()) {
            Node node = toVisit.pop();
            if (!node.isExplored()) {
                node.setExplored(Boolean.TRUE);
                traversed.add(node.getKey());
                for (Edge incident : node.getIncidents()) {
                    if (!getNode(incident.getHead()).isExplored()) {
                        toVisit.add(getNode(incident.getHead()));
                    }
                }
            }
        }
        resetExplored();
        return traversed;
    }

    /**
     * Breadth-first traverses the graph but returns edges traversed rather than nodes.
     * @param start The key of the node to start the traversal from.
     * @return A list of traversed edges.
     */
    public java.util.ArrayList<Edge> breadthFirstEdgeTraversal(KeyType start) {
        java.util.ArrayList<KeyType> traversed = this.breadthFirstTraversal(start);
        java.util.ArrayList<Edge> traversedEdges = new java.util.ArrayList<>();
        for (int i = 0; i < traversed.size() - 1; ++i) {
            traversedEdges.add(this.getEdge(traversed.get(i), traversed.get(i + 1)));
        }
        return traversedEdges;
    }

    /**
     * Breadth-first traverses the graph from 'from', until 'to' is reached.
     * @param from The node to start from.
     * @param to The node to look for.
     * @return A list of edges in the path from 'from' to 'to'.
     */
    public java.util.ArrayList<Edge> breadthFirstFindPath(KeyType from, KeyType to) {
        HashMap<KeyType, KeyType> previous = new HashMap<>(); // visited key : previously found key in path

        Node startNode = getNode(from);
        if (startNode == null) {
            throw new IndexOutOfBoundsException("Invalid node key: " + startNode.toString());
        }

        java.util.LinkedList<Node> toVisit = new LinkedList<>();
        toVisit.add(startNode);
        while (!toVisit.isEmpty()) {
            Node node = toVisit.pop();
            if (!node.isExplored()) {
                node.setExplored(Boolean.TRUE);
                for (Edge incident : node.getIncidents()) {
                    if (!getNode(incident.getHead()).isExplored()) {
                        toVisit.add(getNode(incident.getHead()));
                        previous.put(incident.getHead(), node.getKey());
                        if (incident.getHead().equals(to)) {
                            java.util.ArrayList<Edge> path = new java.util.ArrayList<>();
                            KeyType last = incident.getHead();
                            do {
                                path.add(0, this.getEdge(previous.get(last), last));
                                last = previous.get(last);
                            } while (!last.equals(from));
                            resetExplored();
                            return path;
                        }
                    }
                }
            }
        }
        resetExplored();
        return new java.util.ArrayList<>();
    }

    /**
     * @return A visual representation of the graph.
     */
    @Override
    public String toString() {
        String string = "";
        java.util.ArrayList<KeyType> keys = new java.util.ArrayList<>(adjacencyList.keySet());
        Collections.sort(keys);
        for (KeyType key : keys) {
            string += key.toString() + ":";
            Node node = getNode(key);
            for (Edge child : node.getIncidents()) {
                string += " -> " + this.getNode(child.getHead()).key.toString();
            }
            string += "\n";
        }
        return string;
    }

    /**
     * The implementation of the depthFirstTraversal. Calls itself recursively.
     * @param next The next node to traverse
     * @return A list of traversed node keys.
     */
    protected java.util.ArrayList<KeyType> depthFirstTraversal(Node next) {
        java.util.ArrayList<KeyType> traversed = new ArrayList<>();
        traversed.add(next.getKey());
        next.setExplored(Boolean.TRUE);
        for (Edge node : next.getIncidents()) {
            if (!this.getNode(node.getHead()).isExplored()) {
                traversed.addAll(depthFirstTraversal(node.getHead()));
            }
        }
        return traversed;
    }

    /**
     * Iterates over all of the nodes in the graph and sets their explored variable back to unexplored. Used internally
     * to allow for multiple traversals of the same graph.
     */
    protected void resetExplored() {
        for (KeyType key : adjacencyList.keySet()) {
            getNode(key).setExplored(Boolean.FALSE);
        }
    }

}