package ch.uzh.csg.coinblesk.server.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.azazar.bitcoin.jsonrpcclient.IBitcoinRPC.Transaction;

import ch.uzh.csg.coinblesk.server.clientinterface.IServerTransaction;
import ch.uzh.csg.coinblesk.server.dao.ServerTransactionDAO;
import ch.uzh.csg.coinblesk.server.domain.ServerTransaction;
import ch.uzh.csg.coinblesk.server.util.exceptions.ServerAccountNotFoundException;
import ch.uzh.csg.coinblesk.server.web.model.HistoryServerAccountTransaction;

/**
 * Service class for {@link ServerTransaction}s.
 * 
 */
@Service
public class ServerTransactionService implements IServerTransaction{
	
	//TODO: mehmet move to a config file
	public static final String BALANCE = "Not sufficient funds. Payment rejected.";
	public static final String NEGATIVE_AMOUNT = "The transaction amount can't be negative or equals 0.";
	public static final String INTERNAL_ERROR = "An internal error occured. Please try again later.";
	public static final String PAYMENT_REFUSE = "The server refused the payment.";

	@Autowired
	private ServerTransactionDAO serverTransactionDAO;
	
	@Override
	@Transactional
	public void createServerTransaction(Transaction transaction, boolean recieved){
		// TODO mehmet what should be passed (ServerTransaction)
		
	}

	@Override
	@Transactional(readOnly = true)
	public List<HistoryServerAccountTransaction> getLast5Transactions() {
		return serverTransactionDAO.getLast5Transactions();
	}

	@Override
	@Transactional(readOnly = true)
	public List<HistoryServerAccountTransaction> getLast5ServerAccountTransaction(String url) throws ServerAccountNotFoundException {
		return serverTransactionDAO.getLast5ServerAccountTransaction(url);
	}

	@Override
	@Transactional(readOnly = true)
	public List<HistoryServerAccountTransaction> getHistory(int page) {
		return serverTransactionDAO.getHistory(page);
	}

	@Override
	@Transactional(readOnly = true)
	public List<HistoryServerAccountTransaction> getPayeeHistory(int page) {
		return serverTransactionDAO.getPayeeHistory(page);
	}

	@Override
	@Transactional(readOnly = true)
	public List<HistoryServerAccountTransaction> getPayerHistory(int page) {
		return serverTransactionDAO.getPayerHistory(page);
	}

	@Override
	@Transactional(readOnly=true)
	public List<HistoryServerAccountTransaction> getServerAccountTransactions(String url, int page) throws ServerAccountNotFoundException {
		return serverTransactionDAO.getServerAccountTransactions(url, page);
	}
	
	@Override
	@Transactional(readOnly = true)
	public long getHistoryCount() {
		return serverTransactionDAO.getHistoryCount();
	}
	
	@Override
	@Transactional(readOnly = true)
	public long getServerAccountHistoryCount(String url){
		return serverTransactionDAO.getServerAccountHistoryCount(url);
	}
}
