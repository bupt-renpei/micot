package gov.lanl.micot.deprecated.rdt.algorithm.ep.mip.variable.scenario;

import gov.lanl.micot.infrastructure.ep.model.ElectricPowerModel;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerNode;
import gov.lanl.micot.infrastructure.model.Scenario;
import gov.lanl.micot.infrastructure.optimize.mathprogram.variable.ScenarioVariableFactory;
import gov.lanl.micot.util.math.solver.Variable;
import gov.lanl.micot.util.math.solver.exception.InvalidVariableException;
import gov.lanl.micot.util.math.solver.exception.NoVariableException;
import gov.lanl.micot.util.math.solver.exception.VariableExistsException;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgram;

import java.util.ArrayList;
import java.util.Collection;

/**
 * variable for determining if a scenario has a resilience constraint is violated
 * 
 * @author Russell Bent
 */
public class ScenarioChanceConstrainedVariableFactory extends ScenarioVariableFactory<ElectricPowerNode, ElectricPowerModel> {

  /**
   * Constructor
   * 
   * @param scenarios
   */
  public ScenarioChanceConstrainedVariableFactory(Collection<Scenario> scenarios) {
    super(scenarios);
  }

  /**
   * Function for create the variable name associated with a load
   * 
   * @param node
   * @return
   */
  private String getVariableName(Scenario scenario) {
    return "ChanceConstraint-" + scenario.getIndex();
  }

  @Override
  public Collection<Variable> createVariables(MathematicalProgram program, ElectricPowerModel model) throws VariableExistsException, InvalidVariableException {
    ArrayList<Variable> variables = new ArrayList<Variable>();

    for (Scenario scenario : getScenarios()) {
      variables.add(program.makeDiscreteVariable(getVariableName(scenario)));
    }
   
    return variables;
  }

  @Override
  public Variable getVariable(MathematicalProgram program, Object asset) throws NoVariableException {
    return null;
  }

  /**
   * Get a variable
   * 
   * @param program
   * @param load
   * @param scenario
   * @return
   * @throws NoVariableException
   */
  public Variable getVariable(MathematicalProgram program, Scenario scenario) throws NoVariableException {
    return program.getVariable(getVariableName(scenario));
  }

}
