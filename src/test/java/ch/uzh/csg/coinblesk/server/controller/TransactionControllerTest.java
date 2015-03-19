package ch.uzh.csg.coinblesk.server.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.KeyPair;

import javax.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.uzh.csg.coinblesk.customserialization.Currency;
import ch.uzh.csg.coinblesk.customserialization.DecoderFactory;
import ch.uzh.csg.coinblesk.customserialization.PKIAlgorithm;
import ch.uzh.csg.coinblesk.customserialization.PaymentRequest;
import ch.uzh.csg.coinblesk.customserialization.PaymentResponse;
import ch.uzh.csg.coinblesk.customserialization.ServerPaymentRequest;
import ch.uzh.csg.coinblesk.customserialization.ServerPaymentResponse;
import ch.uzh.csg.coinblesk.customserialization.ServerResponseStatus;
import ch.uzh.csg.coinblesk.keys.CustomKeyPair;
import ch.uzh.csg.coinblesk.responseobject.GetHistoryTransferObject;
import ch.uzh.csg.coinblesk.responseobject.HistoryTransferRequestObject;
import ch.uzh.csg.coinblesk.responseobject.PayOutTransactionObject;
import ch.uzh.csg.coinblesk.responseobject.TransactionObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.server.clientinterface.IUserAccount;
import ch.uzh.csg.coinblesk.server.domain.UserAccount;
import ch.uzh.csg.coinblesk.server.json.CustomObjectMapper;
import ch.uzh.csg.coinblesk.server.security.KeyHandler;
import ch.uzh.csg.coinblesk.server.service.TransactionService;
import ch.uzh.csg.coinblesk.server.service.UserAccountService;
import ch.uzh.csg.coinblesk.server.util.BitcoindController;
import ch.uzh.csg.coinblesk.server.util.Constants;
import ch.uzh.csg.coinblesk.server.util.CredentialsBean;
import ch.uzh.csg.coinblesk.server.util.exceptions.EmailAlreadyExistsException;
import ch.uzh.csg.coinblesk.server.util.exceptions.InvalidEmailException;
import ch.uzh.csg.coinblesk.server.util.exceptions.InvalidUrlException;
import ch.uzh.csg.coinblesk.server.util.exceptions.InvalidUsernameException;
import ch.uzh.csg.coinblesk.server.util.exceptions.UserAccountNotFoundException;
import ch.uzh.csg.coinblesk.server.util.exceptions.UsernameAlreadyExistsException;
import ch.uzh.csg.coinblesk.util.Converter;

import com.azazar.bitcoin.jsonrpcclient.BitcoinException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:context.xml",
		"classpath:test-database.xml",
		"classpath:view.xml",
		"classpath:security.xml"})
@WebAppConfiguration
public class TransactionControllerTest {
	
	@Autowired
	private WebApplicationContext webAppContext;
	
	@Autowired
	private FilterChainProxy springSecurityFilterChain;
	
	@Autowired
	private IUserAccount userAccountService;
	
	private static MockMvc mockMvc;
	
	private static boolean initialized = false;
	private static UserAccount test1_1;
	private static UserAccount test1_2;
	private static UserAccount test2_1;
	private static UserAccount test2_2;
	private static UserAccount test3_1;
	private static UserAccount test3_2;
	private static UserAccount test4_1;
	private static UserAccount test4_2;
	private static UserAccount test5_1;
	private static UserAccount test6_1;
	private static UserAccount test6_2;
	private static UserAccount test7_1;
	private static UserAccount test8_1;
	
	private String password = "asdf";
	
	private static final BigDecimal TRANSACTION_AMOUNT = new BigDecimal(10.1).setScale(8, RoundingMode.HALF_UP);
	
	   
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
		
		if (!initialized) {
			mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).addFilter(springSecurityFilterChain).build();
			test1_1 = new UserAccount("test1_1@https://mbps.csg.uzh.ch", "test1_1@bitcoin.csg.uzh.ch", password);
			test1_2 = new UserAccount("test1_2@https://mbps.csg.uzh.ch", "test1_2@bitcoin.csg.uzh.ch", password);
			test2_1 = new UserAccount("test2_1@https://mbps.csg.uzh.ch", "test2_1@bitcoin.csg.uzh.ch", password);
			test2_2 = new UserAccount("test2_2@https://mbps.csg.uzh.ch", "test2_2@bitcoin.csg.uzh.ch", password);
			test3_1 = new UserAccount("test3_1@https://mbps.csg.uzh.ch", "test3_1@bitcoin.csg.uzh.ch", password);
			test3_2 = new UserAccount("test3_2@https://mbps.csg.uzh.ch", "test3_2@bitcoin.csg.uzh.ch", password);
			test4_1 = new UserAccount("test4_1@https://mbps.csg.uzh.ch", "test4_1@bitcoin.csg.uzh.ch", password);
			test4_2 = new UserAccount("test4_2@https://mbps.csg.uzh.ch", "test4_2@bitcoin.csg.uzh.ch", password);
			test5_1 = new UserAccount("test5_1@https://mbps.csg.uzh.ch", "test5_1@bitcoin.csg.uzh.ch", password);
			test6_1 = new UserAccount("test6_1@https://mbps.csg.uzh.ch", "test6_1@bitcoin.csg.uzh.ch", password);
			test6_2 = new UserAccount("test6_2@https://mbps.csg.uzh.ch", "test6_2@bitcoin.csg.uzh.ch", password);
			test7_1 = new UserAccount("test7_1@https://mbps.csg.uzh.ch", "test7_1@bitcoin.csg.uzh.ch", password);
			test8_1 = new UserAccount("test8_1@https://mbps.csg.uzh.ch", "test8_1@bitcoin.csg.uzh.ch", password);
			
			KeyPair keypair = KeyHandler.generateKeyPair();
			
			Constants.SERVER_KEY_PAIR = new CustomKeyPair(PKIAlgorithm.DEFAULT.getCode(), (byte) 1, KeyHandler.encodePublicKey(keypair.getPublic()), KeyHandler.encodePrivateKey(keypair.getPrivate()));
				
			initialized = true;
		}
	}
	
	@After
	public void tearDown() {
		UserAccountService.disableTestingMode();
	}
	
	@Test
	public void testCreateTransaction_failNotAuthenticated() throws Exception {
		assertTrue(userAccountService.createAccount(test1_1));
		assertTrue(userAccountService.createAccount(test1_2));
		
		UserAccount payerAccount = userAccountService.getByUsername(test1_1.getUsername());
		UserAccount payeeAccount  = userAccountService.getByUsername(test1_2.getUsername());
		payerAccount.setEmailVerified(true);
		payerAccount.setBalance(TRANSACTION_AMOUNT.add(BigDecimal.ONE));
		userAccountService.updateAccount(payerAccount);
		payeeAccount.setEmailVerified(true);
		payeeAccount.setBalance(TRANSACTION_AMOUNT);
		userAccountService.updateAccount(payeeAccount);
		
		KeyPair keyPairPayer = KeyHandler.generateKeyPair();
		
		byte keyNumberPayer = userAccountService.saveUserPublicKey(payerAccount.getId(), PKIAlgorithm.DEFAULT, KeyHandler.encodePublicKey(keyPairPayer.getPublic()));
		
		PaymentRequest paymentRequestPayer = new PaymentRequest(
				PKIAlgorithm.DEFAULT, 
				keyNumberPayer, 
				payerAccount.getUsername(), 
				payeeAccount.getUsername(), 
				Currency.BTC, 
				Converter.getLongFromBigDecimal(TRANSACTION_AMOUNT),
				System.currentTimeMillis());
		paymentRequestPayer.sign(keyPairPayer.getPrivate());
		
		ServerPaymentRequest request = new ServerPaymentRequest(paymentRequestPayer);
		TransactionObject t = new TransactionObject();
		t.setServerPaymentResponse(request.encode());
		
		CustomObjectMapper mapper = new CustomObjectMapper();
		String asString = mapper.writeValueAsString(t);
		
		mockMvc.perform(post("/transaction/create").secure(false).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testSendMoney() throws Exception {
		assertTrue(userAccountService.createAccount(test2_1));
		assertTrue(userAccountService.createAccount(test2_2));
		
		UserAccount payerAccount = userAccountService.getByUsername(test2_1.getUsername());
		UserAccount payeeAccount  = userAccountService.getByUsername(test2_2.getUsername());
		payerAccount.setEmailVerified(true);
		payerAccount.setBalance(TRANSACTION_AMOUNT.add(BigDecimal.ONE));
		userAccountService.updateAccount(payerAccount);
		payeeAccount.setEmailVerified(true);
		payeeAccount.setBalance(TRANSACTION_AMOUNT);
		userAccountService.updateAccount(payeeAccount);
		
		KeyPair payerKeyPair = KeyHandler.generateKeyPair();
	
		byte keyNumberPayer = userAccountService.saveUserPublicKey(payerAccount.getId(), PKIAlgorithm.DEFAULT, KeyHandler.encodePublicKey(payerKeyPair.getPublic()));
		
		PaymentRequest paymentRequestPayer = new PaymentRequest(
				PKIAlgorithm.DEFAULT, 
				keyNumberPayer, 
				payerAccount.getUsername(), 
				payeeAccount.getUsername(), 
				Currency.BTC, 
				Converter.getLongFromBigDecimal(TRANSACTION_AMOUNT),
				Currency.CHF, 
				Converter.getLongFromBigDecimal(new BigDecimal("0.5")), 
				System.currentTimeMillis());

		paymentRequestPayer.sign(payerKeyPair.getPrivate());
		
		ServerPaymentRequest spr = new ServerPaymentRequest(paymentRequestPayer);
		
		TransactionObject t = new TransactionObject();
		t.setServerPaymentResponse(spr.encode());
		
		BigDecimal payerBalanceBefore = payerAccount.getBalance();
		BigDecimal payeeBalanceBefore = payeeAccount.getBalance();
		
		CustomObjectMapper mapper = new CustomObjectMapper();
		String asString = mapper.writeValueAsString(t);
		
		HttpSession session = loginAndGetSession(test2_1.getUsername(), password);
		
		MvcResult mvcResult = mockMvc.perform(post("/transaction/create").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isOk())
				.andReturn();
		
		TransactionObject result = mapper.readValue(mvcResult.getResponse().getContentAsString(), TransactionObject.class);
		
		byte[] serverPaymentResponseEncoded = result.getServerPaymentResponse();
		ServerPaymentResponse serverPaymentResponse = DecoderFactory.decode(ServerPaymentResponse.class, serverPaymentResponseEncoded);
	
		UserAccount payerAccountUpdated = userAccountService.getById(payerAccount.getId());
		UserAccount payeeAccountUpdated = userAccountService.getById(payeeAccount.getId());
		
		assertTrue(result.isSuccessful());
		assertEquals(ServerResponseStatus.SUCCESS, serverPaymentResponse.getPaymentResponsePayer().getStatus());
		
		assertEquals(0, payerBalanceBefore.subtract(TRANSACTION_AMOUNT).compareTo(payerAccountUpdated.getBalance()));
		assertEquals(0, payeeBalanceBefore.add(TRANSACTION_AMOUNT).compareTo(payeeAccountUpdated.getBalance()));
		
		assertTrue(serverPaymentResponse.getPaymentResponsePayer().verify(KeyHandler.decodePublicKey(Constants.SERVER_KEY_PAIR.getPublicKey())));
		
		PaymentResponse responsePayer = serverPaymentResponse.getPaymentResponsePayer();
		
		assertEquals(paymentRequestPayer.getAmount(), responsePayer.getAmount());
		assertEquals(paymentRequestPayer.getUsernamePayer(), responsePayer.getUsernamePayer());
		assertEquals(paymentRequestPayer.getUsernamePayee(), responsePayer.getUsernamePayee());
	}
	
	@Test
	public void testSendMoney_failNotAuthenticatedUser() throws Exception {
		assertTrue(userAccountService.createAccount(test3_1));
		assertTrue(userAccountService.createAccount(test3_2));
		
		UserAccount payerAccount = userAccountService.getByUsername(test3_1.getUsername());
		UserAccount payeeAccount  = userAccountService.getByUsername(test3_2.getUsername());
		payerAccount.setEmailVerified(true);
		payerAccount.setBalance(TRANSACTION_AMOUNT.add(BigDecimal.ONE));
		userAccountService.updateAccount(payerAccount);
		payeeAccount.setEmailVerified(true);
		payeeAccount.setBalance(TRANSACTION_AMOUNT);
		userAccountService.updateAccount(payeeAccount);
		
		KeyPair payerKeyPair = KeyHandler.generateKeyPair();
	
		byte keyNumberPayer = userAccountService.saveUserPublicKey(payerAccount.getId(), PKIAlgorithm.DEFAULT, KeyHandler.encodePublicKey(payerKeyPair.getPublic()));
		
		PaymentRequest paymentRequestPayer = new PaymentRequest(
				PKIAlgorithm.DEFAULT, 
				keyNumberPayer, 
				payerAccount.getUsername(), 
				payeeAccount.getUsername(), 
				Currency.BTC, 
				Converter.getLongFromBigDecimal(TRANSACTION_AMOUNT),
				Currency.CHF, 
				Converter.getLongFromBigDecimal(new BigDecimal("0.5")), 
				System.currentTimeMillis());

		paymentRequestPayer.sign(payerKeyPair.getPrivate());
		
		ServerPaymentRequest spr = new ServerPaymentRequest(paymentRequestPayer);
		
		TransactionObject t = new TransactionObject();
		t.setServerPaymentResponse(spr.encode());
		
		BigDecimal payerBalanceBefore = payerAccount.getBalance();
		BigDecimal payeeBalanceBefore = payeeAccount.getBalance();
		
		CustomObjectMapper mapper = new CustomObjectMapper();
		String asString = mapper.writeValueAsString(t);
		
		HttpSession session = loginAndGetSession(test3_2.getUsername(), password);
		
		MvcResult mvcResult = mockMvc.perform(post("/transaction/create").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isOk())
				.andReturn();
		
		TransactionObject result = mapper.readValue(mvcResult.getResponse().getContentAsString(), TransactionObject.class);
		
		byte[] serverPaymentResponseEncoded = result.getServerPaymentResponse();
		ServerPaymentResponse serverPaymentResponse = DecoderFactory.decode(ServerPaymentResponse.class, serverPaymentResponseEncoded);
	
		UserAccount payerAccountUpdated = userAccountService.getById(payerAccount.getId());
		UserAccount payeeAccountUpdated = userAccountService.getById(payeeAccount.getId());
		
		assertTrue(result.isSuccessful());
		assertEquals(ServerResponseStatus.FAILURE, serverPaymentResponse.getPaymentResponsePayer().getStatus());
		assertEquals(TransactionService.NOT_AUTHENTICATED_USER, serverPaymentResponse.getPaymentResponsePayer().getReason());
		
		assertTrue(serverPaymentResponse.getPaymentResponsePayer().verify(KeyHandler.decodePublicKey(Constants.SERVER_KEY_PAIR.getPublicKey())));
		
		assertTrue(payerBalanceBefore.equals(payerAccountUpdated.getBalance()));
		assertTrue(payeeBalanceBefore.equals(payeeAccountUpdated.getBalance()));
		
		PaymentResponse responsePayer = serverPaymentResponse.getPaymentResponsePayer();
		
		assertEquals(paymentRequestPayer.getAmount(), responsePayer.getAmount());
		assertEquals(paymentRequestPayer.getUsernamePayer(), responsePayer.getUsernamePayer());
		assertEquals(paymentRequestPayer.getUsernamePayee(), responsePayer.getUsernamePayee());
	}
	
	@Test
	public void testCreateTransaction() throws Exception {
		assertTrue(userAccountService.createAccount(test4_1));
		test4_1 = userAccountService.getByUsername(test4_1.getUsername());
		test4_1.setEmailVerified(true);
		test4_1.setBalance(TRANSACTION_AMOUNT);
		userAccountService.updateAccount(test4_1);
		
		String plainTextPw = test4_2.getPassword();
		assertTrue(userAccountService.createAccount(test4_2));
		test4_2 = userAccountService.getByUsername(test4_2.getUsername());
		test4_2.setEmailVerified(true);
		userAccountService.updateAccount(test4_2);
		
		KeyPair keyPairPayer = KeyHandler.generateKeyPair();
		byte keyNumberPayer = userAccountService.saveUserPublicKey(test4_1.getId(), PKIAlgorithm.DEFAULT, KeyHandler.encodePublicKey(keyPairPayer.getPublic()));
		
		KeyPair keyPairPayee = KeyHandler.generateKeyPair();
		byte keyNumberPayee = userAccountService.saveUserPublicKey(test4_2.getId(), PKIAlgorithm.DEFAULT, KeyHandler.encodePublicKey(keyPairPayee.getPublic()));
		
		long timestamp = System.currentTimeMillis();
		
		PaymentRequest paymentRequestPayer = new PaymentRequest(
				PKIAlgorithm.DEFAULT, 
				keyNumberPayer, 
				test4_1.getUsername(), 
				test4_2.getUsername(), 
				Currency.BTC, 
				Converter.getLongFromBigDecimal(TRANSACTION_AMOUNT),
				timestamp);
		paymentRequestPayer.sign(keyPairPayer.getPrivate());
		
		PaymentRequest paymentRequestPayee = new PaymentRequest(
				PKIAlgorithm.DEFAULT, 
				keyNumberPayee, 
				test4_1.getUsername(), 
				test4_2.getUsername(), 
				Currency.BTC, 
				Converter.getLongFromBigDecimal(TRANSACTION_AMOUNT),
				timestamp);
		paymentRequestPayee.sign(keyPairPayee.getPrivate());
		
		ServerPaymentRequest request = new ServerPaymentRequest(paymentRequestPayer, paymentRequestPayee);
		
		TransactionObject t = new TransactionObject();
		t.setServerPaymentResponse(request.encode());
		
		CustomObjectMapper mapper = new CustomObjectMapper();
		String asString = mapper.writeValueAsString(t);
		
		BigDecimal payerBalanceBefore = userAccountService.getByUsername(test4_1.getUsername()).getBalance();
		BigDecimal payeeBalanceBefore = userAccountService.getByUsername(test4_2.getUsername()).getBalance();
		
		
		HttpSession session = loginAndGetSession(test4_2.getUsername(), plainTextPw);
		
		MvcResult mvcResult = mockMvc.perform(post("/transaction/create").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isOk())
				.andReturn();
		
		TransactionObject result = mapper.readValue(mvcResult.getResponse().getContentAsString(), TransactionObject.class);
		assertTrue(result.isSuccessful());
		assertNotNull(result.getServerPaymentResponse());
		ServerPaymentResponse response = DecoderFactory.decode(ServerPaymentResponse.class, result.getServerPaymentResponse());
		assertNotNull(response);
		assertNotNull(response.getPaymentResponsePayer());
		
		assertEquals(ServerResponseStatus.SUCCESS, response.getPaymentResponsePayer().getStatus());
		assertTrue(response.getPaymentResponsePayer().verify(KeyHandler.decodePublicKey(Constants.SERVER_KEY_PAIR.getPublicKey())));
		
		assertEquals(payerBalanceBefore.subtract(TRANSACTION_AMOUNT), userAccountService.getById(test4_1.getId()).getBalance());
		assertEquals(payeeBalanceBefore.add(TRANSACTION_AMOUNT), userAccountService.getById(test4_2.getId()).getBalance());
	}
	
	@Test
	public void testGetHistory_failNotAuthenticated() throws Exception {
		assertTrue(userAccountService.createAccount(test5_1));
		test5_1 = userAccountService.getByUsername(test5_1.getUsername());
		test5_1.setEmailVerified(true);
		test5_1.setBalance(TRANSACTION_AMOUNT.multiply(new BigDecimal(3)));
		userAccountService.updateAccount(test5_1);
		
		mockMvc.perform(get("/transaction/history").secure(false)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testGetHistory() throws Exception {
		assertTrue(userAccountService.createAccount(test6_1));
		test6_1 = userAccountService.getByUsername(test6_1.getUsername());
		test6_1.setEmailVerified(true);
		test6_1.setBalance(TRANSACTION_AMOUNT);
		userAccountService.updateAccount(test6_1);
		
		String plainTextPw = test6_2.getPassword();
		assertTrue(userAccountService.createAccount(test6_2));
		test6_2 = userAccountService.getByUsername(test6_2.getUsername());
		test6_2.setEmailVerified(true);
		userAccountService.updateAccount(test6_2);
		
		KeyPair keyPairPayer = KeyHandler.generateKeyPair();
		byte keyNumberPayer = userAccountService.saveUserPublicKey(test6_1.getId(), PKIAlgorithm.DEFAULT, KeyHandler.encodePublicKey(keyPairPayer.getPublic()));
		
		PaymentRequest paymentRequestPayer = new PaymentRequest(
				PKIAlgorithm.DEFAULT, 
				keyNumberPayer, 
				test6_1.getUsername(), 
				test6_2.getUsername(), 
				Currency.BTC, 
				Converter.getLongFromBigDecimal(TRANSACTION_AMOUNT),
				System.currentTimeMillis());
		paymentRequestPayer.sign(keyPairPayer.getPrivate());
		
		ServerPaymentRequest request = new ServerPaymentRequest(paymentRequestPayer);
		
		TransactionObject t = new TransactionObject();
		t.setServerPaymentResponse(request.encode());
		
		CustomObjectMapper mapper = new CustomObjectMapper();
		String asString = mapper.writeValueAsString(t);
		
		
		HttpSession session = loginAndGetSession(test6_1.getUsername(), plainTextPw);
		
		MvcResult mvcResult = mockMvc.perform(post("/transaction/create").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isOk())
				.andReturn();
		
		TransactionObject result = mapper.readValue(mvcResult.getResponse().getContentAsString(), TransactionObject.class);
		assertEquals(true, result.isSuccessful());
		assertNotNull(result.getServerPaymentResponse());
		ServerPaymentResponse response = DecoderFactory.decode(ServerPaymentResponse.class, result.getServerPaymentResponse());
		assertNotNull(response);
		assertNotNull(response.getPaymentResponsePayer());
		assertEquals(ServerResponseStatus.SUCCESS, response.getPaymentResponsePayer().getStatus());
		
		HistoryTransferRequestObject req = new HistoryTransferRequestObject();
		req.setTxPage(0);
		req.setTxPayInPage(0);
		req.setTxPayOutPage(0);
		req.setTxPayInUnverifiedPage(0);
		
		
		asString = mapper.writeValueAsString(req);
		
		mvcResult = mockMvc.perform(post("/transaction/history")
				.contentType(MediaType.APPLICATION_JSON).content(asString)
				.secure(false).session((MockHttpSession) session))
				.andExpect(status().isOk())
				.andReturn();
		
		GetHistoryTransferObject ghto = mapper.readValue(mvcResult.getResponse().getContentAsString(), GetHistoryTransferObject.class);
		assertTrue(ghto.isSuccessful());
		
		assertNotNull(ghto);
		assertEquals(1, ghto.getTransactionHistory().size());
		
		logout(mvcResult);
		
		mvcResult = mockMvc.perform(post("/transaction/history")
				.contentType(MediaType.APPLICATION_JSON).content(asString)
				.secure(false).session((MockHttpSession) session))
				.andExpect(status().isUnauthorized())
				.andReturn();
	}
	
	private void logout(MvcResult result) {
		result.getRequest().getSession().invalidate();
	}
	
	private HttpSession loginAndGetSession(String username, String plainTextPassword) throws Exception {
		HttpSession session = mockMvc.perform(post("/j_spring_security_check").secure(false).param("j_username", username).param("j_password", plainTextPassword))
				.andExpect(status().isOk())
				.andReturn()
				.getRequest()
				.getSession();
		
		return session;
	}
	
	private void createAccountAndVerifyAndReload(UserAccount userAccount, BigDecimal balance) throws UsernameAlreadyExistsException, UserAccountNotFoundException, BitcoinException, InvalidUsernameException, InvalidEmailException, EmailAlreadyExistsException, InvalidUrlException {
		assertTrue(userAccountService.createAccount(userAccount));
		userAccount = userAccountService.getByUsername(userAccount.getUsername());
		userAccount.setEmailVerified(true);
		userAccount.setBalance(balance);
		userAccountService.updateAccount(userAccount);
	}
	
	@Test
	public void testGetExchangeRateTest() throws Exception{
		createAccountAndVerifyAndReload(test7_1, BigDecimal.ONE);
		String plainTextPw = test7_1.getPassword();
		
		
		CustomObjectMapper mapper = new CustomObjectMapper();
		
		HttpSession session = loginAndGetSession(test7_1.getUsername(), plainTextPw);
		
		MvcResult mvcResult = mockMvc.perform(get("/transaction/exchange-rate").secure(false).session((MockHttpSession) session))
				.andExpect(status().isOk())
				.andReturn();
		
		TransferObject cro2 = mapper.readValue(mvcResult.getResponse().getContentAsString(), TransferObject.class);
		
		assertTrue(cro2.isSuccessful());
		
		String exchangeRate = cro2.getMessage();
		assertNotNull(exchangeRate);
		Double er = Double.valueOf(exchangeRate);
		assertTrue(er>0);
	}

	@Test
	public void testPayOut() throws Exception{
		BitcoindController.TESTING = true;
		createAccountAndVerifyAndReload(test8_1, BigDecimal.ONE);
		String plainTextPw = test8_1.getPassword();
		
		PayOutTransactionObject pot = new PayOutTransactionObject();
		pot.setBtcAddress("mtSKrDw1f1NfstiiwEWzhwYdt96dNQGa1S");
		pot.setAmount(new BigDecimal("0.5"));
		
		CustomObjectMapper mapper = new CustomObjectMapper();
		String asString = mapper.writeValueAsString(pot);
		
		HttpSession session = loginAndGetSession(test8_1.getUsername(), plainTextPw);
		
		MvcResult mvcResult = mockMvc.perform(post("/transaction/payOut").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isOk())
				.andReturn();
		
		TransferObject result = mapper.readValue(mvcResult.getResponse().getContentAsString(), TransferObject.class);
		
		assertTrue(result.isSuccessful());
	}
	
}
