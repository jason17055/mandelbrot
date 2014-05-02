import org.apfloat.*;
import java.io.*;
import java.util.ArrayList;

public class MandelbrotFragment
{
	int width;
	int height;
	byte [][] members;

	// only used for new constructions
	Apfloat originX; //min Real component
	Apfloat originY; //min Imag component
	Apfloat scale;

	static final byte PROBABLY = -2;
	static final byte YES = -1; //in the set
	static final byte NO = 0;
	static final byte UNLIKELY = 64;
	static final byte MIXED = 65;

	static final int BANDS = 64;

	static final Apfloat ONE = new Apfloat(1);
	static final Apfloat TWO = new Apfloat(2);
	static final Apfloat MINUS_TWO_PT_FIVE = new Apfloat(-2.5);

	FixedPrecisionApcomplexHelper precisionHelper =
			new FixedPrecisionApcomplexHelper(32);

	protected MandelbrotFragment()
	{
	}

	public MandelbrotFragment(Apfloat re0, Apfloat im0, Apfloat size, int width, int height)
	{
		assert width == height;

		this.originX = re0;
		this.originY = im0;
		this.scale = size.divide(new Apfloat(width));
		this.width = width;
		this.height = height;

		generate();
	}

	void generate()
	{
		this.members = new byte[height][width];

		byte lastRow = generateRow(height-1, 0, width);
		byte lastCol = generateColumn(width-1, 0, height-1);
		generateBox(0, 0, width-1, height-1, lastRow, lastCol);

		makeProbably();
		makeProbably();
		makeUnlikely();
		makeUnlikely();
	}

	void generateBox(int x0, int y0, int x1, int y1, byte bottomEdge, byte rightEdge)
	{
		byte topEdge = generateRow(y0, x0, x1);
		byte leftEdge = generateColumn(x0, y0+1, y1);

		if (topEdge != MIXED &&
			topEdge == bottomEdge &&
			topEdge == rightEdge &&
			topEdge == leftEdge)
		{
			// all edges the same, so fill the middle
			fillBox(x0+1, y0+1, x1, y1, topEdge);
			return;
		}

		for (int i = y0+1; i < y1; i++) {
			generateRow(i, x0+1, x1);
		}
	}

	void fillBox(int x0, int y0, int x1, int y1, byte b)
	{
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				members[y][x] = b;
			}
		}
	}

	byte generateRow(int y, int x0, int x1)
	{
		byte sum = 0;

		Apfloat fy = originY.add(scale.multiply(new Apfloat(y)));

		for (int x = x0; x < x1; x++) {
			Apfloat fx = originX.add(scale.multiply(new Apfloat(x)));
			members[y][x] = checkMandelbrot(
				new Apcomplex(fx, fy)
				);
			if (x == x0) {
				sum = members[y][x] == YES ? YES : NO;
			}
			else if (sum != MIXED) {
				if (members[y][x] == YES && sum != YES) {
					sum = MIXED;
				}
				if (members[y][x] != YES && sum == YES) {
					sum = MIXED;
				}
			}
		}
		return sum;
	}

	byte generateColumn(int x, int y0, int y1)
	{
		byte sum = 0;

		Apfloat fx = originX.add(scale.multiply(new Apfloat(x)));

		for (int y = y0; y < y1; y++) {
			Apfloat fy = originY.add(scale.multiply(new Apfloat(y)));
			members[y][x] = checkMandelbrot(
				new Apcomplex(fx, fy)
				);
			if (y == y0) {
				sum = members[y][x] == 0 ? YES : NO;
			}
			else if (sum != MIXED) {
				if (members[y][x] == 0 && sum != YES) {
					sum = MIXED;
				}
				if (members[y][x] != 0 && sum != NO) {
					sum = MIXED;
				}
			}
		}
		return sum;
	}

	static final int ESCAPE_THRESHOLD = 50;

	byte checkMandelbrot(Apcomplex c)
	{
		if (c.real().compareTo(ONE) > 0) return 1;

		Apcomplex z = Apcomplex.ZERO;
		for (int i = 0; i < ESCAPE_THRESHOLD; i++) {
			z = precisionHelper.multiply(z, z).add(c);
			if (z.real().compareTo(TWO) > 0 ||
				z.imag().compareTo(TWO) > 0) {
				return (byte)(i % BANDS);
			}
		}
		return YES;
	}

	void makeProbably()
	{
		ArrayList<Integer> probably = new ArrayList<Integer>();

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (members[i][j] != YES) { continue; }
				if ((i > 0 && members[i-1][j] != YES) ||
				    (i+1 < height && members[i+1][j] != YES) ||
				    (j > 0 && members[i][j-1] != YES) ||
				    (j+1 < width && members[i][j+1] != YES)
				) {
					probably.add(i*width+j);
				}
			}
		}
		for (int X : probably)
		{
			members[X/width][X%width] = PROBABLY;
		}
	}

	void makeUnlikely()
	{
		ArrayList<Integer> a = new ArrayList<Integer>();

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (members[i][j] < 0) { continue; }
				if ((i > 0 && members[i-1][j] < 0) ||
				    (i+1 < height && members[i+1][j] < 0) ||
				    (j > 0 && members[i][j-1] < 0) ||
				    (j+1 < width && members[i][j+1] < 0)
				) {
					a.add(i*width+j);
				}
			}
		}
		for (int X : a)
		{
			members[X/width][X%width] = UNLIKELY;
		}
	}

	void writeTo(OutputStream out)
		throws IOException
	{
		for (int i = 0; i < height; i++) {
			out.write(members[i]);
		}
	}

	static MandelbrotFragment readFrom(InputStream in, int pixelWidth)
		throws IOException
	{
		MandelbrotFragment m = new MandelbrotFragment();
		m.width = pixelWidth;
		m.height = pixelWidth;
		m.readFromReal(in);
		return m;
	}

	void readFromReal(InputStream in)
		throws IOException
	{
		members = new byte[height][width];
		for (int i = 0; i < height; i++) {
			byte [] bb = members[i];
			int len = 0;
			while (len < width) {
				int nread = in.read(bb, len, width-len);
				if (nread == -1) {
					throw new IOException("early eof");
				}
				len += nread;
			}
		}
	}
}
