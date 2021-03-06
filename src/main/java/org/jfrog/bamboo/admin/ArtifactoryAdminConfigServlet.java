/*
 * Copyright (C) 2010 JFrog Ltd.
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

package org.jfrog.bamboo.admin;

import com.atlassian.bamboo.build.DefaultJob;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.variable.VariableDefinition;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.jfrog.bamboo.util.ConstantValues;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Exposes features of the Administration Configuration service to serve plugin modules which can't reach it.
 *
 * @author Noam Y. Tenne
 */
public class ArtifactoryAdminConfigServlet extends HttpServlet {

    private PlanManager planManager;
    private VariableDefinitionManager variableDefinitionManager;

    /**
     * Returns the global variable map in JSON format
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> globalVariableMap = Maps.newHashMap();
        String planKey = req.getParameter(ConstantValues.PLAN_KEY_PARAM);
        if (StringUtils.isNotBlank(planKey)) {
            Plan plan = planManager.getPlanByKey(planKey);
            if (plan instanceof DefaultJob) {
                //Default jobs don't have any global vars, so fetch the actual plan itself instead
                plan = planManager.getPlanByKey(StringUtils.removeEnd(plan.getKey(), "-" + plan.getBuildKey()));
            }
            appendVariableDefs(globalVariableMap, variableDefinitionManager.getPlanVariables(plan));
            appendVariableDefs(globalVariableMap, variableDefinitionManager.getGlobalNotOverriddenVariables(plan));
        } else {
            appendVariableDefs(globalVariableMap, variableDefinitionManager.getGlobalVariables());
        }

        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        jsonFactory.setCodec(mapper);

        PrintWriter writer = null;
        try {
            writer = resp.getWriter();
            JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
            jsonGenerator.writeObject(globalVariableMap);
            writer.flush();
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private void appendVariableDefs(Map<String, String> globalVariableMap, List<VariableDefinition> globalVariables) {
        for (VariableDefinition variableDefinition : globalVariables) {
            globalVariableMap.put(variableDefinition.getKey(), variableDefinition.getValue());
        }
    }

    public void setPlanManager(PlanManager planManager) {
        this.planManager = planManager;
    }

    public void setVariableDefinitionManager(VariableDefinitionManager variableDefinitionManager) {
        this.variableDefinitionManager = variableDefinitionManager;
    }
}
