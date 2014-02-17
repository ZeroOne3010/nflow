package com.nitorcreations.nflow.engine.domain;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

public class QueryWorkflowInstances {

  public final List<String> types;
  public final List<String> states;

  private QueryWorkflowInstances(Builder builder) {
    super();
    this.types = new ArrayList<>(builder.types);
    this.states = new ArrayList<>(builder.states);
  }

  public static class Builder {
    List<String> types = new ArrayList<>();
    List<String> states = new ArrayList<>();

    public Builder() {
    }

    public Builder addTypes(String[] types) {
      this.types.addAll(asList(types));
      return this;
    }

    public Builder addStates(String[] states) {
      this.states.addAll(asList(states));
      return this;
    }

    public QueryWorkflowInstances build() {
      return new QueryWorkflowInstances(this);
    }
  }

}
