/* NOTE: You might have a problem with the Arc primitive due to possible
   Endianness differences of your CPU if it differs from x86 */

#include <stdio.h>

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

main()
{
int i,ch,r,g,b,a,rt,gt,bt,at,x0,y0,x1,y1,n,as,ae,size,style;
unsigned int val,val2,val3;
char str[256],str2[256];
FILE *fp;

fp=fopen("outhex","w");
printf("Functions:\n");
printf("  1 - Line\n");
printf("  2 - Text\n");
printf("  3 - Blit Image\n");
printf("  4 - Polygon/line\n");
printf("  5 - Oval/Circle\n");
printf("  6 - Sprite with transparent color\n");
printf("  7 - Point array for LineV\n");
printf("  8 - Arc\n");
printf("  9 - Round rectangle\n");
printf(" 10 - Sprite with full alpha channel\n");
printf(" 11 - Gradient paint\n");
printf(" 12 - Font\n");
printf("Enter choice: ");
scanf("%d",&ch);
switch (ch)
	{
	/*
	case 0: printf("Enter red green blue: ");
		scanf("%d %d %d",&r,&g,&b;
		val=(1<<24)|(r<<16)|(g<<8)|b;
		fprintf(fp,"%x\n",val);
		break;
	*/
	case 1: printf("Enter x0 y0 x1 y1: ");
		scanf("%d %d %d %d",&x0,&y0,&x1,&y1);
		val=(x0<<24)|(y0<<16)|(x1<<8)|y1;
		fprintf(fp,"# Size: 1 Line %d,%d to %d,%d\n",x0,y0,x1,y1);
		fprintf(fp,"%x\n",val);
		break;
	case 2: printf("Enter string: ");
		fgetc(stdin);
		fgets(str,256,stdin);
		str[strlen(str)-1]='\0';
		fprintf(fp,"# Size: %d Text %s\n",strlen(str)+1,str);
		for (i=0;i<strlen(str);i++) { fprintf(fp,"%x ",str[i]); }
		fprintf(fp,"0\n");
		break;
	case 3: printf("Enter PPM filename: ");
		scanf("%s",str);
		processimage(str,fp);
		break;
	case 4: printf("Enter number of points: ");
		scanf(" %d",&n);
		fprintf(fp,"# Size: %d Polyline/Polygon\n",n+1);
		fprintf(fp," %x\n",n);
		for (i=0;i<n;i++)
			{
			printf("Point %d (x y): ",i+1);
			scanf("%d %d",&x0,&y0);
			val=(x0<<8)|y0;
			fprintf(fp,"%x\n",val);
			}
		break;
	case 5: printf("Enter xpos ypos: ");
		scanf("%d %d",&x0,&y0);
		printf("Enter xradius yradius: ");
		scanf("%d %d",&x1,&y1);
		fprintf(fp,"# Size: 1 Oval Pos: %d,%d Radius: %d,%d\n",x0,y0,x1,y1);
		val=(x1<<24)|(y1<<16)|(x0<<8)|y0;
		fprintf(fp,"%x\n",val);
		break;
	case 6: printf("Enter PPM filename: ");
		scanf("%s",str);
		printf("Enter transparent color (r g b): ");
		scanf("%d %d %d",&rt,&gt,&bt);
		processsprite(str,rt,gt,bt,fp);
		break;
	case 7: printf("Enter number of points: ");
		scanf("%d",&n);
		fprintf(fp,"# Size: %d Point array\n",n);
		for (i=0;i<n;i++)
			{
			printf("Point %d (x y): ",i+1);
			scanf("%d %d",&x0,&y0);
			val=(x0<<8)|y0;
			fprintf(fp,"%x\n",val);
			}
		break;
	case 8: printf("Enter xpos ypos: ");
		scanf("%d %d",&x0,&y0);
		printf("Enter xradius yradius: ");
		scanf("%d %d",&x1,&y1);
		printf("Enter Anglestart Angleextent (positive values only): ");
		scanf("%d %d",&as,&ae);
		fprintf(fp,"# Size: 2 Arc Pos: %d,%d Radius: %d,%d AngleS: %d AngleE: %d\n",x0,y0,x1,y1,as,ae);
		val=(x1<<24)|(y1<<16)|(x0<<8)|y0;
		fprintf(fp,"%x ",val);
		val=(as<<16)|ae;
		fprintf(fp,"%x\n",val);
		break;
	case 9: printf("Enter Width Height: ");
		scanf("%d %d",&x0,&y0);
		printf("Enter ArcWidth ArcHeight: ");
		scanf("%d %d",&x1,&y1);
		fprintf(fp,"# Size: 1 RoundRect Dimensions: %d,%d ArcDimensions: %d,%d\n",x0,y0,x1,y1);
		val=(x0<<24)|(y0<<16)|(x1<<8)|y1;
		fprintf(fp,"%x ",val);
		break;
	case 10: printf("Enter PPM filename: ");
		scanf("%s",str);
		printf("Enter (alpha channel) PGM filename: ");
		scanf("%s",str2);
		processsprite2(str,str2,fp);
		break;
	case 11: printf("(Note: if alpha=0, then transparent, if alpha=255, then opaque)\n");
		printf("Enter gradient start: (x y red green blue alpha): ");
		scanf("%d %d %d %d %d %d",&x0,&y0,&r,&g,&b,&a);
		printf("Enter gradient end: (x y red green blue alpha): ");
		scanf("%d %d %d %d %d %d",&x1,&y1,&rt,&gt,&bt,&at);
		printf("Is it cyclic (1=yes, 0=no): ");
		scanf("%d",&n);
		fprintf(fp,"# Size: 4 GradientPaint Pt1: %d,%d Color1: %02x%02x%02x%02x  Pt2: %d,%d Color2: %02x%02x%02x%02x Cyclic: %d\n",x0,y0,a,r,g,b,x1,y1,at,rt,gt,bt,n);
		val=(x0<<24)|(y0<<16)|(x1<<8)|y1;
		val2=(a<<24)|(r<<16)|(g<<8)|b;
		val3=(at<<24)|(rt<<16)|(gt<<8)|bt;
		fprintf(fp,"%x %x %x %x\n",val,val2,val3,n);
		break;
	case 12: printf("Enter font size: ");
		scanf("%d",&size);
		printf("Enter font style (0 = plain, 1 = bold, 2 = italic, 3 = bold-italic): ");
		scanf("%d",&style);
		printf("Enter font name: ");
		fgetc(stdin);
		fgets(str,256,stdin);
		str[strlen(str)-1]='\0';
		fprintf(fp,"# Size: %d Font %s Style: %d Size: %d\n",strlen(str)+2,str,style,size);
		val=(style<<8)|size;
		fprintf(fp,"%x ",val);
		for (i=0;i<strlen(str);i++) { fprintf(fp,"%x ",str[i]); }
		fprintf(fp,"0\n");
		break;
	}
fclose(fp);
}

processimage(char *fname,FILE *fpo)
{
FILE *fp;
long val;
char str[256];
int x,y,w,h,r,g,b;

if ((fp=fopen(fname,"rb"))==NULL)
	{
	printf("Can't open '%s'\n",fname);
	return(0);
	}
fgets(str,256,fp);
if (strncmp(str,"P6",2)!=0) { printf("Not a PPM image\n"); fclose(fp); return(0); }
fgets(str,256,fp);
if (str[0]=='#') { fgets(str,256,fp); }
sscanf(str,"%d %d",&w,&h);
fgets(str,256,fp);
val=((w-1)<<8)|(h-1);
fprintf(fpo,"# Size: %d Image FName: %s Res: %dx%d\n",w*h+1,fname,w,h);
fprintf(fpo," %x\n",val);
for (y=0;y<h;y++)
	{
	for (x=0;x<w;x++)
		{
		r=fgetc(fp); g=fgetc(fp); b=fgetc(fp);
		val=(r<<16)|(g<<8)|b;
		fprintf(fpo,"%x ",val);
		}
	fprintf(fpo,"\n");
	}
fclose(fp);
return(1);
}

processsprite(char *fname,int rt,int gt,int bt,FILE *fpo)
{
FILE *fp;
long val;
char str[256];
int x,y,w,h,r,g,b,a;

if ((fp=fopen(fname,"rb"))==NULL)
	{
	printf("Can't open '%s'\n",fname);
	return(0);
	}
fgets(str,256,fp);
if (strncmp(str,"P6",2)!=0) { printf("Not a PPM image\n"); fclose(fp); return(0); }
fgets(str,256,fp);
if (str[0]=='#') { fgets(str,256,fp); }
sscanf(str,"%d %d",&w,&h);
fgets(str,256,fp);
val=((w-1)<<8)|(h-1);
fprintf(fpo,"# Size: %d Sprite FName: %s Res: %dx%d TransCol: %02x%02x%02x\n",w*h+1,fname,w,h,rt,gt,bt);
fprintf(fpo," %x\n",val);
for (y=0;y<h;y++)
	{
	for (x=0;x<w;x++)
		{
		r=fgetc(fp); g=fgetc(fp); b=fgetc(fp);
		if ((r==rt)&&(g==gt)&&(b==bt)) { a=0; } else { a=255; }
		val=(a<<24)|(r<<16)|(g<<8)|b;
		fprintf(fpo,"%x ",val);
		}
	fprintf(fpo,"\n");
	}
fclose(fp);
return(1);
}

processsprite2(char *fname,char *fnamea,FILE *fpo)
{
FILE *fp,*fpa;
long val;
char str[256];
int x,y,w,h,r,g,b,a,wa,ha;

if ((fp=fopen(fname,"rb"))==NULL)
	{
	printf("Can't open '%s'\n",fname);
	return(0);
	}
if ((fpa=fopen(fnamea,"rb"))==NULL)
	{
	printf("Can't open '%s'\n",fnamea);
	fclose(fp);
	return(0);
	}
fgets(str,256,fp);
if (strncmp(str,"P6",2)!=0) { printf("%s: Not a PPM image\n",fname); fclose(fp); fclose(fpa); return(0); }
fgets(str,256,fpa);
if (strncmp(str,"P5",2)!=0) { printf("%s: Not a PGM image\n",fnamea); fclose(fp); fclose(fpa); return(0); }
fgets(str,256,fp);
if (str[0]=='#') { fgets(str,256,fp); }
sscanf(str,"%d %d",&w,&h);
fgets(str,256,fp);
fgets(str,256,fpa);
if (str[0]=='#') { fgets(str,256,fpa); }
sscanf(str,"%d %d",&wa,&ha);
fgets(str,256,fpa);
if ((w!=wa)||(h!=ha)) { printf("Warning: the two images are not the same resolution\n"); }
val=((w-1)<<8)|(h-1);
fprintf(fpo,"# Size: %d Sprite FNames: %s and %s Res: %dx%d\n",w*h+1,fname,fnamea,w,h);
fprintf(fpo," %x\n",val);
for (y=0;y<h;y++)
	{
	for (x=0;x<w;x++)
		{
		r=fgetc(fp); g=fgetc(fp); b=fgetc(fp); a=fgetc(fpa);
		val=(a<<24)|(r<<16)|(g<<8)|b;
		fprintf(fpo,"%x ",val);
		}
	fprintf(fpo,"\n");
	}
fclose(fp);
fclose(fpa);
return(1);
}
