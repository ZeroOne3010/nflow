package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

import com.nitorcreations.nflow.engine.internal.workflow.StateExecutionImpl;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowDefinitionScanner;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod;

public abstract class AbstractWorkflowDefinition<S extends WorkflowState> {

  private final String type;
  private String name;
  private String description;
  private final S initialState;
  private final S errorState;
  private final WorkflowSettings settings;
  protected final Map<String, List<String>> allowedTransitions = new LinkedHashMap<>();
  protected final Map<String, WorkflowState> failureTransitions = new LinkedHashMap<>();
  private Map<String, WorkflowStateMethod> stateMethods;

  protected AbstractWorkflowDefinition(String type, S initialState, S errorState) {
    this(type, initialState, errorState, new WorkflowSettings(null));
  }

  protected AbstractWorkflowDefinition(String type, S initialState, S errorState, WorkflowSettings settings) {
    Assert.notNull(initialState, "initialState must not be null");
    Assert.notNull(errorState, "errorState must not be null");
    this.type = type;
    this.initialState = initialState;
    this.errorState = errorState;
    this.settings = settings;
    this.stateMethods = new WorkflowDefinitionScanner().getStateMethods(getClass());
    requireStateMethodExists(initialState);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public S getInitialState() {
    return initialState;
  }

  public S getErrorState() {
    return errorState;
  }

  public abstract Set<S> getStates();

  public Map<String, List<String>> getAllowedTransitions() {
    return new LinkedHashMap<>(allowedTransitions);
  }

  public Map<String, WorkflowState> getFailureTransitions() {
    return new LinkedHashMap<>(failureTransitions);
  }

  protected AbstractWorkflowDefinition<S> permit(S originState, S targetState) {
    return permit(originState, targetState, null);
  }

  protected AbstractWorkflowDefinition<S> permit(S originState, S targetState, S failureState) {
    requireStateMethodExists(originState);
    requireStateMethodExists(targetState);
    allowedTransitionsFor(originState).add(targetState.name());
    if (failureState != null) {
      requireStateMethodExists(failureState);
      failureTransitions.put(originState.name(), failureState);
    }
    return this;
  }

  private List<String> allowedTransitionsFor(S state) {
    if (!allowedTransitions.containsKey(state.name())) {
      allowedTransitions.put(state.name(), new ArrayList<String>());
    }
    return allowedTransitions.get(state.name());
  }

  public WorkflowSettings getSettings() {
    return settings;
  }

  boolean isStateMethodObligatory(S state) {
    return state.getType() != manual && state.getType() != end;
  }

  void requireStateMethodExists(S state) {
    if (!stateMethods.containsKey(state.name()) && isStateMethodObligatory(state)) {
      String msg = format("Class '%s' is missing state handling method 'public NextAction %s(StateExecution execution, ... args)'",
          this.getClass().getName(), state.name());
      throw new IllegalArgumentException(msg);
    }
  }

  public void handleRetry(StateExecutionImpl execution) {
    if (execution.getRetries() >= getSettings().getMaxRetries()) {
      execution.setRetry(false);
      WorkflowState failureState = failureTransitions.get(execution.getCurrentStateName());
      if (failureState != null) {
        execution.setNextState(failureState);
        execution.setNextStateReason("Max retry count exceeded");
        execution.setNextActivation(getSettings().getErrorTransitionActivation());
      } else {
        execution.setNextState(errorState);
        execution.setNextStateReason("Max retry count exceeded, no failure state defined");
        execution.setNextActivation(null);
      }
    } else {
      execution.setNextActivation(getSettings().getErrorTransitionActivation());
    }
  }

  public WorkflowStateMethod getMethod(String stateName) {
    return stateMethods.get(stateName);
  }

  public WorkflowState getState(String state) {
    for (WorkflowState s : getStates()) {
      if (state.equals(s.getName())) {
        return s;
      }
    }
    throw new IllegalStateException("No state '" + state + "' in workflow definiton " + getType());
  }

  public boolean isStartState(String state) {
    return getState(state).getType() == WorkflowStateType.start;
  }

}