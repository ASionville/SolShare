package ethInfo;

public class Config {
	// 1. Setting up Ethereum Basis
	public static final long chainID = Long.parseLong("YOUR_CHAIN_ID"); // your Ethereum chain ID
	public static final String datastore = "Path\\To\\Your\\Ethereum\\DataStore\\"; // path to your Ethereum data directory
	public static final String pipeLine = "\\\\.\\pipe\\geth.ipc"; // IPC path
	public static String credential = "Path\\To\\Your\\Ethereum\\keystore\\"; // path to your Ethereum wallet file
	public static String password = "your_password"; // password for your Ethereum wallet

	// 2. Setting up SolShare Configuration
	public static final String platform = "Windows"; // Windows, Linux or MacOS
	public static final boolean useANSIColors = true; // disable if your console does not support ANSI colors
}