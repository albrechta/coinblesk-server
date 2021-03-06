/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.coinblesk.server.controller;

import com.coinblesk.json.v1.ExchangeRateTO;
import com.coinblesk.server.config.BeanConfig;
import com.coinblesk.server.config.SecurityConfig;
import com.coinblesk.util.SerializeUtils;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author Thomas Bocek
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(
        {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class,
            DbUnitTestExecutionListener.class})
@ContextConfiguration(
        classes = {BeanConfig.class, SecurityConfig.class})
@WebAppConfiguration
public class ForexTest {
    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;
    
    private static MockMvc mockMvc;
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty("coinblesk.config.dir", "/tmp/lib/coinblesk");
    }
    
    @Before
    public void setUp() {
         mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).addFilter(springSecurityFilterChain).build();   
    }
    
    @Test
    public void testV1() throws Exception {
        MvcResult res = mockMvc.perform(get("/w/x/CHF").secure(true)).andExpect(status().isOk()).andReturn();
        ExchangeRateTO rate = SerializeUtils.GSON.fromJson(res.getResponse().getContentAsString(), ExchangeRateTO.class);
        System.out.println("rate is: " + rate.rate()+"/"+rate.name());
        Assert.assertNotNull(rate);
    }
    
    @Test
    public void testV2() throws Exception {
        MvcResult res = mockMvc.perform(get("/v2/x/r/CHF-EUR").secure(true)).andExpect(status().isOk()).andReturn();
        ExchangeRateTO rate = SerializeUtils.GSON.fromJson(res.getResponse().getContentAsString(), ExchangeRateTO.class);
        System.out.println("rate is: " + rate.rate()+"/"+rate.name());
        Assert.assertNotNull(rate);
    }
}
