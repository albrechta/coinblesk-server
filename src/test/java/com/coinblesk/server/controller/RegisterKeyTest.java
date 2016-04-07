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

import com.coinblesk.server.config.BeanConfig;
import com.coinblesk.server.config.SecurityConfig;
import com.coinblesk.server.utilTest.TestBean;

import com.coinblesk.json.KeyTO;
import com.coinblesk.json.Type;
import com.coinblesk.util.SerializeUtils;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 *
 * @author draft
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(
            {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class,
                DbUnitTestExecutionListener.class})
@ContextConfiguration(classes = {TestBean.class, BeanConfig.class, SecurityConfig.class})
@WebAppConfiguration
public class RegisterKeyTest {

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private static MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).addFilter(springSecurityFilterChain)
                .build();
    }

    @Test
    public void testRegister() throws Exception {
        //no object
        mockMvc.perform(post("/p/x").secure(true)).andExpect(status().is4xxClientError());
        //with object, but no public key
        KeyTO keyTO = new KeyTO();
        MvcResult res = mockMvc.perform(post("/p/x").secure(true).contentType(MediaType.APPLICATION_JSON)
                .content(SerializeUtils.GSON.toJson(keyTO))).andExpect(status().isOk()).andReturn();
        KeyTO status = SerializeUtils.GSON.fromJson(res.getResponse().getContentAsString(), KeyTO.class);
        Assert.assertEquals(false, status.isSuccess());
        Assert.assertEquals(Type.SERVER_ERROR, status.type());
        Assert.assertNull(status.publicKey());
        //with bogus key
        keyTO = new KeyTO().publicKey("bogus=======".getBytes());
        res = mockMvc.perform(post("/p/x").secure(true).contentType(MediaType.APPLICATION_JSON).content(
                SerializeUtils.GSON.toJson(keyTO))).andExpect(status().isOk()).andReturn();
        status = SerializeUtils.GSON.fromJson(res.getResponse().getContentAsString(), KeyTO.class);
        Assert.assertEquals(false, status.isSuccess());
        Assert.assertEquals(Type.SERVER_ERROR, status.type());
        Assert.assertNull(status.publicKey());
        //with good pubilc key
        ECKey ecKeyClient = new ECKey();
        keyTO = new KeyTO().publicKey(ecKeyClient.getPubKey());
        res = mockMvc.perform(post("/p/x").secure(true).contentType(MediaType.APPLICATION_JSON).content(
                SerializeUtils.GSON.toJson(keyTO))).andExpect(status().isOk()).andReturn();
        status = SerializeUtils.GSON.fromJson(res.getResponse().getContentAsString(), KeyTO.class);
        Assert.assertEquals(true, status.isSuccess());
        Assert.assertNotNull(status.publicKey());
    }
    
    @Test
    @DatabaseSetup("classpath:DbUnitFiles/clientKey.xml")
    @DatabaseTearDown("classpath:DbUnitFiles/emptyAddresses.xml")
    public void testCreateAddress() throws Exception {
    	mockMvc
    		.perform(post("/payment/createTimeLockedAddress").secure(true))
    		.andExpect(status().is4xxClientError());
    	
        ECKey clientKey = KeyTestUtil.ALICE_CLIENT;
		ECKey serverKey = KeyTestUtil.ALICE_SERVER; // key is already stored in DB
        
        KeyTO keyTO = new KeyTO().publicKey(clientKey.getPubKey());
        String jsonKeyTO = SerializeUtils.GSON.toJson(keyTO);
        
        MvcResult res = mockMvc
        					.perform(post("/payment/createTimeLockedAddress").secure(true).contentType(MediaType.APPLICATION_JSON).content(jsonKeyTO))
        					.andExpect(status().isOk())
        					.andReturn();
        TimeLockedAddressTO response = SerializeUtils.GSON.fromJson(res.getResponse().getContentAsString(), TimeLockedAddressTO.class);
        assertTrue(response.isSuccess());
    	TimeLockedAddress addressResponse = response.timeLockedAddress();
    	assertNotNull(addressResponse);
    	assertNotNull(addressResponse.getAddressHash());
    	assertArrayEquals(addressResponse.getUserPubKey(), clientKey.getPubKey());
    	assertArrayEquals(addressResponse.getServicePubKey(), serverKey.getPubKey());
    	assertTrue(addressResponse.getLockTime() > 0);
    }
    
    public static class KeyTestUtil {
    	/**
    	 * Keys correspond to the keys in the clientKey.xml dataset.
    	 */
    	public static final ECKey ALICE_CLIENT = ECKey.fromPrivate(Sha256Hash.hash("alice-client".getBytes()));
    	public static final ECKey ALICE_SERVER = ECKey.fromPrivate(Sha256Hash.hash("alice-server".getBytes()));
    	
    	public static final ECKey BOB_CLIENT = ECKey.fromPrivate(Sha256Hash.hash("bob-client".getBytes()));
    	public static final ECKey BOB_SERVER = ECKey.fromPrivate(Sha256Hash.hash("bob-server".getBytes()));

    	public static final ECKey CAROL_CLIENT = ECKey.fromPrivate(Sha256Hash.hash("carol-client".getBytes()));
    	public static final ECKey CAROL_SERVER = ECKey.fromPrivate(Sha256Hash.hash("carol-server".getBytes()));
    	
    	public static final ECKey DAVE_CLIENT = ECKey.fromPrivate(Sha256Hash.hash("dave-client".getBytes()));
    	public static final ECKey DAVE_SERVER = ECKey.fromPrivate(Sha256Hash.hash("dave-server".getBytes()));
    }
}
