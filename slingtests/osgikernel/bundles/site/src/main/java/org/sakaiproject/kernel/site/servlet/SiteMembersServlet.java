/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.kernel.site.servlet;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.api.site.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGetServlet</code>
 * 
 * @scr.component immediate="true" label="SiteGetServlet"
 *                description="Get members servlet for site service"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="Gets lists of members for a site"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sakai/site"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" value="members"
 */
public class SiteMembersServlet extends AbstractSiteServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(SiteMembersServlet.class);
  private static final long serialVersionUID = 4874392318687088747L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.info("Got get to SiteServiceGetServlet");
    Node site = request.getResource().adaptTo(Node.class);
    if (site == null) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find site node");
      return;
    }
    RequestParameter startParam = request.getRequestParameter(SiteService.PARAM_START);
    RequestParameter itemsParam = request.getRequestParameter(SiteService.PARAM_ITEMS);
    RequestParameter[] sortParam = request.getRequestParameters(SiteService.PARAM_SORT);
    int start = 0;
    int items = 25;
    Sort[] sort = null;
    if (startParam != null) {
      try {
        start = Integer.parseInt(startParam.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cant parse {} as  {} ", SiteService.PARAM_START, startParam
            .getString());
      }
    }
    if (itemsParam != null) {
      try {
        items = Integer.parseInt(itemsParam.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cant parse {} as  {} ", SiteService.PARAM_ITEMS, startParam
            .getString());
      }
    }
    if (sortParam != null) {
      sort = new Sort[sortParam.length];
      int i = 0;
      for (RequestParameter p : sortParam) {
        sort[i++] = new Sort(p.toString());
      }
    }

    Iterator<User> members = getSiteService().getMembers(site, start, items, sort);

    try {
    JSONWriter output = new JSONWriter(response.getWriter());
    output.array();
    for (; members.hasNext();) {
      User u = members.next();
      Resource resource = request.getResourceResolver().resolve(
          "/system/userManager/user/" + u.getID());
      output.value(resource.adaptTo(ValueMap.class));
    }
    output.endArray();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());      
    }
    return;
  }

}
