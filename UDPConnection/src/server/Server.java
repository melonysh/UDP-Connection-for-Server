package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Server {

	private HashMap<String, ClientInfo> clientInfo;
	// private HashSet<String> IPSet = new HashSet<String>();
	private HashMap<String, String> userInfo;// <userName,userPassword>

	private ArrayList<String> userList;

	private int serverPort = 8800;
	private int clientCounter = 0;

	DatagramSocket dgSocket = null;
	// 2.�������ݱ������ڽ��տͻ��˷��͵�����
	byte[] data = null;// �����ֽ����飬ָ�����յ����ݰ��Ĵ�С
	DatagramPacket dgPacket = null;

	public Server(int serverPort) {
		this.serverPort = serverPort;
		try {
			initServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clientInfo = new HashMap<>();
		userInfo = new HashMap<>();
		userList = new ArrayList<>();
	}

	public Server() {
		try {
			initServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clientInfo = new HashMap<>();
		userInfo = new HashMap<>();
		userList = new ArrayList<>();
	}

	private void initServer() throws IOException {
		dgSocket = new DatagramSocket(serverPort);
		System.out.println("----��������������!----");
	}

	class ReceivingThread extends Thread {

		private String clientIP;
		private int clientPort;
		private InetAddress clientAddress = null;

		@Override
		public void run() {
			while (true) {
				data = new byte[1024];
				dgPacket = new DatagramPacket(data, data.length);
				try {
					dgSocket.receive(dgPacket); // �˷����ڽ��յ����ݱ�֮ǰ��һֱ����
				} catch (IOException e) {
					e.printStackTrace();
				}

				clientIP = dgPacket.getAddress().getHostAddress();
				clientPort = dgPacket.getPort();
				clientAddress = dgPacket.getAddress();

				String message = new String(data, 0, dgPacket.getLength());
				String[] msgContent = message.split("--");
				switch (msgContent[0]) {
					case "ConnectServer": //format:ConnectServer--userName--userPassword
						new ConnectServerThread(msgContent[1], msgContent[2]).start();
						break;
					case "ConnectPeer": //format:ConnectPeer--sourseName--destinationName
						new ConnectPeerThread(msgContent[1],msgContent[2]).start();
						break;
					case "Quit"://format:Quit--userName
						RemoveUser(msgContent[1]);
						break;
					case "Maintain":
						break;
					case "PeerMsg": //format:PeerMsg--source--destination--content
						transmitMsg(msgContent[1],msgContent[2],msgContent[3]);
						break;
					default:
						System.out.println("----�����쳣��Ϣ��" + message);
				}
				
				// ��ʱ�������߳�����
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}

		class ConnectServerThread extends Thread {

			private String userName;
			private String userPassword;
			
			byte[] data = null;
			DatagramPacket dgPacket = null;

			public ConnectServerThread(String userName, String userPassword) {
				this.userName = userName;
				this.userPassword = userPassword;
			}

			@Override
			public void run() {
				if (userInfo.containsKey(userName)) {	//�û��Ѵ���
					if (userInfo.get(userName).equals(userPassword)) {
						if(clientInfo.containsKey(userName)) {//���û��Ѿ���¼��
							data = "Redundant".getBytes();
							System.out.println("�û��ظ���¼:" + userName + "  IP:" + clientIP + "  Port:" + clientPort);	
							System.out.println("��ǰ��¼�û�������" + clientCounter);
							clientInfo.put(userName, new ClientInfo(userName, clientIP, clientPort));
							dgPacket = new DatagramPacket(data, data.length, clientAddress, clientPort);
							try {
								dgSocket.send(dgPacket);
							} catch (IOException e) {
								e.printStackTrace();
							}
							/*
							 * ����ϵͳ��½�û�״̬�������пͻ��˷��͸�����Ϣ
							 */
							userList.remove(userName);
							if(userList.isEmpty()) {
								data = "USER_EMPTY".getBytes();
							} else {
								String peerNames = "";	//���¼���ͻ��˵���Ϣ
								//data = ("ServerAddUser--" + userName).getBytes();	//��֪�����û����û��ļ���
								for (String name:userList)
									peerNames += "--" + name;
								data = ("USER_LIST" + peerNames).getBytes();
							}
							userList.add(userName);
							dgPacket = new DatagramPacket(data, data.length, clientAddress, clientPort);
							try {
								//Thread.sleep(100);//�ȴ��ͻ��˴ӵ�½��������û�����
								dgSocket.send(dgPacket);//��֪���û������û�������Ϣ
							} catch (IOException e) {
								e.printStackTrace();
							} 
							return;
						}
						data = "WELCOME".getBytes();
						System.out.println("�����û�:" + userName + "  IP:" + clientIP + "  Port:" + clientPort);					
					} else {	//�������
						System.out.println("�û���½ʧ�ܣ��������");
						data = "WRONG_PASSWD".getBytes();
						dgPacket = new DatagramPacket(data, data.length, clientAddress, clientPort);
						try {
							dgSocket.send(dgPacket);
						} catch (IOException e) {
							e.printStackTrace();
						}
						return;
					}
				} else {		//�û������ڣ�ע�����û�
					userInfo.put(userName, userPassword);
					data = "WELCOME_NEW".getBytes();
					System.out.println("��ע���û�:" + userName + "  IP:" + clientIP + "  Port:" + clientPort);
				}
				clientCounter++;
				System.out.println("��ǰ��¼�û�������" + clientCounter);
				clientInfo.put(userName, new ClientInfo(userName, clientIP, clientPort));
				//userList.add(userName);
				dgPacket = new DatagramPacket(data, data.length, clientAddress, clientPort);
				try {
					dgSocket.send(dgPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				/*
				 * ����ϵͳ��½�û�״̬�������пͻ��˷��͸�����Ϣ
				 */
				if(userList.isEmpty()) {
					data = "USER_EMPTY".getBytes();
				} else {
					String peerNames = "";	//���¼���ͻ��˵���Ϣ
					data = ("ServerAddUser--" + userName).getBytes();	//��֪�����û����û��ļ���
					for (String name:userList) {
						try {
							dgPacket = new DatagramPacket(data, data.length, 
									InetAddress.getByName(clientInfo.get(name).getClientIP()), 
									clientInfo.get(name).getClientPort());
							dgSocket.send(dgPacket);
						} catch (IOException e) {
							e.printStackTrace();
						}
						peerNames += "--" + name;
					}
					data = ("USER_LIST" + peerNames).getBytes();
				}
				userList.add(userName);
				dgPacket = new DatagramPacket(data, data.length, clientAddress, clientPort);
				try {
					//Thread.sleep(100);//�ȴ��ͻ��˴ӵ�½��������û�����
					dgSocket.send(dgPacket);//��֪���û������û�������Ϣ
				} catch (IOException e) {
					e.printStackTrace();
				} 
//				catch (InterruptedException e) {
//					e.printStackTrace();
//				} 
				System.out.println("--------------------\n");
			}
		}

		class ConnectPeerThread extends Thread {
			private String sourceName;
			private String destinationName;
			
			public ConnectPeerThread(String sourceName,String destinationName) {
				this.sourceName = sourceName;
				this.destinationName = destinationName;
			}

			@Override
			public void run() {
				clientInfo.put(sourceName, new ClientInfo(sourceName, clientIP, clientPort));  //���¿ͻ���������Ϣ
				System.out.println("  " + sourceName + "  �������ӣ� " + destinationName);
				ClientInfo peerInfo = clientInfo.get(destinationName);
				if (peerInfo != null) {
					String peerID = peerInfo.getClientID();
					String peerIP = peerInfo.getClientIP();
					String peerPort = "" + peerInfo.getClientPort();
					byte[] data = ("PEER_INFO--" + peerID + "--" + peerIP + "--" + peerPort).getBytes();
					DatagramPacket dgPacket = new DatagramPacket(data, data.length, clientAddress, clientPort);		
					try {
						InetAddress pAddress = InetAddress.getByName(peerIP);
						byte[] pData = ("REQUEST_PEER_INFO--" +sourceName+ "--" + clientIP + "--" + clientPort).getBytes();
						DatagramPacket pPacket = new DatagramPacket(pData, pData.length, pAddress, peerInfo.getClientPort());
						Thread.sleep(800);
						dgSocket.send(dgPacket);
						System.out.println(sourceName + "��" + dgPacket.getAddress().getHostAddress()+"  "+dgPacket.getPort());
						dgSocket.send(pPacket);
						System.out.println(destinationName + "��" + pPacket.getAddress().getHostAddress()+"  "+pPacket.getPort());
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("----ͨ�ŵ�ַ��Ϣת�����");
				} else {
					data = ("PEER_NOT_FOUND").getBytes();
					dgPacket = new DatagramPacket(data, data.length, clientAddress, clientPort);
					try {
						dgSocket.send(dgPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("----δ�ҵ���Ӧ�û�");
				}
				System.out.println("------------------------------\n");
			}
		}
		
		private void RemoveUser(String userName) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					clientInfo.remove(userName);
					userList.remove(userName);
					System.out.println("  " + userName + "  �˳�ϵͳ\n��ǰ�û�������" + (--clientCounter) + "\n---------------\n");
					Set<Map.Entry<String,ClientInfo>> set = clientInfo.entrySet();
					data = ("ServerRemoveUser--" + userName).getBytes();
					for(Iterator<Map.Entry<String,ClientInfo>> iterator = set.iterator();iterator.hasNext();) {
		                Map.Entry<String,ClientInfo> entry = iterator.next();
						try {
							InetAddress cAddr = InetAddress.getByName(entry.getValue().getClientIP());
							dgPacket = new DatagramPacket(data,data.length,cAddr,entry.getValue().getClientPort());
							dgSocket.send(dgPacket);
						} catch (IOException e) {
							e.printStackTrace();
						}
		            }
				}
			}).start();
		}
		
		private void transmitMsg(String source,String destination,String content) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					data = ("PeerMsg--" + source + "--" + content).getBytes();
					try {
						InetAddress addr = InetAddress.getByName(clientInfo.get(destination).getClientIP());
						dgPacket = new DatagramPacket(data, data.length, addr, clientInfo.get(destination).getClientPort());
						dgSocket.send(dgPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	// Main
	public static void main(String[] args) throws IOException {
		Server server = new Server();
		server.new ReceivingThread().start();
	}

}
