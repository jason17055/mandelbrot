public class FragmentAddress
{
	int depth;
	long x;
	long y;

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof FragmentAddress) {
			FragmentAddress rhs = (FragmentAddress) other;
			return this.depth == rhs.depth &&
				this.x == rhs.x &&
				this.y == rhs.y;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return (depth * 33 + (int)y) * 33 + (int)x;
	}
}
