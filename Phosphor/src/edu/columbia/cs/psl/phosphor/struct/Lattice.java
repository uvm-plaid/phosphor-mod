package edu.columbia.cs.psl.phosphor.struct;

import java.util.ArrayList;
import java.util.*;

public class Lattice<Element> {

    private Graph<Element> latticeGraph;
    private Graph<Element> reverseLatticeGraph;

    private Element minimal;

    private Integer size;

    public Lattice() {
        latticeGraph = new Graph<>();
        reverseLatticeGraph = new Graph<>();
        minimal = null;
        size = 0;
    }

    public Lattice(Collection<Element> elements) {
        latticeGraph = new Graph<>();
        reverseLatticeGraph = new Graph<>();
        minimal = null;
        size = 0;

        for (Element element : elements) {
            addElement(element);
        }
    }

    public synchronized void addElement(Element element) {
        latticeGraph.addNode(latticeGraph.new Node(element));
        reverseLatticeGraph.addNode(reverseLatticeGraph.new Node(element));

        size += 1;
    }

    public synchronized void addOrdering(Element lessThan, Element greaterThan) {
        latticeGraph.addEdge(latticeGraph.new Edge(lessThan, greaterThan));
        reverseLatticeGraph.addEdge(reverseLatticeGraph.new Edge(greaterThan, lessThan));

        if (minimal == null) {
            minimal = lessThan;
        } else {
            if (compareElements(lessThan, minimal) > 0) {
                minimal = lessThan;
            }
        }
    }

    public synchronized Element leastUpperBound(Collection<Element> elements) {
        if (elements.size() == 0) {
            return null;
        }

        ArrayList<HashSet<Element>> elementUpperBounds = new ArrayList<>();
        for (Element lubElement : elements) {
            ArrayList<Element> traversed = latticeGraph.breadthFirstTraversal(lubElement);
            elementUpperBounds.add(new HashSet<>(traversed));
        }

        HashSet<Element> sharedUpperBounds = elementUpperBounds.get(0);
        for (int i = 1; i < elementUpperBounds.size(); ++i) {
            sharedUpperBounds.retainAll(elementUpperBounds.get(i));
        }

        /*
        // breaks in phosphor
        PriorityQueue<Element> upperBoundsQueue = new PriorityQueue<>(size, new Comparator<Element>() {
            @Override
            public int compare(Element o1, Element o2) {
                return compareElements(o2, o1);
            }
        });
        upperBoundsQueue.addAll(sharedUpperBounds);
        return upperBoundsQueue.poll();
        */

        Element leastUpperBound = null;
        for (Element element : sharedUpperBounds) {
            if (leastUpperBound == null) {
                leastUpperBound = element;
            } else {
                if (compareElements(leastUpperBound, minimal) > 0) {
                    leastUpperBound = element;
                }
            }
        }
        return leastUpperBound;
    }

    public synchronized Element greatestLowerBound(Collection<Element> elements) {
        if (elements.size() == 0) {
            return null;
        }

        ArrayList<HashSet<Element>> elementLowerBounds = new ArrayList<>();
        for (Element glbElement : elements) {
            elementLowerBounds.add(new HashSet<>(reverseLatticeGraph.breadthFirstTraversal(glbElement)));
        }

        HashSet<Element> sharedLowerBounds = elementLowerBounds.get(0);
        for (int i = 1; i < elementLowerBounds.size(); ++i) {
            sharedLowerBounds.retainAll(elementLowerBounds.get(i));
        }

        /*
        // breaks in phosphor
        PriorityQueue<Element> lowerBoundsQueue = new PriorityQueue<>(size, new Comparator<Element>() {
            @Override
            public int compare(Element o1, Element o2) {
                return compareElements(o1, o2);
            }
        });
        lowerBoundsQueue.addAll(sharedLowerBounds);
        return lowerBoundsQueue.poll();
        */

        Element greatestUpperBound = null;
        for (Element element : sharedLowerBounds) {
            if (greatestUpperBound == null) {
                greatestUpperBound = element;
            } else {
                if (compareElements(greatestUpperBound, minimal) < 0) {
                    greatestUpperBound = element;
                }
            }
        }
        return greatestUpperBound;
    }

    public synchronized int compareElements(Element first, Element second) {
        // starting from the first element, if the second element is discovered then it is greater than the first
        for (Element bfsElement : latticeGraph.breadthFirstTraversal(first)) {
            if (bfsElement.equals(second)) {
                return 1;
            }
        }
        // starting from the second element, if the first element is discovered then it is less than the first
        for (Element bfsElement : latticeGraph.breadthFirstTraversal(second)) {
            if (bfsElement.equals(first)) {
                return -1;
            }
        }
        // if neither is found then they cannot be compared
        return 0;
    }

}
