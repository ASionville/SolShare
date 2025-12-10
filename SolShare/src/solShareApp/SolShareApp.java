package solShareApp;

import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import ethInfo.EthBasis;
import ethSC.SolShareHandler;

public class SolShareApp {
	private static Scanner sc = new Scanner(System.in);

		public static void main(String[] args) throws Exception {
			login();
			mainMenu();
		}

		public static void login() throws Exception {
			File keystoreDir = new File(EthBasis.credential);
			File[] keystores = keystoreDir.listFiles();
			if (keystores != null) {
				System.out.println("Please choose the Ethereum account you want to use:");
				for (int i = 0; i < keystores.length; i++) {
					System.out.println(i + " : " + keystores[i].getName());
				}
				System.out.print("Your choice (0...): ");
				int choice = Integer.parseInt(sc.nextLine());
				if (choice >= 0 && choice < keystores.length) {
					EthBasis.credential = keystores[choice].getAbsolutePath();
				} else {
					System.out.println("Abort.");
					System.exit(1);
				}
				System.out.print("Please enter account password: ");
				EthBasis.password = sc.nextLine();
			} else {
				System.err.println("No Ethereum account found on this machine.");
				System.exit(1);
			}
		}

		public static void mainMenu() throws Exception {
			boolean running = true;
			while (running) {
				System.out.println("\n=== SolShare Main Menu ===");
				System.out.println("1. Connect to a SolShare contract");
				System.out.println("2. Quit");
				System.out.print("Your choice (1/2): ");
				int choice = Integer.parseInt(sc.nextLine());
				switch (choice) {
					case 1:
						connectToContract();
						break;
					case 2:
						running = false;
						System.out.println("Goodbye!");
						break;
					default:
						System.out.println("Invalid choice.");
				}
			}
		}

		private static void connectToContract() {
			SolShareFileIO contractIO = new SolShareFileIO();
			java.util.List<String> contractList = contractIO.getAllPools();
			for (int i = 0; i < contractList.size(); i++) {
				System.out.println(i + " : " + contractList.get(i));
			}
			System.out.println(contractList.size() + " : Not in the list");
			System.out.print("Your choice (0...): ");
			int contractChoice = Integer.parseInt(sc.nextLine());
			String contractAddress;
			if (contractChoice == contractList.size()) {
				System.out.print("Please enter SolShare contract address: ");
				contractAddress = sc.nextLine();
				contractIO.addPool(contractAddress);
			} else if (contractChoice >= 0 && contractChoice < contractList.size()) {
				contractAddress = contractList.get(contractChoice);
			} else {
				System.err.println("Invalid choice.");
				return;
			}

			SolShareHandler handler = new SolShareHandler(contractAddress);
			groupMenu(handler);
		}

		private static void groupMenu(SolShareHandler handler) {
			boolean inGroupMenu = true;
			while (inGroupMenu) {
				System.out.println("\n=== Group Menu ===");
				System.out.println("1. Create a new group");
				System.out.println("2. Connect to an existing group");
				System.out.println("3. My groups");
				System.out.println("4. Back to main menu");
				System.out.print("Your choice (1/2/3/4): ");
				int choice = Integer.parseInt(sc.nextLine());
				switch (choice) {
					case 1:
						createGroupFlow(handler);
						break;
					case 2:
						connectToGroupFlow(handler);
						break;
					case 3:
						myGroupsFlow(handler);
						break;
					case 4:
						inGroupMenu = false;
						break;
					default:
						System.out.println("Invalid choice.");
				}
			}
		}

		private static void myGroupsFlow(SolShareHandler handler) {
			String myAddress = handler.getMyAddress();
			List<BigInteger> myGroups = UserGroupsManager.getUserGroups(myAddress, EthBasis.password);
			if (myGroups.isEmpty()) {
				System.out.println("No saved groups found.");
				return;
			}
			System.out.println("Select a group:");
			for (int i = 0; i < myGroups.size(); i++) {
				BigInteger gid = myGroups.get(i);
				String gName = handler.getGroupName(gid);
				System.out.println((i + 1) + ". " + gName + " (ID: " + gid + ")");
			}
			System.out.print("Your choice (0 to cancel): ");
			try {
				int choice = Integer.parseInt(sc.nextLine());
				if (choice > 0 && choice <= myGroups.size()) {
					manageGroup(handler, myGroups.get(choice - 1));
				}
			} catch (Exception e) {
				System.out.println("Invalid input.");
			}
		}

		private static void createGroupFlow(SolShareHandler handler) {
			System.out.print("Enter group name: ");
			String groupName = sc.nextLine();
			System.out.print("Enter group description: ");
			String groupDesc = sc.nextLine();
			System.out.print("Enter group currency (e.g. EUR, TWD): ");
			String currency = sc.nextLine().toUpperCase();
			System.out.print("Enter your name: ");
			String creatorName = sc.nextLine();
			java.math.BigInteger groupId = handler.createGroup(groupName, groupDesc, currency, creatorName);
			if (groupId.compareTo(java.math.BigInteger.ZERO) > 0) {
				System.out.println("Group created with unique ID: " + groupId.toString());
				UserGroupsManager.saveUserGroup(handler.getMyAddress(), groupId, EthBasis.password);
				// Optionally, you can store groupId locally if needed
				manageGroup(handler, groupId);
			} else {
				System.out.println("Failed to create group.");
			}
		}

		private static void connectToGroupFlow(SolShareHandler handler) {
			System.out.print("Enter group ID to connect: ");
			String groupIdStr = sc.nextLine();
			try {
				java.math.BigInteger groupId = new java.math.BigInteger(groupIdStr);
				
				List<String> members = handler.getGroupMembers(groupId);
				if (members == null) {
					System.out.println("Group not found.");
					return;
				}

				String myAddress = handler.getMyAddress();
				if (members.contains(myAddress)) {
					manageGroup(handler, groupId);
				} else {
					String gName = handler.getGroupName(groupId);
					String name;
					while (true) {
						System.out.print("Enter your name to join: ");
						name = sc.nextLine();
						if (handler.isNameTaken(groupId, name)) {
							System.out.println("Name already taken. Please choose another.");
						} else {
							break;
						}
					}
					
					if (handler.joinGroup(groupId, name)) {
						System.out.println("Joined group successfully.");
						UserGroupsManager.saveUserGroup(myAddress, groupId, EthBasis.password);
						manageGroup(handler, groupId);
					} else {
						System.out.println("Failed to join group.");
					}
				}
			} catch (Exception e) {
				System.out.println("Invalid group ID.");
			}
		}

		private static void manageGroup(SolShareHandler handler, java.math.BigInteger groupId) {
			String groupName = handler.getGroupName(groupId);
			String myAddress = handler.getMyAddress();

			boolean managing = true;
			while (managing) {
				List<String> admins = handler.getGroupAdmins(groupId);
				boolean isAdmin = admins != null && admins.contains(myAddress);

				System.out.println("\n=== Group: " + groupName + " ===");
				if (isAdmin) {
					System.out.println("1. Add expense");
					System.out.println("2. View expenses");
					System.out.println("3. View members balances");
					System.out.println("4. Manage members");
					System.out.println("5. Efficient settlement plan");
					System.out.println("6. Get group code");
					System.out.println("7. Leave group");
					System.out.println("8. Back to group menu");
				} else {
					System.out.println("1. Add expense");
					System.out.println("2. View expenses");
					System.out.println("3. View members balances");
					System.out.println("4. Efficient settlement plan");
					System.out.println("5. Get group code");
					System.out.println("6. Leave group");
					System.out.println("7. Back to group menu");
				}
				System.out.print("Your choice: ");
				int op = -1;
				try {
					op = Integer.parseInt(sc.nextLine());
				} catch (NumberFormatException e) {
					System.out.println("Invalid input.");
					continue;
				}

				if (isAdmin) {
					switch (op) {
						case 1: addExpenseFlow(handler, groupId); break;
						case 2: viewExpensesFlow(handler, groupId); break;
						case 3: viewBalancesFlow(handler, groupId); break;
						case 4: manageMembersMenu(handler, groupId); break;
						case 5: showEfficientSettlement(handler, groupId); break;
						case 6: System.out.println("Group Code (ID): " + groupId); break;
						case 7: 
							if (leaveGroupFlow(handler, groupId)) managing = false; 
							break;
						case 8: managing = false; break;
						default: System.out.println("Invalid choice.");
					}
				} else {
					switch (op) {
						case 1: addExpenseFlow(handler, groupId); break;
						case 2: viewExpensesFlow(handler, groupId); break;
						case 3: viewBalancesFlow(handler, groupId); break;
						case 4: showEfficientSettlement(handler, groupId); break;
						case 5: System.out.println("Group Code (ID): " + groupId); break;
						case 6: 
							if (leaveGroupFlow(handler, groupId)) managing = false; 
							break;
						case 7: managing = false; break;
						default: System.out.println("Invalid choice.");
					}
				}
			}
		}

		private static void manageMembersMenu(SolShareHandler handler, BigInteger groupId) {
			boolean inMenu = true;
			while (inMenu) {
				System.out.println("\n=== Manage Members ===");
				System.out.println("1. Promote admin");
				System.out.println("2. Demote admin");
				System.out.println("3. Remove user");
				System.out.println("4. Back");
				System.out.print("Your choice: ");
				int op = -1;
				try {
					op = Integer.parseInt(sc.nextLine());
				} catch (NumberFormatException e) {
					System.out.println("Invalid input.");
					continue;
				}
				switch (op) {
					case 1: promoteAdminFlow(handler, groupId); break;
					case 2: demoteAdminFlow(handler, groupId); break;
					case 3: removeUserFlow(handler, groupId); break;
					case 4: inMenu = false; break;
					default: System.out.println("Invalid choice.");
				}
			}
		}

		private static void addExpenseFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.print("Amount: ");
			java.math.BigDecimal originalAmountBD = new java.math.BigDecimal(sc.nextLine());
			System.out.print("Description: ");
			String desc = sc.nextLine();
			System.out.print("Currency (e.g. TWD): ");
			String originalCurrency = sc.nextLine().toUpperCase();

			String groupCurrency = handler.getGroupCurrency(groupId);
			java.math.BigDecimal normalizedAmountBD = CurrencyConverter.convert(originalAmountBD, originalCurrency, groupCurrency);

			if (normalizedAmountBD == null) {
				System.out.println("Could not convert currency from " + originalCurrency + " to " + groupCurrency + ". Aborting.");
				return;
			}

			// Scale by 100 to keep 2 decimals precision in integer storage
			java.math.BigInteger amount = normalizedAmountBD.multiply(new java.math.BigDecimal(100)).toBigInteger();
			java.math.BigInteger originalAmount = originalAmountBD.multiply(new java.math.BigDecimal(100)).toBigInteger();

			System.out.print("Participants (comma separated names): ");
			String[] parts = sc.nextLine().split(",");
			java.util.List<String> participants = new java.util.ArrayList<>();
			boolean allFound = true;
			for (String pName : parts) {
				String addr = handler.getMemberAddress(groupId, pName.trim());
				if (addr == null || addr.equals("0x0000000000000000000000000000000000000000") || addr.equals("0x0") || addr.equals("0")) {
					System.out.println("User " + pName.trim() + " not found.");
					allFound = false;
					break;
				}
				participants.add(addr);
			}
			if (!allFound) return;

			if (handler.addExpense(groupId, amount, desc, participants, originalAmount, originalCurrency, false))
				System.out.println("Expense added.");
			else
				System.out.println("Failed to add expense.");
		}

		private static void viewExpensesFlow(SolShareHandler handler, BigInteger groupId) {
			java.util.List<ethSC.SolShare.Expense> expenses = handler.getExpenses(groupId);
			if (expenses != null) {
				for (ethSC.SolShare.Expense e : expenses) {
					String payerName = handler.getMemberName(groupId, e.payer);
					java.math.BigDecimal displayAmount = new java.math.BigDecimal(e.amount).divide(new java.math.BigDecimal(100));
					System.out.println("Expense #" + e.id + ": " + e.description + ", amount: " + displayAmount + ", payer: " + payerName);
				}
			} else {
				System.out.println("Could not retrieve expenses.");
			}
		}

		private static void viewBalancesFlow(SolShareHandler handler, BigInteger groupId) {
			java.util.List<String> members = handler.getGroupMembers(groupId);
			if (members != null) {
				for (String member : members) {
					java.math.BigInteger bal = handler.getNetBalance(groupId, member);
					java.math.BigDecimal displayBal = new java.math.BigDecimal(bal).divide(new java.math.BigDecimal(100));
					String memberName = handler.getMemberName(groupId, member);
					System.out.println("Member " + memberName + " balance: " + displayBal);
				}
			} else {
				System.out.println("Could not retrieve group members.");
			}
		}

		private static boolean leaveGroupFlow(SolShareHandler handler, BigInteger groupId) {
			if (handler.leaveGroup(groupId)) {
				System.out.println("Left group.");
				return true;
			} else {
				System.out.println("Failed to leave group.");
				return false;
			}
		}

		private static void promoteAdminFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.print("Enter member name to promote: ");
			String promoteName = sc.nextLine();
			String promoteAddr = handler.getMemberAddress(groupId, promoteName);
			if (promoteAddr == null || promoteAddr.equals("0x0000000000000000000000000000000000000000") || promoteAddr.equals("0x0") || promoteAddr.equals("0")) {
				System.out.println("User " + promoteName + " not found.");
				return;
			}
			if (handler.promoteAdmin(groupId, promoteAddr))
				System.out.println("Promoted.");
			else
				System.out.println("Failed to promote.");
		}

		private static void demoteAdminFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.print("Enter member name to demote: ");
			String demoteName = sc.nextLine();
			String demoteAddr = handler.getMemberAddress(groupId, demoteName);
			if (demoteAddr == null || demoteAddr.equals("0x0000000000000000000000000000000000000000") || demoteAddr.equals("0x0") || demoteAddr.equals("0")) {
				System.out.println("User " + demoteName + " not found.");
				return;
			}
			if (handler.demoteAdmin(groupId, demoteAddr))
				System.out.println("Demoted.");
			else
				System.out.println("Failed to demote.");
		}

		private static void removeUserFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.println("Remove user functionality is not implemented in the backend.");
		}

		private static void showEfficientSettlement(SolShareHandler handler, BigInteger groupId) {
			List<String> members = handler.getGroupMembers(groupId);
			if (members == null) return;
			
			List<String> debtors = new ArrayList<>();
			List<BigInteger> debtAmounts = new ArrayList<>();
			List<String> creditors = new ArrayList<>();
			List<BigInteger> creditAmounts = new ArrayList<>();
			
			for (String m : members) {
				BigInteger bal = handler.getNetBalance(groupId, m);
				if (bal.compareTo(BigInteger.ZERO) < 0) {
					debtors.add(m);
					debtAmounts.add(bal.abs());
				} else if (bal.compareTo(BigInteger.ZERO) > 0) {
					creditors.add(m);
					creditAmounts.add(bal);
				}
			}
			
			System.out.println("--- Efficient Settlement Plan ---");
			int dIndex = 0;
			int cIndex = 0;
			
			while (dIndex < debtors.size() && cIndex < creditors.size()) {
				String debtor = debtors.get(dIndex);
				BigInteger debt = debtAmounts.get(dIndex);
				String creditor = creditors.get(cIndex);
				BigInteger credit = creditAmounts.get(cIndex);
				
				BigInteger amount = debt.min(credit);
				String debtorName = handler.getMemberName(groupId, debtor);
				String creditorName = handler.getMemberName(groupId, creditor);
				
				java.math.BigDecimal displayAmount = new java.math.BigDecimal(amount).divide(new java.math.BigDecimal(100));
				System.out.println(debtorName + " pays " + creditorName + ": " + displayAmount);
				
				debt = debt.subtract(amount);
				credit = credit.subtract(amount);
				
				if (debt.equals(BigInteger.ZERO)) {
					dIndex++;
				} else {
					debtAmounts.set(dIndex, debt);
				}
				
				if (credit.equals(BigInteger.ZERO)) {
					cIndex++;
				} else {
					creditAmounts.set(cIndex, credit);
				}
			}
		}
}
