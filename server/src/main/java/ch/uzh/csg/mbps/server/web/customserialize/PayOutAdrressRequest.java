package ch.uzh.csg.mbps.server.web.customserialize;

import java.nio.charset.Charset;

import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.SignedSerializableObject;
import ch.uzh.csg.mbps.customserialization.exceptions.IllegalArgumentException;
import ch.uzh.csg.mbps.customserialization.exceptions.NotSignedException;
import ch.uzh.csg.mbps.customserialization.exceptions.SerializationException;

/**
 * This class represents a payout address request, which is transferred between two servers. 
 * The byte array serialization allows keeping the payload and
 * signature as as small as possible.
 * 
 * 
 */
public class PayOutAdrressRequest extends SignedSerializableObject {

	private String payOutAddressRequest;
	
	protected PayOutAdrressRequest(){}
	
	public String getPayOutAddressRequest() {
		return payOutAddressRequest;
	}

	public void setPayOutAddressRequest(String payOutAddressRequest) {
		this.payOutAddressRequest = payOutAddressRequest;
	}

	/**
	 * This constructor instantiates a new object.
	 * 
	 * @param pkiAlgorithm
	 *            the {@link PKIAlgorithm} to be used for
	 *            {@link SignedSerializableObject} super class
	 * @param keyNumber
	 *            the key number to be used for the
	 *            {@link SignedSerializableObject} super class
	 * @param payOutAddress
	 *            the payout address
	 * @throws IllegalArgumentException
	 *             if any argument is null or does not fit into the foreseen
	 *             primitive type
	 */
	public PayOutAdrressRequest(int version, PKIAlgorithm pkiAlgorithm, int keyNumber, String payOutAddress) throws IllegalArgumentException{		
		super(version, pkiAlgorithm,keyNumber);
		payOutAddressRequest = payOutAddress;
		setPayload();
	}
	
	public void setPayload(){
		byte[] payOutAddressRequestRaw = payOutAddressRequest.getBytes(Charset.forName("UTF-8"));
		int length;
		/*
		 * version
		 * + signatureAlgorithm.getCode()
		 * + keyNumber
		 * + payOutAddressRequestRaw.length
		 * + payOutAddressRequestRaw
		 */
		length = 1+1+1+1+payOutAddressRequestRaw.length;
		
		byte[] payload = new byte[length];
		
		int index = 0;
		payload[index++] = (byte) getVersion();
		payload[index++] = getPKIAlgorithm().getCode();
		payload[index++] = (byte) getKeyNumber();
		payload[index++] = (byte) payOutAddressRequestRaw.length;
		for(byte b: payOutAddressRequestRaw) {
			payload[index++] = b;
		}
		
		this.payload = payload;
		
	}
	
	@Override
	public PayOutAdrressRequest decode(byte[] bytes) throws IllegalArgumentException, SerializationException {
		if (bytes == null)
			throw new IllegalArgumentException("The argument can't be null.");
		
		try {
			int index = 0;
			
			int version = bytes[index++] & 0xFF;
			PKIAlgorithm pkiAlgorithm = PKIAlgorithm.getPKIAlgorithm(bytes[index++]);
			int keyNumber = bytes[index++] & 0xFF;
			int payOutAddressRequestLength = bytes[index++] & 0xFF;
			byte[] payOutAddressRequestBytes = new byte[payOutAddressRequestLength];
			for(int i = 0; i < payOutAddressRequestLength; i++){
				payOutAddressRequestBytes[i] = bytes[index++];
			}
			String payOutAddressRequest = new String(payOutAddressRequestBytes);
			
			PayOutAdrressRequest par = new PayOutAdrressRequest(version, pkiAlgorithm, keyNumber, payOutAddressRequest);
			
			int signatureLength = bytes.length - index;
			if (signatureLength == 0) {
				throw new NotSignedException();
			} else {
				byte[] signature = new byte[signatureLength];
				for (int i=0; i<signature.length; i++) {
					signature[i] = bytes[index++];
				}
				par.signature = signature;
			}
			
			return par;
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("The given byte array is corrupt (not long enough).");
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof PayOutAdrressRequest))
			return false;
		
		
		PayOutAdrressRequest other = (PayOutAdrressRequest) o;
		if (getVersion() != other.getVersion())
			return false;
		if (getPKIAlgorithm().getCode() != other.getPKIAlgorithm().getCode())
			return false;
		if (getKeyNumber() != other.getKeyNumber())
			return false;
		if (!this.payOutAddressRequest.equals(other.payOutAddressRequest))
			return false;
		
		return true;
	}

}