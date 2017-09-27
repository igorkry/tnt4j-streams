/*
 * Copyright 2014-2017 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.scenario;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

/**
 * This class defines TNT4J-Streams-WS configuration scenario.
 *
 * @version $Revision: 1 $
 */
public class WsScenario {
	private String name;
	private List<WsScenarioStep> stepsList;
	private WsScenarioStep loginStep;

	/**
	 * Constructs a new WsScenario. Defines scenario name.
	 *
	 * @param name
	 *            scenario name
	 */
	public WsScenario(String name) {
		this.name = name;
	}

	/**
	 * Returns scenario name.
	 *
	 * @return scenario name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Adds scenario step to steps list.
	 *
	 * @param scenarioStep
	 *            scenario step to add
	 */
	public void addStep(WsScenarioStep scenarioStep) {
		if (stepsList == null) {
			stepsList = new ArrayList<>();
		}

		if ("login".equalsIgnoreCase(scenarioStep.getName())) { // NON-NLS
			loginStep = scenarioStep;
		} else {
			stepsList.add(scenarioStep);
		}
	}

	/**
	 * Returns scenario steps list.
	 *
	 * @return scenario steps list
	 */
	public List<WsScenarioStep> getStepsList() {
		return stepsList;
	}

	/**
	 * Checks if scenario has no steps defined.
	 *
	 * @return flag indicating scenario has no steps defined
	 */
	public boolean isEmpty() {
		return CollectionUtils.isEmpty(stepsList);
	}

	/**
	 * Returns scenario step defining SOAP login call.
	 *
	 * @return login call step
	 */
	public WsScenarioStep getLoginStep() {
		return loginStep;
	}
}
