import javax.swing.*;
import org.apfloat.*;

class FragmentHolder
{
	Apcomplex origin;
	Apfloat size;

	HolderState state = HolderState.INIT;
	MandelbrotFragment m;

	// valid only if state == ZOOM
	FragmentHolder parent;
	int parentX;
	int parentY;

	static final int FSIZE = 128;

	FragmentHolder(Apcomplex origin, Apfloat size)
	{
		this.origin = origin;
		this.size = size;

		new FragmentGen(this).execute();
	}

	FragmentHolder neighbor(int dx, int dy)
	{
		Apfloat fx = size.multiply(new Apfloat(dx));
		Apfloat fy = size.multiply(new Apfloat(dy));

		return new FragmentHolder(
			new Apcomplex(
				origin.real().add(fx),
				origin.imag().add(fy)
				),
			size);
	}

	FragmentHolder innerQuadrant(int ax, int ay)
	{
		Apfloat hsize = this.size.divide(new Apfloat(2));
		Apfloat re0 = this.origin.real();
		Apfloat im0 = this.origin.imag();

		FragmentHolder h = new FragmentHolder(
			new Apcomplex(
				origin.real().add(hsize.multiply(new Apfloat(ax))),
				origin.imag().add(hsize.multiply(new Apfloat(ay)))
				),
			hsize);
		h.state = HolderState.ZOOM;
		h.setParent(this, ax, ay);
		return h;
	}

	void setParent(FragmentHolder h, int px, int py)
	{
		this.parent = h;
		this.parentX = px;
		this.parentY = py;
	}

	int get(int x, int y)
	{
		switch (state) {
		case READY: return colorOf(m.members[y][x]);
		case ZOOM:
			return parent.get((x+FSIZE*parentX)/2,
				(y+FSIZE*parentY)/2);
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
				h.origin.real(),
				h.origin.imag(),
				h.origin.real().add(h.size),
				h.origin.imag().add(h.size),
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
