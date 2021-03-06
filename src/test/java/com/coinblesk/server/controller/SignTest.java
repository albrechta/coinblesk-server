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

import com.coinblesk.server.config.AppConfig;
import com.coinblesk.server.config.BeanConfig;
import com.coinblesk.server.config.SecurityConfig;
import com.coinblesk.server.service.WalletService;
import com.coinblesk.json.v1.SignTO;
import com.coinblesk.json.v1.Type;
import com.coinblesk.server.utilTest.Client;
import com.coinblesk.server.utilTest.ServerCalls;
import com.coinblesk.util.Pair;
import com.coinblesk.util.SerializeUtils;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import java.util.Date;
import java.util.List;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

//http://www.soroushjp.com/2014/12/20/bitcoin-multisig-the-hard-way-understanding-raw-multisignature-bitcoin-transactions/
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
public class SignTest {

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private WalletService walletService;

    private static MockMvc mockMvc;

    private NetworkParameters params;
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty("coinblesk.config.dir", "/tmp/lib/coinblesk");
    }

    @Before
    public void setUp() throws Exception {
        walletService.shutdown();
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .addFilter(springSecurityFilterChain).build();
        walletService.init();
        params = appConfig.getNetworkParameters();
    }

    @Test
    public void testAddressEmpty() throws Exception {
        Client client = new Client(params, mockMvc);
        Transaction funding = Client.sendFakeCoins(params, Coin.valueOf(123450), client.p2shAddress(), 0,
                walletService.blockChain());
        Coin amountToRequest = Coin.valueOf(9876);
        Date now = new Date();
        List<Pair<byte[], Long>> outpointCoinPair = client.outpointsRaw(funding);
        SignTO prepareHalfSignTO = ServerCalls.signServerCallInput(outpointCoinPair,
                new ECKey().toAddress(params), amountToRequest.value, client.ecKey(), now);
        prepareHalfSignTO.p2shAddressTo("1");
        SerializeUtils.signJSON(prepareHalfSignTO, client.ecKey());
        SignTO status = ServerCalls.signServerCallOutput(mockMvc, prepareHalfSignTO);
        Assert.assertFalse(status.isSuccess());
        Assert.assertEquals(Type.ADDRESS_EMPTY, status.type());
    }

    @Test
    public void testAddressNotEnoughFunds() throws Exception {
        Client client = new Client(params, mockMvc);
        Transaction funding = Client.sendFakeCoins(params, Coin.valueOf(1), client.p2shAddress(), 0,
                walletService.blockChain());
        Coin amountToRequest = Coin.valueOf(9876);
        Date now = new Date();
        SignTO status = ServerCalls.signServerCall(mockMvc, client.outpointsRaw(funding),
                new ECKey().toAddress(params), amountToRequest.value, client, now);

        Assert.assertFalse(status.isSuccess());
        Assert.assertEquals(Type.NOT_ENOUGH_COINS, status.type());
    }

    @Test
    public void testAddressOnlyDust() throws Exception {
        Client client = new Client(params, mockMvc);
        Transaction funding = Client.sendFakeCoins(params, Coin.valueOf(700), client.p2shAddress(), 0,
                walletService.blockChain());
        Coin amountToRequest = Coin.valueOf(100);
        Date now = new Date();
        SignTO status = ServerCalls.signServerCall(mockMvc, client.outpointsRaw(funding),
                new ECKey().toAddress(params), amountToRequest.value, client, now);
        Assert.assertFalse(status.isSuccess());
        Assert.assertEquals(Type.TX_ERROR, status.type());
    }

    @Test
    @DatabaseTearDown(value = {"EmptyTx.xml"}, type = DatabaseOperation.DELETE_ALL)
    @ExpectedDatabase(value = "TxTwice.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testSignTwice() throws Exception {
        Client client = new Client(params, mockMvc);
        Transaction funding = Client.sendFakeCoins(params, Coin.valueOf(123450), client.p2shAddress(), 0,
                walletService.blockChain());
        Date now = new Date();
        Coin amountToRequest = Coin.valueOf(9876);

        SignTO status = ServerCalls.signServerCall(mockMvc, client.outpointsRaw(funding),
                new ECKey().toAddress(params), amountToRequest.value, client, now);
        Assert.assertTrue(status.isSuccess());
        Date now2 = new Date(now.getTime() + 5000L);
        status = ServerCalls.signServerCall(mockMvc, client.outpointsRaw(funding),
                new ECKey().toAddress(params), amountToRequest.value, client, now2);
        Assert.assertTrue(status.isSuccess());
    }

    @Test
    @DatabaseTearDown(value = {"EmptyTx.xml"}, type = DatabaseOperation.DELETE_ALL)
    public void testServerSignatures() throws Exception {
        Client client = new Client(params, mockMvc);
        Transaction funding = Client.sendFakeCoins(params, Coin.valueOf(123450),
                client.p2shAddress(), 0, walletService.blockChain());
        Date now = new Date();
        Coin amountToRequest = Coin.valueOf(9876);

        SignTO status = ServerCalls.signServerCall(mockMvc, client.outpointsRaw(funding),
                new ECKey().toAddress(params), amountToRequest.value, client, now);
        Assert.assertTrue(status.isSuccess());

        Transaction tx = new Transaction(params, status.transaction());

        List<TransactionSignature> sigs = SerializeUtils.deserializeSignatures(
                status.signatures());
        Assert.assertTrue(SerializeUtils.verifyTxSignatures(tx, sigs,
                client.redeemScript(), client.ecKeyServer()));
        Assert.assertFalse(SerializeUtils.verifyTxSignatures(tx, sigs,
                client.redeemScript(), client.ecKey()));
    }
}
