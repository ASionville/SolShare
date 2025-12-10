// SPDX-License-Identifier: GPL-3.0
pragma solidity >0.8.0;

contract SolShare {
	struct Member {
		address addr;
		string name;
		bool isAdmin;
		bool exists;
	}

	struct Expense {
		uint256 id;
		address payer;
		uint256 amount; // Normalized amount in Group Currency
		string description;
		uint256 timestamp;
		address[] participants;
		string originalCurrency;
		uint256 originalAmount;
		bool isSettlement;
		bool exists;
	}

	struct Group {
		uint256 id;
		string name;
		string description;
		string currency; // Group Currency
		address[] members;
		mapping(address => Member) memberInfo;
		address[] admins;
		Expense[] expenses;
		bool exists;
	}

	mapping(uint256 => Group) public groups;

	// Events
	event GroupCreated(uint256 indexed groupId, string name, string description, string currency, address creator);
	event MemberAdded(uint256 indexed groupId, address member, string name);
	event MemberJoined(uint256 indexed groupId, address member);
	event MemberLeft(uint256 indexed groupId, address member);
	event AdminPromoted(uint256 indexed groupId, address member);
	event AdminDemoted(uint256 indexed groupId, address member);
	event ExpenseAdded(uint256 indexed groupId, uint256 expenseId, address payer, uint256 amount, uint256 originalAmount, string originalCurrency, string description, bool isSettlement);
	event ExpenseEdited(uint256 indexed groupId, uint256 expenseId);
	event ExpenseDeleted(uint256 indexed groupId, uint256 expenseId);

	// Modifiers
	modifier groupExists(uint256 groupId) {
		require(groups[groupId].exists, "Group does not exist");
		_;
	}

	modifier onlyMember(uint256 groupId) {
		require(groups[groupId].memberInfo[msg.sender].exists, "Not a group member");
		_;
	}

	modifier onlyAdmin(uint256 groupId) {
		require(groups[groupId].memberInfo[msg.sender].isAdmin, "Not an admin");
		_;
	}

	// Group management
	function createGroup(string memory name, string memory description, string memory currency, string memory creatorName) public returns (uint256) {
		uint256 groupId = uint256(keccak256(abi.encodePacked(name, description, currency, msg.sender, block.timestamp)));
		require(!groups[groupId].exists, "Group already exists");
		Group storage g = groups[groupId];
		g.id = groupId;
		g.name = name;
		g.description = description;
		g.currency = currency;
		g.exists = true;
		g.members.push(msg.sender);
		g.admins.push(msg.sender);
		g.memberInfo[msg.sender] = Member(msg.sender, creatorName, true, true);
		emit GroupCreated(groupId, name, description, currency, msg.sender);
		emit MemberAdded(groupId, msg.sender, creatorName);
		emit AdminPromoted(groupId, msg.sender);
		return groupId;
	}

	function joinGroup(uint256 groupId, string memory memberName) public groupExists(groupId) {
		Group storage g = groups[groupId];
		require(!g.memberInfo[msg.sender].exists, "Already a member");
		g.members.push(msg.sender);
		g.memberInfo[msg.sender] = Member(msg.sender, memberName, false, true);
		emit MemberJoined(groupId, msg.sender);
		emit MemberAdded(groupId, msg.sender, memberName);
	}

	function leaveGroup(uint256 groupId) public groupExists(groupId) onlyMember(groupId) {
		Group storage g = groups[groupId];
		require(getNetBalance(groupId, msg.sender) == 0, "Debt must be zero to leave");
		g.memberInfo[msg.sender].exists = false;
		// Remove from members array
		for (uint i = 0; i < g.members.length; i++) {
			if (g.members[i] == msg.sender) {
				g.members[i] = g.members[g.members.length - 1];
				g.members.pop();
				break;
			}
		}
		emit MemberLeft(groupId, msg.sender);
	}

	function promoteAdmin(uint256 groupId, address member) public groupExists(groupId) onlyAdmin(groupId) {
		Group storage g = groups[groupId];
		require(g.memberInfo[member].exists, "Not a member");
		require(!g.memberInfo[member].isAdmin, "Already admin");
		g.memberInfo[member].isAdmin = true;
		g.admins.push(member);
		emit AdminPromoted(groupId, member);
	}

	function demoteAdmin(uint256 groupId, address member) public groupExists(groupId) onlyAdmin(groupId) {
		Group storage g = groups[groupId];
		require(g.memberInfo[member].isAdmin, "Not an admin");
		require(g.admins.length > 1, "At least one admin required");
		g.memberInfo[member].isAdmin = false;
		// Remove from admins array
		for (uint i = 0; i < g.admins.length; i++) {
			if (g.admins[i] == member) {
				g.admins[i] = g.admins[g.admins.length - 1];
				g.admins.pop();
				break;
			}
		}
		emit AdminDemoted(groupId, member);
	}

	// Expense management
	function addExpense(uint256 groupId, uint256 amount, string memory description, address[] memory participants, uint256 originalAmount, string memory originalCurrency, bool isSettlement) public groupExists(groupId) onlyMember(groupId) {
		require(amount > 0, "Expense must be > 0");
		Group storage g = groups[groupId];
		require(g.memberInfo[msg.sender].exists, "Payer must be member");
		for (uint i = 0; i < participants.length; i++) {
			require(g.memberInfo[participants[i]].exists, "Participant must be member");
		}
		uint256 expenseId = g.expenses.length;
		// Emit event before creating the struct to reduce stack usage
		emit ExpenseAdded(groupId, expenseId, msg.sender, amount, originalAmount, originalCurrency, description, isSettlement);
		Expense memory e = Expense(expenseId, msg.sender, amount, description, block.timestamp, participants, originalCurrency, originalAmount, isSettlement, true);
		g.expenses.push(e);
	}

	function editExpense(uint256 groupId, uint256 expenseId, uint256 amount, string memory description, address[] memory participants, uint256 originalAmount, string memory originalCurrency, bool isSettlement) public groupExists(groupId) onlyAdmin(groupId) {
		Group storage g = groups[groupId];
		require(expenseId < g.expenses.length, "Invalid expense");
		require(amount > 0, "Expense must be > 0");
		Expense storage e = g.expenses[expenseId];
		require(e.exists, "Expense does not exist");
		e.amount = amount;
		e.description = description;
		e.participants = participants;
		e.originalCurrency = originalCurrency;
		e.originalAmount = originalAmount;
		e.isSettlement = isSettlement;
		emit ExpenseEdited(groupId, expenseId);
	}

	function deleteExpense(uint256 groupId, uint256 expenseId) public groupExists(groupId) onlyAdmin(groupId) {
		Group storage g = groups[groupId];
		require(expenseId < g.expenses.length, "Invalid expense");
		Expense storage e = g.expenses[expenseId];
		require(e.exists, "Expense does not exist");
		e.exists = false;
		emit ExpenseDeleted(groupId, expenseId);
	}

	// Balance computation
	function getNetBalance(uint256 groupId, address member) public view groupExists(groupId) returns (int256) {
		Group storage g = groups[groupId];
		int256 net = 0;
		for (uint i = 0; i < g.expenses.length; i++) {
			Expense storage e = g.expenses[i];
			if (!e.exists) continue;
			uint256 share = e.amount / e.participants.length;
			for (uint j = 0; j < e.participants.length; j++) {
				if (e.participants[j] == member) {
					net -= int256(share);
				}
			}
			if (e.payer == member) {
				net += int256(e.amount);
			}
		}
		return net;
	}


	// Checks if a name is already taken in the group
	function isNameTaken(uint256 groupId, string memory name) public view groupExists(groupId) returns (bool) {
		Group storage g = groups[groupId];
		for (uint i = 0; i < g.members.length; i++) {
			address addr = g.members[i];
			if (keccak256(bytes(g.memberInfo[addr].name)) == keccak256(bytes(name))) {
				return true;
			}
		}
		return false;
	}

	// Gets the address of a member by their name
	function getMemberAddress(uint256 groupId, string memory name) public view groupExists(groupId) returns (address) {
		Group storage g = groups[groupId];
		for (uint i = 0; i < g.members.length; i++) {
			address addr = g.members[i];
			if (keccak256(bytes(g.memberInfo[addr].name)) == keccak256(bytes(name))) {
				return addr;
			}
		}
		return address(0);
	}

	// Getters
	function getGroupMembers(uint256 groupId) public view groupExists(groupId) returns (address[] memory) {
		return groups[groupId].members;
	}

	function getMemberName(uint256 groupId, address member) public view groupExists(groupId) returns (string memory) {
		return groups[groupId].memberInfo[member].name;
	}

	function getGroupAdmins(uint256 groupId) public view groupExists(groupId) returns (address[] memory) {
		return groups[groupId].admins;
	}

	function getExpenses(uint256 groupId) public view groupExists(groupId) returns (Expense[] memory) {
		return groups[groupId].expenses;
	}
}
