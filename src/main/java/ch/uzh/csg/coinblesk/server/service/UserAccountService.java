/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.uzh.csg.coinblesk.server.service;

import ch.uzh.csg.coinblesk.server.dao.UserAccountDAO;
import ch.uzh.csg.coinblesk.server.entity.UserAccount;
import ch.uzh.csg.coinblesk.server.utils.Pair;
import com.coinblesk.json.UserAccountStatusTO;
import com.coinblesk.json.UserAccountTO;
import java.util.Date;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 *
 * @author draft
 */

@Service
public class UserAccountService {

    //as senn in: http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
    private static final String EMAIL_PATTERN = 
		"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
		+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    
    @Autowired
    private UserAccountDAO userAccountDao;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Transactional
    public UserAccount getByEmail(String email) {
        return userAccountDao.getByAttribute("email", email);
    }
    
    //TODO: only used for testing. remove if possible
    @Transactional
    public void save(UserAccount userAccount) {
        userAccountDao.save(userAccount);
    }
    
    @Transactional
    public Pair<UserAccountStatusTO, UserAccount> create(final UserAccountTO userAccountTO) {
        final String email = userAccountTO.email();
        if(email == null){
            return new Pair(new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.NO_EMAIL), null);
	}
        if(!email.matches(EMAIL_PATTERN)){
            return new Pair(new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.INVALID_EMAIL), null);
	}
        if(userAccountTO.password() == null || userAccountTO.password().length() < 6) {
            return new Pair(new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.PASSWORD_TOO_SHORT), null);
	}
        
        final UserAccount found = userAccountDao.getByAttribute("email", email);
        if(found != null) {
            if(found.getEmailToken()!=null) {
                return new Pair(new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.EMAIL_ALREADY_EXISTS_NOT_ACTIVATED), found);
            }
            return new Pair(new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.EMAIL_ALREADY_EXISTS_ACTIVATED), found);
        }
        
        //convert TO to Entity
        UserAccount userAccount = new UserAccount();
        userAccount.setEmail(email);
        userAccount.setPassword(passwordEncoder.encode(userAccountTO.password()));
        userAccount.setCreationDate(new Date());
	userAccount.setDeleted(false);
	userAccount.setVersion((byte)2);
        userAccount.setEmailToken(UUID.randomUUID().toString());
        userAccountDao.save(userAccount);
        return new Pair(new UserAccountStatusTO().setSuccess(), userAccount);
    }

    @Transactional
    public UserAccountStatusTO activate(String email, String token) {
        final UserAccount found = userAccountDao.getByAttribute("email", email);
        if(found == null) {
            //no such email address - notok
            return new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.NO_EMAIL);
        }
        if(found.getEmailToken() == null) {
            //already acitaveted - ok
            return new UserAccountStatusTO().setSuccess();
        }
        
        if(!found.getEmailToken().equals(token)) {
            //wrong token - notok
            return new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.INVALID_EMAIL_TOKEN);
        }
        
        //activate, enitiy is in attached state
        found.setEmailToken(null);
        return new UserAccountStatusTO().setSuccess();
    }

    @Transactional
    public UserAccountStatusTO delete(String email) {
        final int result = userAccountDao.remove(email);
        if(result == 0) {
            return new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.NO_ACCOUNT);
        }
        if(result > 1) {
            return new UserAccountStatusTO().reason(UserAccountStatusTO.Reason.ACCOUNT_ERROR);
        }
        return new UserAccountStatusTO().setSuccess();
    }

    @Transactional
    public UserAccountTO get(String email) {
        final UserAccount userAccount = userAccountDao.getByAttribute("email", email);
        if(userAccount == null) {
            return null;
        }
        final UserAccountTO userAccountTO = new UserAccountTO();
        userAccountTO.email(userAccount.getEmail());
        return userAccountTO;
    }
    
    //for debugging
    @Transactional
    public String getToken(String email) {
        final UserAccount userAccount = userAccountDao.getByAttribute("email", email);
        if(userAccount == null) {
            return null;
        }
        return userAccount.getEmailToken();
    }
}