package gov.lanl.micot.application.lpnorm.io;

import gov.lanl.micot.infrastructure.config.AssetModification;
import gov.lanl.micot.infrastructure.ep.io.ElectricPowerModelFileFactory;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerFlowConnection;
import gov.lanl.micot.infrastructure.ep.optimize.ElectricPowerMathProgramOptimizerFlags;
import gov.lanl.micot.infrastructure.model.Asset;
import gov.lanl.micot.infrastructure.project.AlgorithmConfiguration;
import gov.lanl.micot.infrastructure.project.ApplicationConfiguration;
import gov.lanl.micot.infrastructure.project.ModelConfiguration;
import gov.lanl.micot.infrastructure.project.ProjectConfiguration;
import gov.lanl.micot.infrastructure.project.ScenarioConfiguration;
import gov.lanl.micot.infrastructure.project.SimulatorConfiguration;
import gov.lanl.micot.application.lpnorm.model.LPNormModelConstants;
import gov.lanl.micot.application.rdt.algorithm.AlgorithmConstants;
import gov.lanl.micot.application.rdt.algorithm.ep.bp.BPResilienceFactory;
import gov.lanl.micot.application.rdt.algorithm.ep.heuristic.HeuristicResilienceFactory;
import gov.lanl.micot.application.rdt.algorithm.ep.mip.MIPResilienceFactory;
import gov.lanl.micot.application.rdt.RDTApplicationFactory;
import gov.lanl.micot.util.io.Flags;
import gov.lanl.micot.util.io.FlagsImpl;
import gov.lanl.micot.util.io.json.JSON;
import gov.lanl.micot.util.io.json.JSONArray;
import gov.lanl.micot.util.io.json.JSONObject;
import gov.lanl.micot.util.io.json.JSONReader;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgramFlags;
import gov.lanl.micot.util.math.solver.quadraticprogram.bonmin.BonminQuadraticProgramFactory;
import gov.lanl.micot.util.math.solver.quadraticprogram.scip.ScipQuadraticProgramFactory;

//import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Class for reading in an LPNORM configuration from Json schema
 * 
 * @author Russell Bent
 */
public class LPNormJsonProjectConfigurationReader {

  private static final double DEFAULT_CRITICAL_LOAD_MET = 0.98;
  private static final double DEFAULT_TOTAL_LOAD_MET = 0.5;
  private static final double DEFAULT_PHASE_VARIATION = 0.15;

  private static final LPNormFidelity DEFAULT_FIDELITY = LPNormFidelity.LOW;
  private static final String DEFAULT_SOLVER = LPNormIOConstants.SCIP_TAG;
  private static final double DEFAULT_TIMEOUT = Double.POSITIVE_INFINITY;
  
  /**
   * Constructor
   */
  public LPNormJsonProjectConfigurationReader() {    
  }
  
  /**
   * Read in the configuration files
   * @param applicationFilename
   * @param algorithmFilenames
   * @param modelFilenames
   * @param scenarioFilenames
   * @param simulatorFilenames
   * @return
   * @throws IOException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   */
  public ProjectConfiguration readConfiguration(String filename) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {    
    ProjectConfiguration configuration = null;
    try {
      FileInputStream fstream = new FileInputStream(filename);
      JSONReader reader = JSON.getDefaultJSON().createReader(fstream);
      JSONObject object = reader.readObject();
      
      JSONObject algorithm = object;
      JSONObject model = object;
      JSONArray scenarios = object.getArray(LPNormIOConstants.SCENARIOS_TAG);
      JSONObject simulator = object;
      JSONObject application = object;

      configuration = createConfigurationFrom(filename, application, algorithm, model, scenarios, simulator);
    }
    catch (FileNotFoundException e) {
      System.out.println("Error: The file " + filename + " does not exist. Program will exit abnormally.");
      System.exit(-1);     
//      throw new RuntimeException(e.getMessage());
    }
    return configuration;
  }


  /**
   * 
   * Pull the data from the JSON objects
   * 
   * @param applicationObject
   * @param algorithmObjects
   * @param modelObjects
   * @param scenarioObjects
   * @param simulatorObjects
   * @return
   * @throws ConfigXMLIOException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   */
  private ProjectConfiguration createConfigurationFrom(String filename, JSONObject application, JSONObject algorithm, JSONObject model, JSONArray scenarios, JSONObject simulator) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    ProjectConfiguration configuration = new ProjectConfiguration();

    // get the application data
    readApplication(configuration, application);
    
    // get the algorithm data
    readAlgorithm(configuration, algorithm);
        
    // get the model data
    readModel(configuration, model, filename);

    // get the simulator data
    readSimulator(configuration,simulator);

    // count number of scenarios
    double numScenarios = scenarios.size();
    
    // get the scenario data
    for (int i = 0; i < scenarios.size(); ++i) {
      JSONObject scenario = scenarios.getObject(i);
      readScenario(configuration, scenario, i+1, 1.0 / numScenarios);
    }
    return configuration;
  }

  /**
   * Fill the configuration data
   * @param configuration
   * @param scenarioDocument
   * @throws ConfigXMLIOException 
   */
  private void readScenario(ProjectConfiguration configuration, JSONObject scenario, int defaultIdx, double defaultProb) {
    double probability = defaultProb;    
    ScenarioConfiguration scenarioConfiguration = new ScenarioConfiguration(defaultIdx, probability);
    String build = "";    
    scenarioConfiguration.setScenarioBuilderFactoryClass(build);

    Flags flags = new FlagsImpl();
    scenarioConfiguration.setScenarioBuilderFlags(flags);
    
    JSONArray disabledModifications = scenario.getArray(LPNormIOConstants.SCENARIO_DISABLED_TAG);
    JSONArray hardenedDisabledModifications = scenario.getArray(LPNormIOConstants.SCENARIO_HARDENED_DISABLED_TAG);

    Collection<AssetModification> modifications = readModifications(disabledModifications,hardenedDisabledModifications);    
    scenarioConfiguration.setComponentModifications(modifications);
    configuration.addScenarioConfiguration(scenarioConfiguration);    
  }

  /**
   * Read out the algorithm choices and fill in defaults if necessary
   * @param fidelity
   * @param configuration
   * @param algorithm
   */
  private void readAlgorithmChoice(LPNormFidelity fidelity, AlgorithmConfiguration configuration, JSONObject algorithm) {
    String algorithmChoice = null;
    
    if (fidelity == LPNormFidelity.LOW || fidelity == LPNormFidelity.MEDIUM_LOW || fidelity == LPNormFidelity.MEDIUM) {
      algorithmChoice = LPNormIOConstants.HEURISTIC_TAG;
    }
    else if (fidelity == LPNormFidelity.MEDIUM_HIGH || fidelity == LPNormFidelity.HIGH) {
      algorithmChoice = LPNormIOConstants.SBD_TAG;      
    }
    
    if (algorithm.containsKey(LPNormIOConstants.ALGORITHM_TAG)) {
      algorithmChoice = algorithm.getString(LPNormIOConstants.ALGORITHM_TAG);
    }
  
    if (algorithmChoice.equals(LPNormIOConstants.SBD_TAG)) {
      configuration.setAlgorithmFactoryClass(gov.lanl.micot.application.rdt.algorithm.ep.sbd.SBDResilienceFactory.class.getCanonicalName());
    }
    else if (algorithmChoice.equals(LPNormIOConstants.MIQP_TAG)) { 
      configuration.setAlgorithmFactoryClass(MIPResilienceFactory.class.getCanonicalName());
    }
    else if (algorithmChoice.equals(LPNormIOConstants.BP_TAG)) { 
      configuration.setAlgorithmFactoryClass(BPResilienceFactory.class.getCanonicalName());
    }
    else if (algorithmChoice.equals(LPNormIOConstants.HEURISTIC_TAG)) { 
      configuration.setAlgorithmFactoryClass(HeuristicResilienceFactory.class.getCanonicalName());
    }
    else {
      throw new RuntimeException("Error: " + algorithmChoice + " is not a valid algorithm choice");
    }
  }

  /**
   * Read the power flow choices into the configuration
   * @param fidelity
   * @param flags
   * @param algorithm
   */
  private void readPowerFlowChoice(LPNormFidelity fidelity, Flags flags, JSONObject algorithm) {
    String powerFlowChoice = null;
    if (fidelity == LPNormFidelity.LOW || fidelity == LPNormFidelity.MEDIUM_LOW) {
      powerFlowChoice = AlgorithmConstants.TRANSPORTATION_POWER_FLOW_MODEL;
    }
    else if (fidelity == LPNormFidelity.MEDIUM || fidelity == LPNormFidelity.MEDIUM_HIGH || fidelity == LPNormFidelity.HIGH) {
      powerFlowChoice = AlgorithmConstants.LINDIST_FLOW_POWER_FLOW_MODEL;
    }

    if (algorithm.containsKey(LPNormIOConstants.POWER_FLOW_TAG)) {
      powerFlowChoice = algorithm.getString(LPNormIOConstants.POWER_FLOW_TAG);
    }
 
    flags.put(AlgorithmConstants.POWER_FLOW_MODEL_KEY, powerFlowChoice);    
  }

  /**
   * Read tje discrete choices into algorithm configuration
   * @param fidelity
   * @param flags
   * @param algorithm
   */
  private void readDiscreteChoice(LPNormFidelity fidelity, Flags flags, JSONObject algorithm) {
    boolean isDiscrete = true;
    
    if (fidelity == LPNormFidelity.LOW) {
      isDiscrete = false;
    }
    else if (fidelity == LPNormFidelity.MEDIUM_LOW || fidelity == LPNormFidelity.MEDIUM || fidelity == LPNormFidelity.MEDIUM_HIGH || fidelity == LPNormFidelity.HIGH) {
      isDiscrete = true;
    }

    if (algorithm.containsKey(LPNormIOConstants.IS_DISCRETE_TAG)) {
      isDiscrete = algorithm.getBoolean(LPNormIOConstants.IS_DISCRETE_TAG);
    }
      
    flags.put(AlgorithmConstants.IS_DISCRETE_MODEL_KEY, isDiscrete);
  }
  
  /**
   * Read the cycle choices into algorithm configuration
   * @param fidelity
   * @param flags
   * @param algorithm
   */
  private void readCycleChoice(LPNormFidelity fidelity, Flags flags, JSONObject algorithm) {
    AlgorithmConstants.CycleModel cycleModel = null;
    
    if (fidelity == LPNormFidelity.LOW || fidelity == LPNormFidelity.MEDIUM_LOW || fidelity == LPNormFidelity.MEDIUM || fidelity == LPNormFidelity.MEDIUM_HIGH) {
      cycleModel = AlgorithmConstants.CycleModel.NONE;
    }
    else if (fidelity == LPNormFidelity.HIGH) {
      cycleModel = AlgorithmConstants.CycleModel.FLOW;
    }

    flags.put(AlgorithmConstants.CYCLE_MODEL_CONSTRAINT_KEY, cycleModel);        
  }
  
  private void readSolverChoice(LPNormFidelity fidelity, Flags flags, JSONObject algorithm) {
    String solverChoice = algorithm.containsKey(LPNormIOConstants.SOLVER_TAG) ? algorithm.getString(LPNormIOConstants.SOLVER_TAG) : DEFAULT_SOLVER;
    
    if (solverChoice.equals(LPNormIOConstants.SCIP_TAG)) {
      flags.put(ElectricPowerMathProgramOptimizerFlags.MATH_PROGRAM_FACTORY_KEY, ScipQuadraticProgramFactory.class.getCanonicalName());
    }
    else if (solverChoice.equals(LPNormIOConstants.CPLEX_TAG)) {
      flags.put(ElectricPowerMathProgramOptimizerFlags.MATH_PROGRAM_FACTORY_KEY, "gov.lanl.micot.util.math.solver.quadraticprogram.cplex.CPLEXQuadraticProgramFactory");
    }
    else if (solverChoice.equals(LPNormIOConstants.BONMIN_TAG)) {
      flags.put(ElectricPowerMathProgramOptimizerFlags.MATH_PROGRAM_FACTORY_KEY, BonminQuadraticProgramFactory.class.getCanonicalName());
    }
    else {
      throw new RuntimeException("Error: " + solverChoice + " is not a valid solver choice");
    }

  }  
  
  /**
   * Read the algorithm
   * @param configuration
   * @param log
   * @param path
   * @throws ConfigXMLIOException
   */
  private void readAlgorithm(ProjectConfiguration projectConfiguration, JSONObject algorithm)  {    
    AlgorithmConfiguration configuration = new AlgorithmConfiguration();
    Flags flags = new FlagsImpl();
  
    LPNormFidelity fidelity = algorithm.containsKey(LPNormIOConstants.FIDELITY_TAG) ? LPNormFidelity.get(algorithm.getInt(LPNormIOConstants.FIDELITY_TAG)) : DEFAULT_FIDELITY;
    readAlgorithmChoice(fidelity, configuration, algorithm);
    readPowerFlowChoice(fidelity, flags, algorithm);
    readDiscreteChoice(fidelity, flags, algorithm);
    readCycleChoice(fidelity, flags, algorithm);  
    readSolverChoice(fidelity, flags, algorithm);
    
    double timeout = algorithm.containsKey(LPNormIOConstants.SOLVER_TIMEOUT_TAG) ? algorithm.getDouble(LPNormIOConstants.SOLVER_TIMEOUT_TAG) : DEFAULT_TIMEOUT;    
    double criticalLoadMet = algorithm.containsKey(LPNormIOConstants.CRITICAL_LOAD_MET_TAG) ? algorithm.getDouble(LPNormIOConstants.CRITICAL_LOAD_MET_TAG) : DEFAULT_CRITICAL_LOAD_MET;
    double totalMet = algorithm.containsKey(LPNormIOConstants.TOTAL_LOAD_MET_TAG) ? algorithm.getDouble(LPNormIOConstants.TOTAL_LOAD_MET_TAG) : DEFAULT_TOTAL_LOAD_MET;
    double phaseVariation = algorithm.containsKey(LPNormIOConstants.PHASE_VARIATION_TAG) ? algorithm.getDouble(LPNormIOConstants.PHASE_VARIATION_TAG) : DEFAULT_PHASE_VARIATION;
        
    flags.put(AlgorithmConstants.CRITICAL_LOAD_MET_KEY, criticalLoadMet);
    flags.put(AlgorithmConstants.LOAD_MET_KEY, totalMet);
    flags.put(AlgorithmConstants.PHASE_VARIATION_KEY, phaseVariation);
    flags.put(MathematicalProgramFlags.TIMEOUT_FLAG, timeout);
            
    configuration.setAlgorithmFlags(flags);    
    projectConfiguration.addAlgorithmConfiguration(configuration);
  }

  /**
   * Read the application data
   * @param projectConfiguration
   * @param application
   * @throws ConfigXMLIOException
   */
  private void readApplication(ProjectConfiguration projectConfiguration, JSONObject application)  {
    ApplicationConfiguration configuration = new ApplicationConfiguration();    
    configuration.setApplicationFactoryClass(RDTApplicationFactory.class.getCanonicalName());
    Flags flags = new FlagsImpl();
    configuration.setApplicationFlags(flags);    
    projectConfiguration.setApplicationConfiguration(configuration);
  }

  /**
   * Read the algorithm
   * @param configuration
   * @param log
   * @param path
   * @throws ConfigXMLIOException
   */
  private void readSimulator(ProjectConfiguration projectConfiguration, JSONObject simulator) {    
    SimulatorConfiguration configuration = new SimulatorConfiguration();    
    configuration.setSimulatorFactoryClass(null);
    Flags flags = new FlagsImpl();
    configuration.setSimulatorFlags(flags);    
    projectConfiguration.addSimulatorConfiguration(configuration);
  }
  
  /**
   * Reads in the model data
   * @param configuration
   * @param model
   * @throws ConfigXMLIOException
   */
  private void readModel(ProjectConfiguration projectConfiguration, JSONObject model, String filename)  {
    ModelConfiguration configuration = new ModelConfiguration();
    String modelFileFactoryClass = ElectricPowerModelFileFactory.class.getCanonicalName();
    configuration.setModelFileFactoryClass(modelFileFactoryClass);
    configuration.setModelFile(filename);

    ElectricPowerModelFileFactory.registerExtension("json", LPNormFile.class);
    
    Collection<AssetModification> mods = new ArrayList<AssetModification>(); 
    configuration.setComponentModifications(mods);
    projectConfiguration.addModelConfiguration(configuration);    
  }
  
  /**
   * Reads in the model modification data
   * @param configuration
   * @param modifications
   * @throws ConfigXMLIOException 
   */
  private Collection<AssetModification> readModifications(JSONArray disabled, JSONArray hardenedDisabled)  {
    ArrayList<AssetModification> mods = new ArrayList<AssetModification>();
    // get the disabled modifications
    for (int i = 0; i < disabled.size(); ++i) {
      String id = disabled.getString(i);
      AssetModification config = new AssetModification();

      // get the component type
      String type = ElectricPowerFlowConnection.class.getCanonicalName(); 
      config.setComponentClass(type);

      // add the identifier
      String key = id;
      config.addKey(LPNormModelConstants.LPNORM_LEGACY_ID_KEY, key);

      // add the attribute that is modified
      config.addAttribute(Asset.IS_FAILED_KEY, true);
      mods.add(config);
    }
    
    // get the harden disabled modifications
    for (int i = 0; i < hardenedDisabled.size(); ++i) {
      String id = hardenedDisabled.getString(i);
      AssetModification config = new AssetModification();

      // get the component type
      String type = ElectricPowerFlowConnection.class.getCanonicalName(); 
      config.setComponentClass(type);

      // add the identifier
      String key = id;
      config.addKey(LPNormModelConstants.LPNORM_LEGACY_ID_KEY, key);

      // add the attribute that is modified
      config.addAttribute(AlgorithmConstants.HARDENED_DISABLED_KEY, true);
      mods.add(config);
    }
    
    return mods;
  }
    
  /**
   * Parse the provided string content into a JsonObject.
   * @return.
   */
  /*private JSONObject parse(String content) {
    InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));    
    JSONReader reader = JSON.getDefaultJSON().createReader(stream);
    JSONObject object = reader.readObject();
    return object;
  }*/
  

}
