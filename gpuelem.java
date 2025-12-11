import java.util.Scanner;
import java.util.StringTokenizer;
import java.io.*;

public class gpuelem
{
public static void main(String[] args)
{
gpuelem gpuel=new gpuelem();
gpuel.gpuelem(args);
}

/* NOTE: You might have a problem with the Arc primitive due to possible
   Endianness differences of your CPU if it differs from x86 */

/*
func (R*M means RAM/ROM)
0 Clear screen with specified color RGB
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

void gpuelem(String[] args)
{
int i,ch;
long r,g,b,a,rt,gt,bt,at,x0,y0,x1,y1,n,as,ae,size,style;
long val,val2,val3;
//FILE *fp;
String str,str2;
Scanner sin=new Scanner(System.in);
File fp=new File("outhex");
FileWriter fw;
PrintWriter pw=null;

try
	{
	fw=new FileWriter(fp,false);
	pw=new PrintWriter(fw);
	}
catch(IOException ex)
	{
	System.out.printf("File error\n");
	}
System.out.printf("Functions:\n");
System.out.printf("  1 - Line\n");
System.out.printf("  2 - Text\n");
System.out.printf("  3 - Blit Image\n");
System.out.printf("  4 - Polygon/line\n");
System.out.printf("  5 - Oval/Circle\n");
System.out.printf("  6 - Sprite with transparent color\n");
System.out.printf("  7 - Point array for LineV\n");
System.out.printf("  8 - Arc\n");
System.out.printf("  9 - Round rectangle\n");
System.out.printf(" 10 - Sprite with full alpha channel\n");
System.out.printf(" 11 - Gradient paint\n");
System.out.printf(" 12 - Font\n");
System.out.printf("Enter choice: ");
ch=sin.nextInt();
System.out.printf("Input: %d\n",ch);
switch (ch)
	{
/*
	case 0: System.out.printf("Enter red green blue: ");
		r=sin.nextLong(); g=sin.nextLong(); b=sin.nextLong();
		val=(1<<24)|(r<<16)|(g<<8)|b;
		pw.printf("%x\n",val);
		break;
*/
	case 1: System.out.printf("Enter x0 y0 x1 y1: ");
		x0=sin.nextLong(); y0=sin.nextLong(); x1=sin.nextLong(); y1=sin.nextLong();
		val=(x0<<24)|(y0<<16)|(x1<<8)|y1;
		pw.printf("# Size: 1 Line %d,%d to %d,%d\n",x0,y0,x1,y1);
		pw.printf("%x\n",val);
		break;
	case 2: System.out.printf("Enter string: ");
		str=sin.nextLine(); // get rid of newline character from menu choice
		str=sin.nextLine();
		pw.printf("# Size: %d Text %s\n",str.length()+1,str);
		for (i=0;i<str.length();i++) { pw.printf("%x ",(int)str.charAt(i)); }
		pw.printf("0\n");
		break;
	case 3: System.out.printf("Enter PPM filename: ");
		str=sin.nextLine();
		str=sin.nextLine();
		processimage(str,pw);
		break;
	case 4: System.out.printf("Enter number of points: ");
		n=sin.nextLong();
		pw.printf("# Size: %d Polyline/Polygon\n",n+1);
		pw.printf(" %x\n",n);
		for (i=0;i<n;i++)
			{
			System.out.printf("Point %d (x y): ",i+1);
			x0=sin.nextLong(); y0=sin.nextLong();
			val=(x0<<8)|y0;
			pw.printf("%x\n",val);
			}
		break;
	case 5: System.out.printf("Enter xpos ypos: ");
		x0=sin.nextLong(); y0=sin.nextLong();
		System.out.printf("Enter xradius yradius: ");
		x1=sin.nextLong(); y1=sin.nextLong();
		pw.printf("# Size: 1 Oval Pos: %d,%d Radius: %d,%d\n",x0,y0,x1,y1);
		val=(x1<<24)|(y1<<16)|(x0<<8)|y0;
		pw.printf("%x\n",val);
		break;
	case 6: System.out.printf("Enter PPM filename: ");
		str=sin.nextLine();
		str=sin.nextLine();
		System.out.printf("Enter transparent color (r g b): ");
		rt=sin.nextLong(); gt=sin.nextLong(); bt=sin.nextLong();
		processsprite(str,rt,gt,bt,pw);
		break;
	case 7: System.out.printf("Enter number of points: ");
		n=sin.nextLong();
		pw.printf("# Size: %d Point array\n",n);
		for (i=0;i<n;i++)
			{
			System.out.printf("Point %d (x y): ",i+1);
			x0=sin.nextLong(); y0=sin.nextLong();
			val=(x0<<8)|y0;
			pw.printf("%x\n",val);
			}
		break;
	case 8: System.out.printf("Enter xpos ypos: ");
		x0=sin.nextLong(); y0=sin.nextLong();
		System.out.printf("Enter xradius yradius: ");
		x1=sin.nextLong(); y1=sin.nextLong();
		System.out.printf("Enter Anglestart Angleextent (positive values only): ");
		as=sin.nextLong(); ae=sin.nextLong();
		pw.printf("# Size: 2 Arc Pos: %d,%d Radius: %d,%d AngleS: %d AngleE: %d\n",x0,y0,x1,y1,as,ae);
		val=(x1<<24)|(y1<<16)|(x0<<8)|y0;
		pw.printf("%x ",val);
		val=(as<<16)|ae;
		pw.printf("%x\n",val);
		break;
	case 9: System.out.printf("Enter Width Height: ");
		x0=sin.nextLong(); y0=sin.nextLong();
		System.out.printf("Enter ArcWidth ArcHeight: ");
		x1=sin.nextLong(); y1=sin.nextLong();
		pw.printf("# Size: 1 RoundRect Dimensions: %d,%d ArcDimensions: %d,%d\n",x0,y0,x1,y1);
		val=(x0<<24)|(y0<<16)|(x1<<8)|y1;
		pw.printf("%x ",val);
		break;
	case 10: System.out.printf("Enter PPM filename: ");
		str=sin.nextLine();
		str=sin.nextLine();
		System.out.printf("Enter (alpha channel) PGM filename: ");
		str2=sin.nextLine();
		processsprite2(str,str2,pw);
		break;
	case 11: System.out.printf("(Note: if alpha=0, then transparent, if alpha=255, then opaque)\n");
		System.out.printf("Enter gradient start: (x y red green blue alpha): ");
		x0=sin.nextLong(); y0=sin.nextLong(); r=sin.nextLong(); g=sin.nextLong(); b=sin.nextLong(); a=sin.nextLong();
		System.out.printf("Enter gradient end: (x y red green blue alpha): ");
		x1=sin.nextLong(); y1=sin.nextLong(); rt=sin.nextLong(); gt=sin.nextLong(); bt=sin.nextLong(); at=sin.nextLong();
		System.out.printf("Is it cyclic (1=yes, 0=no): ");
		n=sin.nextLong();
		pw.printf("# Size: 4 GradientPaint Pt1: %d,%d Color1: %02x%02x%02x%02x  Pt2: %d,%d Color2: %02x%02x%02x%02x Cyclic: %d\n",x0,y0,a,r,g,b,x1,y1,at,rt,gt,bt,n);
		val=(x0<<24)|(y0<<16)|(x1<<8)|y1;
		val2=(a<<24)|(r<<16)|(g<<8)|b;
		val3=(at<<24)|(rt<<16)|(gt<<8)|bt;
		pw.printf("%x %x %x %x\n",val,val2,val3,n);
		break;
	case 12: System.out.printf("Enter font size: ");
		size=sin.nextLong();
		System.out.printf("Enter font style (0 = plain, 1 = bold, 2 = italic, 3 = bold-italic): ");
		style=sin.nextLong();
		System.out.printf("Enter font name: ");
		str=sin.nextLine();
		str=sin.nextLine();
		pw.printf("# Size: %d Font %s Style: %d Size: %d\n",str.length()+2,str,style,size);
		val=(style<<8)|size;
		pw.printf("%x ",val);
		for (i=0;i<str.length();i++) { pw.printf("%x ",(int)str.charAt(i)); }
		pw.printf("0\n");
		break;
	}
if (pw!=null) { pw.close(); }
}

int processimage(String fname,PrintWriter fpo)
{
long val;
String str;
int x,y,w,h,r,g,b,c;

try
	{
	FileInputStream in=new FileInputStream(fname);
	str=""; while ((c=in.read())!='\n') { str+=(char)c; }
	if (!str.startsWith("P6")) { System.out.printf("Not a PPM image\n"); in.close(); return(0); }
	str=""; while ((c=in.read())!='\n') { str+=(char)c; }
	if (str.charAt(0)=='#') { str=""; while ((c=in.read())!='\n') { str+=(char)c; } }
	StringTokenizer st=new StringTokenizer(str);
	w=Integer.parseInt(st.nextToken());
	h=Integer.parseInt(st.nextToken());
	str=""; while ((c=in.read())!='\n') { str+=(char)c; }
	val=((w-1)<<8)|(h-1);
	fpo.printf("# Size: %d Image FName: %s Res: %dx%d\n",w*h+1,fname,w,h);
	fpo.printf(" %x\n",val);
	for (y=0;y<h;y++)
		{
		for (x=0;x<w;x++)
			{
			r=in.read(); g=in.read(); b=in.read();
			val=(r<<16)|(g<<8)|b;
			fpo.printf("%x ",val);
			}
		fpo.printf("\n");
		}
	in.close();
	}
catch(IOException ex)
	{
	System.out.printf("File error '%s'\n",fname); return(0);
	}
return(1);
}

int processsprite(String fname,long rt,long gt,long bt,PrintWriter fpo)
{
long val;
String str;
long x,y,w,h,r,g,b,a,c;

try
	{
	FileInputStream in=new FileInputStream(fname);
	str=""; while ((c=in.read())!='\n') { str+=(char)c; }
	if (!str.startsWith("P6")) { System.out.printf("Not a PPM image\n"); in.close(); return(0); }
	str=""; while ((c=in.read())!='\n') { str+=(char)c; }
	if (str.charAt(0)=='#') { str=""; while ((c=in.read())!='\n') { str+=(char)c; } }
	StringTokenizer st=new StringTokenizer(str);
	w=Integer.parseInt(st.nextToken());
	h=Integer.parseInt(st.nextToken());
	str=""; while ((c=in.read())!='\n') { str+=(char)c; }
	val=((w-1)<<8)|(h-1);
	fpo.printf("# Size: %d Sprite FName: %s Res: %dx%d TransCol: %02x%02x%02x\n",w*h+1,fname,w,h,rt,gt,bt);
	fpo.printf(" %x\n",val);
	for (y=0;y<h;y++)
		{
		for (x=0;x<w;x++)
			{
			r=in.read(); g=in.read(); b=in.read();
			if ((r==rt)&&(g==gt)&&(b==bt)) { a=0; } else { a=255; }
			val=(a<<24)|(r<<16)|(g<<8)|b;
			fpo.printf("%x ",val);
			}
		fpo.printf("\n");
		}
	in.close();
	}
catch(IOException ex)
	{
	System.out.printf("File error '%s'\n",fname); return(0);
	}
return(1);
}

int processsprite2(String fname,String fnamea,PrintWriter fpo)
{
long val;
String str;
long x,y,w,h,r,g,b,a,wa,ha,c;
FileInputStream in,ina;

try
	{
	in=new FileInputStream(fname);
	try
		{
		ina=new FileInputStream(fnamea);
		}
	catch(IOException ex) { System.out.printf("File error '%s'\n",fnamea); in.close(); return(0); }
	}
catch(IOException ex) { System.out.printf("File error '%s'\n",fname); return(0); }
try
	{
	str=""; while ((c=in.read())!='\n') { str+=(char)c; }
	if (!str.startsWith("P6")) { System.out.printf("%s: Not a PPM image\n",fname); in.close(); ina.close(); return(0); }
	str=""; while ((c=ina.read())!='\n') { str+=(char)c; }
	if (!str.startsWith("P5")) { System.out.printf("%s: Not a PGM image\n",fnamea); in.close(); ina.close(); return(0); }

	str=""; while ((c=in.read())!='\n') { str+=(char)c; }
	if (str.charAt(0)=='#') { str=""; while ((c=in.read())!='\n') { str+=(char)c; } }

	StringTokenizer st=new StringTokenizer(str);
	w=Integer.parseInt(st.nextToken());
	h=Integer.parseInt(st.nextToken());
	str=""; while ((c=in.read())!='\n') { str+=(char)c; }

	str=""; while ((c=ina.read())!='\n') { str+=(char)c; }
	if (str.charAt(0)=='#') { str=""; while ((c=ina.read())!='\n') { str+=(char)c; } }

	st=new StringTokenizer(str);
	wa=Integer.parseInt(st.nextToken());
	ha=Integer.parseInt(st.nextToken());
	str=""; while ((c=ina.read())!='\n') { str+=(char)c; }

	if ((w!=wa)||(h!=ha)) { System.out.printf("Warning: the two images are not the same resolution\n"); }
	val=((w-1)<<8)|(h-1);
	fpo.printf("# Size: %d Sprite FNames: %s and %s Res: %dx%d\n",w*h+1,fname,fnamea,w,h);
	fpo.printf(" %x\n",val);
	for (y=0;y<h;y++)
		{
		for (x=0;x<w;x++)
			{
			r=in.read(); g=in.read(); b=in.read(); a=ina.read();
			val=(a<<24)|(r<<16)|(g<<8)|b;
			fpo.printf("%x ",val);
			}
		fpo.printf("\n");
		}
	in.close();
	ina.close();
	}
catch(IOException ex)
	{
	System.out.printf("File error '%s' or '%s'\n",fname,fnamea); return(0);
	}
return(1);
}

};
