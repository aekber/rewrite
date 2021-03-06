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
package org.ocpsoft.rewrite.servlet.config;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ocpsoft.common.util.Assert;
import org.ocpsoft.rewrite.bind.Binding;
import org.ocpsoft.rewrite.bind.Bindings;
import org.ocpsoft.rewrite.bind.Evaluation;
import org.ocpsoft.rewrite.bind.ParameterizedPattern;
import org.ocpsoft.rewrite.bind.RegexCapture;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.ocpsoft.rewrite.servlet.http.event.HttpOutboundServletRewrite;
import org.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;
import org.ocpsoft.rewrite.servlet.util.ParameterStore;
import org.ocpsoft.rewrite.servlet.util.URLBuilder;

/**
 * A {@link org.ocpsoft.rewrite.config.Condition} that inspects the value of {@link HttpServletRequest#getScheme()}
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class Scheme extends HttpCondition implements IScheme
{
   private final ParameterizedPattern expression;
   private final ParameterStore<SchemeParameter> parameters = new ParameterStore<SchemeParameter>();

   private Scheme(final String pattern)
   {
      Assert.notNull(pattern, "Scheme must not be null.");
      this.expression = new ParameterizedPattern(pattern);

      for (RegexCapture parameter : this.expression.getParameters().values()) {
         where(parameter.getName()).bindsTo(Evaluation.property(parameter.getName()));
      }
   }

   /**
    * Inspect the current request scheme, comparing against the given pattern.
    * <p>
    * The given pattern may be parameterized using the following format:
    * <p>
    * <code>
    *    https
    *    {scheme}
    *    {scheme}-custom <br>
    *    ... and so on
    * </code>
    * <p>
    * By default, matching parameter values are bound to the {@link org.ocpsoft.rewrite.context.EvaluationContext}. See also {@link #where(String)}
    */
   public static Scheme matches(final String pattern)
   {
      return new Scheme(pattern);
   }

   @Override
   public SchemeParameter where(final String param)
   {
      return parameters.where(param, new SchemeParameter(this, expression.getParameter(param)));
   }

   @Override
   public SchemeParameter where(final String param, final Binding binding)
   {
      return where(param).bindsTo(binding);
   }

   @Override
   public boolean evaluateHttp(final HttpServletRewrite event, final EvaluationContext context)
   {
      String scheme = null;

      if (event instanceof HttpOutboundServletRewrite)
      {
         String url = event.getURL();
         URLBuilder builder = URLBuilder.createFrom(url);
         scheme = builder.toURI().getScheme();
         if (scheme == null)
            scheme = event.getRequest().getScheme();
      }
      else
         scheme = event.getRequest().getScheme();

      if (scheme != null && expression.matches(event, context, scheme))
      {
         Map<RegexCapture, String[]> parameters = expression.parse(event, context, scheme);

         for (RegexCapture capture : parameters.keySet()) {
            if (!Bindings.enqueueSubmission(event, context, where(capture.getName()), parameters.get(capture)))
               return false;
         }
         return true;
      }
      return false;
   }

   /**
    * Get the underlying {@link ParameterizedPattern} for this {@link Scheme}
    * <p>
    * See also: {@link #where(String)}
    */
   public ParameterizedPattern getExpression()
   {
      return expression;
   }

   @Override
   public String toString()
   {
      return expression.toString();
   }
}