package gov.lanl.micot.application.rdt.algorithm.ep.variable.scenario;

import gov.lanl.micot.infrastructure.ep.model.ElectricPowerModel;
import gov.lanl.micot.infrastructure.ep.model.Generator;
import gov.lanl.micot.infrastructure.ep.optimize.VariableFactory;
import gov.lanl.micot.infrastructure.model.Scenario;
import gov.lanl.micot.application.rdt.algorithm.AlgorithmConstants;
import gov.lanl.micot.util.math.solver.Variable;
import gov.lanl.micot.util.math.solver.exception.InvalidVariableException;
import gov.lanl.micot.util.math.solver.exception.NoVariableException;
import gov.lanl.micot.util.math.solver.exception.VariableExistsException;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgram;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Generator construction variables
 * 
 * These are the u_i variables from the paper
 * 
 * @author Russell Bent
 */
public class GeneratorConstructionVariableFactory implements VariableFactory {

  private Scenario scenario = null;
  private Boolean isDiscrete = true;
  
  /**
   * Constructor
   * @param scenario
   */
  public GeneratorConstructionVariableFactory(Scenario scenario, Boolean isDiscrete) {
    this.scenario = scenario;
    this.isDiscrete = isDiscrete;
  }
  
  /**
   * Function for create the variable name associated with microgrid size
   * 
   * @param node
   * @return
   */
  private String getGeneratorVariableName(Generator node, Scenario scenario) {
    return "u-" + node.toString() + "-" + scenario.getIndex();
  }
  
  @Override
  public Collection<Variable> createVariables(MathematicalProgram program, ElectricPowerModel model) throws VariableExistsException, InvalidVariableException {
    ArrayList<Variable> variables = new ArrayList<Variable>();    
    for (Generator producer : model.getGenerators()) {
      if (hasVariable(producer)) {
        if (isDiscrete) {
          variables.add(program.makeDiscreteVariable(getGeneratorVariableName(producer, scenario)));
        }
        else {
          variables.add(program.makeContinuousVariable(getGeneratorVariableName(producer, scenario)));          
        }
      }
    }     
    return variables;
  }

  /**
   * Determine if this 
   * @param producer
   * @return
   */
  public boolean hasVariable(Generator producer) {
    boolean isNew = producer.getAttribute(AlgorithmConstants.IS_NEW_MICROGRID_KEY) != null && producer.getAttribute(AlgorithmConstants.IS_NEW_MICROGRID_KEY, Boolean.class);                 
    return producer.getStatus() && isNew;
  }
  
  @Override
  public Variable getVariable(MathematicalProgram program, Object asset) throws NoVariableException {
    return null;
  }
  
  public Variable getVariable(MathematicalProgram program, Generator generator, Scenario scenario) throws NoVariableException {
    return program.getVariable(getGeneratorVariableName(generator, scenario));
  }

  
}
