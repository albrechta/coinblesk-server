package ch.uzh.csg.coinblesk.server.controllerui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.uzh.csg.coinblesk.responseobject.ServerAccountTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.server.clientinterface.IActivities;
import ch.uzh.csg.coinblesk.server.clientinterface.IServerAccount;
import ch.uzh.csg.coinblesk.server.clientinterface.IServerAccountTasks;
import ch.uzh.csg.coinblesk.server.clientinterface.IUserAccount;
import ch.uzh.csg.coinblesk.server.domain.ServerAccount;
import ch.uzh.csg.coinblesk.server.domain.UserAccount;
import ch.uzh.csg.coinblesk.server.service.ServerAccountTasksService.ServerAccountTaskTypes;
import ch.uzh.csg.coinblesk.server.util.AuthenticationInfo;
import ch.uzh.csg.coinblesk.server.util.Config;
import ch.uzh.csg.coinblesk.server.util.ServerProperties;
import ch.uzh.csg.coinblesk.server.util.exceptions.InvalidEmailException;
import ch.uzh.csg.coinblesk.server.util.exceptions.InvalidUrlException;
import ch.uzh.csg.coinblesk.server.util.exceptions.ServerAccountNotFoundException;
import ch.uzh.csg.coinblesk.server.util.exceptions.ServerAccountTasksAlreadyExists;
import ch.uzh.csg.coinblesk.server.util.exceptions.UrlAlreadyExistsException;
import ch.uzh.csg.coinblesk.server.util.exceptions.UserAccountNotFoundException;
import ch.uzh.csg.coinblesk.server.web.response.ServerAccountObject;

import com.azazar.bitcoin.jsonrpcclient.BitcoinException;

@Controller
@RequestMapping("/relation")
public class RelationController {
	
	@Autowired
	private IActivities activitiesService;
	@Autowired
	private IServerAccountTasks serverAccountTasksService;
	@Autowired
	private IServerAccount serverAccountService;
	@Autowired
	private IUserAccount userAccountService;
	
	@RequestMapping(method = RequestMethod.GET)
	public String relation() {
        return "html/relation";
    }
	
	@RequestMapping(value={"/accounts"}, method = RequestMethod.POST, produces="application/json")
	@ResponseBody public ServerAccountTransferObject getAccounts() {
		ServerAccountTransferObject response = new ServerAccountTransferObject();
		List<ServerAccount> accounts = serverAccountService.getAll();
		List<ch.uzh.csg.coinblesk.model.ServerAccount> transformAccounts = new ArrayList<ch.uzh.csg.coinblesk.model.ServerAccount>();
		for(ServerAccount account: accounts){
			transformAccounts.add(transformServer(account));
		}
		response.setServerAccountList(transformAccounts);
		response.setMessage(Config.SUCCESS);
		response.setSuccessful(true);
		return response;
    }
	
	private ch.uzh.csg.coinblesk.model.ServerAccount transformServer(ServerAccount account) {
		ch.uzh.csg.coinblesk.model.ServerAccount o = new ch.uzh.csg.coinblesk.model.ServerAccount();
		o.setId(account.getId());
		o.setUrl(account.getUrl());
		o.setPayinAddress(account.getPayinAddress());
		o.setPayoutAddress(account.getPayoutAddress());
		o.setTrustLevel(account.getTrustLevel());
		o.setActiveBalance(account.getActiveBalance());
		o.setBalanceLimit(account.getBalanceLimit());
		o.setUserBalanceLimit(account.getUserBalanceLimit());
		return o;
	}
	
	@RequestMapping(value={"/fulltrust"}, method = RequestMethod.POST, produces="application/json")
	@ResponseBody public ServerAccountTransferObject getFullTrust() {
		ServerAccountTransferObject response = new ServerAccountTransferObject();
		List<ServerAccount> accounts = serverAccountService.getByTrustLevel(2);
		List<ch.uzh.csg.coinblesk.model.ServerAccount> transformAccounts = new ArrayList<ch.uzh.csg.coinblesk.model.ServerAccount>();
		for(ServerAccount account: accounts){
			transformAccounts.add(transformServer(account));
		}
		response.setServerAccountList(transformAccounts);
		response.setMessage(Config.SUCCESS);
		response.setSuccessful(true);
		return response;
    }
	
	@RequestMapping(value={"/account"}, method = RequestMethod.POST, consumes="application/json", produces="application/json")
	@ResponseBody public ServerAccountObject account(@RequestBody ServerAccountObject request) throws ServerAccountNotFoundException{
		ServerAccountObject response = new ServerAccountObject();
		ServerAccount account = serverAccountService.getById(request.getId());
		response = transformServerObject(account);
		response.setMessage(Config.SUCCESS);
		response.setSuccessful(true);
		return response;
	}

	private ServerAccountObject transformServerObject(ServerAccount account) {
		ServerAccountObject o = new ServerAccountObject();
		o.setId(account.getId());
		o.setUrl(account.getUrl());
		o.setPayinAddress(account.getPayinAddress());
		o.setPayoutAddress(account.getPayoutAddress());
		o.setTrustLevel(account.getTrustLevel());
		o.setActiveBalance(account.getActiveBalance());
		o.setBalanceLimit(account.getBalanceLimit());
		o.setUserBalanceLimit(account.getUserBalanceLimit());
		return o;
	}
	
	@RequestMapping(value = { "/createNewAccount" }, method = RequestMethod.POST, consumes="application/json", produces="application/json")
	@ResponseBody public TransferObject createAccount(@RequestBody ServerAccountObject request) throws Exception {		

		if(request.getUrl()== null){
			throw new UserAccountNotFoundException(request.getUrl());
		}
		
		if(request.getUrl().equals(ServerProperties.getProperty("url.base")))
			throw new InvalidUrlException();
		
		TransferObject response = new TransferObject();
		UserAccount user;
		try{			
			user = userAccountService.getByUsername(AuthenticationInfo.getPrincipalUsername());
		} catch(UserAccountNotFoundException e) {
			throw new UserAccountNotFoundException(AuthenticationInfo.getPrincipalUsername());
		}
		
		//check if the request is already launched
		//if yes do not proceed throw exception
		if(serverAccountTasksService.checkIfExists(request.getUrl(), ServerAccountTaskTypes.CREATE_ACCOUNT.getCode())){
			throw new ServerAccountTasksAlreadyExists();
		}
		
		if(serverAccountService.isDeletedByUrl(request.getUrl()) || !serverAccountService.checkIfExistsByUrl(request.getUrl())){
			serverAccountTasksService.createNewAccount(request.getUrl(),request.getEmail(),user, null);
		}
		
		response.setMessage(Config.SUCCESS);
		response.setSuccessful(true);
		return response;
	}
	
	@RequestMapping(value = { "/persistNewAccount" }, method = RequestMethod.POST, consumes="application/json")
	@ResponseBody public TransferObject persistAccount(@RequestBody ServerAccount request) {
		TransferObject response = new TransferObject();
		try {
			serverAccountService.persistAccount(request);
			response.setMessage(Config.SUCCESS);
			response.setSuccessful(true);
			return response;
		} catch (UrlAlreadyExistsException | BitcoinException e) {
			response.setMessage(Config.FAILED);
			response.setSuccessful(false);
			return response;
		} catch (InvalidEmailException | InvalidUrlException e) {
			response.setMessage(Config.FAILED);
			response.setSuccessful(false);
			return response;
		}
	}
}
