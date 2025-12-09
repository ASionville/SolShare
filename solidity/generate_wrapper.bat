solc --overwrite -o SolShare --bin --abi --evm-version=london .\SolShare.sol
cd SolShare
web3j generate solidity --binFile=./SolShare.bin --abiFile=./SolShare.abi --outputDir=./javaWrapper --package=ethSC
del /f ./SolShare.bin
del /f ./SolShare.abi