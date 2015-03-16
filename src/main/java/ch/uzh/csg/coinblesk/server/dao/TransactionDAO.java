package ch.uzh.csg.coinblesk.server.dao;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import ch.uzh.csg.coinblesk.customserialization.Currency;
import ch.uzh.csg.coinblesk.model.HistoryTransaction;
import ch.uzh.csg.coinblesk.server.domain.DbTransaction;
import ch.uzh.csg.coinblesk.server.domain.UserAccount;
import ch.uzh.csg.coinblesk.server.util.Config;
import ch.uzh.csg.coinblesk.server.util.exceptions.UserAccountNotFoundException;
import ch.uzh.csg.coinblesk.util.Converter;

/**
 * DatabaseAccessObject for {@link DbTransaction}. Handles all DB operations regarding
 * {@link DbTransaction}s between two {@link UserAccount}s.
 * 
 */
@Repository
public class TransactionDAO {
	private static Logger LOGGER = Logger.getLogger(TransactionDAO.class);

	@PersistenceContext
	private EntityManager em;
	
	/**
	 * Returns defined amount of {@link DbTransaction}s assigned to the given
	 * username as an ArrayList. Number of Transactions and selection is defined
	 * in the Config-File and by the parameter "page".
	 * 
	 * @param username
	 *            for which history is requested
	 * @param page
	 *            which defines which page of Transactions shall be returned
	 *            (NrX to NrY)
	 * @return ArrayList with requested amount of HistoryTransactions
	 * @throws UserAccountNotFoundException
	 */
	public List<HistoryTransaction> getHistory(UserAccount userAccount, int page) throws UserAccountNotFoundException {
		if (page < 0) {
			return null;
		}
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<HistoryTransaction> cq = cb.createQuery(HistoryTransaction.class);
		Root<DbTransaction> root = cq.from(DbTransaction.class);
		cq.select(cb.construct(HistoryTransaction.class,root.get("timestamp"), root.get("usernamePayer"),root.get("usernamePayee"), 
				root.get("amount"), root.get("inputCurrency"), root.get("inputCurrencyAmount"), root.get("serverPayer"), root.get("serverPayee")));
		
		Predicate condition1 = cb.equal(root.get("usernamePayer"), userAccount.getUsername());
		Predicate condition2 = cb.equal(root.get("usernamePayee"), userAccount.getUsername());
		Predicate condition3 = cb.or(condition1, condition2);
		cq.where(condition3);
		
		cq.orderBy(cb.desc(root.get("timestamp")));
		List<HistoryTransaction> resultWithAliasedBean = em.createQuery(cq)
				.setFirstResult(page * Config.TRANSACTIONS_MAX_RESULTS)
				.setMaxResults(Config.TRANSACTIONS_MAX_RESULTS)
				.getResultList();
		
		return resultWithAliasedBean;
	}

	/**
	 * Counts number of {@link DbTransaction}-entries for given username and
	 * returns number as long.
	 * 
	 * @param username
	 *            for which Transactions shall be counted.
	 * @return number of Transactions assigned to username.
	 * @throws UserAccountNotFoundException
	 */
	public long getHistoryCount(UserAccount userAccount) throws UserAccountNotFoundException {
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<DbTransaction> root = cq.from(DbTransaction.class);
		cq.select(cb.count(root));
		
		Predicate condition1 = cb.equal(root.get("usernamePayer"), userAccount.getUsername());
		Predicate condition2 = cb.equal(root.get("usernamePayee"), userAccount.getUsername());
		Predicate condition3 = cb.or(condition1, condition2);
		cq.where(condition3);
		return em.createQuery(cq).getSingleResult();
	}

	/**
	 * Saves a new {@link DbTransaction} in the database.
	 * 
	 * @param tx
	 *            to save in the DB
	 * @param buyerAccount
	 *            UserAccount from which transaction-amount is subtracted.
	 * @param sellerAccount
	 *            UserAccount to which transaction-amount is added.
	 */
	public void createTransaction(DbTransaction tx, UserAccount buyerAccount, UserAccount sellerAccount) {
		em.persist(tx);
		buyerAccount.setBalance(buyerAccount.getBalance().subtract(tx.getAmount()));
		em.merge(buyerAccount);
		sellerAccount.setBalance(sellerAccount.getBalance().add(tx.getAmount()));
		em.merge(sellerAccount);
		LOGGER.info("Transaction created: " + tx.toString());
	}

	/**
	 * Saves a new {@link DbTransaction} in the database. But this is called
	 * when a cross server transaction happened. 
	 * 
	 * @param tx
	 *            to save in the DB
	 * @param account
	 *           UserAccount from which transaction-amount is subtracted or added.
	 * @param received
	 *           Boolean tells if the user is a receiver or sender.
	 */
	public void createTransaction(DbTransaction tx, UserAccount account, boolean received) {
		em.persist(tx);
		if(received){
			account.setBalance(account.getBalance().add(tx.getAmount()));			
		}else{			
			account.setBalance(account.getBalance().subtract(tx.getAmount()));
		}
		em.merge(account);
		LOGGER.info("Transaction created: " + tx.toString());
	}
	
	/**
	 * Checks if a Transaction with the given parameters does already exist.
	 * 
	 * @param usernamePayer
	 *            the payer's username
	 * @param usernamePayee
	 *            the payee's username
	 * @param currency
	 *            the currency
	 * @param amount
	 *            the amount
	 * @param timestamp
	 *            the payer's timestamp
	 * @return
	 */
	public boolean exists(String usernamePayer, String usernamePayee, Currency currency, long amount, long timestampPayer) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		
		Root<DbTransaction> root = cq.from(DbTransaction.class);
		cq.select(cb.count(root));
		
		Predicate condition1 = cb.equal(root.get("usernamePayer"), usernamePayer);
		Predicate condition2 = cb.equal(root.get("usernamePayee"), usernamePayee);
		Predicate condition3= cb.equal(root.get("currency"), currency.getCurrencyCode());
		Predicate condition4 = cb.equal(root.get("amount"), Converter.getBigDecimalFromLong(amount));
		Predicate condition5= cb.equal(root.get("timestampPayer"), timestampPayer);
		
		Predicate condition6 = cb.and(condition1, condition2, condition3, condition4, condition5);
		
		cq.where(condition6);
		Long count = em.createQuery(cq).getSingleResult();
		
		return count > 0;
	}

	/**
	 * Returns 5 newest Transactions as {@link HistoryTransaction}s in
	 * descending order.
	 * 
	 * @param username
	 * @return ArrayList<HistoryTransaction>
	 * @throws UserAccountNotFoundException
	 */
	public List<HistoryTransaction> getLast5Transactions(UserAccount userAccount) throws UserAccountNotFoundException {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<HistoryTransaction> cq = cb.createQuery(HistoryTransaction.class);
		Root<DbTransaction> root = cq.from(DbTransaction.class);
		cq.select(cb.construct(HistoryTransaction.class,root.get("timestamp"), root.get("usernamePayer"),root.get("usernamePayee"), 
				root.get("amount"), root.get("inputCurrency"), root.get("inputCurrencyAmount"), root.get("serverPayer"), root.get("serverPayee")));
		
		Predicate condition1 = cb.equal(root.get("usernamePayer"), userAccount.getUsername());
		Predicate condition2 = cb.equal(root.get("usernamePayee"), userAccount.getUsername());
		Predicate condition3= cb.or(condition1, condition2);
		cq.where(condition3);
		
		cq.orderBy(cb.desc(root.get("timestamp")));
		List<HistoryTransaction> resultWithAliasedBean = em.createQuery(cq)
				.setMaxResults(5)
				.getResultList();
		
		return resultWithAliasedBean;	
	}

	public List<HistoryTransaction> getAll() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<HistoryTransaction> cq = cb.createQuery(HistoryTransaction.class);
		Root<DbTransaction> root = cq.from(DbTransaction.class);
		cq.select(cb.construct(HistoryTransaction.class,root.get("timestamp"), root.get("usernamePayer"),root.get("usernamePayee"), 
				root.get("amount"), root.get("inputCurrency"), root.get("inputCurrencyAmount"), root.get("serverPayer"), root.get("serverPayee")));
		List<HistoryTransaction> resultWithAliasedBean = em.createQuery(cq)
				.getResultList();
		
		return resultWithAliasedBean;
    }

	public List<HistoryTransaction> getAll(UserAccount userAccount) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<HistoryTransaction> cq = cb.createQuery(HistoryTransaction.class);
		Root<DbTransaction> root = cq.from(DbTransaction.class);
		cq.select(cb.construct(HistoryTransaction.class,root.get("timestamp"), root.get("usernamePayer"),root.get("usernamePayee"), 
				root.get("amount"), root.get("inputCurrency"), root.get("inputCurrencyAmount"), root.get("serverPayer"), root.get("serverPayee")));
		
		Predicate condition1 = cb.equal(root.get("usernamePayer"), userAccount.getUsername());
		Predicate condition2 = cb.equal(root.get("usernamePayee"), userAccount.getUsername());
		Predicate condition3= cb.and(condition1, condition2);
		cq.where(condition3);
		
		cq.orderBy(cb.desc(root.get("timestamp")));
		List<HistoryTransaction> resultWithAliasedBean = em.createQuery(cq)
				.getResultList();
		
		return resultWithAliasedBean;
    }

	public BigDecimal transactionSumByServerAsPayer(String url, String username) {
		
		BigDecimal sum = new BigDecimal(em.createQuery("SELECT SUM(t.amount) "
				+ "FROM DbTransaction t "
				+ "WHERE t.serverPayee=:serverUrl AND t.usernamePayer=:payerUsername")
				.setParameter("serverUrl", url)
				.setParameter("payerUsername", username)
				.getSingleResult().toString());
		
		return sum;
	}

	public BigDecimal transactionSumByServerAsPayee(String url, String username) {
		
		BigDecimal sum = new BigDecimal(em.createQuery("SELECT SUM(t.amount) "
				+ "FROM DbTransaction t "
				+ "WHERE t.serverPayer=:serverUrl AND t.usernamePayee=:payeeUsername")
				.setParameter("serverUrl", url)
				.setParameter("payeeUsername", username)
				.getSingleResult().toString());

		return sum;
	}

	/**
	 * Returns the sum of all transaction made as payer by the given parameter server url.
	 * 
	 * @return sumOfBalances
	 */
	public BigDecimal getCurrentBalanceTransactionAsPayer(String serverUrl, String username) {
		BigDecimal amount;
		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE,0);
		today.set(Calendar.SECOND, 0);
		try{			
			amount = new BigDecimal(em.createQuery("SELECT SUM(db.amount) "
					+ "FROM DbTransaction db "
					+ "WHERE db.usernamePayer=:username AND db.serverPayee=:serverUrl AND db.timestamp >=:time")
					.setParameter("username", username)
					.setParameter("serverUrl", serverUrl)
					.setParameter("time", today.getTime())
					.getSingleResult().toString());
		} catch(Exception e) {
			String s = e.getMessage();
			amount = new BigDecimal("0.00000000");
		}
		return amount;
	}

	/**
	 * Returns the sum of all transaction made as payee by the given parameter server url.
	 * 
	 * @return sumOfBalances
	 */
	public BigDecimal getCurrentBalanceTransactionAsPayee(String serverUrl, String username) {
		BigDecimal amount;
		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE,0);
		today.set(Calendar.SECOND, 0);
		try{			
			amount= new BigDecimal(em.createQuery("SELECT SUM(db.amount) "
					+ "FROM DbTransaction db "
					+ "WHERE db.usernamePayee=:username AND db.serverPayer=:serverUrl AND db.timestamp >=:time")
					.setParameter("username", username)
					.setParameter("serverUrl", serverUrl)
					.setParameter("time", today.getTime())
					.getSingleResult().toString());
		} catch(Exception e) {
			String s = e.getMessage();
			amount = new BigDecimal("0.00000000");			
		}
		return amount;
	}
}
