package eu.heliovo.tavernademonstrator;

import static eu.heliovo.tavernaserver.util.Registry.getHelioGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import eu.heliovo.myexperiment.Workflow;
import eu.heliovo.myexperiment.helio.Helio;
import eu.heliovo.tavernaserver.Run;
import eu.heliovo.tavernaserver.Server;

import uk.org.taverna.ns._2010.xml.server.soap.UnknownRunException;

public class Demonstrator {
	static void printProperty(Run r, String property) throws Exception {
		String value = r.getListener("io").getProperty(property);
		System.out.println("---" + property + "---\n" + value);
	}

	public static Server server = new Server("taverna", "taverna");
	private static final boolean DO_EXPIRY_TEST = false;
	private static final boolean DO_TERM_NOTIFY_TEST = false;
	private static final boolean NEW_API = false;

	public static void main(String... args) throws Exception {
		if (args.length < 2 || args.length % 2 == 1) {
			System.err
					.println("wrong # args: "
							+ "requires \"workflow.t2flow output.zip {inputName inputData ...}\"");
			System.exit(1);
		}

		if (NEW_API) {
			for (Workflow wf : getHelioGroup().getWorkflows()) {
				System.out.println(wf.getId() + ": " + wf.getTitle() + " ("
						+ wf.getUploader() + ")");
			}
		} else {
			for (Workflow wf : Helio.group().getWorkflows()) {
				System.out.println(wf.getId() + ": " + wf.getTitle() + " ("
						+ wf.getUploader() + ")");
			}
		}

		Run r = server.createRun(new File(args[0]));
		try {
			System.out.println("expected inputs: " + r.getInputNames());
			System.out.println("id: " + r.getId());
			for (int i = 2; i < args.length; i += 2) {
				r.setInput(args[i], args[i + 1].getBytes());
			}
			// r.outputToBaclava();
			r.runUntilFinished(new Run.Callback() {
				@Override
				public void tick() {
					System.out.print(".");
				}
			});
			System.out.println("\ndone");
			printProperty(r, "stdout");
			printProperty(r, "stderr");
			printProperty(r, "exitcode");
			printProperty(r, "usageRecord");
			// r.retrieveOutputBaclava(new File(args[1]));
			System.out.println("====listing directory===");
			for (String name : r.listDirectory("out")) {
				String contentType = r.getFileType(name);
				long length = r.getFileLength(name);
				System.out.println(name + "\t" + length + " bytes; type: "
						+ contentType);
			}
			byte[] zip = r.getOutputZip();
			System.out.println("====saving output to " + args[1] + "====");
			FileOutputStream fos = new FileOutputStream(args[1]);
			fos.write(zip);
			fos.close();
			if (DO_EXPIRY_TEST) {
				System.out.println("====running expiry test====");
				r.setExpiryToNowPlus(15);
				Thread.sleep(75 * 1000);
				try {
					System.out.println("existing at " + new Date()
							+ " but expiry is " + r.getExpiry().getTime());
				} catch (UnknownRunException e) {
					System.out.println("====PASSED!====");
					r = null;
				}
			} else if (DO_TERM_NOTIFY_TEST) {
				System.out
						.println("waiting for 30 seconds to allow termination notification test");
				Thread.sleep(30 * 1000);
			}
		} finally {
			if (r != null)
				r.destroy();
		}
	}
}