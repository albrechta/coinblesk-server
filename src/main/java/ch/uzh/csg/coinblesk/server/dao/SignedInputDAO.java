package ch.uzh.csg.coinblesk.server.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import ch.uzh.csg.coinblesk.server.entity.SignedInput;

/**
 * DatabaseAccessObject for storing bitcoin transaction inputs of time-locked
 * transactions that have been signed by the server.
 * 
 */
@Repository
final public class SignedInputDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignedInputDAO.class);

    @PersistenceContext()
    private EntityManager em;

    /**
     * Get the lock time (block height) of a previously signed refund
     * transaction input. A transaction input is identified by the hash of the
     * referencing transaction input, and the index of the output in the
     * transaction. For more information, see
     * https://bitcoin.org/en/glossary/outpoint
     * 
     * @param txHash
     *            the hash of the referencing transaction
     * @param outputIndex
     *            the index of the output
     * @return Long.MAX_VALUE if the input wasn't found, or the lock time if
     *         this input has been signed earlier.
     */
    public long getLockTime(final byte[] txHash, final long outputIndex) {
    	final CriteriaBuilder cb = em.getCriteriaBuilder();
    	final CriteriaQuery<SignedInput> qb = cb.createQuery(SignedInput.class);
    	final Root<SignedInput> root = qb.from(SignedInput.class);

    	final Predicate condition1 = cb.equal(root.get("txHash"), txHash);
    	final Predicate condition2 = cb.equal(root.get("outputIndex"), outputIndex);
    	final Predicate finalCondition = cb.and(condition1, condition2);

        qb.where(finalCondition);

        final SignedInput signedInput = getSingle(qb, em);
        LOGGER.debug("lock time for {},{} is {}", txHash, outputIndex, signedInput);
        return signedInput == null ? Long.MAX_VALUE : signedInput.getLockTime();
    }
    
    /**
     * Persists a time-locked input that has been signed by the server.
     * @param txHash
     * @param outputIndex
     * @param lockTime
     */
    public void addSignedInput(final byte[] txHash, final long outputIndex, final long lockTime) {
        
        // check if signed input already exists
    	final SignedInput savedInput = getSignedInput(txHash, outputIndex);
        
        if(savedInput != null) {
            if(savedInput.getLockTime() <= lockTime) {
                // input was already signed and saved with a lower lockTime -> ignore
                
            } else {
                // update the lock time
                savedInput.setLockTime(lockTime);
                em.refresh(savedInput);
            }
        } else {
        	LOGGER.debug("never seen this output before -> save it {},{}, locktime {}", txHash, outputIndex, lockTime);
        	final SignedInput signedInput = new SignedInput(lockTime, txHash, outputIndex);
            em.persist(signedInput);
        }
        em.flush();
    }
    
    void removeSignedInput(byte[] txHash, int outputIndex) {
        SignedInput savedInput = getSignedInput(txHash, outputIndex);
        em.remove(savedInput);
        em.flush();
    }
    
    private SignedInput getSignedInput(byte[] txHash, long outputIndex) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SignedInput> qb = cb.createQuery(SignedInput.class);
        Root<SignedInput> root = qb.from(SignedInput.class);

        Predicate condition1 = cb.equal(root.get("txHash"), txHash);
        Predicate condition2 = cb.equal(root.get("outputIndex"), outputIndex);
        Predicate finalCondition = cb.and(condition1, condition2);


        qb.where(finalCondition);

        return getSingle(qb, em);
    }

    private <K> K getSingle(CriteriaQuery<K> cq, EntityManager em) {
        List<K> list = em.createQuery(cq).getResultList();
        if (list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

}
