package servers;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class Downlink {

  public static void main(String[] argv){
    int port = Definition.PORT_DOWNLINK;
    ServerSocket server = null;
    File logFile = ExperimentWorker.getLogFile("downlink");
    
    try {
      server = new ServerSocket(port);
      while (true) {
        System.out.println("Downlink server starts on port " + port);
        DownlinkWorker downlinkWorker = new DownlinkWorker(logFile);
        downlinkWorker.setSocket(server.accept());
        downlinkWorker.start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
