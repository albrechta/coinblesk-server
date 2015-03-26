package ch.uzh.csg.coinblesk.server.controllerui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.uzh.csg.coinblesk.server.domain.UserAccount;
import ch.uzh.csg.coinblesk.server.service.ServerAccountService;
import ch.uzh.csg.coinblesk.server.service.UserAccountService;
import ch.uzh.csg.coinblesk.server.util.CredentialsBean;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:context.xml", "classpath:test-database.xml", "classpath:view.xml", "classpath:security.xml" })
@DbUnitConfiguration(databaseConnection = "dataSource")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class })
@WebAppConfiguration
public class HomeControllerTest {

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private static MockMvc mockMvc;

    private static boolean initialized = false;

    private final String PLAIN_TEXT_PASSWORD = "wwww";
    
    private static UserAccount test70;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // mock JNDI
        SimpleNamingContextBuilder contextBuilder = new SimpleNamingContextBuilder();
        CredentialsBean credentials = new CredentialsBean();
        contextBuilder.bind("java:comp/env/bean/CredentialsBean", credentials);
        contextBuilder.activate();
    }

    @Before
    public void setUp() throws Exception {
        UserAccountService.enableTestingMode();
        ServerAccountService.enableTestingMode();
        if (!initialized) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).addFilter(springSecurityFilterChain).build();
            
            test70 = new UserAccount("test70@https://mbps.csg.uzh.ch", "test60@bitcoin.csg.uzh.ch", "asdf");

            initialized = true;
        }
    }

    @After
    public void tearDown() {
        UserAccountService.disableTestingMode();
        ServerAccountService.disableTestingMode();
    }

    @Test
    @DatabaseSetup(value = "classpath:DbUnitFiles/Controllers/userAccountServerAccountData.xml", type = DatabaseOperation.CLEAN_INSERT)
    // @DatabaseTearDown(value="classpath:DbUnitFiles/Controllers/homeControllerUserAccountData.xml",
    // type=DatabaseOperation.DELETE_ALL)
    public void testGetPageHome_FailedNotAuthorized() throws Exception {
        mockMvc.perform(get("/home").secure(false)).andExpect(status().isUnauthorized());
    }

}