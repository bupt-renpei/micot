package gov.lanl.micot.application.rdt.algorithm.ep.constraint.scenario.cycle.cutset;

import gov.lanl.micot.infrastructure.ep.model.ElectricPowerModel;
import gov.lanl.micot.infrastructure.ep.model.ElectricPowerNode;
import gov.lanl.micot.infrastructure.ep.optimize.ConstraintFactory;
import gov.lanl.micot.infrastructure.model.Scenario;
import gov.lanl.micot.application.rdt.algorithm.ep.variable.scenario.cycle.EdgeActiveVariable;
import gov.lanl.micot.util.math.solver.LinearConstraint;
import gov.lanl.micot.util.math.solver.LinearConstraintLessEq;
import gov.lanl.micot.util.math.solver.exception.InvalidConstraintException;
import gov.lanl.micot.util.math.solver.exception.NoVariableException;
import gov.lanl.micot.util.math.solver.exception.VariableExistsException;
import gov.lanl.micot.util.math.solver.mathprogram.MathematicalProgram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

/**
 * This constraint ensures that there are no cycles in the network
 * 
 * Constraint 15 in the AAAI 2015 paper
 * 
 * @author Russell Bent
 */
public class CycleConstraint implements ConstraintFactory {

  private Map<ElectricPowerNode, Boolean> visited = null;
  private Map<ElectricPowerNode, Integer> depth = null;
  private Map<ElectricPowerNode, Integer> low = null;
  private Map<ElectricPowerNode, ElectricPowerNode> parent = null;

  private ArrayList<Stack<ElectricPowerNode>> cycles = null;

  private Scenario scenario = null;
  
  /**
   * Constraint
   */
  public CycleConstraint(Scenario scenario) {
    this.scenario = scenario;
  }
 
  @Override
  public void constructConstraint(MathematicalProgram problem, ElectricPowerModel model) throws VariableExistsException, NoVariableException, InvalidConstraintException {
    EdgeActiveVariable lineFactory = new EdgeActiveVariable(scenario);

    // grab the cycles
    if (cycles == null) {
      getCyclesBiConnected(model); 
    }

    for (Stack<ElectricPowerNode> tree : cycles) {
      String name = getConstraintName(tree);
      if (problem.getLinearConstraint(name) != null) {
        continue;
      }
        
      LinearConstraint constraint = new LinearConstraintLessEq(name);
      constraint.setRightHandSide(tree.size() - 2);

      Iterator<ElectricPowerNode> it = tree.iterator();
      ElectricPowerNode node1 = it.next();

      while (it.hasNext()) {
        ElectricPowerNode node2 = it.next();
        constraint.addVariable(lineFactory.getVariable(problem, node1, node2), 1.0);
        node1 = node2;
      }
      problem.addLinearConstraint(constraint);
    }
  }

  /**
   * Get the flow constraint name
   * 
   * @param edge
   * @return
   */
  private String getConstraintName(Collection<ElectricPowerNode> nodes) {
    TreeSet<ElectricPowerNode> tree = new TreeSet<ElectricPowerNode>();
    tree.addAll(nodes);

    String constraintName = "NoCycle-" + scenario + "-";
    for (ElectricPowerNode node : tree) {
      constraintName += node + ",";
    }
    return constraintName;
  }

  /**
   * The following code performs a biconnected component decomposition of an undirected graph 
   * and uses a DFS variant to find all cycles within each biconnected component.  
   */
  private void getCyclesBiConnected(ElectricPowerModel model) {
    cycles = new ArrayList<Stack<ElectricPowerNode>>();

    visited = new HashMap<ElectricPowerNode, Boolean>();
    depth = new HashMap<ElectricPowerNode, Integer>();
    low = new HashMap<ElectricPowerNode, Integer>();
    parent = new HashMap<ElectricPowerNode, ElectricPowerNode>();
    
    for (ElectricPowerNode node : model.getNodes()) {
      visited.put(node, false);
      depth.put(node, -1);
      low.put(node, -1);
    }
        
    ArrayList<ArrayList<ArrayList<ElectricPowerNode>>> c = new ArrayList<ArrayList<ArrayList<ElectricPowerNode>>>();
    Stack<ArrayList<ElectricPowerNode>> s = new Stack<ArrayList<ElectricPowerNode>>();
    dfs(model, s, c);
          
    for (int i = 0; i < c.size(); ++i) {
      Set<ElectricPowerNode> V = new HashSet<ElectricPowerNode>();
      generateSubGraph(c.get(i), V);
      ArrayList<ArrayList<ElectricPowerNode>> E = new ArrayList<ArrayList<ElectricPowerNode>>();
      subGraphDFS(model, E, V);
      for (int j = 0; j < E.size(); ++j) {
        Stack<ElectricPowerNode> path = new Stack<ElectricPowerNode>();
        path.add(E.get(j).get(0));
        findAllstPaths(model, cycles, path, E.get(j).get(0), E.get(j).get(1), V);
      }
    } 
    
    // make it an explict loop
    for (Stack<ElectricPowerNode> cycle : cycles) {
      cycle.push(cycle.get(0));
    }

    // clear out memory
    visited = new HashMap<ElectricPowerNode, Boolean>();
    depth = new HashMap<ElectricPowerNode, Integer>();
    low = new HashMap<ElectricPowerNode, Integer>();
    parent = new HashMap<ElectricPowerNode, ElectricPowerNode>();    
  }

  /**
   * Perform the depth first search
   * @param model
   * @param s
   * @param c
   */
  private void dfs(ElectricPowerModel model, Stack<ArrayList<ElectricPowerNode>> s, ArrayList<ArrayList<ArrayList<ElectricPowerNode>>> c) {
    int depth = 0;
    for (ElectricPowerNode v : model.getNodes()) {
      if (!visited.get(v)) {
        dfs_visit (model, s, v, depth, c); 
      }
    }
  }

  private void generateSubGraph(ArrayList<ArrayList<ElectricPowerNode>> C, Set<ElectricPowerNode> V) {
    for (ArrayList<ElectricPowerNode> it : C) {
      V.add(it.get(0));
      V.add(it.get(1));
    } 
  }
  
  /**
   * Visit the depth first search
   * @param model
   * @param s
   * @param u
   * @param d
   * @param c
   */
  private void dfs_visit(ElectricPowerModel model, Stack<ArrayList<ElectricPowerNode>> s, ElectricPowerNode u, int d, ArrayList<ArrayList<ArrayList<ElectricPowerNode>>> c) {
    d = d+1;
    visited.put(u, true);
    depth.put(u, d);
    low.put(u, d);
    
    for (ElectricPowerNode v : model.getNeighbors(u)) {
      if (!(visited.get(v))) {
        ArrayList<ElectricPowerNode> e = new ArrayList<ElectricPowerNode>();
        e.add(u); e.add(v);
        s.push(e);
        parent.put(v,u);
        dfs_visit(model, s, v, d, c);
        if (low.get(v) >= depth.get(u)) {
          outputComponent(s, e, c);
        }
        low.put(u, Math.min(low.get(u), low.get(v)));
      } 
      else if (parent.get(u) != null && depth.get(v) < depth.get(u) && !parent.get(u).equals(v)) {
        // uv is a back edge from u to its ancestor v
        ArrayList<ElectricPowerNode> e = new ArrayList<ElectricPowerNode>();
        e.add(u); e.add(v);
        s.push(e);
        low.put(u,Math.min(low.get(u), depth.get(v)));
      }
    }
  }

  /**
   * Visit the depth first search of a sub graph
   * @param model
   * @param s
   * @param u
   * @param d
   * @param vSet
   */
  private void subGraphDFS_visit(ElectricPowerModel model, ArrayList<ArrayList<ElectricPowerNode>> s, ElectricPowerNode u, int d, Set<ElectricPowerNode> vSet) {
    d = d+1;
    visited.put(u, true);
    depth.put(u,d);
    low.put(u,d);
    
    for (ElectricPowerNode v : model.getNeighbors(u)) {
      if (!(visited.get(v)) && vSet.contains(v)) {
        parent.put(v, u);
        subGraphDFS_visit(model, s, v, d, vSet);
      } 
      else if (parent.get(u) != null && vSet.contains(v) && !parent.get(u).equals(v)) {
        // uv is a back edge
        ArrayList<ElectricPowerNode> e = new ArrayList<ElectricPowerNode>();
        e.add(u); e.add(v);
        s.add(e); 
      }
    }
  }

  /**
   * Do a depth first search on a sub graph
   * @param model
   * @param s
   * @param v
   */
  private void subGraphDFS(ElectricPowerModel model, ArrayList<ArrayList<ElectricPowerNode>> s, Set<ElectricPowerNode> v) {
    int depth = 0;
    for (ElectricPowerNode node : model.getNodes()) {
      visited.put(node, false);
    }    
    for (ElectricPowerNode it : v) {
      if (!visited.get(it)) {
        subGraphDFS_visit (model, s, it, depth, v); 
      }
    }
  }

  /**
   * Find all paths between v and t
   * @param model
   * @param cycles
   * @param path
   * @param u
   * @param t
   * @param vSet
   */
  private void findAllstPaths(ElectricPowerModel model, ArrayList<Stack<ElectricPowerNode>> cycles, Stack<ElectricPowerNode> path, ElectricPowerNode u, ElectricPowerNode t, Set<ElectricPowerNode> vSet) {
    for (ElectricPowerNode v : model.getNeighbors(u)) {
      if (v.equals(t) && path.size() == 1) {
        continue;
      }
      if (!visited(path, v) && vSet.contains(v)) {
        if (v.equals(t)) {
          path.add(v);
          insertPath(cycles, path);
          path.pop();
        } 
        else {
          path.push(v);
          findAllstPaths(model, cycles, path, v, t, vSet);
          path.pop();
        }
      }
    }
  }

  /**
   * Output a component of the graph
   * @param s
   * @param e
   * @param c
   */
  private void outputComponent (Stack<ArrayList<ElectricPowerNode>> s, ArrayList<ElectricPowerNode> e, ArrayList<ArrayList<ArrayList<ElectricPowerNode>>> c) {
    ArrayList<ArrayList<ElectricPowerNode>> Comp = new ArrayList<ArrayList<ElectricPowerNode>>();
    ArrayList<ElectricPowerNode> f = new ArrayList<ElectricPowerNode>();
    do {
      f = s.peek();
      Comp.add(f);
      s.pop();
    } 
    while (!f.get(0).equals(e.get(0)) || !f.get(1).equals(e.get(1)));
    c.add(Comp);
  }

  /**
   * Determine if a node has been visited or not
   * @param path
   * @param v
   * @return
   */
  private boolean visited(Stack<ElectricPowerNode> path, ElectricPowerNode v) {
    for (int i = 0; i < path.size(); ++i) {
      if (path.get(i).equals(v)) {
        return true;
      }
    }
    return false; 
  }

  @SuppressWarnings("unchecked")
  /**
   * Add a path to the list of cycles
   * @param cycles
   * @param path
   */
  private void insertPath(ArrayList<Stack<ElectricPowerNode>> cycles, Stack<ElectricPowerNode> path) {
    TreeSet<ElectricPowerNode> sorted = new TreeSet<ElectricPowerNode>();
    sorted.addAll(path);    
    if (isNewPath(cycles, path)) {
      cycles.add((Stack<ElectricPowerNode>) path.clone());
    }
  }

  /**
   * Determine if a path is new or not
   * @param cycles
   * @param path
   * @return
   */
  private boolean isNewPath(ArrayList<Stack<ElectricPowerNode>> cycles, Stack<ElectricPowerNode> path) {
    int n = path.size();
    for (int i = 0; i < cycles.size(); ++i) {
      if (n == cycles.get(i).size()) {
        if (equalPath(path, cycles.get(i))) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Are two paths equal or not
   * @param p_1
   * @param p_2
   * @return
   */
  private boolean equalPath (Stack<ElectricPowerNode> path1, Stack<ElectricPowerNode> path2) {
    int n = path1.size();
    for (int j = 0; j < n; ++j) {
      if (path1.get(j) != path2.get(j)) {
        return false;
      }
    }
    return true;
  }

  
}
