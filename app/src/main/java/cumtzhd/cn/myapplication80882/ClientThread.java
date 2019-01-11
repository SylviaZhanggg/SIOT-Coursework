package cumtzhd.cn.myapplication80882;

import android.annotation.SuppressLint;
import android.media.Ringtone;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;


@SuppressLint("HandlerLeak")
public class ClientThread extends Thread {
	private OutputStream outputStream = null;
	private InputStream inputStream = null;
	private Socket socket;
	private SocketAddress socketAddress;
	public static Handler childHandler;
	private boolean key = true;

	private RxThread rxThread;
	private Ringtone r;
	private Message message;
	private boolean falg=true;
	private String strIP;
	private int iPort;
	/*public ClientThread(PrintInterface printClass) {

		this.printClass = printClass;
	}*/
	public ClientThread(String ip, int port) {
		strIP = ip;
		iPort =port;
	}
	/**
	 * connect
	 */
	void connect() {
		key = true;
		socketAddress = new InetSocketAddress(strIP, iPort);
		socket = new Socket();

		try {

			socket.connect(socketAddress, 5000);
			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();

			message = MainActivity.mainHandler.obtainMessage(0);  //
			MainActivity.mainHandler.sendMessage(message);

			//printClass.printf("connected");


			MainActivity.mainHandler.post(new Runnable() {
				public void run() {
					//Activity1.ptext.setText("connected");


				}

			});

			rxThread = new RxThread();  //
			rxThread.start();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			//printClass.printf("failed to connect");
			message = MainActivity.mainHandler.obtainMessage(1);  //
			MainActivity.mainHandler.sendMessage(message);

			Log.d("Error", "failed to connect...");
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block

		}

	}

	void initChildHandler() {

		
		Looper.prepare();

		childHandler = new Handler() {
			/**
			 * 
			 */
			public void handleMessage(Message msg) {

				// 
				switch (msg.what) {
					case 0:
						int len = msg.arg1;
						try {
							//

							outputStream.write((byte [])msg.obj, 0, len);
							//outputStream.write(((String) (msg.obj)).getBytes());
							//	System.out.println(((String) (msg.obj)).getBytes());
							outputStream.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case 1:

						key = false;
						try {
							inputStream.close();
							outputStream.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						childHandler.getLooper().quit();// 



						break;

					default:
						break;
				}

			}
		};

		// 
		Looper.loop();

	}

	public void run() {
		connect();
		initChildHandler();
		//printClass.printf("connect to Internet");

	}

	public class RxThread extends Thread {



		private Handler handler;


		public void run() {

			//printClass.printf("Start receiving");
			byte[] buffer = new byte[1024];

			while (key) {

				try {
					int readSize = inputStream.read(buffer);
					if (readSize > 0) {
						String str = new String(buffer, 0, readSize);

						Log.d("Message:", str);
						//System.out.println("Message:"+str);
						//message.obj=str;
						message= MainActivity.mainHandler.obtainMessage(2,str);  //

						MainActivity.mainHandler.sendMessage(message);




						//	printClass.printf("<< " + str);

					} else {

						inputStream.close();
						Log.d("error:", "close connect...");
						message = MainActivity.mainHandler.obtainMessage(3);  //
						MainActivity.mainHandler.sendMessage(message);
						//	printClass.printf("Disconnect from the server");
						break;

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				if (socket.isConnected())          //key=false, switch off socket
					socket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}



		}

	}

}
