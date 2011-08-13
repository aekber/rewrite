/*
 * Copyright 2011 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocpsoft.rewrite.cdi.bridge;

import junit.framework.Assert;

import org.apache.http.client.methods.HttpGet;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ocpsoft.rewrite.cdi.CDIRoot;
import com.ocpsoft.rewrite.test.HttpAction;
import com.ocpsoft.rewrite.test.RewriteTestBase;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
@RunWith(Arquillian.class)
public class CDIFeaturesTest extends RewriteTestBase
{
   @Deployment(testable = true)
   public static WebArchive getDeployment()
   {
      WebArchive deployment = RewriteTestBase.getDeployment()
               .addPackages(true, CDIRoot.class.getPackage())
               .addAsResource(new StringAsset("com.ocpsoft.rewrite.cdi.bind.BindingTestConfigProvider"),
                        "/META-INF/services/com.ocpsoft.rewrite.config.ConfigurationProvider");

      return deployment;
   }

   @Test
   public void testRewriteProviderBridgeAcceptsChanges()
   {
      HttpAction<HttpGet> action = get("/success");
      Assert.assertEquals(200, action.getResponse().getStatusLine().getStatusCode());
   }

   @Test
   public void testRewriteRedirect301()
   {
      HttpAction<HttpGet> action = get("/redirect-301");
      Assert.assertEquals(200, action.getResponse().getStatusLine().getStatusCode());
      Assert.assertEquals("/outbound-rewritten", action.getCurrentURL());
   }

   @Test
   public void testELExpressionBinding()
   {
      HttpAction<HttpGet> action = get("/one/2");
      Assert.assertEquals("/2/one", action.getCurrentRelativeURL());
      Assert.assertEquals(404, action.getResponse().getStatusLine().getStatusCode());
   }
}