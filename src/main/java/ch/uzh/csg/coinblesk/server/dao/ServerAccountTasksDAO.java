package ch.uzh.csg.coinblesk.server.dao;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import ch.uzh.csg.coinblesk.server.domain.ServerAccountTasks;
import ch.uzh.csg.coinblesk.server.util.exceptions.ServerAccountNotFoundException;

/**
 * DatabaseAccessObject for {@link ServerAccountTasks}. Handles all DB operations
 * regarding {@link ServerAccountTasks}.
 * 
 */
@Repository
public class ServerAccountTasksDAO {
	private static Logger LOGGER = Logger.getLogger(ServerAccountTasksDAO.class);

	@PersistenceContext
	private EntityManager em;
	
	public void persistAccount(ServerAccountTasks account){
		em.persist(account);
		em.flush();
		LOGGER.info("Server Account saved: serverAccount token: " + account.getToken() + ", url: " + account.getUrl());
	}

	/**
	 * 
	 * @param token
	 * @return server account tasks
	 */
	public ServerAccountTasks getAccountTasksByToken(String token){
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		Predicate condition = cb.equal(root.get("token"), token);
		cq.where(condition);
		
		ServerAccountTasks account = getSingle(cq, em);
		return account;
	}
	
	/**
	 * 
	 * @param cq
	 * @param em
	 * @return
	 */
	public static<K> K getSingle(CriteriaQuery<K> cq, EntityManager em) {
		List<K> list =  em.createQuery(cq).getResultList();
		if(list.size() == 0) {
			return null;
		}
		return list.get(0);
	}

	/**
	 * 
	 * @param code
	 * @param url
	 */
	public void deleteTask(int type, String token) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		Predicate condition = cb.equal(root.get("token"), token);
		Predicate condition2 = cb.equal(root.get("type"), type);
		Predicate condition3 = cb.and(condition, condition2);
		cq.where(condition3);
		
		ServerAccountTasks account = getSingle(cq, em);

		em.remove(account);
	}

	/**
	 * Return Server Account Task
	 * 
	 * @param url
	 * @param type 
	 * @return ServerAccountTask
	 * @throws ServerAccountNotFoundException 
	 */
	public ServerAccountTasks getAccountTasksByUrl(String url, int type) throws ServerAccountNotFoundException {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		
		Predicate condition1 = cb.equal(root.get("url"), url);
		Predicate condition2 = cb.equal(root.get("type"), type);
		Predicate condition3 = cb.equal(root.get("proceed"), false);
		Predicate condition4 = cb.and(condition1, condition2, condition3);
		cq.where(condition4);
		
		try{			
			ServerAccountTasks account = em.createQuery(cq).getSingleResult();
			return account;
		} catch (Exception e){			
			throw new ServerAccountNotFoundException(url);
		}
	}

	/**
	 * Gets all {@link ServerAccountTasks} by given parameter type
	 * @param type 
	 * 
	 * @return List of server account tasks
	 */
	public List<ServerAccountTasks> getAllAccountTasksBySubject(int type) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);

		Predicate condition1 = cb.equal(root.get("type"), type);
		Predicate condition2 = cb.equal(root.get("proceed"), false);
		Predicate condition3 = cb.and(condition1, condition2);
		cq.where(condition3);
		
		cq.orderBy(cb.desc(root.get("timestamp")));
		List<ServerAccountTasks> resultWithAliasedBean = em.createQuery(cq)
				.getResultList();

		return resultWithAliasedBean;
	}

	/**
	 * 
	 * @return List of ServerAccountTasks
	 */
	public List<ServerAccountTasks> getProceedAccounts() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);

		Predicate condition = cb.equal(root.get("proceed"), true);
		cq.where(condition);
		
		List<ServerAccountTasks> resultWithAliasedBean = em.createQuery(cq)
				.getResultList();

		return resultWithAliasedBean;
	}

	public void updatedProceed(ServerAccountTasks task) {
		task.setProceed(true);
		em.merge(task);
	}

	/**
	 * 
	 * @param url
	 * @param date
	 * @return ServerAccountTasks
	 */
	public ServerAccountTasks getAccountTaskByUrlAndDate(String url, Date date) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		Predicate condition1 = cb.equal(root.get("url"), url);
		Predicate condition2 = cb.equal(root.get("creationDate"), date);
		Predicate condition3 = cb.and(condition1, condition2);
		cq.where(condition3);
		
		ServerAccountTasks account = getSingle(cq, em);
		return account;
	}

	/**
	 * Get all not proceeded accounts by given parameter type. 
	 * 
	 * @param type
	 * @return List of ServerAccountTasks
	 */
	public List<ServerAccountTasks> getAccountsByType(int type) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		Predicate condition1 = cb.equal(root.get("type"), type);
		Predicate condition2 = cb.equal(root.get("proceed"), false);
		Predicate condition3 = cb.and(condition1, condition2);
		cq.where(condition3);

		List<ServerAccountTasks> resultWithAliasedBean = em.createQuery(cq)
				.getResultList();
		
		return resultWithAliasedBean;
	}

	
}
