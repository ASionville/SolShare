package ethSC;

import java.math.BigInteger;
import java.util.List;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.ipc.UnixIpcService;
import org.web3j.protocol.ipc.WindowsIpcService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import ethInfo.EthBasis;
import ethSC.SolShare.Expense;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;

public class SolShareHandler {
	private static String address = "";
	private static Web3j web3j;
	private static Credentials cr;
	private static ContractGasProvider cgp;
	private static SolShare solshare;
	private static TransactionManager trm;

	public SolShareHandler(String contractAddress) {
		// Charge un contrat existant
		web3j = Web3j.build(new WindowsIpcService(EthBasis.pipeLine));
		try {
			cr = WalletUtils.loadCredentials(EthBasis.password, EthBasis.credential);
			Admin admin = Admin.build(new UnixIpcService(EthBasis.pipeLine));
			admin.personalUnlockAccount(cr.getAddress(), EthBasis.password, BigInteger.ZERO);
		} catch (Exception e) {
			System.err.println("Bad Wallet, Check Password or Credential File");
			e.printStackTrace();
		}
		cgp = new DefaultGasProvider();
		trm = new RawTransactionManager(web3j, cr, EthBasis.chainID);
		try {
			solshare = SolShare.load(contractAddress, web3j, trm, cgp);
			address = contractAddress;
		} catch (Exception e) {
			System.err.println("Cannot load smart contract.");
			e.printStackTrace();
		}
	}

	public String getContractAddress() {
		return address;
	}

	public String getMyAddress() {
		return cr.getAddress();
	}

	// GROUP MANAGEMENT
	public BigInteger createGroup(String name, String description, String currency, String creatorName) {
		try {
			org.web3j.protocol.core.methods.response.TransactionReceipt receipt = solshare.createGroup(name, description, currency, creatorName).send();
			List<SolShare.GroupCreatedEventResponse> events = solshare.getGroupCreatedEvents(receipt);
			if (!events.isEmpty()) {
				// Use generated hash as group ID
				return events.get(0).groupId;
			}
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "createGroup"));
		}
		return BigInteger.valueOf(-1);
	}

	public boolean joinGroup(BigInteger groupId, String memberName) {
		try {
			solshare.joinGroup(groupId, memberName).send();
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "joinGroup"));
			return false;
		}
	}

	public boolean leaveGroup(BigInteger groupId) {
		try {
			solshare.leaveGroup(groupId).send();
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "leaveGroup"));
			return false;
		}
	}

	public boolean promoteAdmin(BigInteger groupId, String memberAddress) {
		try {
			solshare.promoteAdmin(groupId, memberAddress).send();
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "promoteAdmin"));
			return false;
		}
	}

	public boolean demoteAdmin(BigInteger groupId, String memberAddress) {
		try {
			solshare.demoteAdmin(groupId, memberAddress).send();
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "demoteAdmin"));
			return false;
		}
	}

	// EXPENSE MANAGEMENT
	public boolean addExpense(BigInteger groupId, BigInteger amount, String description, List<String> participants, List<BigInteger> participantAmounts, BigInteger originalAmount, String originalCurrency, boolean isSettlement) {
		try {
			SolShare.ExpenseInput input = new SolShare.ExpenseInput(amount, description, participants, participantAmounts, originalAmount, originalCurrency, isSettlement);
			solshare.addExpense(groupId, input).send();
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "addExpense"));
			return false;
		}
	}

	public boolean editExpense(BigInteger groupId, BigInteger expenseId, BigInteger amount, String description, List<String> participants, List<BigInteger> participantAmounts, BigInteger originalAmount, String originalCurrency, boolean isSettlement) {
		try {
			List<String> addresses = participants;
			solshare.editExpense(groupId, expenseId, amount, description, addresses, participantAmounts, originalAmount, originalCurrency, isSettlement).send();
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "editExpense"));
			return false;
		}
	}

	public boolean deleteExpense(BigInteger groupId, BigInteger expenseId) {
		try {
			solshare.deleteExpense(groupId, expenseId).send();
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "deleteExpense"));
			return false;
		}
	}

	// BALANCE COMPUTATION
	public BigInteger getNetBalance(BigInteger groupId, String memberAddress) {
		try {
			return solshare.getNetBalance(groupId, memberAddress).send();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "getNetBalance"));
			return BigInteger.ZERO;
		}
	}

	public boolean isNameTaken(BigInteger groupId, String memberName) {
		try {
			return solshare.isNameTaken(groupId, memberName).send();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "isNameTaken"));
			return false;
		}
	}

	public String getMemberAddress(BigInteger groupId, String memberName) {
		try {
			return solshare.getMemberAddress(groupId, memberName).send();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "getMemberAddress"));
			return "";
		}
	}

	// GETTERS
	public String getGroupName(BigInteger groupId) {
		try {
			Tuple5<BigInteger, String, String, String, Boolean> result = solshare.groups(groupId).send();
			return result.component2();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "getGroupName"));
			return groupId.toString();
		}
	}

	public String getGroupCurrency(BigInteger groupId) {
		try {
			Tuple5<BigInteger, String, String, String, Boolean> result = solshare.groups(groupId).send();
			return result.component4();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "getGroupCurrency"));
			return "EUR";
		}
	}

	public List<String> getGroupMembers(BigInteger groupId) {
		try {
			return solshare.getGroupMembers(groupId).send();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "getGroupMembers"));
			return null;
		}
	}

	public String getMemberName(BigInteger groupId, String memberAddress) {
		try {
			return solshare.getMemberName(groupId, memberAddress).send();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "getMemberName"));
			return "Unknown";
		}
	}

	public List<String> getGroupAdmins(BigInteger groupId) {
		try {
			return solshare.getGroupAdmins(groupId).send();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "getGroupAdmins"));
			return null;
		}
	}

	public List<Expense> getExpenses(BigInteger groupId) {
		try {
			return solshare.getExpenses(groupId).send();
		} catch (Exception e) {
			System.err.println("Error: " + getContractErrorMessage(e, "getExpenses"));
			return null;
		}
	}

	// Helper to extract contract error message
	private String getContractErrorMessage(Exception e, String command) {
		String msg = e.getMessage();
		if (msg == null) msg = "Unknown error";
		// Try to extract revert reason from message
		String reason = "";
		int idx = msg.indexOf("revert ");
		if (idx != -1) {
			int end = msg.indexOf("\n", idx);
			if (end == -1) end = msg.length();
			reason = msg.substring(idx + 7, end).trim();
		}
		if (!reason.isEmpty()) {
			return reason + " (command: " + command + ")";
		} else {
			return msg + " (command: " + command + ")";
		}
	}

	// EVENTS
	public List<SolShare.GroupCreatedEventResponse> getGroupCreatedEvents(org.web3j.protocol.core.methods.response.TransactionReceipt receipt) {
		return solshare.getGroupCreatedEvents(receipt);
	}
}
