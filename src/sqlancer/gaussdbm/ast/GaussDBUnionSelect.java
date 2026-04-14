package sqlancer.gaussdbm.ast;

import java.util.List;

public class GaussDBUnionSelect implements GaussDBExpression {

    private final List<GaussDBSelect> branches;
    private final boolean unionAll;

    public GaussDBUnionSelect(List<GaussDBSelect> branches, boolean unionAll) {
        this.branches = branches;
        this.unionAll = unionAll;
    }

    public List<GaussDBSelect> getBranches() {
        return branches;
    }

    public boolean isUnionAll() {
        return unionAll;
    }
}
