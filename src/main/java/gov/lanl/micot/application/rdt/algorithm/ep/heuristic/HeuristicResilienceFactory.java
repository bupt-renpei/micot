package gov.lanl.micot.application.rdt.algorithm.ep.heuristic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import gov.lanl.micot.application.rdt.algorithm.AlgorithmConstants;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerModel;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerNode;
import gov.lanl.micot.infrastructure.model.Scenario;
import gov.lanl.micot.infrastructure.optimize.Optimizer;
import gov.lanl.micot.infrastructure.optimize.OptimizerFactoryImpl;
import gov.lanl.micot.infrastructure.optimize.OptimizerFlags;
import gov.lanl.micot.infrastructure.optimize.mathprogram.MathProgramOptimizerFlags;
import gov.lanl.micot.infrastructure.project.AlgorithmConfiguration;
import gov.lanl.micot.infrastructure.project.ProjectConfiguration;
import gov.lanl.micot.infrastructure.project.ScenarioConfiguration;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgramFlags;

/**
 * A factory for creating a heuristic algorithm
 * @author Russell Bent
 */
public class HeuristicResilienceFactory extends OptimizerFactoryImpl<ElectricPowerNode, ElectricPowerModel> {
	
	/**
	 *  Constructor
	 */
	public HeuristicResilienceFactory() {		
	}
	
	@Override
	public HeuristicResilienceAlgorithm createOptimizer(OptimizerFlags oFlags) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
	  HeuristicResilienceFlags flags = new HeuristicResilienceFlags(oFlags);
	  
	  Collection<Scenario> scenarios = flags.getCollection(HeuristicResilienceFlags.SCENARIOS_KEY, Scenario.class);
    
	  HeuristicResilienceAlgorithm algorithm = new HeuristicResilienceAlgorithm(scenarios);
	  
	  double innerTimeout = oFlags.containsKey(MathematicalProgramFlags.TIMEOUT_FLAG) ? oFlags.getDouble(MathematicalProgramFlags.TIMEOUT_FLAG) : Double.POSITIVE_INFINITY;
    double outerTimeout = oFlags.containsKey(MathematicalProgramFlags.TIMEOUT_FLAG) ? oFlags.getDouble(MathematicalProgramFlags.TIMEOUT_FLAG) : Double.POSITIVE_INFINITY;
	  
	  algorithm.addInnerMathProgramFlag(MathematicalProgramFlags.DEBUG_ON_FLAG, false);
	  algorithm.addInnerMathProgramFlag(MathematicalProgramFlags.MIP_GAP_TOLERANCE_FLAG, 1e-3);
    algorithm.addInnerMathProgramFlag(MathematicalProgramFlags.TIMEOUT_FLAG, innerTimeout);
	  algorithm.addOuterMathProgramFlag(MathematicalProgramFlags.DEBUG_ON_FLAG, false);
	  algorithm.addOuterMathProgramFlag(MathematicalProgramFlags.MIP_GAP_TOLERANCE_FLAG, 1e-3);
	  algorithm.addOuterMathProgramFlag(MathematicalProgramFlags.TIMEOUT_FLAG, outerTimeout);

	  // pull out and overwrite parameters
	  for (String key : flags.keySet()) {
	    if (key.startsWith(HeuristicResilienceFlags.OUTER_PREFIX)) {
	      algorithm.addOuterMathProgramFlag(key.substring(HeuristicResilienceFlags.OUTER_PREFIX.length(), key.length()), flags.get(key));
	    }
	    if (key.startsWith(HeuristicResilienceFlags.INNER_PREFIX)) {
	      algorithm.addInnerMathProgramFlag(key.substring(HeuristicResilienceFlags.INNER_PREFIX.length(), key.length()), flags.get(key));
	    }
	  }
    
	  double criticalLoadMet = oFlags.getDouble(AlgorithmConstants.CRITICAL_LOAD_MET_KEY);
	  double nonCriticalLoadMet = oFlags.getDouble(AlgorithmConstants.LOAD_MET_KEY);
    String powerflow = oFlags.getString(AlgorithmConstants.POWER_FLOW_MODEL_KEY);    
    double threshold = oFlags.getDouble(AlgorithmConstants.PHASE_VARIATION_KEY);
    boolean isDiscrete = oFlags.getBoolean(AlgorithmConstants.IS_DISCRETE_MODEL_KEY);
   
	  algorithm.setCriticalLoadMet(criticalLoadMet);
    algorithm.setNonCriticalLoadMet(nonCriticalLoadMet);
    algorithm.setFlowModel(powerflow);
    algorithm.setPhaseVariationThreshold(threshold);
    algorithm.setIsDiscrete(isDiscrete);
    
	  return algorithm;
	}
 
  @Override
  public Optimizer<ElectricPowerNode,ElectricPowerModel> constructOptimizer(ProjectConfiguration projectConfiguration, AlgorithmConfiguration configuration, ElectricPowerModel model) {
    OptimizerFlags flags = new OptimizerFlags();
    flags.fill(configuration.getAlgorithmFlags());
    Collection<Scenario> scenarios = new ArrayList<Scenario>();
    for (ScenarioConfiguration sc : projectConfiguration.getScenarioConfigurations()) {
      scenarios.add(sc.getScenario());
    }    
    flags.put(MathProgramOptimizerFlags.SCENARIOS_KEY, scenarios);

    try {
      return createOptimizer(flags);
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    } 
  }

  

	
}
