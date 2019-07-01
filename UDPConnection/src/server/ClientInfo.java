package server;

class ClientInfo {
	private String clientID;
	private String clientIP;
	private int clientPort;
	
	ClientInfo(String clientID, String clientIP, int clientPort) {
		this.clientID = clientID;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
	}

	public String getClientID() {
		return clientID;
	}
	
	public String getClientIP() {
		return clientIP;
	}
	
	public int getClientPort() {
		return clientPort;
	}
	

}
