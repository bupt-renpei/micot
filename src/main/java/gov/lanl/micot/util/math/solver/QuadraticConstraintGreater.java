package gov.lanl.micot.util.math.solver;

import gov.lanl.micot.util.collection.Pair;

/**
 * Greater than constraint
 * @author Russell Bent
 */
public class QuadraticConstraintGreater extends QuadraticConstraint {

	/**
	 * Constructor
	 * @param name
	 */
	protected QuadraticConstraintGreater(String name) {
		super(name);
	}
	
	@Override
	protected void visit(QuadraticConstraintVisitor v) {
		v.applyConstraintGreater(this);
	}


	protected String getRHSExpr(){
		return 	" > " + _rhs;
	}

  @Override
  public boolean isSatisfied(Solution solution) {
    double lhs = 0;
    for (Variable variable : getVariables()) {
      lhs += getCoefficient(variable) * solution.getValueDouble(variable);
    }    
    
    for (Pair<Variable,Variable> vars : getVariablePairs()) {
      lhs += getCoefficient(vars.getOne(), vars.getTwo()) * solution.getValueDouble(vars.getOne()) * solution.getValueDouble(vars.getTwo());
    }

    
    return lhs > _rhs;
  }


}
