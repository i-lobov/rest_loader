package rest.loader;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.lang.StringBuilder;

public class App {

	public static String propFileName = "application.properties";

	public static Properties prop = new Properties();

	public static void main(String args[]) {

		int threadsNumber;

		loadProperties();

		threadsNumber = Integer.valueOf(getProperties("threads.number"));

		ExecutorService service = Executors.newFixedThreadPool(threadsNumber);

		List<Worker> taskList = new ArrayList<>();

		for (int i = 0; i < threadsNumber; i++) {
			taskList.add(new Worker());
		}

		// Execute all tasks and get reference to Future objects
		List<Future<Long>> resultList = null;
		System.out.println("Start sending");
		Long startTime = System.currentTimeMillis();
		try {
			resultList = service.invokeAll(taskList);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Long endTime = System.currentTimeMillis();
		printStatus(endTime - startTime);
		service.shutdown();

	}

	public static void loadProperties() {
		try {
			File base = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
			String configFile = base.getAbsolutePath() + "/" + propFileName;
			FileInputStream inputStream = new FileInputStream(configFile);
			prop.load(inputStream);
		} catch (Exception e) {
			System.out.println("Error during reading properties " + e.getMessage());
		}
	}

	public static String getProperties(String key) {
		return prop.getProperty(key);
	}

	public static String generateString(int targetStringLength) {
		int leftLimit = 97; // letter 'a'
		int rightLimit = 122; // letter 'z'
		Random random = new Random();

		String generatedString = random.ints(leftLimit, rightLimit + 1).limit(targetStringLength * 1024)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

		return generatedString;
	}

	public static void printStatus(Long totalTime) {

		String operationType = getProperties("operation.type").toLowerCase();

		int loadRequestsCount = Integer.valueOf(getProperties("loader.cnt"));

		int thrNumber = Integer.valueOf(getProperties("threads.number"));

		if (!Worker.exceptionHappened) {

			switch (operationType) {
			case "delete":
				System.out
						.println(loadRequestsCount * thrNumber + " delete requests sent to " + getProperties("qm.name")
								+ " queue " + getProperties("qm.queue.name") + " in " + totalTime + " ms.");
				break;
			default:
				System.out.println(loadRequestsCount * thrNumber + " post requests sent to " + getProperties("qm.name")
						+ " queue " + getProperties("qm.queue.name") + " in " + totalTime + " ms.");
			}

		}
	}

}
