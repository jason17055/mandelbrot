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
	static final byte MIXED = 64;
	static final byte UNKNOWN = 65;

	static final int BANDS = 64;

	static final Apfloat ONE = new Apfloat(1);
	static final Apfloat TWO = new Apfloat(2);
	static final Apfloat MINUS_TWO_PT_FIVE = new Apfloat(-2.5);

	static byte known(byte b)
	{
		if (b == YES) { return b; }
		else if (b >= 0 && b < 64) { return NO; }
		else { return UNKNOWN; }
	}

	static MandelbrotCompute compute = new MandelbrotCompute();

	FixedPrecisionApcomplexHelper precisionHelper =
			new FixedPrecisionApcomplexHelper(32);

	protected MandelbrotFragment()
	{
	}

	public MandelbrotFragment(Apfloat re0, Apfloat im0, Apfloat size, int pixelWidth)
	{
		assert width == height;

		this.originX = re0;
		this.originY = im0;
		this.scale = size.divide(new Apfloat(pixelWidth));
		this.width = pixelWidth;
		this.height = pixelWidth;
	}

	void generateFromParent(FragmentParentIfc p)
	{
		this.members = new byte[height][width];

		for (int i = 0; i < height; i+=2) {
			for (int j = 0; j < width; j+=2) {

				// check neighbor pixels from parent
				byte b1 = known(p.get(j/2 - 1, i/2 + 1));
				byte b2 = known(p.get(j/2,     i/2 + 1));
				byte b3 = known(p.get(j/2 + 1, i/2 + 1));
				byte b4 = known(p.get(j/2 - 1, i/2));
				byte b5 = known(p.get(j/2, i/2));
				byte b6 = known(p.get(j/2 + 1, i/2));
				byte b8 = known(p.get(j/2,     i/2 - 1));
				byte b9 = known(p.get(j/2 + 1, i/2 - 1));

				// copy one pixel directly from parent
				if (b5 != UNKNOWN) {
					members[i][j] = b5;
				}
				else {
					generatePixel(j,i);
				}

				if (b5 != UNKNOWN &&
					b5 == b8 && b5 == b9 && b5 == b6 &&
					b5 == b2 && b5 == b3)
				{
					members[i][j+1] = members[i][j];
				}
				else {
					generatePixel(j+1,i);
				}

				if (b5 != UNKNOWN &&
					b5 == b4 && b5 == b6 && b5 == b1 &&
					b5 == b2 && b5 == b3)
				{
					members[i+1][j] = members[i][j];
				}
				else {
					generatePixel(j, i+1);
				}

				if (b5 != UNKNOWN &&
					b5 == b6 && b5 == b2 && b5 == b3)
				{
					members[i+1][j+1] = members[i][j];
				}
				else {
					generatePixel(j+1, i+1);
				}
			}
		}

		makeProbably();
		makeProbably();
		makeMixed();
		makeMixed();
	}

	void generate()
	{
		this.members = new byte[height][width];

		byte lastRow = generateRow(height-1, 0, width);
		byte lastCol = generateColumn(width-1, 0, height-1);
		generateBox(0, 0, width-1, height-1, lastRow, lastCol);

		makeProbably();
		makeProbably();
		makeMixed();
		makeMixed();
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

	void generatePixel(int x, int y)
	{
		Apfloat fx = originX.add(scale.multiply(new Apfloat(x)));
		Apfloat fy = originY.add(scale.multiply(new Apfloat(y)));

		members[y][x] = compute.check(
			new Apcomplex(fx, fy)
			);
	}

	byte generateRow(int y, int x0, int x1)
	{
		byte sum = 0;

		Apfloat fy = originY.add(scale.multiply(new Apfloat(y)));

		for (int x = x0; x < x1; x++) {
			Apfloat fx = originX.add(scale.multiply(new Apfloat(x)));
			members[y][x] = compute.check(
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
			members[y][x] = compute.check(
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

	void makeMixed()
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
			members[X/width][X%width] = MIXED;
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
