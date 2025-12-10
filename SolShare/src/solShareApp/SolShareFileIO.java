package solShareApp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class SolShareFileIO {
	private static final String poolFileName = "solshare_pools.dat";
	private static File poolFile;
	private static ArrayList<String> poolAddresses = new ArrayList<>();

	public SolShareFileIO() {
		loadPools();
	}

	public void addPool(String newPoolAddress) {
		if (poolAddresses.contains(newPoolAddress)) {
			System.out.println("It's already in the list");
			return;
		}
		poolAddresses.add(newPoolAddress);
		updatePoolFile();
	}

	public ArrayList<String> getAllPools() {
		return poolAddresses;
	}

	public void updatePoolFile() {
		poolFile = new File(poolFileName);
		if (poolFile.exists()) {
			poolFile.delete();
		}
		try {
			poolFile.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(poolFile));
			for (String poolAddress : poolAddresses) {
				bw.write(poolAddress + "\n");
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			System.out.println("Cannot update pool record file.");
			e.printStackTrace();
		}
	}

	private void loadPools() {
		poolFile = new File(poolFileName);
		if (poolFile.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(poolFile));
				String line;
				while ((line = br.readLine()) != null) {
					if (!line.trim().isEmpty()) poolAddresses.add(line.trim());
				}
				br.close();
			} catch (Exception e) {
				System.err.println("Cannot read pool record file.");
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			try {
				poolFile.createNewFile();
			} catch (Exception e) {
				System.err.println("Cannot create pool record file.");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
