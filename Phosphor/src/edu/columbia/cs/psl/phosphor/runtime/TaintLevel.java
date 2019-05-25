package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.Lattice;

import java.util.Arrays;
import java.util.Collection;

public enum TaintLevel {
    UNKNOWN,
    NOT_TAINTED,
    MAYBE_TAINTED,
    TAINTED;

    private static Lattice<TaintLevel> taintLevelLattice;

    static {
        taintLevelLattice = new Lattice<>(Arrays.asList(values()));

        taintLevelLattice.addOrdering(UNKNOWN, NOT_TAINTED);
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
        return taintLevelLattice.greatestLowerBound(taintLevels);
    }

    public static TaintLevel getLeastUpperBound(Collection<TaintLevel> taintLevels) {
        return taintLevelLattice.leastUpperBound(taintLevels);
    }

    public TaintLevel greatestLowerBound(TaintLevel taintLevel) {
        return taintLevelLattice.greatestLowerBound(Arrays.asList(this, taintLevel));
    }

    public TaintLevel leastUpperBound(TaintLevel taintLevel) {
        return taintLevelLattice.leastUpperBound(Arrays.asList(this, taintLevel));
    }
}
