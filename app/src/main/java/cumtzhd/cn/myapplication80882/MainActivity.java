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
    private LineChartData lineChartData;		//折线图显示的数据（包括坐标上的点）
    private List<Line> linesList;
    private List<PointValue> points;			//要显示的点
    private List<PointValue> points2;
    private List<PointValue> pointValueList;
    private List<PointValue> pointValueList2;
    private int position = 0;


    private Axis axisX;							//X轴
    private Axis axisY;							//Y轴
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
        initAxisView();							//初始化坐标轴
        //showMovingLineChart();					//动态显示折线变化
        init();
        initMainHandler();
    }

    private void init() {
        //湿度
        t_hum=(TextView) findViewById(R.id.hum);


        //环境温度
        t_temp=(TextView) findViewById(R.id.temp);





        //连接网络
        setting =(Button) findViewById(R.id.btn_network);
        setting.setOnClickListener(this);

        //退出
        exit =(Button) findViewById(R.id.btn_exit);
        exit.setOnClickListener(this);

        //提示
        textTips=(TextView) findViewById(R.id.tips);
    }

    protected void handledata(String data) {
        System.out.println(data);
        //#1068@
            tem = data.substring(1,3);
            thumity=data.substring(3,5);
        //判断是否为数字
        if (isInteger(tem)&&isInteger(thumity)){
            //显示
            t_temp.setText(tem+"℃");
            t_hum.setText(thumity+"%");


            points.add(new PointValue(i + 1, Integer.parseInt(tem)));
            points2.add(new PointValue(i + 1, Integer.parseInt(thumity)));
            i=i+1;


            //加载待显示数据
            message = mainHandler.obtainMessage(4);  //通知画图
            mainHandler.sendMessage(message);
        }



        }







    private void initMainHandler() {
        mainHandler = new Handler() {
            @Override
            /**
             * 主线程消息处理中心
             */
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:

                        Toast.makeText(MainActivity.this, "链接成功", Toast.LENGTH_LONG).show();
                        textTips.setText("");
                        break;

                    case 2://处理消息并显示
                        String data = (String)msg.obj;
                        if (data.length()>4) {
                            //#1068@
                            points = new ArrayList<PointValue>();
                            points2 = new ArrayList<PointValue>();
                            handledata(data);
                        }
                        break;
                    case 1:

                        Toast.makeText(MainActivity.this, "无法连接到服务器", Toast.LENGTH_LONG).show();
                        break;

                    case 3:

                        Toast.makeText(MainActivity.this, "与服务器断开链接", Toast.LENGTH_LONG).show();
                        break;
                    case 4://画图

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
            textTips.setText("提示信息：请先连接网络");
            return;
        }
        switch (v.getId()) {

            case R.id.btn_network://网络连接

                showDialog(MainActivity.this);
                break;
            case R.id.btn_exit://退出
                Message message = ClientThread.childHandler.obtainMessage(1);  //1为子线程开退出
                ClientThread.childHandler.sendMessage(message);
                finish();
                break;

            default:
                break;
        }

    }





    /**
     * 初始化显示坐标轴
     */
    private void initAxisView() {

        lineChartView = (LineChartView) findViewById(R.id.line_chart);
        pointValueList = new ArrayList<PointValue>();
        pointValueList2 = new ArrayList<PointValue>();
        linesList = new ArrayList<Line>();

        /** 初始化Y轴 */
        axisY = new Axis();
        axisY.setName("temperature and humidity.");						//添加Y轴的名称
        axisY.setHasLines(true);							//Y轴分割线
        axisY.setTextSize(10);								//设置字体大小
        //        axisY.setTextColor(Color.parseColor("#AFEEEE"));	//设置Y轴颜色，默认浅灰色
        lineChartData = new LineChartData(linesList);
        lineChartData.setAxisYLeft(axisY);					//设置Y轴在左边

        /** 初始化X轴 */
        axisX = new Axis();
        axisX.setHasTiltedLabels(false);  					//X坐标轴字体是斜的显示还是直的，true是斜的显示
        //        axisX.setTextColor(Color.CYAN);  					//设置X轴颜色
        axisX.setName("Time (unit: s)");  						//X轴名称
        axisX.setHasLines(true);							//X轴分割线
        axisX.setTextSize(10);								//设置字体大小
        axisX.setMaxLabelChars(1); 							//设置0的话X轴坐标值就间隔为1
        List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();
        for (int i = 0; i < 61; i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(i+""));
        }
        axisX.setValues(mAxisXValues);  					//填充X轴的坐标名称
        lineChartData.setAxisXBottom(axisX); 				//X轴在底部

        lineChartView.setLineChartData(lineChartData);

        Viewport port = initViewPort(0,10);					//初始化X轴10个间隔坐标
        lineChartView.setCurrentViewportWithAnimation(port);
        lineChartView.setInteractive(false);				//设置不可交互
        lineChartView.setScrollEnabled(true);
        lineChartView.setValueTouchEnabled(false);
        lineChartView.setFocusableInTouchMode(false);
        lineChartView.setViewportCalculationEnabled(false);
        lineChartView.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        lineChartView.startDataAnimation();


    }

    private Viewport initViewPort(float left,float right) {
        Viewport port = new Viewport();
        port.top = 100;				//Y轴上限，固定(不固定上下限的话，Y轴坐标值可自适应变化)
        port.bottom = 0;			//Y轴下限，固定
        port.left = left;			//X轴左边界，变化
        port.right = right;			//X轴右边界，变化
        return port;
    }



    /**
     * 数据点动态刷新
     */
    private void showMovingLineChart() {


            pointValueList.add(points.get(position));        //实时添加新的点
            pointValueList2.add(points2.get(position));        //实时添加新的点
            //根据新的点的集合画出新的线
            Line line = new Line(pointValueList);
            line.setColor(getResources().getColor(R.color.gold));        //设置折线颜色
            line.setShape(ValueShape.CIRCLE);                //设置折线图上数据点形状为 圆形 （共有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
            line.setCubic(false);                            //曲线是否平滑，true是平滑曲线，false是折线
            line.setHasLabels(true);                        //数据是否有标注
            //                    line.setHasLabelsOnlyForSelected(true);		//点击数据坐标提示数据,设置了line.setHasLabels(true);之后点击无效
            line.setHasLines(true);                            //是否用线显示，如果为false则没有曲线只有点显示
            line.setHasPoints(true);                        //是否显示圆点 ，如果为false则没有原点只有点显示（每个数据点都是个大圆点）

            Line line2 = new Line(pointValueList2);
            line2.setColor(getResources().getColor(R.color.crimson));        //设置折线颜色
            line2.setShape(ValueShape.CIRCLE);                //设置折线图上数据点形状为 圆形 （共有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
            line2.setCubic(false);                            //曲线是否平滑，true是平滑曲线，false是折线
            line2.setHasLabels(true);                        //数据是否有标注
            //                    line.setHasLabelsOnlyForSelected(true);		//点击数据坐标提示数据,设置了line.setHasLabels(true);之后点击无效
            line2.setHasLines(true);                            //是否用线显示，如果为false则没有曲线只有点显示
            line2.setHasPoints(true);

            linesList.add(line);
            linesList.add(line2);

            lineChartData = new LineChartData(linesList);
            lineChartData.setAxisYLeft(axisY);                    //设置Y轴在左
            lineChartData.setAxisXBottom(axisX);                //X轴在底部
            lineChartView.setLineChartData(lineChartData);

            float xAxisValue = points.get(position).getX();
            //根据点的横坐标实时变换X坐标轴的视图范围
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


    //显示连接对话框，以前链接wifi需要。
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
        tv2.setText("端口:");
        final EditText editPort = new EditText(context);
        editPort.setText("8080");
        layout.addView(tv2);
        layout.addView(editPort);


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("服务器设置");
        builder.setView(view);
        builder.setPositiveButton("连接", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String strIpAddr = editIP.getText().toString();
                int iPort=Integer.parseInt(editPort.getText().toString());
                boolean ret = isIPAddress(strIpAddr);

                if (ret) {
                    //textTips.setText("IP地址:" + strIpAddr + ",端口:"+iPort);
                } else {
                    Toast.makeText(MainActivity.this, "地址不合法，请重新设置", Toast.LENGTH_LONG).show();
                    //textTips.setText("地址不合法，请重新设置");
                    return;
                }
                rxListenerThread = new ClientThread(strIpAddr, iPort);//建立客户端线程
                rxListenerThread.start();
                //clientThread = new ClientThread(strIpAddr, iPort);//建立客户端线程
                //clientThread.start();

                //				if(clientThread != null && clientThread.socketConnect()){
                //					textTips.setText("start timer");
                //					mainTimer = new Timer();//定时查询所有终端信息
                //					setTimerTask();
                //				}
            }
        });
        builder.setNeutralButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
				/*if (clientThread != null) {
					MainMsg = ClientThread.childHandler
							.obtainMessage(ClientThread.RX_EXIT);
					ClientThread.childHandler.sendMessage(MainMsg);
					//textTips.setText("与服务器断开连接");
				}*/
            }
        });

        builder.show();
    }
    //判断输入IP是否合法
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


