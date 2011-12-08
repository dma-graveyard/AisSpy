package dk.frv.aisspy.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

public class HttpServer extends Thread {

	private static final Logger LOG = Logger.getLogger(HttpServer.class);

	private ServerSocket serverSocket;

	public HttpServer(int serverPort) throws IOException {
		try {
			serverSocket = new ServerSocket(serverPort);
		} catch (IOException e) {
			LOG.error("Failed to open server socket on port " + serverPort + ": " + e.getMessage());
			throw e;
		}
	}

	@Override
	public void run() {
		LOG.info("Waiting for HTTP connections on port " + serverSocket.getLocalPort());
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				HttpHandler handler = new HttpHandler(socket);
				handler.start();
			} catch (IOException e) {
				LOG.error("HTTP server failed: " + e.getMessage());
				return;
			}
		}
	}

}
