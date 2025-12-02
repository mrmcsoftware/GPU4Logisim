/* Copyright © 2025 Mark Craig (https://www.youtube.com/MrMcSoftware) */

/* Initial video screen code from Cornell's Logisim */

package com.mcsoftware.logisim.mygpulib;

import java.util.*;
import java.io.*;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GradientPaint;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.imageio.ImageIO;
import java.net.URL;
import javax.help.HelpSet;
import javax.help.JHelp;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
//import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.gui.generic.LFrame;


// GPU with 256 x 256 pixel LCD display with 8bpp color (byte addressed)
class GPU extends ManagedComponent implements ToolTipMaker
{
	public static final ComponentFactory factory = new Factory();

	static final String BLINK_YES = "Blinking Dot";
	static final String BLINK_NO = "No Cursor";
	static final String[] BLINK_OPTIONS = { BLINK_YES, BLINK_NO };
	static final String RESET_ASYNC = "Asynchronous";
	static final String RESET_SYNC = "Synchronous";
	static final String[] RESET_OPTIONS = { RESET_ASYNC, RESET_SYNC };
	static final String TRIG_NONE = "No Designated Trigger";
	static final String TRIG_FALLING = "Falling Edge";
	static final String TRIG_RISING = "Rising Edge";
	static final String TRIG_HIGH = "High Level";
	static final String TRIG_LOW = "Low Level";
	static final String[] TRIGGER_OPTIONS = { TRIG_NONE, TRIG_RISING, TRIG_FALLING, TRIG_HIGH, TRIG_LOW };
	static final String DEBUG_ON = "On";
	static final String DEBUG_OFF = "Off";
	static final String[] DEBUG_OPTIONS = { DEBUG_OFF, DEBUG_ON };

	public static final Attribute<?> BLINK_OPTION = Attributes.forOption("cursor",
		new MyStringGetter("Cursor"), BLINK_OPTIONS);
	public static final Attribute<?> RESET_OPTION = Attributes.forOption("reset",
		new MyStringGetter("Reset Behavior"), RESET_OPTIONS);
	public static final Attribute<?> DEBUG_OPTION = Attributes.forOption("debug",
		new MyStringGetter("Debug"), DEBUG_OPTIONS);
	public static final Attribute<?> TRIGGER_OPTION = Attributes.forOption("trigger",
		new MyStringGetter("GPU Clk Trigger"), TRIGGER_OPTIONS);
	public static final Attribute<String> ROM_FILE = Attributes.forString("romfile",
		new MyStringGetter("ROM Filename"));

	private static final Attribute[] ATTRIBUTES = { BLINK_OPTION, RESET_OPTION, TRIGGER_OPTION, DEBUG_OPTION, ROM_FILE };

	private final long GPURAMSIZE=0x100000L;
	private final long GPUROMSIZE=0x100000L;
	private final int NUM_BLITS=17; // 16 for func 10 and 1 for func 11
	private final int NUM_SPRITES=17; // 16 for func 12 and 1 for func 13
	private final int NUM_USERFONTS=16;
	private long[] gpuRAM=new long[(int)(GPURAMSIZE)];
	private long[] gpuROM=new long[(int)(GPUROMSIZE)];
	private BufferedImage[] image=new BufferedImage[NUM_BLITS];
	private BufferedImage[] sprite=new BufferedImage[NUM_SPRITES];
	private BufferedImage dbuff;
	private boolean db=false;
	private int[] blitalx=new int[NUM_BLITS];
	private int[] blitaly=new int[NUM_BLITS];
	private int[] blitw=new int[NUM_BLITS];
	private int[] blith=new int[NUM_BLITS];
	private int[] sprx=new int[NUM_SPRITES];
	private int[] spry=new int[NUM_SPRITES];
	private boolean[] spron=new boolean[NUM_SPRITES];
	private int[] spralx=new int[NUM_SPRITES];
	private int[] spraly=new int[NUM_SPRITES];
	private int[] sprw=new int[NUM_SPRITES];
	private int[] sprh=new int[NUM_SPRITES];
	private boolean sprites=false;
	private GradientPaint gradient=null;
	private int gpuCurX,gpuCurY,gpuColorVal;
	private boolean gpuXOR=false;
	private long gpuAddr;
	private Color gpuColor=new Color(0xffffff),gpuColorXOR;
	private float[] dashingPattern1={2f,2f};
	private float[] dashingPattern2={10f,4f};
	private Stroke stroke=new BasicStroke(1f);
	private Font sf=new Font("Arial",Font.BOLD,12);
	private Font sf2=new Font("Courier New",Font.PLAIN,12);
	private Font[] sfu=new Font[NUM_USERFONTS];
	//private Font sf2=new Font("Courier",Font.PLAIN,12);
	//private Font sf2=new Font("Monospaced",Font.PLAIN,13);
	private Font sf3=new Font("Tahoma",Font.PLAIN,11);
	// I thought I would need a stack, but I don't since
	// I'm using the natural local variable stack nature of
	// recursion
	//private Stack<Long> gpuRunStack=new Stack<Long>();
	private boolean debug=false;
	private String debugString=null;

	private static class Factory extends AbstractComponentFactory
	{
		private Factory() { }

		public String getName() { return "GPU:LCD Video 256x256x24"; }

		public String getDisplayName() { return "GPU:LCD Video 256x256x24"; }

		public AttributeSet createAttributeSet()
		{
		return AttributeSets.fixedSet(ATTRIBUTES, new Object[] { BLINK_OPTIONS[0], RESET_OPTIONS[0], TRIGGER_OPTIONS[0], DEBUG_OPTIONS[0], "" });
		}

		public Component createComponent(Location loc, AttributeSet attrs) { return new GPU(loc, attrs); }

		public Bounds getOffsetBounds(AttributeSet attrs) { return Bounds.create(-270, -140, 270, 270); }

		public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs)
		{
		drawVideoIcon(context, x, y);
		}
	}

	static final BitWidth EIGHT = BitWidth.create(8);
	static final BitWidth BPP = BitWidth.create(24);
	static final BitWidth GSEL = BitWidth.create(2);
	static final BitWidth GDATA = BitWidth.create(32);

	static final int P_CLK = 0;
	static final int P_WE = 1;
	static final int P_X = 2;
	static final int P_Y = 3;
	static final int P_DATA = 4;
	static final int P_RST = 5;
	static final int P_GPU_SEL = 6;
	static final int P_GPU_DATAI = 7;
	static final int P_GPU_DATAO = 8;
	static final int P_GPU_ADDR = 9;

	private GPU(Location loc, AttributeSet attrs)
	{
	super(loc, attrs, 10); // 10 is number of ends
	setEnd(P_CLK, getLocation().translate(-220, 130), BitWidth.ONE, EndData.INPUT_ONLY);
	setEnd(P_WE, getLocation().translate(-200, 130), BitWidth.ONE, EndData.INPUT_ONLY);
	setEnd(P_X, getLocation().translate(-140, 130), EIGHT, EndData.INPUT_ONLY);
	setEnd(P_Y, getLocation().translate(-130, 130), EIGHT, EndData.INPUT_ONLY);
	setEnd(P_DATA, getLocation().translate(-120, 130), BPP, EndData.INPUT_ONLY);
	setEnd(P_RST, getLocation().translate(-240, 130), BitWidth.ONE, EndData.INPUT_ONLY);
	setEnd(P_GPU_SEL, getLocation().translate(0, 90), GSEL, EndData.INPUT_ONLY);
	setEnd(P_GPU_DATAI, getLocation().translate(0, 100), GDATA, EndData.INPUT_ONLY);
	setEnd(P_GPU_DATAO, getLocation().translate(0, 110), GDATA, EndData.OUTPUT_ONLY);
	setEnd(P_GPU_ADDR, getLocation().translate(0, 80), BPP, EndData.INPUT_ONLY);
	debug=attrs.getValue(DEBUG_OPTION).equals("On");
	if (!attrs.getValue(ROM_FILE).equals(""))
		{
		try { openHex(true,new File(attrs.getValue(ROM_FILE))); }
		catch (IOException e)
			{
			JOptionPane.showMessageDialog(null, e.getMessage(),
				"Load Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public ComponentFactory getFactory() { return factory; }

	Location loc(int pin) { return getEndLocation(pin); }
	Value val(CircuitState s, int pin) { return s.getValue(loc(pin)); }
	int addr(CircuitState s, int pin) { return (int)(val(s, pin).toLongValue()); }

	public void propagate(CircuitState circuitState)
	{
	State state = getState(circuitState);
	AttributeSet attrs = getAttributeSet();
	int x = addr(circuitState, P_X);
	int y = addr(circuitState, P_Y);
	int color = addr(circuitState, P_DATA);
	state.last_x = x;
	state.last_y = y;
	state.color = color;
	int gpusel = addr(circuitState, P_GPU_SEL);
	Value clk=val(circuitState, P_CLK);
	int i;

	Object reset_option = attrs.getValue(RESET_OPTION);
	if (reset_option == null) reset_option = RESET_OPTIONS[0];
	Object trigger_option = attrs.getValue(TRIGGER_OPTION);
	if (trigger_option == null) trigger_option = TRIGGER_OPTIONS[0];
	debug=attrs.getValue(DEBUG_OPTION).equals("On");

	if (state.tick(clk) && val(circuitState, P_WE) == Value.TRUE)
		{
		Graphics g = state.img.getGraphics();
		// Both drawLine and setRGB work.  Surprisingly,
		// there doesn't seem to be any difference in speed
		g.setColor(new Color(state.img.getColorModel().getRGB(color)));
		g.drawLine(x, y, x, y);
		//state.img.setRGB(x,y,color);
		if (RESET_SYNC.equals(reset_option) && val(circuitState, P_RST) == Value.TRUE)
			{
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 256, 256);
			sprites=false; for (i=0;i<NUM_SPRITES;i++) { spron[i]=false; spralx[i]=spraly[i]=0; }
			for (i=0;i<NUM_BLITS;i++) { blitalx[i]=blitaly[i]=0; }
			stroke=new BasicStroke(1f);
			gradient=null;
			}
		}

	if (!RESET_SYNC.equals(reset_option) && val(circuitState, P_RST) == Value.TRUE)
		{
		Graphics g = state.img.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 256, 256);
		sprites=false; for (i=0;i<NUM_SPRITES;i++) { spron[i]=false; spralx[i]=spraly[i]=0; }
		for (i=0;i<NUM_BLITS;i++) { blitalx[i]=blitaly[i]=0; }
		stroke=new BasicStroke(1f);
		gradient=null;
		}

	if (state.tickGPU(clk,trigger_option) && gpusel >= 1)
		{
		int Addr = addr(circuitState, P_GPU_ADDR);
		if (Addr!=-1) { /* System.out.printf("Address: %x\n",Addr); */ gpuAddr=(long)Addr; }
		int gpudata = addr(circuitState, P_GPU_DATAI);
		long mem;
		switch (gpusel)
			{
			case 1: if (debug) { gpuDebug(String.format("set GPUmem address to %x",gpudata)); }
				gpuAddr=gpudata; break;
			case 2: if (debug) { gpuDebug(String.format("set GPUmem at %x to %x",gpuAddr,gpudata)); }
				if (gpuAddr<0x800000) { gpuRAM[(int)(gpuAddr)]=gpudata; } break;
			case 3: /* System.out.printf("gpu func %x\n",gpudata); */
				gpuFunc(state.img,gpudata); break;
			}
		if (gpuAddr>=0x800000) { mem=gpuROM[(int)(gpuAddr-0x800000)]; }
		else { mem=gpuRAM[(int)(gpuAddr)]; }
		circuitState.setValue(loc(P_GPU_DATAO),Value.createKnown(GDATA,(int)mem),this,3);
		}
	}

	public void draw(ComponentDrawContext context)
	{
	Location loc = getLocation();
	int size = getBounds().getWidth();
	State s = getState(context.getCircuitState());
	AttributeSet attrs = getAttributeSet();
	drawVideo(context, loc.getX(), loc.getY(), s, 
		attrs.getValue(BLINK_OPTION), attrs.getValue(RESET_OPTION));
	}

	static void drawVideoIcon(ComponentDrawContext context, int x, int y)
	{
	Graphics g = context.getGraphics();
	g.setColor(Color.BLACK);
	g.drawRoundRect(x+0,y+0,16-1,16-1,3,3);
	g.setColor(Color.BLUE);
	g.fillRect(x+3,y+3,10,10);
	g.setColor(Color.BLACK);
	}

	boolean blink()
	{
	long now = System.currentTimeMillis();
	return (now/1000 % 2 == 0);
	}

	void drawVideo(ComponentDrawContext context, int x, int y, State state, Object blink_option, Object reset_option)
	{
	Graphics g = context.getGraphics();

	x += -270;
	y += -140;

	g.drawRoundRect(x, y, 270-1, 270-1, 6, 6);
	for (int i = P_CLK+1; i <= P_GPU_ADDR; i++)
		context.drawPin(this, i);
	g.drawRect(x+6, y+6, 258-1, 258-1);
	context.drawClock(this, P_CLK, Direction.NORTH);
	if ((debug)&&(debugString!=null)) { g.drawString(debugString,x,y-3); }
	if (sprites)
		{
		BufferedImage tempscr=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
		Graphics g3=tempscr.getGraphics();
		g3.drawImage(state.img, 0, 0, null);
		for (int i=1;i<NUM_SPRITES;i++)
			{
			if (spron[i])
				{
				g3.drawImage(sprite[i], sprx[i], spry[i], null);
				}
			}
		g.drawImage(tempscr, x+7, y+7, null);
		g3.dispose();
		}
	else
		{
		g.drawImage(state.img, x+7, y+7, null);
		}
	// draw a little cursor for sanity
	if (blink_option == null) blink_option = BLINK_OPTIONS[0];
	if (BLINK_YES.equals(blink_option) && blink())
		{
		g.setColor(new Color(state.img.getColorModel().getRGB(state.color)));
		g.drawLine(x+7+state.last_x, y+7+state.last_y, x+7+state.last_x, y+7+state.last_y);
		}
	}

/* My load RAM/ROM routine.  No longer needed since I replaced
   It with a modified version of Logisim's hex open routine

	private void loadGpuMemory(boolean ROM,String str)
	{
	int hasstar,r=0,i;
	long val=0,n=0;

//	str=null;
//	JFileChooser fc=new JFileChooser(direct);
//	int result=fc.showOpenDialog(frame);
//	if (result==JFileChooser.APPROVE_OPTION)
//		{
//		direct=fc.getCurrentDirectory().getPath();
//		str=fc.getSelectedFile().getPath();
//		}
//	else { return; }
//	if (str==null) return;
	File fp=new File(str);
	try
		{
		r=0;
		Scanner scan=new Scanner(fp);
		scan.nextLine();
		while (scan.hasNext())
			{
			str=scan.next();
			if (str.charAt(0)=='#') { scan.nextLine(); continue; }
			hasstar=0;
			for (i=0;i<str.length();i++)
				{
				if (str.charAt((int)i)=='*')
					{
					String strn=str.substring(0,(int)(i));
					n=Long.parseLong(strn);
					strn=str.substring((int)(i+1));
					val=Long.parseLong(strn,16);
					//System.out.printf("'%s': n=%d val=%x\n",str,n,val);
					hasstar=1;
					break;
					}
				}
			if (ROM)
				{
				if (hasstar==1) { for (i=0;i<n;i++) { gpuROM[r+(int)i]=val; } r+=n; } // Need to check limit
				else { gpuROM[r]=Long.parseLong(str,16); r++; }
				if (r>=GPUROMSIZE) { JOptionPane.showMessageDialog(null,"GPU ROM data too big: Truncated at "+GPUROMSIZE+" memory elements.","Load Error",JOptionPane.ERROR_MESSAGE); break; }
				}
			else
				{
				if (hasstar==1) { for (i=0;i<n;i++) { gpuRAM[r+(int)i]=val; } r+=n; } // Need to check limit
				else { gpuRAM[r]=Long.parseLong(str,16); r++; }
				if (r>=GPURAMSIZE) { JOptionPane.showMessageDialog(null,"GPU RAM data too big: Truncated at "+GPURAMSIZE+" memory elements." ,"Load Error",JOptionPane.ERROR_MESSAGE); break; }
				}
			}
		}
	catch (IOException ex)
		{ 
		JOptionPane.showMessageDialog(null,ex.getMessage(),"Load Error",JOptionPane.ERROR_MESSAGE);
		return;
		}
	}
*/

/*
func (R*M means RAM/ROM)
0 Clear screen
1 set current color RGB
2 line (using R*M) PosxPosy
3 moveto (not using R*M) XsYs
4 lineto (not using R*M) XeYe
5 polyline (using R*M) PosxPosy
6 polygon(closed) (using R*M) PosxPosy
7 filled polygon (using R*M) PosxPosy
8 text (using R*M) FontPosxPosy
9 text (not using R*M, one character) CharPosxPosy
a create blit src (using R*M) Num
b blit (using R*M) NumPosxPosy (if Num>0 use already created blit src)
c create sprite src (using R*M) Num
d sprite (using R*M) NumPosxPosy (if Num>0 use already created sprite src)
e sprites on/off On
f oval/circle (using R*M) PosxPosy
10 filled oval/circle (using R*M) PosxPosy
11 Set pixel PosxPosy (using current color)
12 Run program (using R*M) Address
13 End program (using R*M)
14 lineV (using R*M) PsPe
15 arc (using R*M) PosxPosy
16 filled Arc (using R*M) PosxPosy
17 round rectangle (using R*M) PosxPosy
18 filled round rectangle (using R*M) PosxPosy
19 stroke type DashesWidth
1a start double buffer
1b end double buffer
1c Set XOR mode for drawing RGB
1d Set blit alignment NumXalignYalign
1e Set sprite alignment NumXalignYalign
1f Set gradient paint (using R*M) On
20 Create user font (using R*M) Num
ff Reset GPU

R*M format
----------
blit and sprite: WidthHeight width*height-elements
line: XsYsXeYe
polygon: Num XY*
text: Char* (ends when Char is 0)
oval: XradYradXposYpos
arc: XradYradXposYpos AnglestartAngleextent
round rectangle: WidthHeightArcwidthArcheight
gradient paint: X1Y1X2Y2 Alpha1Red1Green1Blue1 Alpha2Red2Green2Blue2 Cyclic
font: StyleSize Fontname(ends with 0)
      (Style: plain 0, bold 1, italic 2, bold-italic 3)
*/

	private void gpuFunc(BufferedImage vidscr,long val)
	{
	int func,b1,b2,b3,x0,y0,x1,y1,w,h,addr,i;
	long val2,val3,val4;
	b3=(int)(val&0x000000ffL);
	b2=(int)((val>>8)&0x000000ffL);
	b1=(int)((val>>16)&0x000000ffL);
	func=(int)((val>>24)&0x000000ffL);
	//System.out.printf("%x (%x %x %x %x)\n",val,func,b1,b2,b3);
	//System.out.printf("Func: %d\n",func);
	Graphics2D g2;
	if (db) { g2=(Graphics2D)(dbuff.getGraphics()); }
	else { g2=(Graphics2D)(vidscr.getGraphics()); }
	//g2.setColor(new Color((int)r,(int)g,(int)b));
	g2.setColor(gpuColor);
	g2.setStroke(stroke);
	if (gradient!=null) { g2.setPaint(gradient); }
	if (gpuXOR) { g2.setXORMode(gpuColorXOR); }
	//g2.fillRect(0,0,vidx,vidy);
	String str;
	int[] pa,px,py;
	int n;
	switch (func)
		{
		case 0: if (debug) { gpuDebug(String.format("Clear screen (Color: %x))",(int)val)); }
			g2.setColor(new Color((int)val)); g2.fillRect(0, 0, 256, 256); break;
		case 1: gpuColorVal=(int)(val&0xffffffL); if (debug) { gpuDebug(String.format("Set color to %x",gpuColorVal)); }
			gpuColor=new Color(gpuColorVal); break;
		case 2:
			if (gpuAddr>=0x800000L) { val=gpuROM[(int)(gpuAddr-0x800000L)]; }
			else { val=gpuRAM[(int)(gpuAddr)]; }
			y1=(int)(val&0x000000ffL)+b3;
			x1=(int)((val>>8)&0x000000ffL)+b2;
			y0=(int)((val>>16)&0x000000ffL)+b3;
			x0=(int)((val>>24)&0x000000ffL)+b2;
			if (debug) { gpuDebug(String.format("Draw line from %d,%d to %d,%d",x0,y0,x1,y1)); }
			g2.drawLine(x0,y0,x1,y1);
			break;
		case 3: if (debug) { gpuDebug(String.format("Set current position to %d,%d",b2,b3)); }
			gpuCurX=b2; gpuCurY=b3; break;
		case 4: if (debug) { gpuDebug(String.format("Draw line (from %d,%d) to %d,%d",gpuCurX,gpuCurY,b2,b3)); }
			g2.drawLine(gpuCurX,gpuCurY,b2,b3); gpuCurX=b2; gpuCurY=b3; break;
		case 5:
		case 6:
		case 7:
			//Polygon p=new Polygon();
			//p.addPoint(x,y);
			//g2.fillPolygon(p);
			if (gpuAddr>=0x800000L)
				{
				addr=(int)(gpuAddr-0x800000L);
				n=(int)gpuROM[addr]; addr++;
				px=new int[n]; py=new int[n];
				for (i=0;i<n;i++)
					{
					py[i]=(int)(gpuROM[addr]&0x000000ffL)+b3;
					px[i]=(int)((gpuROM[addr]>>8)&0x000000ffL)+b2;
					addr++;
					}
				}
			else
				{
				addr=(int)(gpuAddr);
				n=(int)gpuRAM[addr]; addr++;
				px=new int[n]; py=new int[n];
				for (i=0;i<n;i++)
					{
					py[i]=(int)(gpuRAM[addr]&0x000000ffL)+b3;
					px[i]=(int)((gpuRAM[addr]>>8)&0x000000ffL)+b2;
					addr++;
					}
				}
			switch (func)
				{
				case 5: g2.drawPolyline(px,py,n); break;
				case 6: g2.drawPolygon(px,py,n); break;
				case 7: g2.fillPolygon(px,py,n); break;
				}
			if ((debug)&&(n>0)) { gpuDebug(String.format("Draw poly* Num: %d 1st point: %d,%d filled: %b",n,px[0],py[0],(func==7))); }
			break;
		case 8: if (b1==0) { g2.setFont(sf); } else if (b1==1) { g2.setFont(sf2); } else if (b1==2) { g2.setFont(sf3); }
			else if ((b1>=3)&&(b1<3+NUM_USERFONTS)) { g2.setFont(sfu[b1-3]); }
			str="";
			if (gpuAddr>0x800000L)
				{
				addr=(int)(gpuAddr-0x800000L);
				//System.out.printf("Read string from ROM: %d\n",addr);
				while (true)
					{
					//System.out.printf("%d (%s) ",gpuROM[addr],Character.toString((char)gpuROM[addr]));
					if (gpuROM[addr]!=0) { str+=Character.toString((char)gpuROM[addr]); addr++; }
					else { break; }
					if (str.length()>256) { break; } // MARK
					}
				}
			else
				{
				addr=(int)(gpuAddr);
				//System.out.printf("Read string from RAM: %d\n",addr);
				while (true)
					{
					//System.out.printf("%d (%s) ",gpuRAM[addr],Character.toString((char)gpuRAM[addr]));
					if (gpuRAM[addr]!=0) { str+=Character.toString((char)gpuRAM[addr]); addr++; }
					else { break; }
					if (str.length()>256) { break; } // MARK
					}
				}
			if (debug) { gpuDebug(String.format("Draw '%s' at %d,%d using font: %d",str,b2,b3,b1)); }
			g2.drawString(str,b2,b3); break;
		case 9: g2.setFont(sf2);
			str=Character.toString((char)b1);
			if (debug) { gpuDebug(String.format("Draw '%s' at %d,%d",str,b2,b3)); }
			g2.drawString(str,b2,b3); break;
		case 11:
			if (b1>0)
				{
				if (blitalx[b1]==1) { b2-=blitw[b1]; }
				else if (blitalx[b1]==2) { b2-=(blitw[b1]>>1); }
				if (blitaly[b1]==1) { b3-=blith[b1]; }
				else if (blitaly[b1]==2) { b3-=(blith[b1]>>1); }
				if (debug) { gpuDebug(String.format("Blit #%d to %d,%d",b1,b2,b3)); }
				g2.drawImage(image[b1],b2,b3,null);
				break;
				}
		case 10:
			if (gpuAddr>0x800000L)
				{
				addr=(int)(gpuAddr-0x800000L);
				h=(int)(gpuROM[addr]&0x000000ffL)+1;
				w=(int)((gpuROM[addr]>>8)&0x000000ffL)+1;
				addr++;
				//System.out.printf("%d x %d\n",w,h);
				pa=new int[w*h];
				for (i=0;i<pa.length;i++) { pa[i]=(int)gpuROM[addr+i]; }
				}
			else
				{
				addr=(int)(gpuAddr);
				h=(int)(gpuRAM[addr]&0x000000ffL)+1;
				w=(int)((gpuRAM[addr]>>8)&0x000000ffL)+1;
				addr++;
				//System.out.printf("%d x %d\n",w,h);
				//if (w>256) { w=256; } if (h>256) { h=256; }
				pa=new int[w*h];
				for (i=0;i<pa.length;i++) { pa[i]=(int)gpuRAM[addr+i]; }
				}
			if (func==11)
				{
				image[0]=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
				image[0].setRGB(0,0,w,h,pa,0,w);
				if (blitalx[0]==1) { b2-=w; }
				else if (blitalx[0]==2) { b2-=(w>>1); }
				if (blitaly[0]==1) { b3-=h; }
				else if (blitaly[0]==2) { b3-=(h>>1); }
				if (debug) { gpuDebug(String.format("Blit to %d,%d",b2,b3)); }
				g2.drawImage(image[0],b2,b3,null);
				}
			else
				{
				image[b3]=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
				image[b3].setRGB(0,0,w,h,pa,0,w);
				blitw[b3]=w; blith[b3]=h;
				if (debug) { gpuDebug(String.format("Create blit image %d (%dx%d)",b3,w,h)); }
				}
			break;
		case 13:
			if (b1>0)
				{
				//g2.drawImage(sprite[b1],b2,b3,null);
				if ((b2==255)&&(b3==255)) { spron[b1]=false; if (debug) { gpuDebug(String.format("Turn off sprite #%d",b1)); } }
				else
					{
					if (spralx[b1]==1) { b2-=sprw[b1]; }
					else if (spralx[b1]==2) { b2-=(sprw[b1]>>1); }
					if (spraly[b1]==1) { b3-=sprh[b1]; }
					else if (spraly[b1]==2) { b3-=(sprh[b1]>>1); }
					sprx[b1]=b2; spry[b1]=b3; spron[b1]=true; sprites=true;
					if (debug) { gpuDebug(String.format("Move sprite #%d to %d,%d",b1,b2,b3)); }
					}
				break;
				}
		case 12:
			if (gpuAddr>0x800000L)
				{
				addr=(int)(gpuAddr-0x800000L);
				h=(int)(gpuROM[addr]&0x000000ffL)+1;
				w=(int)((gpuROM[addr]>>8)&0x000000ffL)+1;
				addr++;
				//System.out.printf("%d x %d\n",w,h);
				pa=new int[w*h];
				for (i=0;i<pa.length;i++) { pa[i]=(int)gpuROM[addr+i]; }
				}
			else
				{
				addr=(int)(gpuAddr);
				h=(int)(gpuRAM[addr]&0x000000ffL)+1;
				w=(int)((gpuRAM[addr]>>8)&0x000000ffL)+1;
				addr++;
				//System.out.printf("%d x %d\n",w,h);
				//if (w>256) { w=256; } if (h>256) { h=256; }
				pa=new int[w*h];
				for (i=0;i<pa.length;i++) { pa[i]=(int)gpuRAM[addr+i]; }
				}
			if (func==13)
				{
				sprite[0]=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
				sprite[0].setRGB(0,0,w,h,pa,0,w);
				if (spralx[0]==1) { b2-=w; }
				else if (spralx[0]==2) { b2-=(w>>1); }
				if (spraly[0]==1) { b3-=h; }
				else if (spraly[0]==2) { b3-=(h>>1); }
				if (debug) { gpuDebug(String.format("Draw sprite at %d,%d",b2,b3)); }
				g2.drawImage(sprite[0],b2,b3,null);
				}
			else
				{
				sprite[b3]=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
				sprite[b3].setRGB(0,0,w,h,pa,0,w);
				sprw[b3]=w; sprh[b3]=h;
				if (debug) { gpuDebug(String.format("Create Sprite image %d (%dx%d)",b3,w,h)); }
				}
			break;
		case 14: if (b3==0) { sprites=false; } else { sprites=true; }
			if (debug) { gpuDebug(String.format("Sprites on: %b",sprites)); } break;
		case 15:
		case 16:
			if (gpuAddr>=0x800000L) { val=gpuROM[(int)(gpuAddr-0x800000L)]; }
			else { val=gpuRAM[(int)(gpuAddr)]; }
			y1=(int)(val&0x000000ffL)+b3;
			x1=(int)((val>>8)&0x000000ffL)+b2;
			y0=(int)((val>>16)&0x000000ffL);
			x0=(int)((val>>24)&0x000000ffL);
			if (debug) { gpuDebug(String.format("Draw oval: xr: %d yr: %d xp: %d yp: %d filled: %b",x0,y0,x1,y1,(func==16))); }
			if (func==15) { g2.drawOval(x1-x0,y1-y0,x0*2,y0*2); }
			else { g2.fillOval(x1-x0,y1-y0,x0*2,y0*2); }
			break;
		case 17: if (debug) { gpuDebug(String.format("Set pixel %d,%d to %x",b2,b3,(int)gpuColorVal)); }
			if (db) { dbuff.setRGB(b2,b3,(int)gpuColorVal); }
			else { vidscr.setRGB(b2,b3,(int)gpuColorVal); }
			break;
		case 18: if (debug) { gpuDebug(String.format("Run Program at: %x",(int)(val&0x00ffffffL))); }
			gpuRun(vidscr,val&0x00ffffffL); break;
		case 19: if (debug) { gpuDebug("End Program"); } break;
		case 20:
			if (gpuAddr+b2>=0x800000L) { val=gpuROM[(int)(gpuAddr+b2-0x800000L)]; }
			else { val=gpuRAM[(int)(gpuAddr+b2)]; }
			y0=(int)(val&0x000000ffL);
			x0=(int)((val>>8)&0x000000ffL);
			if (gpuAddr+b3>=0x800000L) { val=gpuROM[(int)(gpuAddr+b3-0x800000L)]; }
			else { val=gpuRAM[(int)(gpuAddr+b3)]; }
			y1=(int)(val&0x000000ffL);
			x1=(int)((val>>8)&0x000000ffL);
			if (debug) { gpuDebug(String.format("Draw line from %d,%d to %d,%d",x0,y0,x1,y1)); }
			g2.drawLine(x0,y0,x1,y1);
			break;
		case 21:
		case 22:
			if (gpuAddr>=0x800000L) { addr=(int)(gpuAddr-0x800000L); val=gpuROM[addr]; val2=gpuROM[addr+1]; }
			else { addr=(int)(gpuAddr); val=gpuRAM[addr]; val2=gpuRAM[addr+1]; }
			y1=(int)(val&0x000000ffL)+b3;
			x1=(int)((val>>8)&0x000000ffL)+b2;
			y0=(int)((val>>16)&0x000000ffL);
			x0=(int)((val>>24)&0x000000ffL);
			if (debug) { gpuDebug(String.format("Arc: xr: %d yr: %d xp: %d yp: %d as: %d ae: %d filled: %b",x0,y0,x1,y1,(int)((val2>>16)&0x0000ffffL),(int)((val2)&0x0000ffffL),(func==22))); }
			if (func==21) { g2.drawArc(x1-x0,y1-y0,x0*2,y0*2,(int)((val2>>16)&0x0000ffffL),(int)((val2)&0x0000ffffL)); }
			else { g2.fillArc(x1-x0,y1-y0,x0*2,y0*2,(int)((val2>>16)&0x0000ffffL),(int)((val2)&0x0000ffffL)); }
			break;
		case 23:
		case 24:
			if (gpuAddr>=0x800000L) { val=gpuROM[(int)(gpuAddr-0x800000L)]; }
			else { val=gpuRAM[(int)(gpuAddr)]; }
			y1=(int)(val&0x000000ffL);
			x1=(int)((val>>8)&0x000000ffL);
			y0=(int)((val>>16)&0x000000ffL);
			x0=(int)((val>>24)&0x000000ffL);
			if (debug) { gpuDebug(String.format("RoundRect: xp: %d yp: %d w: %d h: %d aw: %d ah: %d filled: %b",b2,b3,x0,y0,x1,y1,(func==24))); }
			if (func==23) { g2.drawRoundRect(b2,b3,x0,y0,x1,y1); }
			else { g2.fillRoundRect(b2,b3,x0,y0,x1,y1); }
			break;
		case 25:
			switch (b2)
				{
				case 0: stroke=new BasicStroke((float)(b3)); break;
				case 1: stroke=new BasicStroke((float)(b3),BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER,1.0f,dashingPattern1,2.0f); break;
				case 2: stroke=new BasicStroke((float)(b3),BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER,1.0f,dashingPattern2,0.0f); break;
				default: return;
				}
			if (debug) { gpuDebug(String.format("Set stroke: type: %d width: %d",b2,b3)); }
			g2.setStroke(stroke);
			break;
		case 26: if (debug) { gpuDebug("Start double buffer"); }
			dbuff=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB); db=true; break;
		case 27: if (debug) { gpuDebug("End double buffer"); }
			g2=(Graphics2D)(vidscr.getGraphics()); g2.drawImage(dbuff,0,0,null); db=false; break;
		case 28: if ((b1==0)&&(b2==0)&&(b3==0)) { gpuXOR=false; } else { gpuColorXOR=new Color((b1<<16)|(b2<<8)|b3); gpuXOR=true; }
			if (debug) { gpuDebug(String.format("XOR mode: %b",gpuXOR)); } break;
		case 29: if (debug) { gpuDebug(String.format("Set blit #%d alignment x: %d y: %d",b1,b2,b3)); }
			blitalx[b1]=b2; blitaly[b1]=b3; break;
		case 30: if (debug) { gpuDebug(String.format("Set sprite #%d alignment x: %d y: %d",b1,b2,b3)); }
			spralx[b1]=b2; spraly[b1]=b3; break;
		case 31:
			if (b3==1)
				{
				if (gpuAddr>=0x800000L)
					{
					addr=(int)(gpuAddr-0x800000L); val=gpuROM[addr]; val2=gpuROM[addr+1]; val3=gpuROM[addr+2]; val4=gpuROM[addr+3];
					}
				else
					{
					addr=(int)(gpuAddr); val=gpuRAM[addr]; val2=gpuRAM[addr+1]; val3=gpuRAM[addr+2]; val4=gpuRAM[addr+3];
					}
				y1=(int)(val&0x000000ffL);
				x1=(int)((val>>8)&0x000000ffL);
				y0=(int)((val>>16)&0x000000ffL);
				x0=(int)((val>>24)&0x000000ffL);
				gpuDebug(String.format("gradient paint: Pts: %d,%d to %d,%d Colors: %08x to %08x Cyclic: %d\n",x0,y0,x1,y1,(int)val2,(int)val3,val4));
				gradient=new GradientPaint((float)x0,(float)y0,new Color((int)val2,true),(float)x1,(float)y1,new Color((int)val3,true),(val4==0)?false:true);
				}
			else { gradient=null; gpuDebug(String.format("turn gradient paint off\n")); }
			break;
		case 32:
			if (gpuAddr>=0x800000L)
				{
				addr=(int)(gpuAddr-0x800000L); val=gpuROM[addr]; addr++; str="";
				while (true)
					{
					if (gpuROM[addr]!=0) { str+=Character.toString((char)gpuROM[addr]); addr++; }
					else { break; }
					if (str.length()>256) { break; } // MARK
					}
				}
			else
				{
				addr=(int)(gpuAddr); val=gpuRAM[addr]; addr++; str="";
				while (true)
					{
					if (gpuRAM[addr]!=0) { str+=Character.toString((char)gpuRAM[addr]); addr++; }
					else { break; }
					if (str.length()>256) { break; } // MARK
					}
				}
			x0=(int)(val&0x000000ff);
			x1=(int)((val>>8)&0x000000ff);
			if ((b3>=0)&&(b3<NUM_USERFONTS)) { sfu[b3]=new Font(str,x1,x0); }
			gpuDebug(String.format("user font: #%d %s Style: %d Size: %d\n",b3,str,x1,x0));
			break;
		case 255: if (debug) { gpuDebug("GPU Reset"); }
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, 256, 256);
			sprites=false; for (i=0;i<NUM_SPRITES;i++) { spron[i]=false; spralx[i]=spraly[i]=0; }
			for (i=0;i<NUM_BLITS;i++) { blitalx[i]=blitaly[i]=0; }
			stroke=new BasicStroke(1f);
			gradient=null;
			break;
		default: if (debug) { gpuDebug(String.format("UNKNOWN GPU FUNCTION: %x %02x%02x%02x",func,b1,b2,b3)); } break;
		}
	}

	private void gpuRun(BufferedImage vidscr,long addr)
	{
	int func;
	long caddr,val;
	boolean iter;

	// Stack not needed, refer to message near top
	// gpuRunStack.push(addr);
	caddr=addr;
	iter=true;
	while (iter)
		{
		/* System.out.printf("ADDR: %x\n",caddr); */
		if (caddr>=0x800000L)
			{
			if (caddr-0x800000L>=GPUROMSIZE-1) { break; }
			val=gpuROM[(int)(caddr-0x800000L)]; caddr++;
			switch ((int)val)
				{
				case 1:
				case 0x800008: gpuAddr=gpuROM[(int)(caddr-0x800000L)]; /* System.out.printf("SET ADDR: %x\n",gpuAddr); */ break;
				case 2:
				case 0x800009: if (gpuAddr<0x800000) { gpuRAM[(int)(gpuAddr)]=gpuROM[(int)(caddr-0x800000L)]; } break;
				case 3:
				case 0x80000a:
					val=gpuROM[(int)(caddr-0x800000L)];
					func=(int)((val>>24)&0x000000ffL);
					if (func==19) { if (debug) { gpuDebug("End Program"); } /* if (!gpuRunStack.empty()) { gpuRunStack.pop(); } */ iter=false; break; }
					gpuFunc(vidscr,val);
					break;
				}
			caddr++;
			}
		else
			{
			if (caddr>=GPURAMSIZE-1) { break; }
			val=gpuRAM[(int)(caddr)]; caddr++;
			switch ((int)val)
				{
				case 1:
				case 0x800008: gpuAddr=gpuRAM[(int)(caddr)]; /* System.out.printf("SET ADDR: %x\n",gpuAddr); */ break;
				case 2:
				case 0x800009: if (gpuAddr<0x800000) { /* System.out.printf("SET RAM at %x to %x\n",gpuAddr,gpuRAM[(int)(caddr)]); */ gpuRAM[(int)(gpuAddr)]=gpuRAM[(int)(caddr)]; } break;
				case 3:
				case 0x80000a:
					val=gpuRAM[(int)(caddr)];
					func=(int)((val>>24)&0x000000ffL);
					if (func==19) { if (debug) { gpuDebug("End Program"); } /* if (!gpuRunStack.empty()) { gpuRunStack.pop(); } */ iter=false; break; }
					gpuFunc(vidscr,val);
					break;
				}
			caddr++;
			}
		}
	}

	private void gpuDebug(String str)
	{
	debugString=str;
	}

	private State getState(CircuitState circuitState)
	{
	State state = (State) circuitState.getData(this);
	if (state == null)
		{
		state = new State(new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB));
		circuitState.setData(this, state);
//loadGpuMemory(false,"c:\\sim\\gpu");
//loadGpuMemory(true,"c:\\sim\\gpupram3");
		}
	return state;
	}

	private class State implements ComponentState, Cloneable
	{
		public Value lastClock = null;
		public Value lastGPUClock = null;
		public BufferedImage img;
		public int last_x, last_y, color;

		State(BufferedImage img)
		{
		this.img = img;
		}

		public Object clone() { try { return super.clone(); } catch(CloneNotSupportedException e) { return null; } }

		public boolean tick(Value clk)
		{
		boolean rising = (lastClock == null || (lastClock == Value.FALSE && clk == Value.TRUE));
		lastClock = clk;
		return rising;
		}

		public boolean tickGPU(Value newClock, Object trigger)
		{
		Value oldClock = lastGPUClock;
		lastGPUClock = newClock;
		if (trigger == TRIG_NONE )
			{
			return true;
			}
		if (trigger == TRIG_RISING)
			{
			return oldClock == Value.FALSE && newClock == Value.TRUE;
			}
		else if (trigger == TRIG_FALLING)
			{
			return oldClock == Value.TRUE && newClock == Value.FALSE;
			}
		else if (trigger == TRIG_HIGH)
			{
			return newClock == Value.TRUE;
			}
		else if (trigger == TRIG_LOW)
			{
			return newClock == Value.FALSE;
			}
		else
			{
			return oldClock == Value.FALSE && newClock == Value.TRUE;
			}
		}
	}

	private class VideoMenu implements ActionListener, MenuExtender
	{
		private Project proj;
		private Frame frame;
		private CircuitState circState;
		private JMenuItem loadRAM;
		private JMenuItem loadROM;
		private JMenuItem GPUhelp;
		private JMenuItem GPUabout;

		VideoMenu()
		{
		}
		
		public void configureMenu(JPopupMenu menu, Project proj)
		{
		this.proj = proj;
		this.frame = proj.getFrame();
		this.circState = proj.getCircuitState();
		
		boolean enabled = circState != null;
		loadRAM = createItem(enabled, "Load RAM");
		loadROM = createItem(enabled, "Load ROM");
		GPUabout = createItem(enabled, "About");
		GPUhelp = createItem(enabled, "Help");

		menu.addSeparator();
		menu.add(loadRAM);
		menu.add(loadROM);
		menu.add(GPUhelp);
		menu.add(GPUabout);
		}

		private JMenuItem createItem(boolean enabled, String label)
		{
		JMenuItem ret = new JMenuItem(label);
		ret.setEnabled(enabled);
		ret.addActionListener(this);
		return ret;
		}

		public void actionPerformed(ActionEvent evt)
		{
		Object src = evt.getSource();
		if (src == loadRAM) doLoad(false);
		else if (src == loadROM) doLoad(true);
		else if (src == GPUhelp) showHelp("");
		else if (src == GPUabout) showAboutDialog(null);
		}

		private void doLoad(boolean ROM)
		{
		JFileChooser chooser = proj.createChooser();
		if (ROM) { chooser.setDialogTitle("Load ROM Memory"); }
		else { chooser.setDialogTitle("Load RAM Memory"); }
		int choice = chooser.showOpenDialog(frame);
		if (choice == JFileChooser.APPROVE_OPTION)
			{
			//loadGpuMemory(ROM,chooser.getSelectedFile().getPath());
			try { openHex(ROM,chooser.getSelectedFile()); }
			catch (IOException e)
				{
				JOptionPane.showMessageDialog(frame, e.getMessage(),
					"Load Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}
	
	@Override
	public Object getFeature(Object key)
	{
	if (key == ToolTipMaker.class) return this;
	else if (key == MenuExtender.class) { return new VideoMenu(); }
	else return super.getFeature(key);
	}

	public String getToolTip(ComponentUserEvent e)
	{
	int end = -1;
	for (int i = getEnds().size() - 1; i >= 0; i--)
		{
		if (getEndLocation(i).manhattanDistanceTo(e.getX(), e.getY()) < 10)
			{
			end = i;
			break;
			}
		}
	switch (end)
		{
		case P_CLK: return "CLK (Clock)";
		case P_WE: return "WE (Write Enable)";
		case P_X: return "X Position";
		case P_Y: return "Y Position";
		case P_DATA: return "Color Data";
		case P_RST: return "RST (Reset)";
		case P_GPU_SEL: return "GPU Select (0: GPU not used, 1: Set GPU memory address, 2: Write value to GPU memory, 3: Do GPU function)";
		case P_GPU_DATAI: return "GPU Data In";
		case P_GPU_DATAO: return "GPU Data Out";
		case P_GPU_ADDR: return "GPU Address (Optional)";
		default: return null;
		}
	}

	private static final String RAW_IMAGE_HEADER = "v2.0 raw";
	private static final String COMMENT_MARKER = "#";
	
	private static class HexReader
	{
		private BufferedReader in;
		private int[] data;
		private StringTokenizer curLine;
		private long leftCount;
		private long leftValue;
		
		public HexReader(BufferedReader in) throws IOException
		{
		this.in = in;
		data = new int[4096];
		curLine = findNonemptyLine();
		}
		
		private StringTokenizer findNonemptyLine() throws IOException
		{
		String line = in.readLine();
		while (line != null)
			{
			int index = line.indexOf(COMMENT_MARKER);
			if (index >= 0) { line = line.substring(0, index); }
			StringTokenizer ret = new StringTokenizer(line);
			if (ret.hasMoreTokens()) return ret;
			line = this.in.readLine();
			}
		return null;
		}
		
		private String nextToken() throws IOException
		{
		if (curLine != null && curLine.hasMoreTokens())
			{
			return curLine.nextToken();
			}
		else
			{
			curLine = findNonemptyLine();
			return curLine == null ? null : curLine.nextToken();
			}
		}
		
		public boolean hasNext() throws IOException
		{
		if (leftCount > 0) { return true; }
		else if (curLine != null && curLine.hasMoreTokens()) { return true; }
		else
			{
			curLine = findNonemptyLine();
			return curLine != null;
			}
		}
		
		public int[] next() throws IOException
		{
		int pos = 0;
		if (leftCount > 0)
			{
			int n = (int) Math.min(data.length - pos, leftCount);
			if (n == 1)
				{
				data[pos] = (int) leftValue;
				pos++;
				leftCount--;
				}
			else
				{
				Arrays.fill(data, pos, pos + n, (int) leftValue);
				pos += n;
				leftCount -= n;
				}
			}
		if (pos >= data.length) return data;
				
		for (String tok = nextToken(); tok != null; tok = nextToken())
			{
			try
				{
				int star = tok.indexOf("*");
				if (star < 0) { leftCount = 1; leftValue = Long.parseLong(tok, 16); }
				else
					{
					leftCount = Long.parseLong(tok.substring(0, star));
					leftValue = Long.parseLong(tok.substring(star + 1), 16);
					}
				}
			catch (NumberFormatException e)
				{
				throw new IOException("Image file has some invalid contents.");
				}

			int n = (int) Math.min(data.length - pos, leftCount);
			if (n == 1)
				{
				data[pos] = (int) leftValue;
				pos++;
				leftCount--;
				}
			else
				{
				Arrays.fill(data, pos, pos + n, (int) leftValue);
				pos += n;
				leftCount -= n;
				}
			if (pos >= data.length) return data;
			}
			
		if (pos >= data.length) { return data; }
		else
			{
			int[] ret = new int[pos];
			System.arraycopy(data, 0, ret, 0, pos);
			return ret;
			}
		}
	}

	public void openHex(boolean ROM, Reader in) throws IOException
	{
	HexReader reader = new HexReader(new BufferedReader(in));
	int offs = 0;
	while (reader.hasNext())
		{
		int[] values = reader.next();
		if (ROM)
			{
			for (int i=0;i<values.length;i++)
				{
				if (i+offs>=GPUROMSIZE) { JOptionPane.showMessageDialog(null,"GPU ROM data too big: Truncated at "+GPUROMSIZE+" memory elements." ,"Load Error",JOptionPane.ERROR_MESSAGE); return; }
				gpuROM[i+offs]=(long)values[i];
				}
			}
		else
			{
			for (int i=0;i<values.length;i++)
				{
				if (i+offs>=GPURAMSIZE) { JOptionPane.showMessageDialog(null,"GPU RAM data too big: Truncated at "+GPURAMSIZE+" memory elements." ,"Load Error",JOptionPane.ERROR_MESSAGE); return; }
				gpuRAM[i+offs]=(long)values[i];
				}
			}
		offs += values.length;
		}
	}
	
	public void openHex(boolean ROM, File src) throws IOException
	{
	BufferedReader in;
	try { in = new BufferedReader(new FileReader(src)); }
	catch (IOException e)
		{
		throw new IOException("Could not open file: "+src.getPath());
		}
	try
		{
		if (ROM)
			{
			AttributeSet attrs = getAttributeSet();
			attrs.setValue(ROM_FILE,src.getPath());
			}
		String header = in.readLine();
		if (!header.equals(RAW_IMAGE_HEADER))
			{
			throw new IOException("Image file has invalid format header. ("+src.getPath()+")");
			}
		openHex(ROM, in);
		try
			{
			BufferedReader oldIn = in;
			in = null;
			oldIn.close();
			}
		catch (IOException e)
			{
			throw new IOException("Error reading file. ("+src.getPath()+")");
			}
		}
	finally
		{
		try { if (in != null) in.close(); }
		catch (IOException e) { }
		}
	}

	private static class MyPanel extends JPanel
	{
		public MyPanel()
		{
		setBackground(Color.BLACK);
		}

		@Override
		public void paintComponent(Graphics g)
		{
		super.paintComponent(g);
		try
			{
			URL url=GPU.class.getResource("/resources/about.png");
			BufferedImage bg=ImageIO.read(url);
			g.drawImage(bg, 0, 0, null);
			int size1=22,size2=12,size3=16;
			Font f1 = new Font("Serif", Font.BOLD, size1);
			g.setFont(f1);
			String s1="Copyright © 2025 Mark Craig";
			int w=g.getFontMetrics().stringWidth(s1);
			if (w>288)
				{
				size1*=.8; size2*=.8; size3*=.8;
				f1 = new Font("Serif", Font.BOLD, size1);
				g.setFont(f1);
				w=g.getFontMetrics().stringWidth(s1);
				}
			g.setColor(Color.BLACK);
			g.drawString(s1,144-w/2+1,141);
			g.drawString(s1,144-w/2+2,142);
			g.setColor(Color.WHITE);
			g.drawString(s1,144-w/2,140);
			String s0="GPU Video Screen";
			w=g.getFontMetrics().stringWidth(s0);
			g.setColor(Color.BLACK);
			g.drawString(s0,144-w/2+1,51);
			g.drawString(s0,144-w/2+2,52);
			g.setColor(Color.WHITE);
			g.drawString(s0,144-w/2,50);
			Font f3 = new Font("Serif", Font.PLAIN, size2);
			g.setFont(f3);
			String s4="This version written for: Logisim 2.7.1";
			w=g.getFontMetrics().stringWidth(s4);
			g.drawString(s4,144-w/2,165);
			Font f2 = new Font("Serif", Font.PLAIN | Font.ITALIC, size3);
			g.setColor(Color.CYAN);
			g.setFont(f2);
			String s2="https://www.youtube.com/MrMcSoftware";
			w=g.getFontMetrics().stringWidth(s2);
			g.drawString(s2,144-w/2,190);
			String s3="https://github.com/mrmcsoftware";
			w=g.getFontMetrics().stringWidth(s3);
			g.drawString(s3,144-w/2,210);
			}
		catch (Throwable t) { }
		}
	}
		
	private void showAboutDialog(JFrame owner)
	{
	JPanel panel = new JPanel(new BorderLayout());
	MyPanel myPanel=new MyPanel();
	panel.add(myPanel);
	panel.setBorder(BorderFactory.createLineBorder(Color.RED, 4));
	panel.setPreferredSize(new Dimension(300, 240));
	panel.setBackground(Color.BLACK);
	JOptionPane.showMessageDialog(owner, panel, "GPU Video Screen", JOptionPane.PLAIN_MESSAGE);
	}

	private HelpSet helpSet;
	private String helpSetUrl = "";
	private JHelp helpComponent;
	//private LFrame helpFrame;
	private JFrame helpFrame;

	private void loadBroker()
	{
	String helpUrl = "doc/GPU.hs";
	if (helpSet == null || helpFrame == null || !helpUrl.equals(helpSetUrl))
		{
		ClassLoader loader = GPU.class.getClassLoader();
		try
			{
			URL hsURL = HelpSet.findHelpSet(loader, helpUrl);
			if (hsURL == null)
				{
				JOptionPane.showMessageDialog(null, "Help data not found");
				return;
				}
			helpSetUrl = helpUrl;
			helpSet = new HelpSet(null, hsURL);
			helpComponent = new JHelp(helpSet);
			if (helpFrame == null)
				{
				//helpFrame = new LFrame();
				helpFrame = new JFrame();
				helpFrame.setTitle("GPU Component Documentation");
				helpFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
				helpFrame.getContentPane().add(helpComponent);
				helpFrame.pack();
				}
			else
				{
				helpFrame.getContentPane().removeAll();
				helpFrame.getContentPane().add(helpComponent);
				helpComponent.revalidate();
				}
			}
		catch (Exception e)
			{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not load help data.");
			return;
			}
		}
	}

	private void showHelp(String target)
	{
	loadBroker();
	try
		{
		helpFrame.toFront();
		helpFrame.setVisible(true);
		}
	catch (Exception e)
		{
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, "Could not display help data.");
		}
	}

	public static class MyStringGetter implements StringGetter
	{
		private String str;

		public MyStringGetter(String str) { this.str = str; }

		public String get() { return str; }

		@Override
		public String toString() { return str; }
	}
}
