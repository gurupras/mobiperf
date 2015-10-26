package servers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public abstract class ExperimentWorker extends Thread {
	public static final File LOGS_DIR = new File("logs");
	
	private String name;
	private File logFile;
	
	public ExperimentWorker(String name) {
		if(!LOGS_DIR.exists()) {
			LOGS_DIR.mkdirs();
		}
		this.name = name;
	}
	
	public ExperimentWorker(String name, File logFile) {
		this.name = name;
		this.logFile = logFile;
	}

	public static File getLogFile(String name) {
		int logNum = 0;
		while(true) {
			File logFile = new File(LOGS_DIR.getAbsolutePath() + File.pathSeparator + name + "." + logNum);
			if(!logFile.exists()) {
				return logFile;
			}
			logNum++;
		}
	}
	
	protected synchronized void log(Object o) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(logFile, true));
			out.write(o.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected int byteArrayToInt(byte[] b) 
	{
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}

	protected byte[] intToByteArray(int a) {
	    return new byte[] {
	        (byte) ((a >> 24) & 0xFF),
	        (byte) ((a >> 16) & 0xFF),   
	        (byte) ((a >> 8) & 0xFF),   
	        (byte) (a & 0xFF)
	    };
	}
	
	public String getExperimentId(Socket socket) {
		String experimentId = null;
		try {
			InputStream iStream = socket.getInputStream();
			byte[] lenArray = new byte[Integer.SIZE / 8];
			iStream.read(lenArray);
			int len = byteArrayToInt(lenArray);
			
			byte[] experimentIdArray = new byte[len];
			iStream.read(experimentIdArray);
			experimentId = new String(experimentIdArray, "UTF-8");
		} catch(Exception e) {
			e.printStackTrace();
		}
		return experimentId;
	}
}
