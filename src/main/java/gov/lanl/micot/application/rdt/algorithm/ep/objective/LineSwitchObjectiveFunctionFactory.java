package gov.lanl.micot.application.rdt.algorithm.ep.objective;

import gov.lanl.micot.infrastructure.ep.model.ElectricPowerFlowConnection;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerModel;
import gov.lanl.micot.infrastructure.ep.optimize.ObjectiveFunctionFactory;
import gov.lanl.micot.application.rdt.algorithm.AlgorithmConstants;
import gov.lanl.micot.application.rdt.algorithm.ep.variable.LineSwitchVariableFactory;
import gov.lanl.micot.util.math.solver.Variable;
import gov.lanl.micot.util.math.solver.exception.NoVariableException;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgram;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgramObjective;

/**
 * General class for creating line switch construction objective coefficients
 * @author Russell Bent
 */
public class LineSwitchObjectiveFunctionFactory implements ObjectiveFunctionFactory {

  @Override
  public void addCoefficients(MathematicalProgram program, ElectricPowerModel model) throws NoVariableException {
    LineSwitchVariableFactory lineVariableFactory = new LineSwitchVariableFactory();
    MathematicalProgramObjective objective = program.getLinearObjective();
  
    for (ElectricPowerFlowConnection edge : model.getFlowConnections()) {
      if (lineVariableFactory.hasVariable(edge)) {
        Variable variable = lineVariableFactory.getVariable(program, edge);
        double cost = edge.getAttribute(AlgorithmConstants.LINE_SWITCH_COST_KEY) != null ? -edge.getAttribute(AlgorithmConstants.LINE_SWITCH_COST_KEY, Number.class).doubleValue() : 0.0;
        objective.addVariable(variable, cost);
      }      
    }    
  }

}
