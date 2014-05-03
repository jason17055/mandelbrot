class FragmentParentIfc
{
	MandelbrotFragment f;
	int offsetX;
	int offsetY;

	static final byte UNKNOWN = MandelbrotFragment.UNKNOWN;

	FragmentParentIfc(MandelbrotFragment f, int offsetX, int offsetY)
	{
		this.f = f;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	public byte get(int x, int y)
	{
		x += offsetX;
		y += offsetY;

		if (x >= 0 && x < f.width &&
			y >= 0 && y < f.height)
		{
			return f.members[y][x];
		}
		else {
			return UNKNOWN;
		}
	}
}
