package gov.lanl.micot.infrastructure.ep.simulate.dew;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import gov.lanl.micot.infrastructure.ep.model.ElectricPowerModel;
import gov.lanl.micot.infrastructure.model.Scenario;
import gov.lanl.micot.infrastructure.project.ProjectConfiguration;
import gov.lanl.micot.infrastructure.project.ScenarioConfiguration;
import gov.lanl.micot.infrastructure.project.SimulatorConfiguration;
import gov.lanl.micot.infrastructure.simulate.Simulator;
import gov.lanl.micot.infrastructure.simulate.SimulatorFactory;
import gov.lanl.micot.infrastructure.simulate.SimulatorFlags;

/**
 * A factory for creating Ieiss Simulations
 * @author Russell Bent
 */
public class DewSimulatorFactory implements SimulatorFactory<ElectricPowerModel> {
	
	/**
	 *  Constructor
	 */
	public DewSimulatorFactory() {		
	}
	
	@Override
	public DewSimulator createSimulator(SimulatorFlags flags) throws IOException {
	  DewSimulatorFlags ieissFlags = new DewSimulatorFlags(flags);
	  DewSimulator simulator = new DewSimulator();
		return simulator;
	}
	
  @Override
  public Simulator<ElectricPowerModel> constructSimulator(ProjectConfiguration projectConfiguration, SimulatorConfiguration configuration, ElectricPowerModel model) {
    SimulatorFlags flags = new SimulatorFlags();
    flags.fill(configuration.getSimulatorFlags());
    
    Collection<Scenario> scenarios = new ArrayList<Scenario>();
    for (ScenarioConfiguration sc : projectConfiguration.getScenarioConfigurations()) {
      scenarios.add(sc.getScenario());
    }    
    flags.put(SimulatorFlags.SCENARIOS_KEY, scenarios);
    
    try {
      return createSimulator(flags);
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    } 
  }

}
