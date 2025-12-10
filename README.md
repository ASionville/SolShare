# SolShare
A Blockchain/Solidity-based group expense pool

![Ethereum Badge](https://img.shields.io/badge/Ethereum-3C3C3D?style=for-the-badge&logo=Ethereum&logoColor=white) ![Solidity Badge](https://img.shields.io/badge/Solidity-e6e6e6?style=for-the-badge&logo=solidity&logoColor=black) ![Eclipse Badge](https://img.shields.io/badge/Eclipse-2C2255?style=for-the-badge&logo=eclipse&logoColor=orange) ![MIT License Badge](https://img.shields.io/badge/MIT-green?style=for-the-badge)

## Overview
SolShare is a decentralized application built on the Ethereum blockchain that enables users to create and manage group expense pools. It allows multiple participants to share expenses transparently and securely using smart contracts.

This project has been developed as part of the *Blockchain and Smart Contracts* course at National Yang Ming Chiao Tung University (NYCU) under the guidance of [Prof. Wayne Chiu](https://github.com/tlchiu40209).

## Features

- **Group Expense Pools**: Create and manage expense pools for groups of friends, roommates, colleagues, etc.
- **Expense Tracking**: Add expenses with details such as amount, currency, description, and participants involved.
- **Balance and Debt Calculation**: Automatically calculate balances and debts among participants based on shared expenses.
- **Multi-Currency Support**: Handle expenses in different currencies with real-time conversion rates.
- **Optimal Settlement**: Suggest optimal settlement transactions to minimize the number of payments required to settle debts.
- **Admin Controls**: Group admins can manage members and expenses within the pool.
- **User-Friendly Interface**: Command-line interface for easy interaction with the application.
- **Limited Trust Policy** : Designed to minimize trust requirements among participants by leveraging blockchain transparency, and not allowing users to leave the group with an active debt.

## Installation

### Prerequisites
- [Java Development Kit (JDK)](https://www.oracle.com/java/technologies/downloads/) 8 or higher
- [Geth](https://geth.ethereum.org/) (Go Ethereum) client
- [Web3j library](https://github.com/LFDT-web3j/web3j/), distributed in the `lib` folder of this project for simplicity (licensed under Apache-2.0, I claim no rights over it, see License section below)

### Setup
1. **Blockchain Setup**:
    - Ensure you have a valid Ethereum node running (e.g., Geth).
    - Create an Ethereum account and fund it with test Ether for deploying and interacting with smart contracts.

2. **Configuration**:
    - Open `Config.java` located in `SolShare/src/ethInfo/`.
    - Update and save the configuration parameters as indicated in the file comments.

3. **Smart Contract**:
    - Deploy the `SolShare.sol` smart contract to your blockchain and note the deployed contract address.

## Usage

1. **Compile and Run**:
    - Compile the Java source files in the `SolShare/src/` directory using your preferred IDE or command-line tools, ensuring that the Web3j library is included in the classpath.
    - Run `solShareApp.SolShareApp` to start the application.

2. **First Time Setup**:
    - On the first run, you will be asked for a contract address. Enter the address of the deployed `SolShare` smart contract.

3. **Interacting with the Application**:
    - Use the command-line interface to create expense pools, add expenses, view balances, and settle debts.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your changes. Ensure that your code adheres to the existing style and includes appropriate tests.

If you encounter any issues or have suggestions for improvements, please open an issue on the GitHub repository or contact me directly.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

The [Web3j library](https://github.com/LFDT-web3j/web3j/) included in this project is licensed under the Apache-2.0 License, see the [Web3j LICENSE](https://github.com/LFDT-web3j/web3j/blob/main/LICENSE) file for details.