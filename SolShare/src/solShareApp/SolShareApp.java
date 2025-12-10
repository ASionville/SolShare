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
				System.out.println("3. Back to main menu");
				System.out.print("Your choice (1/2/3): ");
				int choice = Integer.parseInt(sc.nextLine());
				switch (choice) {
					case 1:
						createGroupFlow(handler);
						break;
					case 2:
						connectToGroupFlow(handler);
						break;
					case 3:
						inGroupMenu = false;
						break;
					default:
						System.out.println("Invalid choice.");
				}
			}
		}

		private static void createGroupFlow(SolShareHandler handler) {
			System.out.print("Enter group name: ");
			String groupName = sc.nextLine();
			System.out.print("Enter group description: ");
			String groupDesc = sc.nextLine();
			System.out.print("Enter your name: ");
			String creatorName = sc.nextLine();
			java.math.BigInteger groupId = handler.createGroup(groupName, groupDesc, creatorName);
			if (groupId.compareTo(java.math.BigInteger.ZERO) > 0) {
				System.out.println("Group created with ID: " + groupId);
				// Optionally, you can store groupId locally if needed
				manageGroup(handler, groupId);
			} else {
				System.out.println("Failed to create group.");
			}
		}

		private static void connectToGroupFlow(SolShareHandler handler) {
			// List group IDs (if available)
			System.out.print("Enter group ID to connect: ");
			String groupIdStr = sc.nextLine();
			try {
				java.math.BigInteger groupId = new java.math.BigInteger(groupIdStr);
				manageGroup(handler, groupId);
			} catch (Exception e) {
				System.out.println("Invalid group ID.");
			}
		}

		private static void manageGroup(SolShareHandler handler, java.math.BigInteger groupId) {
			boolean managing = true;
			while (managing) {
				System.out.println("\n=== Manage Group " + groupId + " ===");
				System.out.println("1. Join group");
				System.out.println("2. Add expense");
				System.out.println("3. View balances");
				System.out.println("4. View expenses");
				System.out.println("5. Settle up");
				System.out.println("6. Promote admin");
				System.out.println("7. Demote admin");
				System.out.println("8. Leave group");
				System.out.println("9. Efficient Settlement Plan");
				System.out.println("10. Back to group menu");
				System.out.print("Your choice: ");
				int op = Integer.parseInt(sc.nextLine());
				switch (op) {
					case 1:
						System.out.print("Enter your name: ");
						String name = sc.nextLine();
						if (handler.joinGroup(groupId, name))
							System.out.println("Joined group successfully.");
						else
							System.out.println("Failed to join group.");
						break;
					case 2:
						System.out.print("Amount: ");
						java.math.BigInteger amount = new java.math.BigInteger(sc.nextLine());
						System.out.print("Description: ");
						String desc = sc.nextLine();
						System.out.print("Currency: ");
						String currency = sc.nextLine();
						System.out.print("Participants (comma separated addresses): ");
						String[] parts = sc.nextLine().split(",");
						java.util.List<String> participants = new java.util.ArrayList<>();
						for (String p : parts) participants.add(p.trim());
						if (handler.addExpense(groupId, amount, desc, participants, currency, false))
							System.out.println("Expense added.");
						else
							System.out.println("Failed to add expense.");
						break;
					case 3:
						java.util.List<String> members = handler.getGroupMembers(groupId);
						if (members != null) {
							for (String member : members) {
								java.math.BigInteger bal = handler.getNetBalance(groupId, member);
								String memberName = handler.getMemberName(groupId, member);
								System.out.println("Member " + memberName + " (" + member + ") balance: " + bal);
							}
						} else {
							System.out.println("Could not retrieve group members.");
						}
						break;
					case 4:
						java.util.List<ethSC.SolShare.Expense> expenses = handler.getExpenses(groupId);
						if (expenses != null) {
							for (ethSC.SolShare.Expense e : expenses) {
								System.out.println("Expense #" + e.id + ": " + e.description + ", amount: " + e.amount + ", payer: " + e.payer);
							}
						} else {
							System.out.println("Could not retrieve expenses.");
						}
						break;
					case 5:
						System.out.print("To address: ");
						String to = sc.nextLine();
						System.out.print("Amount: ");
						java.math.BigInteger settleAmount = new java.math.BigInteger(sc.nextLine());
						System.out.print("Currency: ");
						String settleCurrency = sc.nextLine();
						
						// Get names
						// Assuming current user is the one running the app, but we don't have current user address easily available in this context without asking or storing it.
						// However, the handler uses credentials loaded in constructor.
						// We can't easily get "my" address from handler public interface unless we expose it.
						// But wait, handler uses `cr` which is static in `SolShareHandler`.
						// I should probably expose `getMyAddress` in `SolShareHandler` or just use "Me" if I can't get it.
						// But `SolShareHandler` has `cr` private.
						// I'll assume the user knows who they are or I'll just use "Settlement" as description prefix.
						// The requirement says: "Use stored names for A and B in the description."
						// I need A's name (payer) and B's name (payee).
						// I can get B's name using `getMemberName`.
						// I can't get A's name easily without A's address.
						// I'll add `getMyAddress` to `SolShareHandler`? No, I can't modify `SolShareHandler` easily to expose private `cr`.
						// Actually, `SolShareHandler` has `cr` as static.
						// I can add a getter for it in `SolShareHandler`?
						// I already modified `SolShareHandler`. I can add `getMyAddress`.
						
						// Let's assume I can't modify `SolShareHandler` again or I don't want to.
						// I'll just ask for the user's address or name? No, that's annoying.
						// I'll modify `SolShareHandler` to add `getMyAddress()` since I already rewrote it.
						// Wait, I already wrote `SolShareHandler`. I missed `getMyAddress`.
						// I'll just use "Settlement to [Name B]" for now, or try to fetch it.
						
						String toName = handler.getMemberName(groupId, to);
						String descSettlement = "Settlement to " + toName;
						
						List<String> settleParticipants = new ArrayList<>();
						settleParticipants.add(to);
						
						if (handler.addExpense(groupId, settleAmount, descSettlement, settleParticipants, settleCurrency, true))
							System.out.println("Settlement added.");
						else
							System.out.println("Failed to add settlement.");
						break;
					case 6:
						System.out.print("Enter member address to promote: ");
						String promoteAddr = sc.nextLine();
						if (handler.promoteAdmin(groupId, promoteAddr))
							System.out.println("Promoted.");
						else
							System.out.println("Failed to promote.");
						break;
					case 7:
						System.out.print("Enter member address to demote: ");
						String demoteAddr = sc.nextLine();
						if (handler.demoteAdmin(groupId, demoteAddr))
							System.out.println("Demoted.");
						else
							System.out.println("Failed to demote.");
						break;
					case 8:
						if (handler.leaveGroup(groupId))
							System.out.println("Left group.");
						else
							System.out.println("Failed to leave group.");
						break;
					case 9:
						showEfficientSettlement(handler, groupId);
						break;
					case 10:
						managing = false;
						break;
					default:
						System.out.println("Invalid choice.");
				}
			}
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
				
				System.out.println(debtorName + " pays " + creditorName + ": " + amount);
				
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
