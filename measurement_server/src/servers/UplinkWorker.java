package servers;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class UplinkWorker extends ExperimentWorker {
  private Socket client = null;

  private ArrayList<Double> tps_result;
  public int size = 0;
  public long testStartTime = 0; //test start time, used to determine slow start period
  public long startTime = 0; //start time of this period to calculate throughput
  public final static long SAMPLE_PERIOD = 100; 
  public final static long SLOW_START_PERIOD = 5000; //empirically set to 5 seconds 

  public UplinkWorker() {
	super("uplink");
	init();
  }

  public UplinkWorker(File logFile) {
	  super("uplink", logFile);
	  init();
  }
  
  private void init() {
    tps_result = new ArrayList<Double>();
    testStartTime = System.currentTimeMillis();
  }
  
  public void setSocket(Socket client) {
    this.client = client;
  }

  public void run() {
    InputStream iStream = null;
    OutputStream oStream = null;
    try {
      client.setSoTimeout(Definition.RECV_TIMEOUT);
      client.setTcpNoDelay(true);

      iStream = client.getInputStream();
      oStream = client.getOutputStream();
      /* Get experiment ID */
      String experimentId = getExperimentId(client);
      JsonObject json = new JsonObject();
      json.addProperty("id", experimentId);
      
      SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd:HH:mm:ss:SSS");
      long threadId = this.getId();
      String startDate = sDateFormat.format(new Date()).toString();
      log("[" + startDate + "]" + " Uplink worker <" +
                         threadId + "> Thread starts");
      json.addProperty("startTime", System.currentTimeMillis());
      
      int readLen;
      byte [] buffer = new byte[Definition.BUFFER_SIZE];
      String recvData;
      while (true) {
        readLen = iStream.read(buffer, 0, buffer.length);
        if (readLen > 0) {
          recvData = new String(buffer).substring(0, readLen);
          // str = "data*" last message at str.substring(5-"*".length(), 5)
          if (readLen >= Definition.UPLINK_FINISH_MSG.length() && 
            recvData.substring(readLen - Definition.UPLINK_FINISH_MSG.length(), 
                               readLen).equals(Definition.UPLINK_FINISH_MSG)) {
            log("LAST MSG detected break");
            break;
          }
          updateSize(readLen);
        }
        else break;
      }

/*
 * We do not send results
      if (tps_result.size() > 0) {
        String result = "";
        for (int i = 0; i < tps_result.size() - 1; i++)
          result += tps_result.get(i) + "#";
        result += tps_result.get(tps_result.size() - 1);
        byte [] finalResult = result.getBytes();
        oStream.write(finalResult, 0, finalResult.length);
        oStream.flush();
      }
*/
      JsonArray tps_result_array = new JsonArray();
      for(double d : tps_result)
    	  tps_result_array.add(d);
      json.add("speeds", tps_result_array);
      
      String endDate = sDateFormat.format(new Date()).toString();
      log("[" + endDate + "]" + " Uplink worker <" +
                         threadId + "> Thread ends");
      json.addProperty("endTime", System.currentTimeMillis());
      
      log(json.toString());
    } catch (IOException e) {
      e.printStackTrace();
      log("Uplink worker failed: port <" +
                         Definition.PORT_DOWNLINK + ">");
    } finally {
      if (null != oStream) {
        try {
          oStream.close();
        } catch (IOException e) {
          // nothing to be done, really; logging is probably over kill
          log("Error closing socket output stream.");
        }
      }
      if (null != iStream) {
        try {
          iStream.close();
        } catch (IOException e) {
          // nothing to be done, really; logging is probably over kill
          log("Error closing socket input stream.");
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

  private void updateSize(int delta) {
    double gtime = System.currentTimeMillis() - testStartTime;
    if (gtime < SLOW_START_PERIOD) //ignore slow start
      return;
    if (startTime == 0) {
      startTime = System.currentTimeMillis();
      size = 0;
    }
    size += delta;
    double time = System.currentTimeMillis() - startTime;
    if (time < SAMPLE_PERIOD) {
      return;
    } else {
      //time is in milli, so already kbps
      double throughput = (double)size * 8.0 / time;
      log("_throughput: " + throughput + " kbps_Time(sec): "
                         + (gtime / 1000.0));
      tps_result.add(throughput);
      size = 0;
      startTime = System.currentTimeMillis();
    }  
  }
}
