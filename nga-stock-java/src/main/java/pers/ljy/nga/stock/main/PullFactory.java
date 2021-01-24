package pers.ljy.nga.stock.main;

import java.io.IOException;

public class PullFactory {

	private PullFactory() {
	}

	public static void initPull(String tid, String name) {
		PullMain pull = new PullMain(tid, name);
		try {
			pull.init();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
