package servers;

// import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.google.gson.JsonObject;

public class DownlinkWorker extends ExperimentWorker {
	
  public DownlinkWorker() {
		super("downlink");
  }
  
  public DownlinkWorker(File logFile) {
	  super("downlink", logFile);
  }
  
  private Socket client = null;

  public void setSocket(Socket client) {
    this.client = client;
  }

  public void run() {
    OutputStream oStream = null;
    try {
      JsonObject json = new JsonObject();
      
      client.setSoTimeout(Definition.RECV_TIMEOUT);
      oStream = client.getOutputStream();

      String experimentId = getExperimentId(client);
      json.addProperty("id", experimentId);
      
      SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd:HH:mm:ss:SSS");
      long threadId = this.getId();
      String startDate = sDateFormat.format(new Date()).toString();
      log("[" + startDate + "]" + " Downlink worker <" +
                         threadId + "> Thread starts");
      json.addProperty("startTime", startDate);
      
      long start = System.currentTimeMillis();
      long end = System.currentTimeMillis();

      byte [] buffer = new byte[Definition.THROUGHPUT_DOWN_SEGMENT_SIZE];
      Utilities.genRandomByteArray(buffer);
      while(end - start < Definition.DURATION_IPERF_MILLISECONDS) {
        oStream.write(buffer, 0, buffer.length);
        oStream.flush();
        end = System.currentTimeMillis();
      }
      String endDate = sDateFormat.format(new Date()).toString();
      log("[" + endDate + "]" + " Downlink worker <" +
                         threadId + "> Thread ends");
      json.addProperty("endTime", endDate);
      
      log(json.toString());
    } catch (IOException e) {
      e.printStackTrace();
      log("Downlink worker failed: port <" +
                         Definition.PORT_DOWNLINK + ">");
    } finally {
      if (null != oStream) {
        try {
          oStream.close();
        } catch (IOException e) {
          // nothing to be done, really; logging is probably over kill
          log("Error closing socket output stream.");
        }
        try {
          client.close();
        } catch (IOException e) {
          // nothing to be done, really; logging is probably over kill
          log("Error closing socket client.");
        }
      }
    }
  }
}
