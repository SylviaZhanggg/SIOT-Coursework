package cumtzhd.cn.myapplication80882;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LineChartView lineChartView;
    private LineChartData lineChartData;		//real-time plot
    private List<Line> linesList;
    private List<PointValue> points;			//plot points
    private List<PointValue> points2;
    private List<PointValue> pointValueList;
    private List<PointValue> pointValueList2;
    private int position = 0;


    private Axis axisX;							//X axis
    private Axis axisY;							//Y axis
    private TextView t_hum;
    private TextView t_temp;
    private Button setting;
    private Button exit;
    private TextView history_button;
    private TextView textTips;
    public static Handler mainHandler;
    private ClientThread rxListenerThread;
    private String tem;
    private String thumity;
    private Message message;
    private static int i=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initAxisView();							//initialise axes
        //showMovingLineChart();					//dynamic plot
        init();
        initMainHandler();
    }

    private void init() {
        //humidity
        t_hum=(TextView) findViewById(R.id.hum);


        //temp
        t_temp=(TextView) findViewById(R.id.temp);





        //internet connection
        setting =(Button) findViewById(R.id.btn_network);
        setting.setOnClickListener(this);

        //exit
        exit =(Button) findViewById(R.id.btn_exit);
        exit.setOnClickListener(this);

        //tips
        textTips=(TextView) findViewById(R.id.tips);
    }

    protected void handledata(String data) {
        System.out.println(data);
        //#1068@
            tem = data.substring(1,3);
            thumity=data.substring(3,5);
        //if integer
        if (isInteger(tem)&&isInteger(thumity)){
            //show
            t_temp.setText(tem+"℃");
            t_hum.setText(thumity+"%");


            points.add(new PointValue(i + 1, Integer.parseInt(tem)));
            points2.add(new PointValue(i + 1, Integer.parseInt(thumity)));
            i=i+1;


            //show data
            message = mainHandler.obtainMessage(4);  
            mainHandler.sendMessage(message);
        }



        }







    private void initMainHandler() {
        mainHandler = new Handler() {
            @Override
            /**
             * 
             */
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:

                        Toast.makeText(MainActivity.this, "connected", Toast.LENGTH_LONG).show();
                        textTips.setText("");
                        break;

                    case 2://process data and display
                        String data = (String)msg.obj;
                        if (data.length()>4) {
                            //#1068@
                            points = new ArrayList<PointValue>();
                            points2 = new ArrayList<PointValue>();
                            handledata(data);
                        }
                        break;
                    case 1:

                        Toast.makeText(MainActivity.this, "Unable to connect to server", Toast.LENGTH_LONG).show();
                        break;

                    case 3:

                        Toast.makeText(MainActivity.this, "Disconnected from the server", Toast.LENGTH_LONG).show();
                        break;
                    case 4://plot

                        showMovingLineChart();
                        break;

                }

            }

        };


    }

    @Override
    public void onClick(View v) {
        if (rxListenerThread == null
                && (v.getId() != R.id.btn_exit) && (v.getId() != R.id.btn_network)
               ) {
            textTips.setText("Note:please connect to the Internet");
            return;
        }
        switch (v.getId()) {

            case R.id.btn_network://internet connection

                showDialog(MainActivity.this);
                break;
            case R.id.btn_exit://exit
                Message message = ClientThread.childHandler.obtainMessage(1);  
                ClientThread.childHandler.sendMessage(message);
                finish();
                break;

            default:
                break;
        }

    }





    /**
     * initialise axes
     */
    private void initAxisView() {

        lineChartView = (LineChartView) findViewById(R.id.line_chart);
        pointValueList = new ArrayList<PointValue>();
        pointValueList2 = new ArrayList<PointValue>();
        linesList = new ArrayList<Line>();

        /** initialise y axis */
        axisY = new Axis();
        axisY.setName("temperature and humidity.");						
        axisY.setHasLines(true);							
        axisY.setTextSize(10);								
        //        axisY.setTextColor(Color.parseColor("#AFEEEE"));	
        lineChartData = new LineChartData(linesList);
        lineChartData.setAxisYLeft(axisY);					

        /** initialise x aixs */
        axisX = new Axis();
        axisX.setHasTiltedLabels(false);  					
        //        axisX.setTextColor(Color.CYAN);  					
        axisX.setName("Time (unit: s)");  						
        axisX.setHasLines(true);							
        axisX.setTextSize(10);								
        axisX.setMaxLabelChars(1); 							
        List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();
        for (int i = 0; i < 61; i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(i+""));
        }
        axisX.setValues(mAxisXValues);  					
        lineChartData.setAxisXBottom(axisX); 				

        lineChartView.setLineChartData(lineChartData);

        Viewport port = initViewPort(0,10);					
        lineChartView.setCurrentViewportWithAnimation(port);
        lineChartView.setInteractive(false);				
        lineChartView.setScrollEnabled(true);
        lineChartView.setValueTouchEnabled(false);
        lineChartView.setFocusableInTouchMode(false);
        lineChartView.setViewportCalculationEnabled(false);
        lineChartView.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        lineChartView.startDataAnimation();


    }

    private Viewport initViewPort(float left,float right) {
        Viewport port = new Viewport();
        port.top = 100;				//Y rupper bound
        port.bottom = 0;			//Y lower bound
        port.left = left;			//X left
        port.right = right;			//X right
        return port;
    }



    /**
     * real-time plot points
     */
    private void showMovingLineChart() {


            pointValueList.add(points.get(position));        //add a now point
            pointValueList2.add(points2.get(position));        
            //plot the curve
            Line line = new Line(pointValueList);
            line.setColor(getResources().getColor(R.color.gold));        //colour
            line.setShape(ValueShape.CIRCLE);                
            line.setCubic(false);                          
            line.setHasLabels(true);                      
                       
            line.setHasLines(true);                            //show curve
            line.setHasPoints(true);                        //show points

            Line line2 = new Line(pointValueList2);
            line2.setColor(getResources().getColor(R.color.crimson));        
            line2.setShape(ValueShape.CIRCLE);                
            line2.setCubic(false);                           
            line2.setHasLabels(true);                       
           
            line2.setHasLines(true);                         
            line2.setHasPoints(true);

            linesList.add(line);
            linesList.add(line2);

            lineChartData = new LineChartData(linesList);
            lineChartData.setAxisYLeft(axisY);                    
            lineChartData.setAxisXBottom(axisX);                
            lineChartView.setLineChartData(lineChartData);

            float xAxisValue = points.get(position).getX();
            //update x range 
            Viewport port;
            if (xAxisValue > 10) {
                port = initViewPort(xAxisValue - 10, xAxisValue);
            } else {
                port = initViewPort(0, 10);
            }
            lineChartView.setMaximumViewport(port);
            lineChartView.setCurrentViewport(port);



    }


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

    }


    //show connection
    private void showDialog(Context context) {
        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mInflater.inflate(R.layout.recordlayout, null);
        LinearLayout layout = (LinearLayout) view
                .findViewById(R.id.id_recordlayout);


        TextView tv1 = new TextView(context);
        tv1.setText("IP:");
        final EditText editIP = new EditText(context);
        editIP.setText("192.168.4.1");
        layout.addView(tv1);
        layout.addView(editIP);

        TextView tv2 = new TextView(context);
        tv2.setText("port:");
        final EditText editPort = new EditText(context);
        editPort.setText("8080");
        layout.addView(tv2);
        layout.addView(editPort);


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Server settings");
        builder.setView(view);
        builder.setPositiveButton("connect", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String strIpAddr = editIP.getText().toString();
                int iPort=Integer.parseInt(editPort.getText().toString());
                boolean ret = isIPAddress(strIpAddr);

                if (ret) {
                    //textTips.setText("IP address:" + strIpAddr + ",port:"+iPort);
                } else {
                    Toast.makeText(MainActivity.this, "invalid address", Toast.LENGTH_LONG).show();
                    //textTips.setText("invalid address");
                    return;
                }
                rxListenerThread = new ClientThread(strIpAddr, iPort);//client thread
                rxListenerThread.start();
                //clientThread = new ClientThread(strIpAddr, iPort);
                //clientThread.start();

                //				if(clientThread != null && clientThread.socketConnect()){
                //					textTips.setText("start timer");
                //					mainTimer = new Timer();
                //					setTimerTask();
                //				}
            }
        });
        builder.setNeutralButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
				/*if (clientThread != null) {
					MainMsg = ClientThread.childHandler
							.obtainMessage(ClientThread.RX_EXIT);
					ClientThread.childHandler.sendMessage(MainMsg);
					//textTips.setText("disconnect from server");
				}*/
            }
        });

        builder.show();
    }
    //check IP
    private boolean isIPAddress(String ipaddr) {
        boolean flag = false;
        Pattern pattern = Pattern
                .compile("\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");
        Matcher m = pattern.matcher(ipaddr);
        flag = m.matches();
        return flag;
    }

    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }


}


