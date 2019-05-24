package edu.columbia.cs.psl.phosphor.struct;

import java.util.Collection;
import java.util.ArrayList;

public class Lattice<Element extends Comparable> {

    private Graph<Element> graph;

    private Element minimal;
    private Element maximal;

    public Lattice() {
        graph = new Graph<Element>();
    }

    public Element getGreatestLowerBound(Collection<Element> elements) {
        Element glb = null;
        for (Element element : graph.breadthFirstTraversal(minimal)) {
            boolean isLowerBound = true;
            for (Element glbElement : elements) {
                ArrayList<Graph<Element>.Edge> pathToGlbElement = graph.breadthFirstFindPath(element, glbElement);
                if (pathToGlbElement.size() == 0 || !pathToGlbElement.get(pathToGlbElement.size() - 1).getHead().equals(glbElement)) {
                    // no path from element to glbElement
                    if (!element.equals(glbElement)) {
                        isLowerBound = false;
                        break;
                    }
                }
            }

            if (isLowerBound) {
                if (glb == null || element.compareTo(glb) > 0) {
                    glb = element;
                }
            }
        }
        return glb;
    }

    public Element getLeastUpperBound(Collection<Element> elements) {
        Element lub = null;
        for (Element element : graph.breadthFirstTraversal(minimal)) {
            boolean isUpperBound = true;
            for (Element lubElement : elements) {
                ArrayList<Graph<Element>.Edge> pathFromLubElement = graph.breadthFirstFindPath(lubElement, element);
                if (pathFromLubElement.size() == 0 || !pathFromLubElement.get(pathFromLubElement.size() - 1).getHead().equals(element)) {
                    // no path from lubElement to element
                    if (!lubElement.equals(element)) {
                        isUpperBound = false;
                        break;
                    }
                }
            }

            if (isUpperBound) {
                if (lub == null || element.compareTo(lub) < 0) {
                    lub = element;
                }
            }
        }
        return lub;
    }

    public void addElement(Element element) {
        graph.addNode(graph.new Node(element));
        if (minimal == null || element.compareTo(minimal) < 0) {
            minimal = element;
        }
        if (maximal == null || element.compareTo(maximal) > 0) {
            maximal = element;
        }
    }

    public void addOrdering(Element lessThan, Element greaterThan) {
        graph.addEdge(graph.new Edge(lessThan, greaterThan));
    }

    @Override
    public String toString() {
        return graph.toString();
    }

}
