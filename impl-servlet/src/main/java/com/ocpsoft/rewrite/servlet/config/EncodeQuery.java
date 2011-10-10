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
package com.ocpsoft.rewrite.servlet.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ocpsoft.rewrite.config.Operation;
import com.ocpsoft.rewrite.config.OperationBuilder;
import com.ocpsoft.rewrite.context.EvaluationContext;
import com.ocpsoft.rewrite.event.Rewrite;
import com.ocpsoft.rewrite.servlet.config.encodequery.Base64EncodingStrategy;
import com.ocpsoft.rewrite.servlet.config.encodequery.ChecksumStrategy;
import com.ocpsoft.rewrite.servlet.config.encodequery.EncodingStrategy;
import com.ocpsoft.rewrite.servlet.config.encodequery.HashCodeChecksumStrategy;
import com.ocpsoft.rewrite.servlet.http.event.HttpInboundServletRewrite;
import com.ocpsoft.rewrite.servlet.http.event.HttpOutboundServletRewrite;
import com.ocpsoft.rewrite.servlet.util.QueryStringBuilder;
import com.ocpsoft.rewrite.servlet.util.URLBuilder;

/**
 * Encodes any or many query-parameters into a single parameter using the given {@link ChecksumStrategy} and
 * {@link EncodingStrategy}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class EncodeQuery implements Operation
{
   private String tokenName;
   private ChecksumStrategy checksumStrategy = new HashCodeChecksumStrategy();
   private EncodingStrategy encodingStrategy = new Base64EncodingStrategy();
   private final List<String> params = new ArrayList<String>();
   private final List<String> excludedParams = new ArrayList<String>();
   private boolean inboundCorrection = true;
   private Operation onfailure;

   private EncodeQuery(final String[] params)
   {
      if ((params != null) && (params.length > 0))
         this.params.addAll(Arrays.asList(params));
   }

   /**
    * Create a new {@link EncodeQuery} {@link Operation} for the given query-parameter names. Only encode those given
    * parameters. If no parameter names are supplied, this will encode all query-parameters found in the URL.
    */
   public static EncodeQuery params(final String... params)
   {
      return new EncodeQuery(params);
   }

   /**
    * Exclude the given query-parameter names from encoding.
    */
   public EncodeQuery excluding(final String... params)
   {
      if ((params != null) && (params.length > 0))
         this.excludedParams.addAll(Arrays.asList(params));
      return this;
   }

   /**
    * Use the given {@link EncodingStrategy} when performing encoding.
    */
   public EncodeQuery withEncodingStrategy(final EncodingStrategy strategy)
   {
      this.encodingStrategy = strategy;
      return this;
   }

   /**
    * Use the given {@link ChecksumStrategy} when calculating and embedding checksums.
    */
   public EncodeQuery withChecksumStrategy(final ChecksumStrategy strategy)
   {
      this.checksumStrategy = strategy;
      return this;
   }

   /**
    * Redirect inbound requests to URLs containing matching query-parameters to the encoded URL.
    */
   public EncodeQuery withInboundCorrection(final boolean enable)
   {
      inboundCorrection = enable;
      return this;
   }

   /**
    * {@link Operation} to be performed when the current {@link ChecksumStrategy} detects an inbound checksum failure.
    */
   public OperationBuilder onChecksumFailure(final Operation operation)
   {
      OperationBuilder builder = new OperationBuilder() {
         @Override
         public void perform(final Rewrite event, final EvaluationContext context)
         {
            operation.perform(event, context);
         }
      };
      this.onfailure = builder;
      return builder;
   }

   /**
    * The name of the composite query-parameter to hold the encoded parameters.
    */
   public EncodeQuery to(final String param)
   {
      this.tokenName = param;
      return this;
   }

   @Override
   public void perform(final Rewrite event, final EvaluationContext context)
   {
      if ((event instanceof HttpInboundServletRewrite) && DispatchType.isRequest().evaluate(event, context))
      {
         HttpInboundServletRewrite in = (HttpInboundServletRewrite) event;

         QueryStringBuilder query = QueryStringBuilder.begin();
         query.addParameters(in.getRequestQueryString());

         String token = query.decode().getParameter(tokenName);
         if (token != null)
         {
            String decoded = encodingStrategy.decode(token);

            if (checksumStrategy.checksumValid(decoded))
            {
               decoded = checksumStrategy.removeChecksum(decoded);
               query.removeParameter(tokenName);
               String newUrl = in.getRequestPath() + "?" + decoded;
               in.forward(newUrl);
            }
            else if (onfailure != null)
            {
               onfailure.perform(event, context);
            }
         }
         else if (!query.isEmpty() && inboundCorrection)
         {
            String encoded = checksumStrategy.embedChecksum(in.getRequestQueryString());
            encoded = encodingStrategy.encode(encoded);
            in.redirectTemporary(in.getContextPath() + in.getURL());
         }
      }

      else if (event instanceof HttpOutboundServletRewrite)
      {
         HttpOutboundServletRewrite out = (HttpOutboundServletRewrite) event;

         String outboundURL = out.getOutboundURL();
         URLBuilder url = URLBuilder.build(outboundURL);

         url.getQueryStringBuilder().removeParameter(tokenName);

         if (outboundURL.contains("?") && (outboundURL.startsWith(out.getContextPath()) || outboundURL.startsWith("/")))
         {
            if (!url.getQueryStringBuilder().isEmpty())
            {
               String encoded = checksumStrategy.embedChecksum(url.getQueryStringBuilder().toQueryString());
               encoded = encodingStrategy.encode(encoded);

               out.setOutboundURL(url.toPath() + "?" + tokenName + "=" + encoded);
            }
         }

      }

   }
}