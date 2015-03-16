package ch.uzh.csg.coinblesk.server.service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.minidev.json.JSONObject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.csg.coinblesk.customserialization.DecoderFactory;
import ch.uzh.csg.coinblesk.customserialization.PKIAlgorithm;
import ch.uzh.csg.coinblesk.customserialization.exceptions.UnknownPKIAlgorithmException;
import ch.uzh.csg.coinblesk.keys.CustomPublicKey;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.server.clientinterface.IActivities;
import ch.uzh.csg.coinblesk.server.clientinterface.IServerAccount;
import ch.uzh.csg.coinblesk.server.clientinterface.IServerAccountTasks;
import ch.uzh.csg.coinblesk.server.clientinterface.IUserAccount;
import ch.uzh.csg.coinblesk.server.dao.ServerAccountTasksDAO;
import ch.uzh.csg.coinblesk.server.dao.ServerPublicKeyDAO;
import ch.uzh.csg.coinblesk.server.domain.ServerAccount;
import ch.uzh.csg.coinblesk.server.domain.ServerAccountTasks;
import ch.uzh.csg.coinblesk.server.domain.UserAccount;
import ch.uzh.csg.coinblesk.server.response.HttpRequestHandler;
import ch.uzh.csg.coinblesk.server.response.HttpResponseHandler;
import ch.uzh.csg.coinblesk.server.security.KeyHandler;
import ch.uzh.csg.coinblesk.server.util.Config;
import ch.uzh.csg.coinblesk.server.util.Constants;
import ch.uzh.csg.coinblesk.server.util.ServerProperties;
import ch.uzh.csg.coinblesk.server.util.Subjects;
import ch.uzh.csg.coinblesk.server.util.exceptions.InvalidUrlException;
import ch.uzh.csg.coinblesk.server.util.exceptions.ServerAccountNotFoundException;
import ch.uzh.csg.coinblesk.server.web.customserialize.PayOutAdrressRequest;
import ch.uzh.csg.coinblesk.server.web.customserialize.PayOutAdrressResponse;
import ch.uzh.csg.coinblesk.server.web.response.CreateSAObject;
import ch.uzh.csg.coinblesk.server.web.response.ServerAccountObject;
import ch.uzh.csg.coinblesk.server.web.response.TransferPOAObject;

/**
 * Service class for {@link ServerAccountTasks}.
 * 
 */
@Service
public class ServerAccountTasksService implements IServerAccountTasks{
	private static Logger LOGGER = Logger.getLogger(ServerAccountTasksService.class);
	private static boolean TESTING_MODE = false;

	@Autowired
	private ServerAccountTasksDAO serverAccountTasksDAO;
	@Autowired
	private IUserAccount userAccountService;
	@Autowired
	private IActivities activitiesService;
	@Autowired
	private IServerAccount serverAccountService;
	@Autowired
	private ServerPublicKeyDAO serverPublicKeyDAO;

	/**
	 * Enables testing mode for JUnit Tests.
	 */
	public static void enableTestingMode() {
		TESTING_MODE = true;
	}
	
	public static boolean isTestingMode(){
		return TESTING_MODE;
	}

	/**
	 * Disables testing mode for JUnit Tests.
	 */
	public static void disableTestingMode() {
		TESTING_MODE = false;
	}
	
	public enum ServerAccountTaskTypes {
		CREATE_ACCOUNT((int) 1),
		ACCEPT_TRUST_ACCOUNT((int) 2),
		DECLINE_TRUST_ACCOUNT((int) 3);
		
		private int code;
		
		private ServerAccountTaskTypes(int code) {
			this.code = code;
		}
		
		public int getCode() {
			return code;
		}
	}
	

	public static boolean isValidServerAccountTaskType(int code) {
		if (code == ServerAccountTaskTypes.CREATE_ACCOUNT.getCode())
			return true;
		else if (code == ServerAccountTaskTypes.ACCEPT_TRUST_ACCOUNT.getCode())
			return true;
		else if (code == ServerAccountTaskTypes.DECLINE_TRUST_ACCOUNT.getCode())
			return true;
		else
			return false;
	}
	
	@Override
	@Transactional
	public void persistsCreateNewAccount(String url, String username, String email){
		ServerAccountTasks task = new ServerAccountTasks();
		task.setType(ServerAccountTaskTypes.CREATE_ACCOUNT.getCode());
		task.setUrl(url);
		task.setUsername(username);
		task.setEmail(email);
		task.setToken(java.util.UUID.randomUUID().toString());
		if(isTestingMode()){			
			String strDate = "2014-08-31 15:15:15.0";
			Date date = new Date();
			try {
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(strDate);
			} catch (ParseException e) {
				//ignore
			}
			task.setTimestamp(date);
			task.setToken("123456");
		}
		
		LOGGER.info(task.toString());
		serverAccountTasksDAO.persistAccount(task);
	}

	@Override
	@Transactional
	public void persistsCreateNewAccountPayOutAddress(String url, String username, String email, String payoutAddress){
		ServerAccountTasks task = new ServerAccountTasks();
		task.setType(ServerAccountTaskTypes.CREATE_ACCOUNT.getCode());
		task.setUrl(url);
		task.setUsername(username);
		task.setEmail(email);
		task.setPayoutAddress(payoutAddress);
		task.setToken(java.util.UUID.randomUUID().toString());
		
		if(isTestingMode()){			
			String strDate = "2014-08-31 15:15:15.0";
			Date date = new Date();
			try {
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(strDate);
			} catch (ParseException e) {
				//ignore
			}
			task.setTimestamp(date);
			task.setToken("123457");
		}
		
		serverAccountTasksDAO.persistAccount(task);
	}
	
	@Override
	@Transactional
	public void persistsUpgradeAccount(String url, String username, String email, int trustLevel){
		ServerAccountTasks task = new ServerAccountTasks();
		task.setType(ServerAccountTaskTypes.ACCEPT_TRUST_ACCOUNT.getCode());
		task.setUrl(url);
		task.setUsername(username);
		task.setEmail(email);
		task.setTrustLevel(trustLevel);
		task.setToken(java.util.UUID.randomUUID().toString());
		
		if(isTestingMode()){			
			String strDate = "2014-08-31 15:15:15.0";
			Date date = new Date();
			try {
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(strDate);
			} catch (ParseException e) {
				//ignore
			}
			task.setTimestamp(date);
			task.setToken("123456");
		}
		
		serverAccountTasksDAO.persistAccount(task);
	}

	@Override
	@Transactional
	public void persistsDowngradeAccount(String url, String username, String email, Integer trustLevel) {
		ServerAccountTasks task = new ServerAccountTasks();
		task.setType(ServerAccountTaskTypes.DECLINE_TRUST_ACCOUNT.getCode());
		task.setUrl(url);
		task.setUsername(username);
		task.setEmail(email);
		task.setTrustLevel(trustLevel);
		task.setToken(java.util.UUID.randomUUID().toString());
		
		if(isTestingMode()){			
			String strDate = "2014-08-31 15:15:15.0";
			Date date = new Date();
			try {
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(strDate);
			} catch (ParseException e) {
				//ignore
			}
			task.setTimestamp(date);
			task.setToken("123456");
		}
		
		serverAccountTasksDAO.persistAccount(task);
		
	}
	
	@Override
	@Transactional(readOnly=true)
	public ServerAccountTasks getAccountTaskByUrlAndDate(String url, Date date){
		return serverAccountTasksDAO.getAccountTaskByUrlAndDate(url, date);
	}
	
	@Override
	@Transactional(readOnly = true)
	public ServerAccountTasks getAccountTasksByToken(String token){
		return serverAccountTasksDAO.getAccountTasksByToken(token);
	}

	@Override
	@Transactional
	public void deleteTask(int type, String token) {
		serverAccountTasksDAO.deleteTask(type,token);
	}
	
	@Override
	@Transactional(readOnly=true)
	public boolean checkIfExists(String url, int type){
		try{			
			serverAccountTasksDAO.getAccountTasksByUrl(url, type);
			return true;
		} catch (ServerAccountNotFoundException e) {
			return false;
		}
	}
	
	@Override
	@Transactional
	public List<ServerAccountTasks> processNewAccountTask(int type){
		return serverAccountTasksDAO.getAllAccountTasksBySubject(type);
	}
	
	@Override
	@Transactional
	public List<ServerAccountTasks> getProceedAccounts(){
		return serverAccountTasksDAO.getProceedAccounts();
	}

	@Override
	@Transactional
	public List<ServerAccountTasks> getAccountsByType(int type){
		return serverAccountTasksDAO.getAccountsByType(type);
	}
	
	@Override
	@Transactional
	public void updateProceed(String token){
		ServerAccountTasks task = serverAccountTasksDAO.getAccountTasksByToken(token);
		serverAccountTasksDAO.updatedProceed(task);
	}
	
	@Override
	@Transactional
	public void createNewAccount(String url, String email, UserAccount user, String token) throws Exception {
		//Get custom public key of the server
		CustomPublicKey cpk = new CustomPublicKey(Constants.SERVER_KEY_PAIR.getKeyNumber(), 
				Constants.SERVER_KEY_PAIR.getPkiAlgorithm(), Constants.SERVER_KEY_PAIR.getPublicKey());
		CreateSAObject create = new CreateSAObject();
		create.setUrl(ServerProperties.getProperty("url.base"));
		create.setEmail(user.getEmail());
		create.setKeyNumber((byte) cpk.getKeyNumber());
		create.setPkiAlgorithm(cpk.getPkiAlgorithm());
		create.setPublicKey(cpk.getPublicKey());
		//encode object to json
		JSONObject jsonObj = new JSONObject();
		try {
			create.encode(jsonObj);
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}

		CloseableHttpResponse resBody;
		String urlCreate = url+ Config.CREATE_NEW_SERVER;
		CreateSAObject csao = new CreateSAObject();
		try {
			//execute post request
			resBody = HttpRequestHandler.prepPostResponse(jsonObj, urlCreate);
			try {
				csao = HttpResponseHandler.getResponse(csao, resBody);
			} finally {
				resBody.close();
			}
		} catch (IOException e) {
			//if response not correct store account into db for hourly tasks
			if(token == null){
				persistsCreateNewAccount(url, user.getUsername(), email);
			}
			throw new IOException(e.getMessage());
		}		
		
		if (csao.isSuccessful()) {
			// if urls are different throw exception
			if (!url.equals(csao.getUrl())) {
				if (token == null)
					persistsCreateNewAccount(url, user.getUsername(), email);
				
				throw new InvalidUrlException();
			}
			ServerAccount serverAccount = new ServerAccount(csao.getUrl(),email);

			// if serveraccount is deleted undo delete
			if (serverAccountService.isDeletedByUrl(csao.getUrl())) {
				serverAccountService.undeleteServerAccountByUrl(csao.getUrl());
			} else {
				boolean success = serverAccountService.persistAccount(serverAccount);
				// if creation failed throw exception
				if (!success) {
					if (token == null)
						persistsCreateNewAccount(url, user.getUsername(), email);
					
					throw new Exception();
				}
				ServerAccount serverAccountPK = serverAccountService.getByUrl(csao.getUrl());
				PKIAlgorithm pkiAlgorithm;
				try {
					pkiAlgorithm = PKIAlgorithm.getPKIAlgorithm(csao.getPkiAlgorithm());
				} catch (UnknownPKIAlgorithmException e1) {
					if(token == null)
						persistsCreateNewAccount(url, user.getUsername(), email);
					throw new UnknownPKIAlgorithmException();
				}
				serverPublicKeyDAO.saveUserPublicKey(serverAccountPK.getId(), pkiAlgorithm, csao.getPublicKey());
			}
			
			try {
				updatedPayoutAddress(csao.getUrl(), email, user, token);
			} catch (Exception e) {
				if (token == null)
					persistsCreateNewAccount(url, user.getUsername(), email);
				
				throw new Exception();
			}
			
			if(token != null)
				updateProceed(token);
		} else {
			if (token == null)
				persistsCreateNewAccount(url, user.getUsername(), email);

			LOGGER.info("Http request failed");
			activitiesService.activityLog(user.getUsername(), Subjects.FAILED_CREATE_SERVER_ACCOUNT,"Failed to create a new relation with the server " + url + " and email " + email);
		}
		activitiesService.activityLog(user.getUsername(), Subjects.CREATE_SERVER_ACCOUNT,"Create a new relation with the server " + url + " and email " + email);
	}
	
	@Override
	@Transactional
	public void updatedPayoutAddress(String url, String email, UserAccount user, String token) throws Exception{
		ServerAccount account;
		try {
			account = serverAccountService.getByUrl(url);
		} catch (ServerAccountNotFoundException e1) {
			throw new ServerAccountNotFoundException(url);
		}
		TransferPOAObject requestObject = new TransferPOAObject();
		requestObject.setEmail(user.getEmail());
		requestObject.setUrl(ServerProperties.getProperty("url.base"));
		
		//get keys
		CustomPublicKey cpk = new CustomPublicKey(Constants.SERVER_KEY_PAIR.getKeyNumber(), 
				Constants.SERVER_KEY_PAIR.getPkiAlgorithm(), Constants.SERVER_KEY_PAIR.getPublicKey());
		PKIAlgorithm pkiAlgorithm;
		try {
			pkiAlgorithm = PKIAlgorithm.getPKIAlgorithm(cpk.getPkiAlgorithm());
		} catch (UnknownPKIAlgorithmException e1) {
			if(token == null)
				persistsCreateNewAccount(url, user.getUsername(), email);
			throw new UnknownPKIAlgorithmException();
		}
		PayOutAdrressRequest paar = new PayOutAdrressRequest(1, pkiAlgorithm,cpk.getKeyNumber(), account.getPayinAddress());
		paar.sign(KeyHandler.decodePrivateKey(Constants.SERVER_KEY_PAIR.getPrivateKey()));
		requestObject.setPayOutAddress(paar.encode());
		
		JSONObject jsonAccount = new JSONObject();
		try {
			requestObject.encode(jsonAccount);
		} catch (Exception e) {
			if(token == null)
				persistsCreateNewAccountPayOutAddress(url, user.getUsername(), email, account.getPayinAddress());
			
			throw new Exception(e.getMessage());
		}
//		ServerAccountObject createAccount = new ServerAccountObject();
//		createAccount.setUrl(account.getUrl());
//		createAccount.setEmail(account.getEmail());
		// The payin address for a new server relation is created 
//		createAccount.setPayoutAddress(account.getPayinAddress());

//		JSONObject jsonAccount = new JSONObject();
//		try {
//			createAccount.encode(jsonAccount);
//		} catch (Exception e) {
//			if(token == null)
//				persistsCreateNewAccountPayOutAddress(url, user.getUsername(), email, createAccount.getPayinAddress());
//				
//			throw new Exception(e.getMessage());
//		}
		
		
		// The http request is prepared and send with the information$
		CloseableHttpResponse resBody2;
		String urlCreateData = url+ Config.CREATE_NEW_SERVER_PUBLIC_KEY;
		TransferPOAObject poaResponse = new TransferPOAObject();
//		ServerAccountObject sao = new ServerAccountObject();
		try {
			//execute post request
			resBody2 = HttpRequestHandler.prepPostResponse(jsonAccount, urlCreateData);
			try {
				poaResponse = HttpResponseHandler.getResponse(poaResponse, resBody2);
//				sao = HttpResponseHandler.getResponse(sao, resBody2);
			} finally {
				resBody2.close();
			}
			
		} catch (IOException e) {
			if(token == null)
				persistsCreateNewAccountPayOutAddress(url, user.getUsername(), email, account.getPayinAddress());
//				persistsCreateNewAccountPayOutAddress(url, user.getUsername(), email, createAccount.getPayinAddress());
			
			throw new IOException(e.getMessage());
		}
		
		// If successful store the received payout address into the database.
		if(poaResponse.isSuccessful()) {
			ServerAccount responseAccount = serverAccountService.getByUrl(poaResponse.getUrl());
			PayOutAdrressResponse paarResponse = DecoderFactory.decode(PayOutAdrressResponse.class, poaResponse.getPayOutAddress());
			if(!paarResponse.verify(KeyHandler.decodePublicKey(serverPublicKeyDAO.getServerPublicKey(account.getId(), (byte) paarResponse.getKeyNumber()).getPublicKey()))){
				if(token == null)
					persistsCreateNewAccountPayOutAddress(url, user.getUsername(), email, account.getPayinAddress());
				
				throw new IOException();
			}
			responseAccount.setPayoutAddress(paarResponse.getPayOutAddressResponse());
			serverAccountService.updatePayoutAddressAccount(responseAccount.getUrl(), responseAccount);
		} else {
			if (token == null)
				persistsCreateNewAccountPayOutAddress(url, user.getUsername(), email, account.getPayinAddress());
//				persistsCreateNewAccountPayOutAddress(url, user.getUsername(), email, createAccount.getPayinAddress());
		}
	}
	
	/**
	 * Remove the database entries which are accomplished successfully.
	 * 
	 * @param token
	 * @return boolean
	 */
	@Override
	@Transactional
	public boolean removeProceedTasks(String token){
		ServerAccountTasks task = getAccountTasksByToken(token);
		if(ServerAccountTasksService.isValidServerAccountTaskType(task.getType())){			
			deleteTask(task.getType(), token);
			return true;
		}
		return false;
	}
	
	@Override
	@Transactional
	public void upgradedTrustLevel(String username, String email, String url, int trustLevel, String token) throws Exception {		
		ServerAccountObject updatedAccount = new ServerAccountObject(ServerProperties.getProperty("url.base"), email);
		updatedAccount.setTrustLevel(trustLevel);
		JSONObject jsonAccount = new JSONObject();
		try {
			updatedAccount.encode(jsonAccount);
		} catch (Exception e) {
			if(token == null)
				persistsUpgradeAccount(url, username, email, trustLevel);
			throw new Exception(e.getMessage());
		}
		
		// The http request is prepared and send with the information
		CloseableHttpResponse resBody;
		String urlData = url+ Config.ACCEPT_UPGRADE_TRUST_LEVEL;
		TransferObject response = new TransferObject();
		try {
			//execute post request
			resBody = HttpRequestHandler.prepPostResponse(jsonAccount, urlData);
			try {
				response = HttpResponseHandler.getResponse(response, resBody);

			} finally {
				resBody.close();
			}
			
		} catch (IOException e) {
			if(token == null)
				persistsUpgradeAccount(url, username, email, trustLevel);
			throw new IOException(e.getMessage());
		}
		
		if(response.isSuccessful()){
			serverAccountService.updateTrustLevel(urlData, trustLevel);
			updateProceed(token);
			activitiesService.activityLog(username, Subjects.UPGRADE_TRUST_LEVEL,"Trust relation with server " + url + " is updated to " + trustLevel);
		} else{
			if(token == null)
				persistsUpgradeAccount(url, username, email, trustLevel);
		}
	}

	@Override
	@Transactional
	public void downgradeTrustLevel(String username, String email, String url, Integer trustLevel, String token) throws Exception {
		ServerAccountObject updatedAccount = new ServerAccountObject(ServerProperties.getProperty("backup.dir"), email);
		updatedAccount.setTrustLevel(trustLevel);
		JSONObject jsonAccount = new JSONObject();
		try {
			updatedAccount.encode(jsonAccount);
		} catch (Exception e) {
			if(token == null)
				persistsDowngradeAccount(url, username, email, trustLevel);
			throw new Exception(e.getMessage());
		}
		
		// The http request is prepared and send with the information
		CloseableHttpResponse resBody;
		String urlData = url+ Config.DECLINE_UPGRADE_TRUST_LEVEL;
		TransferObject response = new TransferObject();
		try {
			//execute post request
			resBody = HttpRequestHandler.prepPostResponse(jsonAccount, urlData);
			try {
				response = HttpResponseHandler.getResponse(response, resBody);
			} finally {
				resBody.close();
			}
			
		} catch (IOException e) {
			if(token == null)
				persistsDowngradeAccount(url, username, email, trustLevel);
			throw new IOException(e.getMessage());
		}
		
		if(response.isSuccessful()){
			serverAccountService.updateTrustLevel(urlData, trustLevel);
			updateProceed(token);
			activitiesService.activityLog(username, Subjects.UPGRADE_TRUST_LEVEL,"Trust relation with server " + url + " is updated to " + trustLevel);
		} else{
			if(token == null)
				persistsUpgradeAccount(url, username, email, trustLevel);
		}
	}

	@Override
	public ServerAccountTasks getAccountTasksByUrl(String url, int type) throws ServerAccountNotFoundException {
		return serverAccountTasksDAO.getAccountTasksByUrl(url, type);
	}
}