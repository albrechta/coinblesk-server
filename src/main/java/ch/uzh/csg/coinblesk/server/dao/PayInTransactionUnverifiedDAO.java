package ch.uzh.csg.coinblesk.server.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ch.uzh.csg.coinblesk.server.domain.PayInTransaction;
import ch.uzh.csg.coinblesk.server.domain.PayInTransactionUnverified;
import ch.uzh.csg.coinblesk.server.domain.UserAccount;

@Repository
public class PayInTransactionUnverifiedDAO {
	
	private static Logger LOGGER = Logger.getLogger(PayInTransactionUnverifiedDAO.class);
	
	@PersistenceContext
	private EntityManager em;
	
	@Autowired
	private UserAccountDAO userAccountDAO;

	public void remove(PayInTransaction pit) {
		String hql = "DELETE FROM PayInTransactionUnverified WHERE userID = :userID and transactionID = :transactionID";
		em.createQuery(hql)
			.setParameter("userID", pit.getUserID())
			.setParameter("transactionID", pit.getTransactionID())
			.executeUpdate();
	    LOGGER.debug("removed unverified transaction, as it is now veriefed for user "+pit.getUserID()+" / "+pit.getTransactionID());
    }

	public boolean isNew(PayInTransactionUnverified pit) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		
		Root<PayInTransactionUnverified> root = cq.from(PayInTransactionUnverified.class);
		cq.select(cb.count(root));
		
		Predicate condition1 = cb.equal(root.get("userID"), pit.getUserID());
		Predicate condition2 = cb.equal(root.get("transactionID"), pit.getTransactionID());
		Predicate condition3 = cb.and(condition1, condition2);
		cq.where(condition3);
		
		Long count = em.createQuery(cq).getSingleResult();
		
		return count == 0;
    }

	public void createPayInTransaction(PayInTransactionUnverified tx) {
		em.persist(tx);
    }

//	public List<HistoryPayInTransactionUnverified> getHistory(String username, int page) throws UserAccountNotFoundException {
//		if (page < 0) {
//			return null;
//		}
//		
//		UserAccount userAccount = userAccountDAO.getByUsername(username);
//		
//		CriteriaBuilder cb = em.getCriteriaBuilder();
//		CriteriaQuery<HistoryPayInTransactionUnverified> cq = cb.createQuery(HistoryPayInTransactionUnverified.class);
//		Root<PayInTransactionUnverified> root = cq.from(PayInTransactionUnverified.class);
//		cq.select(cb.construct(HistoryPayInTransactionUnverified.class, root.get("timestamp"),root.get("amount")));
//		
//		Predicate condition = cb.equal(root.get("userID"), userAccount.getId());
//		cq.where(condition);
//		cq.orderBy(cb.desc(root.get("timestamp")));
//		List<HistoryPayInTransactionUnverified> resultWithAliasedBean = em.createQuery(cq)
//				.setFirstResult(page* Config.PAY_INS_MAX_RESULTS)
//				.setMaxResults(Config.PAY_INS_MAX_RESULTS)
//				.getResultList();
//		
//		return resultWithAliasedBean;
//    }

	public long getHistoryCount(UserAccount userAccount) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<PayInTransactionUnverified> root = cq.from(PayInTransactionUnverified.class);
		cq.select(cb.count(root));
		
		Predicate condition = cb.equal(root.get("userID"), userAccount.getId());
		cq.where(condition);
		return em.createQuery(cq).getSingleResult();
    }

//	public List<HistoryPayInTransactionUnverified> getLast5Transactions(UserAccount userAccount) {
//		CriteriaBuilder cb = em.getCriteriaBuilder();
//		CriteriaQuery<HistoryPayInTransactionUnverified> cq = cb.createQuery(HistoryPayInTransactionUnverified.class);
//		Root<PayInTransactionUnverified> root = cq.from(PayInTransactionUnverified.class);
//		cq.select(cb.construct(HistoryPayInTransactionUnverified.class, root.get("timestamp"),root.get("amount")));
//		
//		Predicate condition = cb.equal(root.get("userID"), userAccount.getId());
//		cq.where(condition);
//		cq.orderBy(cb.desc(root.get("timestamp")));
//		List<HistoryPayInTransactionUnverified> resultWithAliasedBean = em.createQuery(cq)
//				.setMaxResults(5)
//				.getResultList();
//		
//		return resultWithAliasedBean;
//    }

}
