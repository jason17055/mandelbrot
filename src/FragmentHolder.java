import java.lang.ref.*;
import java.util.HashMap;
import javax.swing.*;
import org.apfloat.*;

class FragmentHolder
{
	FragmentAddress address;

	Apfloat originX; //min X (real component)
	Apfloat originY; //min Y (imaginary component)
	Apfloat size;

	HolderState state = HolderState.INIT;
	MandelbrotFragment m;

	// valid only if state == ZOOM
	FragmentHolder parent;
	int parentX;
	int parentY;

	static final int FSIZE = 128;
	static final int BRANCHING_FACTOR = 2;

	@SuppressWarnings("unchecked")
	WeakReference<FragmentHolder> children[] = new WeakReference[BRANCHING_FACTOR*BRANCHING_FACTOR];

	static FragmentHolder rootFragment()
	{
		FragmentHolder f = new FragmentHolder(
			new Apfloat(-4),
			new Apfloat(-4),
			new Apfloat(8)
			);
		f.address = new FragmentAddress(0,0,0);
		return f;
	}

	protected FragmentHolder(Apfloat originX, Apfloat originY, Apfloat size)
	{
		this.originX = originX;
		this.originY = originY;
		this.size = size;

		new FragmentGen(this).execute();
	}

	boolean hasNeighbor(int dx, int dy)
	{
		if (dx == 0 && dy == 0) {
			return true;
		}
		else if (parent != null) {

			dx += parentX;
			dy += parentY;
			int px = dx >= 0 ? dx / BRANCHING_FACTOR :
				(dx-1) / BRANCHING_FACTOR;
			int py = dy >= 0 ? dy / BRANCHING_FACTOR :
				(dy-1) / BRANCHING_FACTOR;
			return parent.hasNeighbor(px, py);
		}
		else {
			// root fragment has no neighbors
			return false;
		}
	}

	FragmentHolder getNeighbor(int dx, int dy)
	{
		assert !(dx == 0 && dy == 0);

		if (parent != null) {
			return parent.getChild(parentX+dx, parentY+dy);
		}
		else {
			// root fragment has no neighbors
			return null;
		}
	}


	FragmentHolder getChild(int cx, int cy)
	{
		int px = cx >= 0 ? cx / BRANCHING_FACTOR :
			(cx-1) / BRANCHING_FACTOR;
		int mx = cx - (px*BRANCHING_FACTOR);

		int py = cy >= 0 ? cy / BRANCHING_FACTOR :
			(cy-1) / BRANCHING_FACTOR;
		int my = cy - (py*BRANCHING_FACTOR);

		if (px == 0 && py == 0) {
			return this.getChildReal(mx, my);
		}
		else if (parent != null) {
			return getNeighbor(px, py).getChildReal(mx, my);
		}
		else {
			// root fragment has no neighbor children
			return null;
		}
	}

	FragmentHolder getChildReal(int mx, int my)
	{
		assert mx >= 0 && mx < BRANCHING_FACTOR;
		assert my >= 0 && my < BRANCHING_FACTOR;

		int i = my*BRANCHING_FACTOR+mx;
		if (children[i] != null) {
			FragmentHolder f = children[i].get();
			if (f != null) {
				return f;
			}
		}

		FragmentHolder f = createInnerQuadrant(mx, my);
		f.address = new FragmentAddress(
			this.address.depth+1,
			this.address.x * BRANCHING_FACTOR + mx,
			this.address.y * BRANCHING_FACTOR + my
			);
		children[i] = new WeakReference<FragmentHolder>(f);
		return f;
	}

	FragmentHolder createInnerQuadrant(int ax, int ay)
	{
		Apfloat hsize = this.size.divide(new Apfloat(BRANCHING_FACTOR));

		FragmentHolder h = new FragmentHolder(
			originX.add(hsize.multiply(new Apfloat(ax))),
			originY.add(hsize.multiply(new Apfloat(ay))),
			hsize);
		h.state = HolderState.ZOOM;
		h.setParent(this, ax, ay);
		return h;
	}

	void setParent(FragmentHolder h, int px, int py)
	{
		assert h != null;
		assert px >= 0 && px < BRANCHING_FACTOR;
		assert py >= 0 && py < BRANCHING_FACTOR;

		this.parent = h;
		this.parentX = px;
		this.parentY = py;
	}

	int get(int x, int y)
	{
		switch (state) {
		case READY: return colorOf(m.members[y][x]);
		case ZOOM:
			int c = parent.get((x+FSIZE*parentX)/2,
				(y+FSIZE*parentY)/2);
			return c == 0 ? 0x666666 :
				0xeeeeee;
		default:
			return 0xeeeeee;
		}
	}

	void fireReady()
	{
		if (listener != null) {
			listener.fragmentReady(this);
		}
	}

	void removeListener(Listener l)
	{
		assert this.listener == l;
		this.listener = null;
	}

	void addListener(Listener l)
	{
		assert this.listener == null || this.listener == l;
		this.listener = l;
	}

	public interface Listener
	{
		public void fragmentReady(FragmentHolder f);
	}
	Listener listener;

	static enum HolderState
	{
		INIT,
		ZOOM,
		READY;
	}

	static class FragmentGen extends SwingWorker<MandelbrotFragment,Object>
	{
		FragmentHolder h;
		MandelbrotFragment f;

		FragmentGen(FragmentHolder h)
		{
			this.h = h;
		}

		@Override
		public MandelbrotFragment doInBackground()
		{
			f = new MandelbrotFragment(
				h.originX,
				h.originY,
				h.originX.add(h.size),
				h.originY.add(h.size),
				FSIZE, FSIZE);
			return f;
		}

		@Override
		protected void done() {
			h.m = f;
			h.state = HolderState.READY;
			h.fireReady();
		}
	}

	static int colorOf(byte b)
	{
		if (b == 0) { return 0; }
		int x = b <= 30 ? 255 - 8*(b-1) : 0;
		return 0xff0000 | (x << 8) | x;
	}

}
