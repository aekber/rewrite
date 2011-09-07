/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.ocpsoft.rewrite.faces;

import com.ocpsoft.logging.Logger;
import com.ocpsoft.rewrite.context.EvaluationContext;
import com.ocpsoft.rewrite.event.Rewrite;
import com.ocpsoft.rewrite.faces.config.PhaseAction;
import com.ocpsoft.rewrite.servlet.http.event.HttpInboundServletRewrite;
import com.ocpsoft.rewrite.spi.InvocationResultHandler;

/**
 * (Priority: 100) Implementation of {@link InvocationResultHandler} which handles JavaServer Faces action result and
 * navigation strings. Together with {@link RewritePhaseListener}, integrates {@link PhaseAction} into Faces navigation.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class NavigatingInvocationResultHandler implements InvocationResultHandler
{
   public static final String QUEUED_NAVIGATION = NavigatingInvocationResultHandler.class.getName()
            + "_QUEUED_NAVIGATION";

   private static Logger log = Logger.getLogger(NavigatingInvocationResultHandler.class);

   @Override
   public int priority()
   {
      return 100;
   }

   @Override
   public boolean handles(final Object payload)
   {
      return payload instanceof String;
   }

   @Override
   public void handle(final Rewrite event, final EvaluationContext context, final Object result)
   {
      if (event instanceof HttpInboundServletRewrite)
      {
         if (result instanceof String)
         {
            log.info("Storing Invocation result [" + result + "] as deferred navigation string.");
            ((HttpInboundServletRewrite) event).getRequest().setAttribute(QUEUED_NAVIGATION, result);
         }
      }
   }

}
