package solShareApp;

import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import ethInfo.Config;
import ethSC.SolShareHandler;

public class SolShareApp {
	private static Scanner sc = new Scanner(System.in);

	// ANSI Colors
	public static final String ANSI_RESET;
	public static final String ANSI_RED;
	public static final String ANSI_GREEN;
	public static final String ANSI_YELLOW;
	public static final String ANSI_BLUE;
	public static final String ANSI_CYAN;
	public static final String ANSI_BOLD;

	static {
		if (Config.useANSIColors) {
			ANSI_RESET = "\u001B[0m";
			ANSI_RED = "\u001B[31m";
			ANSI_GREEN = "\u001B[32m";
			ANSI_YELLOW = "\u001B[33m";
			ANSI_BLUE = "\u001B[34m";
			ANSI_CYAN = "\u001B[36m";
			ANSI_BOLD = "\u001B[1m";
		} else {
			ANSI_RESET = "";
			ANSI_RED = "";
			ANSI_GREEN = "";
			ANSI_YELLOW = "";
			ANSI_BLUE = "";
			ANSI_CYAN = "";
			ANSI_BOLD = "";
		}
	}

		public static void main(String[] args) throws Exception {
			login();
			mainMenu();
		}

		public static void login() throws Exception {
			File keystoreDir = new File(Config.credential);
			File[] keystores = keystoreDir.listFiles();
			if (keystores != null) {
				System.out.println("Please choose the Ethereum account you want to use:");
				for (int i = 0; i < keystores.length; i++) {
					System.out.println(i + " : " + keystores[i].getName());
				}
				System.out.print("Your choice (0...): ");
				int choice = Integer.parseInt(sc.nextLine());
				if (choice >= 0 && choice < keystores.length) {
					Config.credential = keystores[choice].getAbsolutePath();
				} else {
					System.out.println("Abort.");
					System.exit(1);
				}
				System.out.print("Please enter account password: ");
				Config.password = sc.nextLine();
			} else {
				System.err.println("No Ethereum account found on this machine.");
				System.exit(1);
			}
		}

		public static void mainMenu() throws Exception {
			boolean running = true;
			while (running) {
				System.out.println("\n" + ANSI_BLUE + ANSI_BOLD + "=== SolShare Main Menu ===" + ANSI_RESET);
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
				System.out.println("\n" + ANSI_BLUE + ANSI_BOLD + "=== Group Menu ===" + ANSI_RESET);
				System.out.println("1. Create a new group");
				System.out.println("2. My groups");
				System.out.println("3. Join an existing group with share code");
				System.out.println("4. Back to main menu");
				System.out.print("Your choice (1/2/3/4): ");
				int choice = Integer.parseInt(sc.nextLine());
				switch (choice) {
					case 1:
						createGroupFlow(handler);
						break;
					case 2:
						myGroupsFlow(handler);
						break;
					case 3:
						connectToGroupFlow(handler);
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
			List<BigInteger> myGroups = UserGroupsManager.getUserGroups(myAddress, Config.password);
			if (myGroups.isEmpty()) {
				System.out.println("No saved groups found.");
				return;
			}
			System.out.println("Select a group:");
			for (int i = 0; i < myGroups.size(); i++) {
				BigInteger gid = myGroups.get(i);
				String gName = handler.getGroupName(gid);
				System.out.println((i + 1) + ". " + gName + " (Share code: " + gid + ")");
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
			System.out.println(ANSI_YELLOW + "Creating group... Confirmation pending..." + ANSI_RESET);
			java.math.BigInteger groupId = handler.createGroup(groupName, groupDesc, currency, creatorName);
			if (groupId.compareTo(java.math.BigInteger.ZERO) > 0) {
				System.out.println(ANSI_GREEN + "Group created with unique share code: " + groupId.toString() + ANSI_RESET);
				UserGroupsManager.saveUserGroup(handler.getMyAddress(), groupId, Config.password);
				// Optionally, you can store groupId locally if needed
				manageGroup(handler, groupId);
			} else {
				System.out.println("Failed to create group.");
			}
		}

		private static void connectToGroupFlow(SolShareHandler handler) {
			System.out.print("Enter group share code to connect: ");
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
					
					System.out.println(ANSI_YELLOW + "Joining group... Confirmation pending..." + ANSI_RESET);
					if (handler.joinGroup(groupId, name)) {
						System.out.println(ANSI_GREEN + "Joined group successfully." + ANSI_RESET);
						UserGroupsManager.saveUserGroup(myAddress, groupId, Config.password);
						manageGroup(handler, groupId);
					} else {
						System.out.println(ANSI_RED + "Failed to join group." + ANSI_RESET);
					}
				}
			} catch (Exception e) {
				System.out.println(ANSI_RED + "Invalid group share code." + ANSI_RESET);
			}
		}

		private static void manageGroup(SolShareHandler handler, java.math.BigInteger groupId) {
			String groupName = handler.getGroupName(groupId);
			String myAddress = handler.getMyAddress();

			boolean managing = true;
			while (managing) {
				List<String> admins = handler.getGroupAdmins(groupId);
				boolean isAdmin = admins != null && admins.contains(myAddress);

				System.out.println("\n" + ANSI_BLUE + ANSI_BOLD + "=== Group: " + groupName + " ===" + ANSI_RESET);
				if (isAdmin) {
					System.out.println("1. Add expense");
					System.out.println("2. View expenses");
					System.out.println("3. View members balances");
					System.out.println("4. Manage members");
					System.out.println("5. Efficient settlement plan");
					System.out.println("6. Get group code");
					System.out.println("7. Leave group");
					System.out.println("8. Manage expenses");
					System.out.println("9. Back to group menu");
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
						case 6: System.out.println("Group share code: " + groupId); break;
						case 7: 
							if (leaveGroupFlow(handler, groupId)) managing = false; 
							break;
						case 8: manageExpensesMenu(handler, groupId); break;
						case 9: managing = false; break;
						default: System.out.println("Invalid choice.");
					}
				} else {
					switch (op) {
						case 1: addExpenseFlow(handler, groupId); break;
						case 2: viewExpensesFlow(handler, groupId); break;
						case 3: viewBalancesFlow(handler, groupId); break;
						case 4: showEfficientSettlement(handler, groupId); break;
						case 5: System.out.println("Group share code: " + groupId); break;
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
				System.out.println("\n" + ANSI_BLUE + ANSI_BOLD + "=== Manage Members ===" + ANSI_RESET);
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
					System.out.println(ANSI_RED + "User " + pName.trim() + " not found." + ANSI_RESET);
					allFound = false;
					break;
				}
				participants.add(addr);
			}
			if (!allFound) return;

			// Split logic
			java.util.List<java.math.BigInteger> participantAmounts = new java.util.ArrayList<>();
			System.out.println("Split type:");
			System.out.println("1. Even split");
			System.out.println("2. Specific amounts");
			System.out.println("3. Percentages");
			System.out.print("Your choice (1/2/3): ");
			int splitChoice = 1;
			try {
				splitChoice = Integer.parseInt(sc.nextLine());
			} catch (Exception e) {}

			if (splitChoice == 2) {
				java.math.BigInteger sum = java.math.BigInteger.ZERO;
				for (int i = 0; i < participants.size(); i++) {
					String pName = handler.getMemberName(groupId, participants.get(i));
					System.out.print("Amount for " + pName + ": ");
					java.math.BigDecimal pAmountBD = new java.math.BigDecimal(sc.nextLine());
					
					java.math.BigDecimal normalizedPAmountBD = CurrencyConverter.convert(pAmountBD, originalCurrency, groupCurrency);
					if (normalizedPAmountBD == null) {
						System.out.println(ANSI_RED + "Conversion failed." + ANSI_RESET);
						return;
					}
					java.math.BigInteger pAmount = normalizedPAmountBD.multiply(new java.math.BigDecimal(100)).toBigInteger();
					participantAmounts.add(pAmount);
					sum = sum.add(pAmount);
				}
				java.math.BigInteger diff = sum.subtract(amount).abs();
				if (diff.compareTo(java.math.BigInteger.valueOf(participants.size() + 5)) <= 0) {
					if (!diff.equals(java.math.BigInteger.ZERO)) {
						System.out.println(ANSI_YELLOW + "Adjusting total amount from " + amount + " to " + sum + " to match sum of parts (rounding)." + ANSI_RESET);
						amount = sum;
					}
				} else {
					System.out.println(ANSI_RED + "Error: Sum of amounts (" + sum + ") does not match total amount (" + amount + ")." + ANSI_RESET);
					return;
				}
			} else if (splitChoice == 3) {
				java.math.BigInteger sum = java.math.BigInteger.ZERO;
				for (int i = 0; i < participants.size(); i++) {
					String pName = handler.getMemberName(groupId, participants.get(i));
					System.out.print("Percentage for " + pName + " (e.g. 33.3): ");
					java.math.BigDecimal percent = new java.math.BigDecimal(sc.nextLine());
					
					java.math.BigInteger pAmount = new java.math.BigDecimal(amount).multiply(percent).divide(new java.math.BigDecimal(100)).toBigInteger();
					participantAmounts.add(pAmount);
					sum = sum.add(pAmount);
				}
				// Adjust rounding error to the first participant
				if (!sum.equals(amount)) {
					java.math.BigInteger diff = amount.subtract(sum);
					participantAmounts.set(0, participantAmounts.get(0).add(diff));
					System.out.println(ANSI_YELLOW + "Rounding difference of " + diff + " applied to " + handler.getMemberName(groupId, participants.get(0)) + ANSI_RESET);
				}
			} else {
				// Even split: pass empty list
			}

			System.out.println(ANSI_YELLOW + "Adding expense... Confirmation pending..." + ANSI_RESET);
			if (handler.addExpense(groupId, amount, desc, participants, participantAmounts, originalAmount, originalCurrency, false))
				System.out.println(ANSI_GREEN + "Expense added." + ANSI_RESET);
			else
				System.out.println(ANSI_RED + "Failed to add expense." + ANSI_RESET);
		}

		private static void manageExpensesMenu(SolShareHandler handler, BigInteger groupId) {
			boolean inMenu = true;
			while (inMenu) {
				System.out.println("\n" + ANSI_BLUE + ANSI_BOLD + "=== Manage Expenses ===" + ANSI_RESET);
				System.out.println("1. Edit expense");
				System.out.println("2. Delete expense");
				System.out.println("3. Back");
				System.out.print("Your choice: ");
				int op = -1;
				try {
					op = Integer.parseInt(sc.nextLine());
				} catch (NumberFormatException e) {
					System.out.println("Invalid input.");
					continue;
				}
				switch (op) {
					case 1: editExpenseFlow(handler, groupId); break;
					case 2: deleteExpenseFlow(handler, groupId); break;
					case 3: inMenu = false; break;
					default: System.out.println("Invalid choice.");
				}
			}
		}

		private static void editExpenseFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.print("Enter expense number to edit: ");
			BigInteger expenseId;
			try {
				expenseId = new BigInteger(sc.nextLine());
			} catch (NumberFormatException e) {
				System.out.println("Invalid number.");
				return;
			}

			List<ethSC.SolShare.Expense> expenses = handler.getExpenses(groupId);
			if (expenses == null || expenseId.compareTo(BigInteger.valueOf(expenses.size())) >= 0) {
				System.out.println("Expense not found.");
				return;
			}
			ethSC.SolShare.Expense e = expenses.get(expenseId.intValue());
			if (!e.exists) {
				System.out.println("Expense has been deleted.");
				return;
			}

			System.out.println("Editing Expense #" + e.id);
			System.out.println("Current Description: " + e.description);
			System.out.print("New Description (Enter to keep): ");
			String newDesc = sc.nextLine();
			if (newDesc.isEmpty()) newDesc = e.description;

			System.out.println("Current Original Amount: " + new java.math.BigDecimal(e.originalAmount).divide(new java.math.BigDecimal(100)));
			System.out.println("Current Original Currency: " + e.originalCurrency);
			System.out.print("New Amount (Enter to keep): ");
			String amountStr = sc.nextLine();
			
			java.math.BigInteger newOriginalAmount;
			String newOriginalCurrency;
			java.math.BigInteger newAmount;

			if (amountStr.isEmpty()) {
				newOriginalAmount = e.originalAmount;
				newOriginalCurrency = e.originalCurrency;
				newAmount = e.amount;
			} else {
				java.math.BigDecimal amountBD = new java.math.BigDecimal(amountStr);
				System.out.print("New Currency (Enter to keep " + e.originalCurrency + "): ");
				String currStr = sc.nextLine().toUpperCase();
				if (currStr.isEmpty()) currStr = e.originalCurrency;
				
				newOriginalCurrency = currStr;
				newOriginalAmount = amountBD.multiply(new java.math.BigDecimal(100)).toBigInteger();
				
				String groupCurrency = handler.getGroupCurrency(groupId);
				java.math.BigDecimal normalizedBD = CurrencyConverter.convert(amountBD, newOriginalCurrency, groupCurrency);
				if (normalizedBD == null) {
					System.out.println("Conversion failed.");
					return;
				}
				newAmount = normalizedBD.multiply(new java.math.BigDecimal(100)).toBigInteger();
			}

			System.out.println("Current Participants: " + e.participants.size() + " members");
			System.out.print("New Participants (comma separated names, Enter to keep): ");
			String partStr = sc.nextLine();
			List<String> newParticipants = new ArrayList<>();
			
			if (partStr.isEmpty()) {
				newParticipants = e.participants;
			} else {
				String[] parts = partStr.split(",");
				boolean allFound = true;
				for (String pName : parts) {
					String addr = handler.getMemberAddress(groupId, pName.trim());
					if (addr == null || addr.equals("0x0000000000000000000000000000000000000000") || addr.equals("0x0") || addr.equals("0")) {
						System.out.println(ANSI_RED + "User " + pName.trim() + " not found." + ANSI_RESET);
						allFound = false;
						break;
					}
					newParticipants.add(addr);
				}
				if (!allFound) return;
			}

			// Split logic for edit
			java.util.List<java.math.BigInteger> newParticipantAmounts = new java.util.ArrayList<>();
			System.out.println("Split type (Enter to keep current logic, or choose new):");
			System.out.println("1. Even split");
			System.out.println("2. Specific amounts");
			System.out.println("3. Percentages");
			System.out.print("Your choice (1/2/3): ");
			String splitChoiceStr = sc.nextLine();
			
			if (splitChoiceStr.isEmpty()) {
				// Default to even split if user hits Enter, as it's the safest default.
			} else {
				int splitChoice = Integer.parseInt(splitChoiceStr);
				if (splitChoice == 2) {
					java.math.BigInteger sum = java.math.BigInteger.ZERO;
					for (int i = 0; i < newParticipants.size(); i++) {
						String pName = handler.getMemberName(groupId, newParticipants.get(i));
						System.out.print("Amount for " + pName + ": ");
						java.math.BigDecimal pAmountBD = new java.math.BigDecimal(sc.nextLine());
						
						java.math.BigDecimal normalizedPAmountBD = CurrencyConverter.convert(pAmountBD, newOriginalCurrency, handler.getGroupCurrency(groupId));
						if (normalizedPAmountBD == null) {
							System.out.println(ANSI_RED + "Conversion failed." + ANSI_RESET);
							return;
						}
						java.math.BigInteger pAmount = normalizedPAmountBD.multiply(new java.math.BigDecimal(100)).toBigInteger();
						newParticipantAmounts.add(pAmount);
						sum = sum.add(pAmount);
					}
					java.math.BigInteger diff = sum.subtract(newAmount).abs();
					if (diff.compareTo(java.math.BigInteger.valueOf(newParticipants.size() + 5)) <= 0) {
						if (!diff.equals(java.math.BigInteger.ZERO)) {
							System.out.println(ANSI_YELLOW + "Adjusting total amount from " + newAmount + " to " + sum + " to match sum of parts (rounding)." + ANSI_RESET);
							newAmount = sum;
						}
					} else {
						System.out.println(ANSI_RED + "Error: Sum of amounts (" + sum + ") does not match total amount (" + newAmount + ")." + ANSI_RESET);
						return;
					}
				} else if (splitChoice == 3) {
					java.math.BigInteger sum = java.math.BigInteger.ZERO;
					for (int i = 0; i < newParticipants.size(); i++) {
						String pName = handler.getMemberName(groupId, newParticipants.get(i));
						System.out.print("Percentage for " + pName + " (e.g. 33.3): ");
						java.math.BigDecimal percent = new java.math.BigDecimal(sc.nextLine());
						
						java.math.BigInteger pAmount = new java.math.BigDecimal(newAmount).multiply(percent).divide(new java.math.BigDecimal(100)).toBigInteger();
						newParticipantAmounts.add(pAmount);
						sum = sum.add(pAmount);
					}
					if (!sum.equals(newAmount)) {
						java.math.BigInteger diff = newAmount.subtract(sum);
						newParticipantAmounts.set(0, newParticipantAmounts.get(0).add(diff));
						System.out.println(ANSI_YELLOW + "Rounding difference of " + diff + " applied to " + handler.getMemberName(groupId, newParticipants.get(0)) + ANSI_RESET);
					}
				}
			}

			System.out.println(ANSI_YELLOW + "Editing expense... Confirmation pending..." + ANSI_RESET);
			if (handler.editExpense(groupId, expenseId, newAmount, newDesc, newParticipants, newParticipantAmounts, newOriginalAmount, newOriginalCurrency, e.isSettlement)) {
				System.out.println(ANSI_GREEN + "Expense edited successfully." + ANSI_RESET);
			} else {
				System.out.println(ANSI_RED + "Failed to edit expense." + ANSI_RESET);
			}
		}

		private static void deleteExpenseFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.print("Enter expense number to delete: ");
			BigInteger expenseId;
			try {
				expenseId = new BigInteger(sc.nextLine());
			} catch (NumberFormatException e) {
				System.out.println("Invalid number.");
				return;
			}
			
			System.out.print("Are you sure? (y/n): ");
			String confirm = sc.nextLine();
			if (confirm.equalsIgnoreCase("y")) {
				System.out.println(ANSI_YELLOW + "Deleting expense... Confirmation pending..." + ANSI_RESET);
				if (handler.deleteExpense(groupId, expenseId)) {
					System.out.println(ANSI_GREEN + "Expense deleted." + ANSI_RESET);
				} else {
					System.out.println(ANSI_RED + "Failed to delete expense." + ANSI_RESET);
				}
			}
		}

		private static void viewExpensesFlow(SolShareHandler handler, BigInteger groupId) {
			java.util.List<ethSC.SolShare.Expense> expenses = handler.getExpenses(groupId);
			if (expenses != null) {
				System.out.println(ANSI_CYAN + "---------------------------------------------------------------------------------" + ANSI_RESET);
				System.out.printf(ANSI_BOLD + "%-5s | %-30s | %-15s | %-20s%n" + ANSI_RESET, "ID", "Description", "Amount", "Payer");
				System.out.println(ANSI_CYAN + "---------------------------------------------------------------------------------" + ANSI_RESET);
				for (ethSC.SolShare.Expense e : expenses) {
					if (!e.exists) continue;
					String payerName = handler.getMemberName(groupId, e.payer);
					java.math.BigDecimal displayAmount = new java.math.BigDecimal(e.amount).divide(new java.math.BigDecimal(100));
					System.out.printf("%-5d | %-30s | %-15s | %-20s%n", e.id, e.description, displayAmount.toString(), payerName);
				}
				System.out.println(ANSI_CYAN + "---------------------------------------------------------------------------------" + ANSI_RESET);
			} else {
				System.out.println(ANSI_RED + "Could not retrieve expenses." + ANSI_RESET);
			}
		}

		private static void viewBalancesFlow(SolShareHandler handler, BigInteger groupId) {
			java.util.List<String> members = handler.getGroupMembers(groupId);
			String groupCurrency = handler.getGroupCurrency(groupId);
			if (members != null) {
				System.out.println(ANSI_CYAN + "---------------------------------------------" + ANSI_RESET);
				System.out.printf(ANSI_BOLD + "%-20s | %-20s%n" + ANSI_RESET, "Member", "Balance (" + groupCurrency + ")");
				System.out.println(ANSI_CYAN + "---------------------------------------------" + ANSI_RESET);
				for (String member : members) {
					java.math.BigInteger bal = handler.getNetBalance(groupId, member);
					java.math.BigDecimal displayBal = new java.math.BigDecimal(bal).divide(new java.math.BigDecimal(100));
					String memberName = handler.getMemberName(groupId, member);
					
					String color = ANSI_RESET;
					if (bal.compareTo(java.math.BigInteger.ZERO) > 0) color = ANSI_GREEN;
					else if (bal.compareTo(java.math.BigInteger.ZERO) < 0) color = ANSI_RED;

					System.out.printf("%-20s | " + color + "%-20s" + ANSI_RESET + "%n", memberName, displayBal.toString());
				}
				System.out.println(ANSI_CYAN + "---------------------------------------------" + ANSI_RESET);
			} else {
				System.out.println(ANSI_RED + "Could not retrieve group members." + ANSI_RESET);
			}
		}

		private static boolean leaveGroupFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.println(ANSI_YELLOW + "Leaving group... Confirmation pending..." + ANSI_RESET);
			if (handler.leaveGroup(groupId)) {
				System.out.println(ANSI_GREEN + "Left group." + ANSI_RESET);
				return true;
			} else {
				System.out.println(ANSI_RED + "Failed to leave group." + ANSI_RESET);
				return false;
			}
		}

		private static void promoteAdminFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.print("Enter member name to promote: ");
			String promoteName = sc.nextLine();
			String promoteAddr = handler.getMemberAddress(groupId, promoteName);
			if (promoteAddr == null || promoteAddr.equals("0x0000000000000000000000000000000000000000") || promoteAddr.equals("0x0") || promoteAddr.equals("0")) {
				System.out.println(ANSI_RED + "User " + promoteName + " not found." + ANSI_RESET);
				return;
			}
			System.out.println(ANSI_YELLOW + "Promoting admin... Confirmation pending..." + ANSI_RESET);
			if (handler.promoteAdmin(groupId, promoteAddr))
				System.out.println(ANSI_GREEN + "Promoted." + ANSI_RESET);
			else
				System.out.println(ANSI_RED + "Failed to promote." + ANSI_RESET);
		}

		private static void demoteAdminFlow(SolShareHandler handler, BigInteger groupId) {
			System.out.print("Enter member name to demote: ");
			String demoteName = sc.nextLine();
			String demoteAddr = handler.getMemberAddress(groupId, demoteName);
			if (demoteAddr == null || demoteAddr.equals("0x0000000000000000000000000000000000000000") || demoteAddr.equals("0x0") || demoteAddr.equals("0")) {
				System.out.println(ANSI_RED + "User " + demoteName + " not found." + ANSI_RESET);
				return;
			}
			System.out.println(ANSI_YELLOW + "Demoting admin... Confirmation pending..." + ANSI_RESET);
			if (handler.demoteAdmin(groupId, demoteAddr))
				System.out.println(ANSI_GREEN + "Demoted." + ANSI_RESET);
			else
				System.out.println(ANSI_RED + "Failed to demote." + ANSI_RESET);
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
				String groupCurrency = handler.getGroupCurrency(groupId);
				
				java.math.BigDecimal displayAmount = new java.math.BigDecimal(amount).divide(new java.math.BigDecimal(100));
				System.out.println(debtorName + " pays " + creditorName + ": " + ANSI_YELLOW + displayAmount + " " + groupCurrency + ANSI_RESET);
				
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
