主程序：
#include <reg52.h>
#include"UART_ESP8266.h"
#include "DHT11.h"
#include"lcd.h"
#include "stdio.h"
void limitdisplay();
uchar wendu[4],shidu[3];
extern int temp_value, humi_value;
char Message[22]="#    @AT+CIPSEND=0,6\r\n";
unsigned char set=0;
unsigned char fazhi[16];

void delay_ms(int ms);

/********************************************************************
* 名称 : Main()
* 功能 : 主函数	   	EX1
* 输入 : 无
* 输出 : 无
***********************************************************************/
void main()
{ 
  LcdInit(); //LCD初始化
  // Beep=0;
   
   Uartinit();
   LCD1602_write_word("    Welcome");
    delay_ms(500); //延时1s,看欢迎界面
   LcdWriteCom(0x01);  //清屏
   ESP8266_Init();
  while(1)
   {
     ReadTempAndHumi(); //读取DHT11温湿度函数
	 LcdWriteCom(0x80);
   LCD1602_write_word(" Temp&Hum Test");
     Message[1]=temp_value/10+48; 
	 Message[2]=temp_value%10+48;
	 Message[3]=humi_value/10+48;
	 Message[4]=humi_value%10+48;
	 Send_String(Message);
	 wendu[0]=temp_value/10+48; 
	 wendu[1]=temp_value%10+48;
	 wendu[2]=0xdf;
	 wendu[3]='\0';

	 shidu[0]=humi_value/10+48;
	 shidu[1]=humi_value%10+48;
	 shidu[2]= '\0';
	 LcdWriteCom(0x80+64);
	 LCD1602_write_word("T:");
	 LCD1602_write_word(wendu);
	 LCD1602_write_word("C");

	 LcdWriteCom(0x80+0x09+64);
	 LCD1602_write_word("H:");
	 LCD1602_write_word(shidu);
	 LCD1602_write_word("%");
	 delay_ms(1000);
    }
}

void delay_ms(int ms)
{
	int i,j;
	for(i=0;i<ms;i++)
	for(j=0;j<110;j++);
}

温湿度数据采集程序：

#include "dht11.h"
#include <reg52.h>
//请根据自己的dht11 接的IO 口来改动位定义
sbit dht11 = P2^0;
//防止在与硬件通信时发生死循环的计数范围
#define NUMBER 20
#define SIZE 5
static unsigned char status;
//存放五字节数据的数组
static unsigned char value_array[SIZE];
 
/**/
int temp_value, humi_value;
static unsigned char ReadValue(void);
 
 extern void Delay_1ms(unsigned int ms)
{
	unsigned int x, y;
	for(x = ms; x > 0; x--)
	 {
		for(y = 124; y > 0; y--);
	 }
}
 
 static void Delay_10us(void)
{
	unsigned char i;
	i--;
	i--;
	i--;
	i--;
	i--;
	i--;
}

/*读一个字节的数据*/
static unsigned char ReadValue(void)
{
	unsigned char count, value = 0, i;
	status = OK; //设定标志为正常状态
	for(i = 8; i > 0; i--)
	{
		//高位在先
		value <<= 1;
		count = 0;
		//每一位数据前会有一个50us 的低电平时间.等待50us 低电平结束
		while(dht11 == 0 && count++ < NUMBER);
	
	if(count >= NUMBER)
	{
		status = ERROR; //设定错误标志
		return 0; //函数执行过程发生错误就退出函数
	}
	//26-28us 的高电平表示该位是0,为70us 高电平表该位1
	Delay_10us();
	Delay_10us();
	Delay_10us();
	//延时30us 后检测数据线是否还是高电平
	if(dht11 != 0)
	{
		//进入这里表示该位是1
		value++;
		//等待剩余(约40us)的高电平结束
		while(dht11 != 0 && count++ < NUMBER)
		  {
				dht11 = 1;
		 }
		
			if(count >= NUMBER)
			{
				status = ERROR; //设定错误标志
				return 0;
		  }
	}
	
  }
	
	return (value);

}
//读一次的数据,共五字节
unsigned char ReadTempAndHumi(void)
{
	unsigned char i = 0, check_value = 0,count = 0;
	EA = 0;
	dht11 = 0; //拉低数据线大于18ms 发送开始信号
	Delay_1ms(20); //需大于18 毫秒
	dht11 = 1; //释放数据线,用于检测低电平的应答信号
	//延时20-40us,等待一段时间后检测应答信号,应答信号是从机拉低数据线80us
	Delay_10us();
	Delay_10us();
	Delay_10us();
	Delay_10us();
	if(dht11 != 0) //检测应答信号,应答信号是低电平
	{
		//没应答信号
		EA = 1;
		return ERROR;
	}
	else
	{
		//有应答信号
		while(dht11 == 0 && count++ < NUMBER); //等待应答信号结束
		if(count >= NUMBER) //检测计数器是否超过了设定的范围
		{
			dht11 = 1;
			EA = 1;
			return ERROR; //读数据出错,退出函数
		}
		count = 0;
		dht11 = 1;//释放数据线
		//应答信号后会有一个80us 的高电平，等待高电平结束
		while(dht11 != 0 && count++ < NUMBER);
		if(count >= NUMBER)
		{
			dht11 = 1;
			EA = 1;
			return ERROR; //退出函数
		}
	//读出湿.温度值
	for(i = 0; i < SIZE; i++)
	{
		value_array[i] = ReadValue();
		if(status == ERROR)//调用ReadValue()读数据出错会设定status 为ERROR
		{
			dht11 = 1;
			EA = 1;
			return ERROR;
		}
		//读出的最后一个值是校验值不需加上去
		if(i != SIZE - 1)
		{
			//读出的五字节数据中的前四字节数据和等于第五字节数据表示成功
			check_value += value_array[i];
		}
	}	//end for
		//在没用发生函数调用失败时进行校验
		if(check_value == value_array[SIZE - 1])
		{
			//将温湿度扩大10 倍方便分离出每一位
			humi_value = value_array[0] ;
			temp_value = value_array[2] ;
			dht11 = 1;
			EA = 1;
			return OK; //正确的读出dht11 输出的数据
		}
		else
		{
		//校验数据出错
			EA = 1;
			return ERROR;
		}
	}
}
液晶显示程序：
#include"lcd.h"

/*******************************************************************************
* 函 数 名         : Lcd1602_Delay1ms
* 函数功能		   : 延时函数，延时1ms
* 输    入         : c
* 输    出         : 无
* 说    名         : 该函数是在12MHZ晶振下，12分频单片机的延时。
*******************************************************************************/

void Lcd1602_Delay1ms(uint c)   //误差 0us
{
    uchar a,b;
	for (; c>0; c--)
	{
		 for (b=199;b>0;b--)
		 {
		  	for(a=1;a>0;a--);
		 }      
	}
    	
}

/*******************************************************************************
* 函 数 名         : LcdWriteCom
* 函数功能		   : 向LCD写入一个字节的命令
* 输    入         : com
* 输    出         : 无
*******************************************************************************/
#ifndef 	LCD1602_4PINS	 //当没有定义这个LCD1602_4PINS时
void LcdWriteCom(uchar com)	  //写入命令
{
	LCD1602_E = 0;     //使能
	LCD1602_RS = 0;	   //选择发送命令
	LCD1602_RW = 0;	   //选择写入
	
	LCD1602_DATAPINS = com;     //放入命令
	Lcd1602_Delay1ms(1);		//等待数据稳定

	LCD1602_E = 1;	          //写入时序
	Lcd1602_Delay1ms(5);	  //保持时间
	LCD1602_E = 0;
}
#else 
void LcdWriteCom(uchar com)	  //写入命令
{
	LCD1602_E = 0;	 //使能清零
	LCD1602_RS = 0;	 //选择写入命令
	LCD1602_RW = 0;	 //选择写入

	LCD1602_DATAPINS = com;	//由于4位的接线是接到P0口的高四位，所以传送高四位不用改
	Lcd1602_Delay1ms(1);

	LCD1602_E = 1;	 //写入时序
	Lcd1602_Delay1ms(5);
	LCD1602_E = 0;

//	Lcd1602_Delay1ms(1);
	LCD1602_DATAPINS = com << 4; //发送低四位
	Lcd1602_Delay1ms(1);

	LCD1602_E = 1;	 //写入时序
	Lcd1602_Delay1ms(5);
	LCD1602_E = 0;
}
#endif
/*******************************************************************************
* 函 数 名         : LcdWriteData
* 函数功能		   : 向LCD写入一个字节的数据
* 输    入         : dat
* 输    出         : 无
*******************************************************************************/		   
#ifndef 	LCD1602_4PINS		   
void LcdWriteData(uchar dat)			//写入数据
{
	LCD1602_E = 0;	//使能清零
	LCD1602_RS = 1;	//选择输入数据
	LCD1602_RW = 0;	//选择写入

	LCD1602_DATAPINS = dat; //写入数据
	Lcd1602_Delay1ms(1);

	LCD1602_E = 1;   //写入时序
	Lcd1602_Delay1ms(5);   //保持时间
	LCD1602_E = 0;
}
#else
void LcdWriteData(uchar dat)			//写入数据
{
	LCD1602_E = 0;	  //使能清零
	LCD1602_RS = 1;	  //选择写入数据
	LCD1602_RW = 0;	  //选择写入

	LCD1602_DATAPINS = dat;	//由于4位的接线是接到P0口的高四位，所以传送高四位不用改
	Lcd1602_Delay1ms(1);

	LCD1602_E = 1;	  //写入时序
	Lcd1602_Delay1ms(5);
	LCD1602_E = 0;

	LCD1602_DATAPINS = dat << 4; //写入低四位
	Lcd1602_Delay1ms(1);

	LCD1602_E = 1;	  //写入时序
	Lcd1602_Delay1ms(5);
	LCD1602_E = 0;
}
#endif
/*******************************************************************************
* 函 数 名       : LcdInit()
* 函数功能		 : 初始化LCD屏
* 输    入       : 无
* 输    出       : 无
*******************************************************************************/		   
#ifndef		LCD1602_4PINS
void LcdInit()						  //LCD初始化子程序
{
 	LcdWriteCom(0x38);  //开显示
	LcdWriteCom(0x0c);  //开显示不显示光标
	LcdWriteCom(0x06);  //写一个指针加1
	LcdWriteCom(0x01);  //清屏
	LcdWriteCom(0x80);  //设置数据指针起点
}
#else
void LcdInit()						  //LCD初始化子程序
{
	LcdWriteCom(0x32);	 //将8位总线转为4位总线
	LcdWriteCom(0x28);	 //在四位线下的初始化
	LcdWriteCom(0x0c);  //开显示不显示光标
	LcdWriteCom(0x06);  //写一个指针加1
	LcdWriteCom(0x01);  //清屏
	LcdWriteCom(0x80);  //设置数据指针起点
}
#endif
//****************************************************
//连续写字符
//****************************************************
void LCD1602_write_word(unsigned char *s)
{
	while(*s>0)
	{
		LcdWriteData(*s);
		s++;
	}
}

 /* 设置显示RAM起始地址，亦即光标位置，(x,y)-对应屏幕上的字符坐标 */
void LcdSetCursor(unsigned char x, unsigned char y)
{
    unsigned char addr;
    
    if (y == 0)  //由输入的屏幕坐标计算显示RAM的地址
        addr = 0x00 + x;  //第一行字符地址从0x00起始
    else
        addr = 0x40 + x;  //第二行字符地址从0x40起始
    LcdWriteCom(addr | 0x80);  //设置RAM地址
}
