/*
 * Copyright 2010 Lincoln Baxter, III
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
package org.ocpsoft.rewrite.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ocpsoft.logging.Logger;
import org.ocpsoft.rewrite.annotation.api.ClassContext;
import org.ocpsoft.rewrite.annotation.api.ClassVisitor;
import org.ocpsoft.rewrite.annotation.context.ClassContextImpl;
import org.ocpsoft.rewrite.annotation.context.FieldContextImpl;
import org.ocpsoft.rewrite.annotation.context.MethodContextImpl;
import org.ocpsoft.rewrite.annotation.context.ParameterContextImpl;
import org.ocpsoft.rewrite.annotation.spi.AnnotationHandler;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.config.Rule;
import org.ocpsoft.rewrite.config.RuleBuilder;

public class ClassVisitorImpl implements ClassVisitor, Configuration
{

   private final Logger log = Logger.getLogger(ClassVisitorImpl.class);

   /**
    * Maps annotation types to a list of handlers supporting the corresponding type
    */
   private final Map<Class<Annotation>, List<AnnotationHandler<Annotation>>> handlerMap = new HashMap<Class<Annotation>, List<AnnotationHandler<Annotation>>>();

   /**
    * The rules created by the visitor
    */
   private final ConfigurationBuilder builder = ConfigurationBuilder.begin();

   /**
    * The visitor must be initialized with the handlers to call for specific annotations
    */
   public ClassVisitorImpl(List<AnnotationHandler<Annotation>> handlers)
   {
      for (AnnotationHandler<Annotation> handler : handlers) {

         // determine the annotation the handler can process
         Class<Annotation> annotationType = handler.handles();

         // register the handler in the handlers map
         List<AnnotationHandler<Annotation>> list = handlerMap.get(annotationType);
         if (list == null) {
            list = new ArrayList<AnnotationHandler<Annotation>>();
            handlerMap.put(annotationType, list);
         }
         list.add(handler);
      }

      if (log.isDebugEnabled()) {
         log.debug("Initialized to use {} AnnotationHandlers..", handlers.size());
      }

   }

   /**
    * Processes the annotation on the supplied class.
    */
   @Override
   public void visit(Class<?> clazz)
   {

      RuleBuilder ruleBuilder = RuleBuilder.define();
      ClassContext context = new ClassContextImpl(builder, ruleBuilder);

      if (log.isTraceEnabled()) {
         log.trace("Scanning class: {}", clazz.getName());
      }

      // first process the class
      visit(clazz, context);

      // then process the fields
      for (Field field : clazz.getDeclaredFields()) {
         visit(field, new FieldContextImpl(context));
      }

      // then the methods
      for (Method method : clazz.getDeclaredMethods()) {
         MethodContextImpl methodContext = new MethodContextImpl(context);
         visit(method, methodContext);

         // then the method parameters
         for (int i = 0; i < method.getParameterTypes().length; i++) {
            visit(new ParameterImpl(method, method.getParameterTypes()[i], method.getParameterAnnotations()[i], i),
                     new ParameterContextImpl(methodContext));
         }
      }

      if (ruleBuilder.getOperationBuilder() != null || ruleBuilder.getConditionBuilder() != null)
      {
         builder.addRule(ruleBuilder);
      }

   }

   /**
    * Process one {@link AnnotatedElement} of the class.
    */
   private void visit(AnnotatedElement element, ClassContext context)
   {

      // each annotation on the element may be interesting for us
      for (Annotation annotation : element.getAnnotations()) {

         // type of this annotation
         Class<? extends Annotation> annotationType = annotation.annotationType();

         // determine the handlers to call for this type
         List<AnnotationHandler<Annotation>> handlers = handlerMap.get(annotationType);

         // process handlers if any
         if (handlers != null) {
            for (AnnotationHandler<Annotation> handler : handlers) {
               handler.process(context, element, annotation);
            }
         }

      }
   }

   @Override
   public List<Rule> getRules()
   {
      return builder.getRules();
   }

}
