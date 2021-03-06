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
package com.coinblesk.server.service;

import com.coinblesk.server.config.BeanConfig;
import com.coinblesk.server.entity.UserAccount;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 *
 * @author Thomas Bocek
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(
            {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class,
                DbUnitTestExecutionListener.class})
@WebAppConfiguration
@ContextConfiguration(classes = {BeanConfig.class})
public class UserTest {

    @Autowired
    UserAccountService userAccountService;
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty("coinblesk.config.dir", "/tmp/lib/coinblesk");
    }

    @Test
    @ExpectedDatabase(value = "UserTestAddUser.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testAddUser() throws Exception {
        UserAccount userAccount = new UserAccount();
        userAccount.setBalance(BigDecimal.ONE)
                .setCreationDate(new Date(1))
                .setDeleted(false)
                .setEmail("test@test.test")
                .setEmailToken(null)
                .setPassword("blub")
                .setUsername("blib");
        userAccountService.save(userAccount);
    }

    @Test
    @DatabaseSetup("UserTestGetUser.xml")
    public void testGetUser() throws Exception {
        UserAccount u1 = userAccountService.getByEmail("test");
        Assert.assertNull(u1);
        UserAccount u2 = userAccountService.getByEmail("test@test.test");
        Assert.assertEquals("blub", u2.getPassword());
    }
}
