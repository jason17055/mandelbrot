import org.apfloat.Apcomplex;

class MandelbrotCompute
{
	static final int ESCAPE_THRESHOLD = 100;
	static final byte YES = MandelbrotFragment.YES;
	static final byte NO = MandelbrotFragment.NO;

	public byte check(Apcomplex c)
	{
		double x0 = c.real().doubleValue();
		double y0 = c.imag().doubleValue();

		if (x0 > 1.0) { return NO; }

		double x = 0.0;
		double y = 0.0;

		for (int i = 0; i < ESCAPE_THRESHOLD; i++) {
			double xtmp = x*x - y*y + x0;
			y = 2*x*y + y0;
			x = xtmp;

			if (x > 2.0 || y > 2.0) {
				return (byte)(i % MandelbrotFragment.BANDS);
			}
		}
		return YES;
	}
}
