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

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.coinblesk.bitcoin.BitcoinNet;
import com.coinblesk.json.v1.Type;
import com.coinblesk.json.v1.VersionTO;
import com.coinblesk.server.config.AppConfig;
import com.coinblesk.server.config.BeanConfig;
import com.coinblesk.server.config.SecurityConfig;
import com.coinblesk.server.utilTest.RESTUtils;
import com.github.springtestdbunit.DbUnitTestExecutionListener;

/**
 * 
 * @author Andreas Albrecht
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
	DependencyInjectionTestExecutionListener.class, 
	TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class})
@ContextConfiguration(classes = { 
		BeanConfig.class, 
		SecurityConfig.class})
@WebAppConfiguration
public class VersionControllerTest {
	private static final String URL_VERSION = "/v1/version";
	
	private static final String SUPPORTED_CLIENT_VERSION;
	static {
		SUPPORTED_CLIENT_VERSION = new AppConfig().getSupportedClientVersions().iterator().next();
	}
	
	private static final String UNSUPPORTED_CLIENT_VERSION = "not supported";
	
	@Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;
    
    private static MockMvc mockMvc;
    
    @Before
    public void setUp() {
         mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).addFilter(springSecurityFilterChain).build();   
    }
    
	@Test
	public void testVersion_NoContent() throws Exception {
		mockMvc
			.perform(post(URL_VERSION).secure(true))
			.andExpect(status().is4xxClientError());
	}
	
	@Test
	public void testVersion_NoInputs() throws Exception {
		VersionTO requestTO = new VersionTO();
		VersionTO responseTO = RESTUtils.postRequest(mockMvc, URL_VERSION, requestTO);
		
		assertNotNull(responseTO);
		assertFalse(responseTO.isSuccess());
		assertFalse(responseTO.isSupported());
		assertEquals(responseTO.type(), Type.INPUT_MISMATCH);
	}
	
	@Test
	public void testVersion_NoNetwork() throws Exception {
		VersionTO requestTO = new VersionTO()
				.clientVersion(SUPPORTED_CLIENT_VERSION);
				// network missing
		VersionTO responseTO = RESTUtils.postRequest(mockMvc, URL_VERSION, requestTO);
		
		assertNotNull(responseTO);
		assertFalse(responseTO.isSuccess());
		assertFalse(responseTO.isSupported());
		assertEquals(responseTO.type(), Type.INPUT_MISMATCH);
	}
	
	@Test
	public void testVersion_NoClientVersion() throws Exception {
		VersionTO requestTO = new VersionTO()
				// client version missing
				.bitcoinNet(BitcoinNet.UNITTEST);
				
		VersionTO responseTO = RESTUtils.postRequest(mockMvc, URL_VERSION, requestTO);
		
		assertNotNull(responseTO);
		assertFalse(responseTO.isSuccess());
		assertFalse(responseTO.isSupported());
		assertEquals(responseTO.type(), Type.INPUT_MISMATCH);
	}
	
	@Test
	public void testVersion_SupportedVersion_SameNetwork() throws Exception {
		VersionTO requestTO = new VersionTO()
				.clientVersion(SUPPORTED_CLIENT_VERSION)
				.bitcoinNet(BitcoinNet.UNITTEST);
		
		VersionTO responseTO = RESTUtils.postRequest(mockMvc, URL_VERSION, requestTO);
		
		assertNotNull(responseTO);
		assertTrue(responseTO.isSuccess());
		assertTrue(responseTO.isSupported());
		assertEquals(responseTO.bitcoinNet(), BitcoinNet.UNITTEST);
		assertEquals(responseTO.type(), Type.SUCCESS);
	}
	
	@Test
	public void testVersion_SupportedVersion_DifferentNetwork() throws Exception {
		VersionTO requestTO = new VersionTO()
				.clientVersion(SUPPORTED_CLIENT_VERSION)
				.bitcoinNet(BitcoinNet.MAINNET);
		
		VersionTO responseTO = RESTUtils.postRequest(mockMvc, URL_VERSION, requestTO);
		
		assertNotNull(responseTO);
		assertTrue(responseTO.isSuccess());
		assertFalse(responseTO.isSupported());
		assertEquals(responseTO.bitcoinNet(), BitcoinNet.UNITTEST);
		assertEquals(responseTO.type(), Type.SUCCESS);
	}
	
	@Test
	public void testVersion_UnsupportedVersion_SameNetwork() throws Exception {
		VersionTO requestTO = new VersionTO()
			.clientVersion(UNSUPPORTED_CLIENT_VERSION)
			.bitcoinNet(BitcoinNet.UNITTEST);
		VersionTO responseTO = RESTUtils.postRequest(mockMvc, URL_VERSION, requestTO);
		
		assertNotNull(responseTO);
		assertTrue(responseTO.isSuccess());
		assertFalse(responseTO.isSupported());
		assertEquals(responseTO.bitcoinNet(), BitcoinNet.UNITTEST);
		assertEquals(responseTO.type(), Type.SUCCESS);
	}
	
	
	
	@Test
	public void testVersion_UnsupportedVersion_DifferentNetwork() throws Exception {
		VersionTO requestTO = new VersionTO()
			.clientVersion(UNSUPPORTED_CLIENT_VERSION)
			.bitcoinNet(BitcoinNet.MAINNET);
		VersionTO responseTO = RESTUtils.postRequest(mockMvc, URL_VERSION, requestTO);
		
		assertNotNull(responseTO);
		assertTrue(responseTO.isSuccess());
		assertFalse(responseTO.isSupported());
		assertEquals(responseTO.bitcoinNet(), BitcoinNet.UNITTEST);
		assertEquals(responseTO.type(), Type.SUCCESS);
	}
	
}
