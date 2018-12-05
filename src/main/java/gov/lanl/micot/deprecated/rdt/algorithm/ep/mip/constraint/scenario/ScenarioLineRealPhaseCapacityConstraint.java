package gov.lanl.micot.deprecated.rdt.algorithm.ep.mip.constraint.scenario;

import gov.lanl.micot.infrastructure.ep.model.ElectricPowerFlowConnection;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerModel;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerNode;
import gov.lanl.micot.infrastructure.model.Scenario;
import gov.lanl.micot.infrastructure.optimize.mathprogram.constraint.ScenarioConstraintFactory;
import gov.lanl.micot.deprecated.rdt.algorithm.ep.mip.variable.scenario.ScenarioRealFlowPhaseVariableFactory;
import gov.lanl.micot.deprecated.rdt.algorithm.ep.mip.variable.scenario.ScenarioVariableFactoryUtility;
import gov.lanl.micot.deprecated.rdt.algorithm.ep.mip.variable.scenario.ScenarioLineUseVariableFactory;
import gov.lanl.micot.util.math.solver.LinearConstraint;
import gov.lanl.micot.util.math.solver.LinearConstraintGreaterEq;
import gov.lanl.micot.util.math.solver.LinearConstraintLessEq;
import gov.lanl.micot.util.math.solver.Variable;
import gov.lanl.micot.util.math.solver.exception.InvalidConstraintException;
import gov.lanl.micot.util.math.solver.exception.NoVariableException;
import gov.lanl.micot.util.math.solver.exception.VariableExistsException;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgram;

import java.util.Collection;

/**
 * The link capacity constraint for all phases. Constraint goes inactive when
 * the line is not built or available
 * 
 * This is constraint 1 in the AAAI 2015 paper
 * 
 * @author Russell Bent
 */
public class ScenarioLineRealPhaseCapacityConstraint extends ScenarioConstraintFactory<ElectricPowerNode, ElectricPowerModel> {

  /**
   * Constraint
   */
  public ScenarioLineRealPhaseCapacityConstraint(Collection<Scenario> scenarios) {
    super(scenarios);
  }

  /**
   * Get the flow constraint name
   * 
   * @param edge
   * @return
   */
  private String getFlowLessThanConstraintName(ElectricPowerFlowConnection edge, String phase, Scenario scenario) {
    return "CapacityL(p)." + phase + "-" + edge.toString() + "." + scenario.getIndex();
  }

  /**
   * Get the flow constraint name
   * 
   * @param edge
   * @return
   */
  private String getFlowGreaterThanConstraintName(ElectricPowerFlowConnection edge, String phase, Scenario scenario) {
    return "CapacityG(p)." + phase + "-" + edge.toString() + "." + scenario.getIndex();
  }

  @Override
  public void constructConstraint(MathematicalProgram problem, ElectricPowerModel model) throws VariableExistsException, NoVariableException, InvalidConstraintException {
    ScenarioLineUseVariableFactory directionFactory = new ScenarioLineUseVariableFactory(getScenarios());
    ScenarioRealFlowPhaseVariableFactory flowFactory = new ScenarioRealFlowPhaseVariableFactory(getScenarios());
    double mvaBase = model.getMVABase();
    
    for (Scenario scenario : getScenarios()) {
      for (ElectricPowerFlowConnection link : model.getFlowConnections()) {
        boolean hasVariable = ScenarioVariableFactoryUtility.doCreateLineUseScenarioVariable(link, scenario);
        Integer constant = ScenarioVariableFactoryUtility.getLineUseScenarioConstant(link, scenario);
        
        // the line is not being used, so don't even create the constraint
        if (!hasVariable && constant == 0) {
          continue;
        }
        
        Variable variable = directionFactory.getVariable(problem, link, scenario);
        
        double capacity = link.getCapacityRating() / mvaBase; //(link.getCapacityRating() / link.getAttribute(ElectricPowerFlowConnection.NUMBER_OF_PHASES_KEY, Number.class).doubleValue()) / mvaBase;
        
        if (link.getAttribute(ElectricPowerFlowConnection.HAS_PHASE_A_KEY, Boolean.class)) {
          LinearConstraint constraint = new LinearConstraintLessEq(getFlowLessThanConstraintName(link, ScenarioRealFlowPhaseVariableFactory.PHASE_A, scenario));
          constraint.addVariable(flowFactory.getVariable(problem, link, ScenarioRealFlowPhaseVariableFactory.PHASE_A, scenario), 1.0);
          if (hasVariable) {
            constraint.addVariable(variable, -capacity);
            constraint.setRightHandSide(0.0);
          }
          else {
            constraint.setRightHandSide(capacity * constant);
          }
          problem.addLinearConstraint(constraint);
          
          constraint = new LinearConstraintGreaterEq(getFlowGreaterThanConstraintName(link, ScenarioRealFlowPhaseVariableFactory.PHASE_A, scenario));
          constraint.addVariable(flowFactory.getVariable(problem, link, ScenarioRealFlowPhaseVariableFactory.PHASE_A, scenario), 1.0);
          if (hasVariable) {
            constraint.addVariable(variable, capacity);
            constraint.setRightHandSide(0.0);
          }
          else {
            constraint.setRightHandSide(-capacity * constant);            
          }
          
//          if (link.toString().equals("A_node701-702")) {
  ///          System.out.println(constraint);
     //     }
          
          problem.addLinearConstraint(constraint);          
        }

        if (link.getAttribute(ElectricPowerFlowConnection.HAS_PHASE_B_KEY, Boolean.class)) {
          LinearConstraint constraint = new LinearConstraintLessEq(getFlowLessThanConstraintName(link, ScenarioRealFlowPhaseVariableFactory.PHASE_B, scenario));
          constraint.addVariable(flowFactory.getVariable(problem, link, ScenarioRealFlowPhaseVariableFactory.PHASE_B, scenario), 1.0);
          if (hasVariable) {
            constraint.addVariable(variable, -capacity);
            constraint.setRightHandSide(0.0);
          }
          else {
            constraint.setRightHandSide(capacity * constant);            
          }
          problem.addLinearConstraint(constraint);
                
          constraint = new LinearConstraintGreaterEq(getFlowGreaterThanConstraintName(link, ScenarioRealFlowPhaseVariableFactory.PHASE_B, scenario));
          constraint.addVariable(flowFactory.getVariable(problem, link, ScenarioRealFlowPhaseVariableFactory.PHASE_B, scenario), 1.0);
          if (hasVariable) {
            constraint.addVariable(variable, capacity);
            constraint.setRightHandSide(0.0);
          }
          else {
            constraint.setRightHandSide(-capacity * constant);
          }
          problem.addLinearConstraint(constraint);      
        }

        if (link.getAttribute(ElectricPowerFlowConnection.HAS_PHASE_C_KEY, Boolean.class)) {
          LinearConstraint constraint = new LinearConstraintLessEq(getFlowLessThanConstraintName(link, ScenarioRealFlowPhaseVariableFactory.PHASE_C, scenario));
          constraint.addVariable(flowFactory.getVariable(problem, link, ScenarioRealFlowPhaseVariableFactory.PHASE_C, scenario), 1.0);
          if (hasVariable) {
            constraint.addVariable(variable, -capacity);
            constraint.setRightHandSide(0.0);
          }
          else {
            constraint.setRightHandSide(capacity * constant);                        
          }
          problem.addLinearConstraint(constraint);
          
          constraint = new LinearConstraintGreaterEq(getFlowGreaterThanConstraintName(link, ScenarioRealFlowPhaseVariableFactory.PHASE_C, scenario));
          constraint.addVariable(flowFactory.getVariable(problem, link, ScenarioRealFlowPhaseVariableFactory.PHASE_C, scenario), 1.0);
          if (hasVariable) {
            constraint.addVariable(variable, capacity);
            constraint.setRightHandSide(0.0);
          }
          else {
            constraint.setRightHandSide(-capacity * constant);            
          }
          problem.addLinearConstraint(constraint);          
        }
      }
    }
  }
  

}
