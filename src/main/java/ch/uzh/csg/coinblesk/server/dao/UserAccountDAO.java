/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.uzh.csg.coinblesk.server.dao;

import ch.uzh.csg.coinblesk.server.entity.UserAccount;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Thomas Bocek
 */
@Repository
public class UserAccountDAO {

    @PersistenceContext()
    private EntityManager em;

    public <T> UserAccount getByAttribute(final String attribute, final T value) {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<UserAccount> query = cb.createQuery(UserAccount.class);
        final Root<UserAccount> from = query.from(UserAccount.class);
        final Predicate condition = cb.equal(from.get(attribute), value);
        CriteriaQuery<UserAccount> select = query.select(from).where(condition);
        return getSingleResultOrNull(em.createQuery(select));
    }
    
    public <T> T save(final T enity) {
        return em.merge(enity);
    }
    
    public int remove(final String email) {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaUpdate<UserAccount> update = cb.createCriteriaUpdate(UserAccount.class);
        final Root from = update.from(UserAccount.class);
        final Predicate condition = cb.equal(from.get("email"), email);
        update.set(from.get("isDeleted"), true).where(condition);
        return em.createQuery(update).executeUpdate();
    }

    /**
     * Returns a single result if there is a single result, or null if there is
     * no result. If there is more than one result, then an the
     * NonUniqueResultException is thrown.
     *
     * @param <T>
     * @param query
     * @return
     */
    public static <T> T getSingleResultOrNull(final TypedQuery<T> query) {
        query.setMaxResults(2);
        final List<T> list = query.getResultList();
        if (list == null || list.isEmpty()) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        }
        throw new NonUniqueResultException();
    }
}
