import java.io.*;
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
			new Apfloat(8),
			new FragmentAddress(0,0,0)
			);
		return f;
	}

	protected FragmentHolder(Apfloat originX, Apfloat originY, Apfloat size, FragmentAddress addr)
	{
		this.originX = originX;
		this.originY = originY;
		this.size = size;
		this.address = addr;

		this.generator = new FragmentGen(this);
		this.generator.execute();
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
		children[i] = new WeakReference<FragmentHolder>(f);
		return f;
	}

	FragmentHolder createInnerQuadrant(int ax, int ay)
	{
		Apfloat hsize = this.size.divide(new Apfloat(BRANCHING_FACTOR));

		FragmentHolder h = new FragmentHolder(
			originX.add(hsize.multiply(new Apfloat(ax))),
			originY.add(hsize.multiply(new Apfloat(ay))),
			hsize,
			new FragmentAddress(
				this.address.depth+1,
				this.address.x * BRANCHING_FACTOR + ax,
				this.address.y * BRANCHING_FACTOR + ay
			)
			);
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

	int getPixel(int x, int y)
	{
		switch (state) {
		case READY: return colorOf(m.members[y][x]);
		case ZOOM:
			int c = parent.getPixel((x+FSIZE*parentX)/2,
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
		assert this.listener == null;
		this.listener = l;
	}

	public interface Listener
	{
		public void fragmentReady(FragmentHolder f);
	}
	Listener listener;
	FragmentGen generator;

	static enum HolderState
	{
		INIT,
		ZOOM,
		READY;
	}

	FragmentParentIfc makeParentIfc()
		throws Exception
	{
		assert parent != null;

		// blocks if parent hasn't finished generating
		MandelbrotFragment pF = parent.generator.get();

		return new FragmentParentIfc(
			pF,
			parentX * FSIZE/2,
			parentY * FSIZE/2
			);
	}

	static class FragmentGen extends SwingWorker<MandelbrotFragment,Object>
	{
		FragmentHolder h;
		MandelbrotFragment f;

		FragmentGen(FragmentHolder h)
		{
			this.h = h;
		}

		boolean tryLoad(File fl)
		{
			try {

			InputStream in = new BufferedInputStream(
				new FileInputStream(fl)
				);
			f = MandelbrotFragment.readFrom(in, FSIZE);
			in.close();

			return true;

			}
			catch (IOException e) {
				System.err.println("Warning: error loading fragment "+fl + " ("+e.getMessage()+")");
				return false;
			}
		}

		@Override
		public MandelbrotFragment doInBackground()
			throws Exception
		{
			File fl = getFragmentFile(h.address);
			if (fl.exists()) {
				if (tryLoad(fl)) {
					return f;
				}
			}

			if (h.parent != null) {
				f = new MandelbrotFragment(
					h.originX,
					h.originY,
					h.size,
					FSIZE);
				f.generateFromParent(h.makeParentIfc());
			}
			else {
				f = new MandelbrotFragment(
					h.originX,
					h.originY,
					h.size,
					FSIZE);
				f.generate();
			}
			return f;
		}

		@Override
		protected void done() {

			// detect uncaught exception
			try {
				get();
			}
			catch (Exception e) {
				throw new Error("Unexpected: " + e, e);
			}

			assert f != null;
			assert f.members != null;

			h.m = f;
			h.state = HolderState.READY;
			h.fireReady();

			new FragmentSave(h).execute();
		}
	}

	static File cacheDir = new File("cache");
	static File getFragmentFile(FragmentAddress a)
	{
		assert a != null;

		return new File(cacheDir,
			String.format("%d_%d,%d.dat",
			a.depth,
			a.x,
			a.y)
			);
	}

	static class FragmentSave extends SwingWorker<Void,Void>
	{
		FragmentHolder h;

		FragmentSave(FragmentHolder h)
		{
			this.h = h;
		}

		@Override
		public Void doInBackground()
			throws IOException
		{
			OutputStream out = new BufferedOutputStream(
				new FileOutputStream(
					getFragmentFile(h.address)
				));
			h.m.writeTo(out);
			out.close();

			return null;
		}
	}

	static int colorOf(byte x)
	{
		if (x == MandelbrotFragment.PROBABLY) {
			return 0x777777;
		}
		else if (x == MandelbrotFragment.MIXED) {
			return 0xffaaff;
		}
		if (x < 0) { return 0; }
		else { return 0xffffff; }
		//return Roygbiv.colors[x % Roygbiv.colors.length];
	}

}
