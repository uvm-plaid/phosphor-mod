package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.Lattice;

import java.util.Arrays;
import java.util.Collection;

public enum TaintLevel {
    NOT_TAINTED,
    MAYBE_TAINTED,
    TAINTED;

    private static Lattice<TaintLevel> taintLevelLattice = new Lattice<>();

    static {
        taintLevelLattice.addElement(NOT_TAINTED);
        taintLevelLattice.addElement(MAYBE_TAINTED);
        taintLevelLattice.addElement(TAINTED);

        taintLevelLattice.addOrdering(NOT_TAINTED, MAYBE_TAINTED);
        taintLevelLattice.addOrdering(MAYBE_TAINTED, TAINTED);
    }

    public static TaintLevel fromTaint(Taint taintLevel) {
        if (taintLevel == null) {
            return NOT_TAINTED;
        } else {
            return taintLevel.getTaintLevel();
        }
    }

    public static TaintLevel getGreatestLowerBound(Collection<TaintLevel> taintLevels) {
        return taintLevelLattice.getGreatestLowerBound(taintLevels);
    }

    public static TaintLevel getLeastUpperBound(Collection<TaintLevel> taintLevels) {
        return taintLevelLattice.getLeastUpperBound(taintLevels);
    }

    public TaintLevel greatestLowerBound(TaintLevel taintLevel) {
        return taintLevelLattice.getGreatestLowerBound(Arrays.asList(this, taintLevel));
    }

    public TaintLevel leastUpperBound(TaintLevel taintLevel) {
        return taintLevelLattice.getLeastUpperBound(Arrays.asList(this, taintLevel));
    }
}
