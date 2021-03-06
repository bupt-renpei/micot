package gov.lanl.micot.application.rdt.algorithm.ep.bound.scenario.cycle.flow;

import gov.lanl.micot.infrastructure.ep.model.ElectricPowerFlowConnection;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerModel;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerNode;
import gov.lanl.micot.infrastructure.ep.optimize.ConstraintFactory;
import gov.lanl.micot.infrastructure.model.Scenario;
import gov.lanl.micot.application.rdt.algorithm.ep.variable.scenario.cycle.flow.FlowVariable;
import gov.lanl.micot.util.math.solver.Variable;
import gov.lanl.micot.util.math.solver.exception.InvalidConstraintException;
import gov.lanl.micot.util.math.solver.exception.NoVariableException;
import gov.lanl.micot.util.math.solver.exception.VariableExistsException;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgram;

/**
 * The flow into the network that is bounded between 0 and |nodes|
 * 
 * @author Russell Bent
 */
public class FlowBound implements ConstraintFactory {

  private Scenario scenario = null;
  
  /**
   * Constraint
   */
  public FlowBound(Scenario scenario) {
    this.scenario = scenario;
  }

  @Override
  public void constructConstraint(MathematicalProgram problem, ElectricPowerModel model) throws VariableExistsException, NoVariableException, InvalidConstraintException {
    FlowVariable vFactory = new FlowVariable(scenario);

    for (ElectricPowerFlowConnection edge : model.getFlowConnections()) {
      ElectricPowerNode node1 = model.getFirstNode(edge);
      ElectricPowerNode node2 = model.getSecondNode(edge);
      Variable variable = vFactory.getVariable(problem, node1, node2);
      problem.addBounds(variable, 0.0, model.getNodes().size());      
    }
  }

}
