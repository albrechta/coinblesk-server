package ch.uzh.csg.coinblesk.server.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class SpentOutputs {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	private byte[] txOutPoint;
	@Temporal(TemporalType.TIMESTAMP)
	public Date timestamp;

	public byte[] getTxOutPoint() {
		return txOutPoint;
	}

	@PrePersist
	protected void onCreate() {
		timestamp = new Date();
	}

	public void setTxOutPoint(byte[] txOutPoint) {
		this.txOutPoint = txOutPoint;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}